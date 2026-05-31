package com.team4.service;

import com.team4.dao.AutoBiddingDAO;
import com.team4.dao.AuctionDAO;
import com.team4.dao.BidTransactionDAO;
import com.team4.dao.UserDAO;
import com.team4.db.DatabaseManager;
import com.team4.dto.auction.BidTransactionResponseDTO;
import com.team4.dto.bidding.BidRequestDTO;
import com.team4.mapper.BidMapper;
import com.team4.model.Auction;
import com.team4.model.AutoBidding;
import com.team4.model.BidTransaction;
import com.team4.model.User;
import com.team4.util.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Xử lý các nghiệp vụ liên quan đến đặt giá (Proxy Bidding).
 */
public class BiddingService {
    private static final Logger logger = LoggerFactory.getLogger(BiddingService.class);
    private final AuctionDAO auctionDAO;
    private final BidTransactionDAO bidTransactionDAO;
    private final UserDAO userDAO;
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
     * Thực hiện đặt giá theo cơ chế Proxy Bidding.
     */
    public void placeBid(BidRequestDTO requestDTO) {
        String auctionId = requestDTO.getAuctionId();
        String bidderId = requestDTO.getBidderId();
        BigDecimal maxAmount = requestDTO.getAmount();

        logger.info("New bid request: auctionId={}, bidderId={}, maxAmount={}", auctionId, bidderId, maxAmount);

        try (Connection conn = DatabaseManager.getConnection()) {
            try {
                DatabaseManager.beginTransaction(conn);

                // 1. Kiểm tra trạng thái phiên đấu giá
                Auction auction = auctionDAO.findById(conn, auctionId);
                if (auction == null || !auction.canBid()) {
                    logger.warn("Bid rejected: auction does not exist or is not active. auctionId={}", auctionId);
                    throw new BusinessException("Auction is not accepting bids at this time.");
                }

                // 2. Kiểm tra tư cách người đấu giá
                User bidder = userDAO.findById(conn, bidderId);
                if (bidder == null || bidder.getRole() != User.Role.BIDDER) {
                    logger.warn("Bid rejected: invalid bidderId={}", bidderId);
                    throw new BusinessException("Only registered bidders can place bids.");
                }

                if (auction.getSellerId().equals(bidderId)) {
                    logger.warn("Bid rejected: seller cannot bid on their own item. sellerId={}", bidderId);
                    throw new BusinessException("Sellers are not allowed to bid on their own auctions.");
                }

                // 3. Kiểm tra số tiền đặt giá
                BigDecimal minRequired = auction.getCurrentPrice().add(auction.getBidIncrement());
                if (maxAmount == null || maxAmount.compareTo(minRequired) < 0) {
                    logger.warn("Bid rejected: amount too low. amount={}, required={}", maxAmount, minRequired);
                    throw new BusinessException("Bid must be at least " + minRequired + ".");
                }

                BigDecimal policyLimit = com.team4.util.BidRules.allowedMaxFor(auction.getCurrentPrice());
                if (maxAmount.compareTo(policyLimit) > 0) {
                    logger.warn("Bid rejected: exceeds policy limit. amount={}, limit={}", maxAmount, policyLimit);
                    throw new BusinessException("Bid exceeds the maximum allowed limit for this price range.");
                }

                BigDecimal lockedInOtherAuctions = autoBiddingDAO.calculateLockedBalance(conn, bidderId, auctionId);
                BigDecimal spendableBalance = bidder.getBalance().subtract(lockedInOtherAuctions);

                if (spendableBalance.compareTo(maxAmount) < 0) {
                    logger.warn("Bid rejected: insufficient balance due to locked funds. bidderId={}, balance={}, locked={}, required={}",
                            bidderId, bidder.getBalance(), lockedInOtherAuctions, maxAmount);
                    throw new BusinessException("Insufficient balance. You have funds locked in other active auctions.");
                }

                // 4. Cập nhật cấu hình Proxy Bidding (AutoBidding)
                AutoBidding existing = autoBiddingDAO.findByAuctionAndBidder(conn, auctionId, bidderId);
                if (existing == null) {
                    AutoBidding newConfig = new AutoBidding(auctionId, bidderId, maxAmount);
                    if (!autoBiddingDAO.insert(conn, newConfig)) {
                        throw new BusinessException("System error while creating bid configuration.");
                    }
                } else {
                    existing.setMaxLimit(maxAmount);
                    if (!existing.isActive()) existing.activate();
                    if (!autoBiddingDAO.update(conn, existing)) {
                        throw new BusinessException("System error while updating bid configuration.");
                    }
                }

                // 5. Tính toán lại kết quả đấu giá dựa trên các Proxy Bidders
                List<AutoBidding> contenders = autoBiddingDAO.findActiveByAuctionId(conn, auctionId);
                ProxyBidResult result = resolveProxyBid(auction, contenders);

                if (result != null) {
                    boolean sameLeader = result.winnerBidderId().equals(auction.getCurrentHighestBidderId());
                    boolean samePrice = result.displayPrice().compareTo(auction.getCurrentPrice()) == 0;

                    if (!sameLeader || !samePrice) {
                        // Áp dụng luật Anti-Sniping
                        if (auction.applyAntiSniping()) {
                            logger.info("Anti-sniping triggered: extended endTime for auctionId={}", auctionId);
                            if (!auctionDAO.updateEndTime(conn, auctionId, auction.getEndTime())) {
                                throw new BusinessException("Failed to extend auction time.");
                            }
                        }

                        // Lưu giao dịch đặt giá mới
                        if (!auctionDAO.updateCurrentBid(conn, auctionId, result.displayPrice(), result.winnerBidderId()) ||
                                !bidTransactionDAO.insert(conn, new BidTransaction(auctionId, result.winnerBidderId(), result.displayPrice()))) {
                            throw new BusinessException("Failed to update bid information.");
                        }
                    }
                }

                // 6. Tắt các AutoBidding đã đạt giới hạn và đang thua
                BigDecimal finalPrice = result != null ? result.displayPrice() : auction.getCurrentPrice();
                for (AutoBidding cfg : contenders) {
                    boolean isLoser = (result != null) && !cfg.getBidderId().equals(result.winnerBidderId());
                    boolean isExhausted = cfg.getMaxLimit().compareTo(finalPrice) <= 0;

                    if (isLoser && isExhausted && cfg.isActive()) {
                        autoBiddingDAO.updateActive(conn, cfg.getId(), false);
                    }
                }

                DatabaseManager.commitTransaction(conn);
                logger.info("Bid successfully processed: auctionId={}, newLeader={}", auctionId, result != null ? result.winnerBidderId() : "none");
            } catch (Exception e) {
                DatabaseManager.rollbackTransaction(conn);
                logger.error("Transaction failed (rolled back): {}", e.getMessage());
                throw (e instanceof BusinessException) ? (BusinessException) e : new BusinessException("Bid processing error: " + e.getMessage());
            }
        } catch (SQLException e) {
            logger.error("Database connection error during bidding: {}", e.getMessage());
            throw new BusinessException("System error during bidding process.");
        }
    }

    /**
     * Logic Proxy Bidding: Tìm người thắng có giới hạn cao nhất.
     */
    private ProxyBidResult resolveProxyBid(Auction auction, List<AutoBidding> contenders) {
        if (contenders == null || contenders.isEmpty()) return null;

        String currentLeaderId = auction.getCurrentHighestBidderId();
        BigDecimal currentPrice = auction.getCurrentPrice();
        BigDecimal increment = auction.getBidIncrement();

        List<AutoBidding> validContenders = new ArrayList<>(contenders);
        validContenders.removeIf(cfg -> cfg.getMaxLimit().compareTo(currentPrice) < 0 && !cfg.getBidderId().equals(currentLeaderId));

        if (validContenders.isEmpty()) return null;

        // Sắp xếp các ứng viên
        validContenders.sort((a, b) -> {
            int cmp = b.getMaxLimit().compareTo(a.getMaxLimit());
            if (cmp != 0) return cmp;
            if (a.getBidderId().equals(currentLeaderId)) return -1;
            if (b.getBidderId().equals(currentLeaderId)) return 1;
            return a.getCreatedAt().compareTo(b.getCreatedAt());
        });

        AutoBidding winner = validContenders.get(0);
        BigDecimal displayPrice = currentPrice;

        if (validContenders.size() >= 2) {
            AutoBidding runnerUp = validContenders.get(1);
            displayPrice = runnerUp.getMaxLimit().add(increment).min(winner.getMaxLimit());
        } else {
            if (currentLeaderId != null && !winner.getBidderId().equals(currentLeaderId)) {
                displayPrice = currentPrice.add(increment).min(winner.getMaxLimit());
            }
        }

        return new ProxyBidResult(winner.getBidderId(), displayPrice);
    }

    private record ProxyBidResult(String winnerBidderId, BigDecimal displayPrice) {}

    /**
     * Lấy lịch sử đấu giá của một phiên (DTO).
     */
    public List<BidTransactionResponseDTO> getBidHistoryByAuction(String auctionId) {
        logger.debug("Retrieving bid history: auctionId={}", auctionId);
        return bidTransactionDAO.findByAuctionId(auctionId).stream()
                .map(BidMapper::toBidTransactionResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Lấy lịch sử đặt giá cá nhân của một người dùng (DTO).
     */
    public List<BidTransactionResponseDTO> getBidHistoryByBidder(String bidderId) {
        logger.debug("Retrieving bidder history: bidderId={}", bidderId);
        return bidTransactionDAO.findByBidderId(bidderId).stream()
                .map(BidMapper::toBidTransactionResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Lấy lượt đặt giá cao nhất của một phiên.
     */
    public BidTransactionResponseDTO getHighestBid(String auctionId) {
        logger.debug("Retrieving highest bid: auctionId={}", auctionId);
        BidTransaction bid = bidTransactionDAO.getHighestBid(auctionId);
        return BidMapper.toBidTransactionResponseDTO(bid);
    }
}
