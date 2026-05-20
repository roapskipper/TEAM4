package com.team4.service;

import com.team4.dao.AuctionDAO;
import com.team4.dao.ItemDAO;
import com.team4.dao.UserDAO;
import com.team4.dao.impl.UserDAOImpl;
import com.team4.db.DatabaseManager;
import com.team4.model.Auction;
import com.team4.model.Item;
import com.team4.model.User;
import com.team4.util.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class AuctionService {
    private static final Logger logger = LoggerFactory.getLogger(AuctionService.class);

    private final AuctionDAO auctionDAO;
    private final ItemDAO itemDAO;
    private final UserDAO userDAO;

    public AuctionService(AuctionDAO auctionDAO, ItemDAO itemDAO) {
        this(auctionDAO, itemDAO, new UserDAOImpl());
    }

    public AuctionService(AuctionDAO auctionDAO, ItemDAO itemDAO, UserDAO userDAO) {
        this.auctionDAO = auctionDAO;
        this.itemDAO = itemDAO;
        this.userDAO = userDAO;
    }

    public Auction createAuction(String itemId, String sellerId, BigDecimal startingPrice, BigDecimal bidIncrement, LocalDateTime endTime) {
        logger.info("Creating auction itemId={} sellerId={} startingPrice={} bidIncrement={} endTime={}",
                itemId, sellerId, startingPrice, bidIncrement, endTime);
        Item item = itemDAO.findById(itemId);
        if (item == null) {
            throw new BusinessException("Item does not exist");
        }
        if (!item.getOwnerId().equals(sellerId)) {
            throw new BusinessException("Seller does not own this item");
        }
        Auction auction = new Auction(itemId, sellerId, startingPrice, bidIncrement, endTime);
        auctionDAO.insert(auction);
        logger.info("Auction created id={} itemId={} sellerId={} status={}",
                auction.getId(), itemId, sellerId, auction.getStatus());
        return auction;
    }

    public Auction getAuctionById(String auctionId) {
        Auction auction = auctionDAO.findById(auctionId);
        if (auction == null) {
            throw new BusinessException("Auction does not exist");
        }
        return auction;
    }

    public List<Auction> getAuctionsByStatus(Auction.AuctionStatus status) {
        return auctionDAO.findByStatus(status);
    }

    public Auction approveAuction(String auctionId) {
        Auction auction = getAuctionById(auctionId);
        if (auction.getStatus() != Auction.AuctionStatus.PENDING) {
            throw new BusinessException("Only pending auctions can be approved");
        }
        auction.approve();
        auctionDAO.updateStatus(auctionId, Auction.AuctionStatus.RUNNING);
        logger.info("Updated auction status id={} from {} to {}",
                auctionId, Auction.AuctionStatus.PENDING, Auction.AuctionStatus.RUNNING);
        return auction;
    }

    public Auction cancelAuction(String auctionId) {
        Auction auction = getAuctionById(auctionId);
        if (auction.getStatus() == Auction.AuctionStatus.PAID) {
            throw new BusinessException("Cannot cancel a paid auction");
        }

        if (auction.getCurrentHighestBidderId() != null) {
            cancelWithRefund(auctionId);
            auction.cancel();
            return auction;
        }

        Auction.AuctionStatus oldStatus = auction.getStatus();
        auction.cancel();
        auctionDAO.updateStatus(auctionId, Auction.AuctionStatus.CANCELLED);
        logger.info("Updated auction status id={} from {} to {}", auctionId, oldStatus, Auction.AuctionStatus.CANCELLED);
        return auction;
    }

    public void rejectAuction(String auctionId) {
        Auction auction = getAuctionById(auctionId);
        if (auction.getStatus() != Auction.AuctionStatus.PENDING) {
            throw new BusinessException("Only pending auctions can be rejected");
        }
        auction.cancel();
        auctionDAO.updateStatus(auctionId, Auction.AuctionStatus.CANCELLED);
        logger.info("Updated auction status id={} from {} to {}",
                auctionId, Auction.AuctionStatus.PENDING, Auction.AuctionStatus.CANCELLED);
    }

    public void closeExpiredAuctions() {
        List<Auction> runningAuctions = auctionDAO.findByStatus(Auction.AuctionStatus.RUNNING);
        LocalDateTime now = LocalDateTime.now();
        int closedCount = 0;

        for (Auction auction : runningAuctions) {
            if (auction.getEndTime().isAfter(now)) {
                continue;
            }

            if (auction.getCurrentHighestBidderId() == null) {
                auction.close();
                if (auctionDAO.updateStatus(auction.getId(), Auction.AuctionStatus.FINISHED)) {
                    closedCount++;
                }
                continue;
            }

            settleFinishedAuction(auction.getId());
            closedCount++;
        }

        if (closedCount > 0) {
            logger.info("Closed expired auctions count={}", closedCount);
        }
    }

    public Auction markPaid(String auctionId) {
        Auction auction = getAuctionById(auctionId);
        if (auction.getStatus() != Auction.AuctionStatus.FINISHED) {
            throw new BusinessException("Only finished auctions can be marked as paid");
        }
        auction.markPaid();
        auctionDAO.updateStatus(auctionId, Auction.AuctionStatus.PAID);
        logger.info("Updated auction status id={} from {} to {}",
                auctionId, Auction.AuctionStatus.FINISHED, Auction.AuctionStatus.PAID);
        return auction;
    }

    public Auction cancelDueToInsufficientFunds(String auctionId) {
        Auction auction = getAuctionById(auctionId);
        if (auction.getStatus() != Auction.AuctionStatus.FINISHED) {
            throw new BusinessException("Only finished auctions can be cancelled");
        }
        auction.cancel();
        auctionDAO.updateStatus(auctionId, Auction.AuctionStatus.CANCELLED);
        return auction;
    }

    private void settleFinishedAuction(String auctionId) {
        try (Connection conn = DatabaseManager.getConnection()) {
            try {
                DatabaseManager.beginTransaction(conn);

                Auction auction = auctionDAO.findById(conn, auctionId);
                if (auction == null || auction.getStatus() != Auction.AuctionStatus.RUNNING || auction.getEndTime().isAfter(LocalDateTime.now())) {
                    DatabaseManager.commitTransaction(conn);
                    return;
                }

                String winnerId = auction.getCurrentHighestBidderId();
                if (winnerId == null) {
                    if (!auctionDAO.updateStatus(conn, auctionId, Auction.AuctionStatus.FINISHED)) {
                        throw new BusinessException("System error while closing auction.");
                    }
                    DatabaseManager.commitTransaction(conn);
                    return;
                }

                User seller = userDAO.findById(conn, auction.getSellerId());
                if (seller == null) {
                    throw new BusinessException("Seller does not exist");
                }
                seller.deposit(auction.getCurrentPrice());
                if (!userDAO.updateBalance(conn, seller.getId(), seller.getBalance())) {
                    throw new BusinessException("System error while crediting seller balance.");
                }
                if (!itemDAO.updateOwner(conn, auction.getItemId(), winnerId)) {
                    throw new BusinessException("System error while transferring item ownership.");
                }
                if (!auctionDAO.updateStatus(conn, auctionId, Auction.AuctionStatus.FINISHED)) {
                    throw new BusinessException("System error while closing auction.");
                }

                DatabaseManager.commitTransaction(conn);
                logger.info("Auction settled auctionId={} winnerId={} sellerId={} amount={}",
                        auctionId, winnerId, auction.getSellerId(), auction.getCurrentPrice());
            } catch (Exception e) {
                DatabaseManager.rollbackTransaction(conn);
                throw e;
            }
        } catch (SQLException e) {
            throw new BusinessException("System error while settling auction: " + e.getMessage());
        }
    }

    private void cancelWithRefund(String auctionId) {
        try (Connection conn = DatabaseManager.getConnection()) {
            try {
                DatabaseManager.beginTransaction(conn);
                Auction auction = auctionDAO.findById(conn, auctionId);
                if (auction == null || auction.getCurrentHighestBidderId() == null) {
                    DatabaseManager.commitTransaction(conn);
                    return;
                }

                User leader = userDAO.findById(conn, auction.getCurrentHighestBidderId());
                if (leader != null) {
                    leader.deposit(auction.getCurrentPrice());
                    if (!userDAO.updateBalance(conn, leader.getId(), leader.getBalance())) {
                        throw new BusinessException("System error while refunding bidder.");
                    }
                }
                if (!auctionDAO.updateStatus(conn, auctionId, Auction.AuctionStatus.CANCELLED)) {
                    throw new BusinessException("System error while cancelling auction.");
                }
                DatabaseManager.commitTransaction(conn);
            } catch (Exception e) {
                DatabaseManager.rollbackTransaction(conn);
                throw e;
            }
        } catch (SQLException e) {
            throw new BusinessException("System error while cancelling auction: " + e.getMessage());
        }
    }
}
