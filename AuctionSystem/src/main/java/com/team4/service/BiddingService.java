package com.team4.service;

import com.team4.dao.AutoBiddingDAO;
import com.team4.dao.AuctionDAO;
import com.team4.dao.BidTransactionDAO;
import com.team4.dao.UserDAO;
import com.team4.db.DatabaseManager;
import com.team4.model.Auction;
import com.team4.model.BidTransaction;
import com.team4.model.User;
import com.team4.util.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class BiddingService {
    private static final Logger logger = LoggerFactory.getLogger(BiddingService.class);

    private final AuctionDAO auctionDAO;
    private final BidTransactionDAO bidTransactionDAO;
    private final UserDAO userDAO;

    @SuppressWarnings("unused")
    private final AutoBiddingDAO autoBiddingDAO;

    public BiddingService(AuctionDAO auctionDAO,
                          BidTransactionDAO bidTransactionDAO,
                          UserDAO userDAO,
                          AutoBiddingDAO autoBiddingDAO) {
        this.auctionDAO = auctionDAO;
        this.bidTransactionDAO = bidTransactionDAO;
        this.userDAO = userDAO;
        this.autoBiddingDAO = autoBiddingDAO;
    }

    /**
     * Direct bidding with escrow:
     * - The leading bid is reserved from the leading bidder balance immediately.
     * - When a new bidder becomes leader, the previous leader is refunded.
     * - The seller receives the reserved amount only when the auction is closed.
     */
    public BidResult placeBid(String auctionId, String bidderId, BigDecimal amount) {
        BigDecimal bidAmount = money(amount, "Bid amount is required.");
        logger.info("Bid request: auctionId={}, bidderId={}, amount={}", auctionId, bidderId, bidAmount);

        try (Connection conn = DatabaseManager.getConnection()) {
            try {
                DatabaseManager.beginTransaction(conn);

                Auction auction = auctionDAO.findById(conn, auctionId);
                if (auction == null || !auction.canBid()) {
                    throw new BusinessException("Auction does not exist or cannot accept bids.");
                }

                User bidder = userDAO.findById(conn, bidderId);
                if (bidder == null || bidder.getRole() != User.Role.BIDDER) {
                    throw new BusinessException("Only valid bidders can place bids.");
                }
                if (auction.getSellerId().equals(bidderId)) {
                    throw new BusinessException("Seller cannot bid on their own auction.");
                }

                BigDecimal minimumBid = auction.getCurrentPrice().add(auction.getBidIncrement());
                if (bidAmount.compareTo(minimumBid) < 0) {
                    throw new BusinessException("Bid must be at least " + minimumBid.toPlainString() + " VND.");
                }

                String previousLeaderId = auction.getCurrentHighestBidderId();
                BigDecimal previousHeldAmount = auction.getCurrentPrice();
                boolean sameLeader = bidderId.equals(previousLeaderId);
                BigDecimal amountToReserve = sameLeader ? bidAmount.subtract(previousHeldAmount) : bidAmount;

                if (amountToReserve.compareTo(BigDecimal.ZERO) > 0) {
                    if (!bidder.hasEnoughBalance(amountToReserve)) {
                        throw new BusinessException("Current balance is not enough to place this bid.");
                    }
                    bidder.withdraw(amountToReserve);
                    if (!userDAO.updateBalance(conn, bidderId, bidder.getBalance())) {
                        throw new BusinessException("System error while reserving bidder balance.");
                    }
                }

                String refundedBidderId = null;
                BigDecimal refundedBidderBalance = null;
                if (previousLeaderId != null && !sameLeader) {
                    User previousLeader = userDAO.findById(conn, previousLeaderId);
                    if (previousLeader != null) {
                        previousLeader.deposit(previousHeldAmount);
                        if (!userDAO.updateBalance(conn, previousLeaderId, previousLeader.getBalance())) {
                            throw new BusinessException("System error while refunding previous bidder.");
                        }
                        refundedBidderId = previousLeaderId;
                        refundedBidderBalance = previousLeader.getBalance();
                    }
                }

                if (auction.applyAntiSniping() && !auctionDAO.updateEndTime(conn, auctionId, auction.getEndTime())) {
                    throw new BusinessException("System error while extending anti-sniping time.");
                }

                if (!auctionDAO.updateCurrentBid(conn, auctionId, bidAmount, bidderId)
                        || !bidTransactionDAO.insert(conn, new BidTransaction(auctionId, bidderId, bidAmount))) {
                    throw new BusinessException("System error while updating auction result.");
                }

                DatabaseManager.commitTransaction(conn);
                logger.info("Bid transaction completed successfully. auctionId={}", auctionId);
                return new BidResult(bidder.getBalance(), refundedBidderId, refundedBidderBalance);
            } catch (Exception e) {
                DatabaseManager.rollbackTransaction(conn);
                logger.error("Error during bidding process (rolled back): {}", e.getMessage());
                if (e instanceof BusinessException businessException) {
                    throw businessException;
                }
                throw new BusinessException("Bid failed: " + e.getMessage());
            }
        } catch (SQLException e) {
            logger.error("Database connection error while bidding: {}", e.getMessage());
            throw new BusinessException("System error while processing bid: " + e.getMessage());
        }
    }

    private BigDecimal money(BigDecimal amount, String errorMessage) {
        if (amount == null) {
            throw new BusinessException(errorMessage);
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Bid amount must be positive.");
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    public List<BidTransaction> getBidHistoryByAuction(String auctionId) {
        logger.debug("Loading bid history for auctionId={}", auctionId);
        return bidTransactionDAO.findByAuctionId(auctionId);
    }

    public List<BidTransaction> getBidHistoryByBidder(String bidderId) {
        logger.debug("Loading bid history for bidderId={}", bidderId);
        return bidTransactionDAO.findByBidderId(bidderId);
    }

    public BidTransaction getBidHistoryByBidderAndAuction(String auctionId) {
        logger.debug("Loading highest bid for auctionId={}", auctionId);
        return bidTransactionDAO.getHighestBid(auctionId);
    }

    public static final class BidResult {
        private final BigDecimal bidderBalance;
        private final String refundedBidderId;
        private final BigDecimal refundedBidderBalance;

        public BidResult(BigDecimal bidderBalance, String refundedBidderId, BigDecimal refundedBidderBalance) {
            this.bidderBalance = bidderBalance;
            this.refundedBidderId = refundedBidderId;
            this.refundedBidderBalance = refundedBidderBalance;
        }

        public BigDecimal bidderBalance() {
            return bidderBalance;
        }

        public String refundedBidderId() {
            return refundedBidderId;
        }

        public BigDecimal refundedBidderBalance() {
            return refundedBidderBalance;
        }
    }
}
