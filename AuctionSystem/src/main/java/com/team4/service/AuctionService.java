package com.team4.service;

import com.team4.dao.AuctionDAO;
import com.team4.dao.ItemDAO;
import com.team4.dto.auction.AuctionResponseDTO;
import com.team4.dto.auction.CreateAuctionRequestDTO;
import com.team4.mapper.AuctionMapper;
import com.team4.model.Auction;
import com.team4.model.Item;
import com.team4.util.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.team4.db.DatabaseManager;
import java.sql.Connection;
import java.sql.SQLException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.team4.dao.UserDAO;
import com.team4.model.User;

/**
 * Quản lý vòng đời của các phiên đấu giá.
 */
public class AuctionService {
    private final AuctionDAO auctionDAO;
    private final ItemDAO itemDAO;
    private final UserDAO userDAO;
    private static final Logger logger = LoggerFactory.getLogger(AuctionService.class);

    public AuctionService(AuctionDAO auctionDAO, ItemDAO itemDAO) {
        this(auctionDAO, itemDAO, new com.team4.dao.impl.UserDAOImpl());
    }

    public AuctionService(AuctionDAO auctionDAO, ItemDAO itemDAO, UserDAO userDAO) {
        this.auctionDAO = auctionDAO;
        this.itemDAO = itemDAO;
        this.userDAO = userDAO;
    }

    /**
     * Tạo phiên đấu giá mới: kiểm tra quyền sở hữu item, trạng thái ban đầu là PENDING.
     */
    public AuctionResponseDTO createAuction(CreateAuctionRequestDTO requestDTO) {
        String itemId            = requestDTO.getItemId();
        String sellerId          = requestDTO.getSellerId();
        BigDecimal startingPrice = requestDTO.getStartingPrice();
        BigDecimal bidIncrement  = requestDTO.getBidIncrement();
        LocalDateTime endTime    = requestDTO.getEndTime();

        logger.info("Creating auction: itemId={}, sellerId={}, startingPrice={}, bidIncrement={}, endTime={}",
                itemId, sellerId, startingPrice, bidIncrement, endTime);

        Item item = itemDAO.findById(itemId);
        if (item == null) {
            logger.warn("Auction creation failed: item does not exist. itemId={}", itemId);
            throw new BusinessException("Item does not exist");
        }
        if (!item.getOwnerId().equals(sellerId)) {
            logger.warn("Auction creation failed: seller does not own the item. itemId={}, sellerId={}, ownerId={}",
                    itemId, sellerId, item.getOwnerId());
            throw new BusinessException("Seller does not own this item");
        }
        if (auctionDAO.findByItemId(itemId) != null) {
            logger.warn("Auction creation failed: item already has an auction. itemId={}", itemId);
            throw new BusinessException("This item already has an auction");
        }

        Auction auction = new Auction(itemId, sellerId, startingPrice, bidIncrement, endTime);
        if (!auctionDAO.insert(auction)) {
            logger.error("Failed to save auction to database: itemId={}", itemId);
            throw new BusinessException("Unable to create auction session.");
        }

        logger.info("Auction created successfully: auctionId={}, status={}", auction.getId(), auction.getStatus());
        return AuctionMapper.toAuctionResponseDTO(auction);
    }

    /**
     * Lấy thông tin chi tiết phiên đấu giá dưới dạng DTO.
     */
    public AuctionResponseDTO getAuctionById(String auctionId) {
        logger.debug("Loading auction details: auctionId={}", auctionId);
        Auction auction = getRawAuctionById(auctionId);
        return AuctionMapper.toAuctionResponseDTO(auction);
    }

    /**
     * Lấy model Auction gốc để sử dụng nội bộ trong server.
     */
    public Auction getRawAuctionById(String auctionId) {
        Auction auction = auctionDAO.findById(auctionId);
        if (auction == null) {
            logger.warn("Auction not found: auctionId={}", auctionId);
            throw new BusinessException("Auction session does not exist");
        }
        return auction;
    }

    /**
     * Lấy danh sách các phiên đấu giá theo trạng thái.
     */
    public List<AuctionResponseDTO> getAuctionsByStatus(Auction.AuctionStatus status) {
        logger.debug("Retrieving auctions by status: status={}", status);
        return auctionDAO.findByStatus(status).stream()
                .map(AuctionMapper::toAuctionResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Admin duyệt phiên: chuyển trạng thái từ PENDING sang RUNNING.
     */
    public AuctionResponseDTO approveAuction(String auctionId) {
        logger.info("Approving auction: auctionId={}", auctionId);
        Auction auction = getRawAuctionById(auctionId);

        if (auction.getStatus() != Auction.AuctionStatus.PENDING) {
            logger.warn("Auction approval failed: invalid status. auctionId={}, currentStatus={}",
                    auctionId, auction.getStatus());
            throw new BusinessException("Only pending auctions can be approved");
        }

        auction.approve();
        if (!auctionDAO.updateStatus(auctionId, Auction.AuctionStatus.RUNNING)) {
            logger.error("Failed to update auction status to RUNNING in database: auctionId={}", auctionId);
            throw new BusinessException("Failed to approve auction due to system error.");
        }

        logger.info("Auction approved successfully: auctionId={}", auctionId);
        return AuctionMapper.toAuctionResponseDTO(auction);
    }

    /**
     * Hủy phiên đấu giá (Chỉ dành cho Super Admin).
     */
    public AuctionResponseDTO cancelAuction(String auctionId) {
        logger.info("Cancelling auction: auctionId={}", auctionId);
        Auction auction = getRawAuctionById(auctionId);

        if (auction.getStatus() == Auction.AuctionStatus.PAID) {
            logger.warn("Auction cancellation failed: already paid. auctionId={}", auctionId);
            throw new BusinessException("Cannot cancel a paid auction");
        }

        Auction.AuctionStatus oldStatus = auction.getStatus();
        auction.cancel();
        if (!auctionDAO.updateStatus(auctionId, Auction.AuctionStatus.CANCELLED)) {
            logger.error("Failed to update auction status to CANCELLED in database: auctionId={}", auctionId);
            throw new BusinessException("Failed to cancel auction due to system error.");
        }

        logger.info("Auction status updated from {} to CANCELLED: auctionId={}", oldStatus, auctionId);
        return AuctionMapper.toAuctionResponseDTO(auction);
    }

    /**
     * Từ chối duyệt một phiên đấu giá đang ở trạng thái PENDING.
     */
    public void rejectAuction(String auctionId) {
        logger.info("Rejecting auction: auctionId={}", auctionId);
        Auction auction = getRawAuctionById(auctionId);

        if (auction.getStatus() != Auction.AuctionStatus.PENDING) {
            logger.warn("Auction rejection failed: invalid status. auctionId={}, currentStatus={}",
                    auctionId, auction.getStatus());
            throw new BusinessException("Only pending auctions can be rejected");
        }

        auction.cancel();
        if (!auctionDAO.updateStatus(auctionId, Auction.AuctionStatus.CANCELLED)) {
            logger.error("Failed to update auction status to CANCELLED (rejected) in database: auctionId={}", auctionId);
            throw new BusinessException("Failed to reject auction due to system error.");
        }
        logger.info("Auction rejected successfully: auctionId={}", auctionId);
    }

    /**
     * Tự động đóng các phiên đã quá thời gian kết thúc.
     */
    public void closeExpiredAuctions() {
        logger.info("Checking for expired auctions to close.");
        List<Auction> activeAuctions = auctionDAO.findByStatus(Auction.AuctionStatus.RUNNING);
        LocalDateTime now = LocalDateTime.now();
        int closedCount = 0;

        for (Auction auction : activeAuctions) {
            if (!auction.getEndTime().isAfter(now)) {
                try {
                    auction.close();
                    if (auctionDAO.updateStatus(auction.getId(), Auction.AuctionStatus.FINISHED)) {
                        closedCount++;
                        logger.info("Auction closed: auctionId={}, endTime={}, closedAt={}",
                                auction.getId(), auction.getEndTime(), now);

                        // Tự động xử lý chuyển giao item và thanh toán tiền đấu giá
                        String winnerId = auction.getCurrentHighestBidderId();
                        if (winnerId != null && !winnerId.isBlank()) {
                            BigDecimal finalPrice = auction.getCurrentPrice();
                            User winner = userDAO.findById(winnerId);
                            if (winner != null && winner.hasEnoughBalance(finalPrice)) {
                                logger.info("Winner has sufficient balance. Processing automated payment: auctionId={}, winnerId={}",
                                        auction.getId(), winnerId);
                                markPaid(auction.getId());
                            } else {
                                logger.info("Winner has insufficient balance or does not exist. Cancelling auction: auctionId={}, winnerId={}",
                                        auction.getId(), winnerId);
                                cancelDueToInsufficientFunds(auction.getId());
                            }
                        }
                    } else {
                        logger.error("Failed to close expired auction in database: auctionId={}", auction.getId());
                    }
                } catch (Exception e) {
                    logger.error("Error processing expired auction closure: auctionId={}, error={}", auction.getId(), e.getMessage());
                }
            }
        }
        logger.info("Expired auction closure complete. Checked: {}, Closed: {}", activeAuctions.size(), closedCount);
    }

    /**
     * Đánh dấu phiên đã thanh toán xong.
     */
    public AuctionResponseDTO markPaid(String auctionId) {
        logger.info("Marking auction as paid: auctionId={}", auctionId);
        
        try (Connection conn = DatabaseManager.getConnection()) {
            DatabaseManager.beginTransaction(conn);
            try {
                Auction auction = auctionDAO.findById(conn, auctionId);
                if (auction == null) {
                    logger.warn("Auction not found in transaction: auctionId={}", auctionId);
                    throw new BusinessException("Auction session does not exist");
                }

                if (auction.getStatus() != Auction.AuctionStatus.FINISHED) {
                    logger.warn("Mark paid failed: invalid status. auctionId={}, currentStatus={}",
                            auctionId, auction.getStatus());
                    throw new BusinessException("Only finished auctions can be marked as paid");
                }

                auction.markPaid();
                if (!auctionDAO.updateStatus(conn, auctionId, Auction.AuctionStatus.PAID)) {
                    logger.error("Failed to update auction status to PAID in database transaction: auctionId={}", auctionId);
                    throw new BusinessException("Failed to mark auction as paid due to system error.");
                }

                String winnerId = auction.getCurrentHighestBidderId();
                if (winnerId != null && !winnerId.isBlank()) {
                    BigDecimal finalPrice = auction.getCurrentPrice();

                    // Deduct from bidder balance
                    User bidder = userDAO.findById(conn, winnerId);
                    if (bidder == null) {
                        logger.warn("Winner not found: winnerId={}", winnerId);
                        throw new BusinessException("Winner does not exist.");
                    }
                    if (!bidder.withdraw(finalPrice)) {
                        logger.warn("Winner has insufficient funds: winnerId={}, price={}", winnerId, finalPrice);
                        throw new BusinessException("Winner has insufficient balance for auction payment.");
                    }
                    if (!userDAO.updateBalance(conn, winnerId, bidder.getBalance())) {
                        logger.error("Failed to update winner balance: winnerId={}", winnerId);
                        throw new BusinessException("Failed to deduct winner balance.");
                    }

                    // Deposit to seller balance
                    String sellerId = auction.getSellerId();
                    User seller = userDAO.findById(conn, sellerId);
                    if (seller == null) {
                        logger.warn("Seller not found: sellerId={}", sellerId);
                        throw new BusinessException("Seller does not exist.");
                    }
                    seller.deposit(finalPrice);
                    if (!userDAO.updateBalance(conn, sellerId, seller.getBalance())) {
                        logger.error("Failed to update seller balance: sellerId={}", sellerId);
                        throw new BusinessException("Failed to credit seller balance.");
                    }

                    // Transfer item ownership
                    if (!itemDAO.updateOwner(conn, auction.getItemId(), winnerId)) {
                        logger.error("Failed to update item owner to winner: itemId={}, winnerId={}",
                                auction.getItemId(), winnerId);
                        throw new BusinessException("Failed to transfer item ownership to the winner.");
                    }
                    logger.info("Item ownership and funds successfully transferred: itemId={}, winnerId={}, sellerId={}, price={}",
                            auction.getItemId(), winnerId, sellerId, finalPrice);
                }

                DatabaseManager.commitTransaction(conn);
                logger.info("Auction marked as paid successfully: auctionId={}", auctionId);
                return AuctionMapper.toAuctionResponseDTO(auction);
            } catch (Exception e) {
                DatabaseManager.rollbackTransaction(conn);
                logger.error("Transaction failed (rolled back) during markPaid: {}", e.getMessage());
                throw (e instanceof BusinessException) ? (BusinessException) e : new BusinessException("Failed to mark auction as paid: " + e.getMessage());
            }
        } catch (SQLException e) {
            logger.error("Database connection error during markPaid: {}", e.getMessage());
            throw new BusinessException("System error during payment process.");
        }
    }

    /**
     * Hủy phiên đấu giá do người thắng không đủ số dư.
     */
    public AuctionResponseDTO cancelDueToInsufficientFunds(String auctionId) {
        logger.info("Cancelling auction due to insufficient funds: auctionId={}", auctionId);
        Auction auction = getRawAuctionById(auctionId);

        if (auction.getStatus() != Auction.AuctionStatus.FINISHED) {
            logger.warn("Cancellation failed: invalid status. auctionId={}, currentStatus={}",
                    auctionId, auction.getStatus());
            throw new BusinessException("Only finished auctions can be cancelled");
        }

        auction.cancel();
        if (!auctionDAO.updateStatus(auctionId, Auction.AuctionStatus.CANCELLED)) {
            logger.error("Failed to update auction status to CANCELLED (insufficient funds) in database: auctionId={}", auctionId);
            throw new BusinessException("Failed to cancel auction due to system error.");
        }

        logger.info("Auction cancelled due to insufficient funds: auctionId={}", auctionId);
        return AuctionMapper.toAuctionResponseDTO(auction);
    }
}
