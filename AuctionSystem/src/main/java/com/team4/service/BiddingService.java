package com.team4.service;

import com.team4.dao.AutoBiddingDAO;
import com.team4.dao.AuctionDAO;
import com.team4.dao.BidTransactionDAO;
import com.team4.dao.UserDAO;
import com.team4.db.DatabaseManager;
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
     * Theo kiểu proxy bidding:
     * - bid tay được hiểu là bidder sẵn sàng trả tối đa maxAmount
     * - hệ thống lưu/cập nhật maxLimit của bidder
     * - sau đó tính lại winner + currentPrice công khai
     */
    public void placeBid(String auctionId, String bidderId, BigDecimal maxAmount) {
        logger.info("Bid request: auctionId={}, bidderId={}, maxAmount={}", auctionId, bidderId, maxAmount);
        try (Connection conn = DatabaseManager.getConnection()) {
            try {
                // Tắt autocommit
                DatabaseManager.beginTransaction(conn);
                Auction auction = auctionDAO.findById(conn, auctionId);
                // Validate phiên đấu giá
                if (auction == null || !auction.canBid()) {
                    logger.warn("Bid failed: auction does not exist or cannot accept bids. auctionId={}", auctionId);
                    throw new BusinessException("Auction does not exist or cannot accept bids.");
                }

                User bidder = userDAO.findById(conn, bidderId);
                if (bidder == null || bidder.getRole() != User.Role.BIDDER) {
                    logger.warn("Bid failed: Invalid bidder. bidderId={}", bidderId);
                    throw new BusinessException("Only valid bidders can place bids.");
                }

                if (auction.getSellerId().equals(bidderId)) {
                    logger.warn("Bid failed: seller cannot bid. bidderId={}, auctionId={}", bidderId, auctionId);
                    throw new BusinessException("Seller cannot bid on their own auction.");
                }

                if (maxAmount == null || maxAmount.compareTo(auction.getCurrentPrice().add(auction.getBidIncrement())) < 0) {
                    logger.warn("Bid failed: bid amount ({}) is not higher than current price ({}) by at least one bid increment ({}).",
                            maxAmount, auction.getCurrentPrice(), auction.getBidIncrement());
                    throw new BusinessException("Maximum bid must exceed the current price by at least one bid increment.");
                }

                if (!bidder.hasEnoughBalance(maxAmount)) {
                    logger.warn("Bid failed: insufficient account balance. balance={}, required={}", bidder.getBalance(), maxAmount);
                    throw new BusinessException("Current balance is not enough to cover this maximum bid.");
                }

                // 1. Upsert cấu hình auto-bid của bidder hiện tại
                AutoBidding existing = autoBiddingDAO.findByAuctionAndBidder(conn, auctionId, bidderId);
                // Nếu chưa có config thì tạo mới
                if (existing == null) {
                    logger.debug("Creating Auto-bid (proxy) configuration for bidderId={}", bidderId);
                    AutoBidding newConfig = new AutoBidding(auctionId, bidderId, maxAmount);
                    if (!autoBiddingDAO.insert(conn, newConfig)) {
                        logger.error("Error while saving new Auto-bid configuration.");
                        throw new BusinessException("System error while saving bid configuration.");
                    }

                    // Nếu đã có config rồi thì cập nhật maxLimit và bật nếu đang tắt
                } else {
                    logger.debug("Updating current Auto-bid limit for bidderId={}, oldMax={}, newMax={}",
                            bidderId, existing.getMaxLimit(), maxAmount);
                    existing.setMaxLimit(maxAmount);
                    if (!existing.isActive()) {
                        existing.activate();
                    }
                    if (!autoBiddingDAO.update(conn, existing)) {
                        logger.error("Error while updating Auto-bid configuration.");
                        throw new BusinessException("System error while updating bid configuration.");
                    }
                }

                // 2. Lấy lại toàn bộ contender đang active trong transaction này
                List<AutoBidding> contenders = new ArrayList<>(
                        autoBiddingDAO.findActiveByAuctionId(conn, auctionId)
                );
                logger.debug("Found {} active Proxy Bidding candidates for auction {}", contenders.size(), auctionId);

                ProxyBidResult result = resolveProxyBid(auction, contenders);
                if (result == null) {
                    logger.debug("No new Proxy Bid result found. Keeping current state.");
                    DatabaseManager.commitTransaction(conn);
                    return;
                }
                // Kiểm tra nếu giá hiển thị mới không đổi so với giá hiện tại thì không cần update
                // tránh ghi log rác và vòng lặp vô tận
                boolean sameLeader = result.winnerBidderId().equals(auction.getCurrentHighestBidderId());
                boolean samePrice = result.displayPrice().compareTo(auction.getCurrentPrice()) == 0;

                if (!sameLeader || !samePrice) {
                    boolean isExtended = auction.applyAntiSniping(); // Kiểm tra và dời giờ trong Model

                    if (isExtended) {
                        logger.info("Anti-sniping activated: moving auction {} end time to {}", auctionId, auction.getEndTime());
                        // Cập nhật thời gian mới xuống Database
                        if (!auctionDAO.updateEndTime(conn, auctionId, auction.getEndTime())) {
                            throw new BusinessException("System error while extending anti-sniping time.");
                        }
                    }
                    logger.info("Updating new leader: auctionId={}, oldLeader={}, newLeader={}, oldPrice={}, newPrice={}",
                            auctionId, auction.getCurrentHighestBidderId(), result.winnerBidderId(), auction.getCurrentPrice(), result.displayPrice());
                    if (!auctionDAO.updateCurrentBid(conn, auctionId, result.displayPrice(), result.winnerBidderId()) ||
                            !bidTransactionDAO.insert(conn, new BidTransaction(auctionId, result.winnerBidderId(), result.displayPrice()))) {
                        logger.error("Error while updating new bid information in the database.");
                        throw new BusinessException("System error while updating auction result.");
                    }
                }

                // 3. Có thể tắt các config đã chạm trần và vẫn thua
                for (AutoBidding cfg : contenders) {
                    boolean losing = !cfg.getBidderId().equals(result.winnerBidderId());
                    boolean exhausted = cfg.getMaxLimit().compareTo(result.displayPrice()) <= 0;

                    if (cfg.isActive() && losing && exhausted) {
                        logger.info("Automatically disabling Auto-bid for bidderId={} because max limit {} was reached", cfg.getBidderId(), cfg.getMaxLimit());
                        cfg.deactivate();
                        if (!autoBiddingDAO.updateActive(conn, cfg.getId(), false)) {
                            logger.error("Error while disabling expired Auto-bid configuration.");
                            throw new BusinessException("System error while disabling expired bid configuration.");
                        }
                    }
                }
                // Commit xuống DB
                DatabaseManager.commitTransaction(conn);
                logger.info("Bid transaction completed successfully. auctionId={}", auctionId);
            } catch (Exception e) {
                DatabaseManager.rollbackTransaction(conn);
                logger.error("Error during bidding process (rolled back): {}", e.getMessage());
                throw (e instanceof BusinessException) ? (BusinessException) e : new BusinessException("Bid failed: " + e.getMessage());
            }
        } catch (SQLException e) {
            logger.error("Database connection error while bidding: {}", e.getMessage());
            throw new BusinessException("System error while processing bid: " + e.getMessage());
        }
    }

    /**
     * Tính winner và giá hiển thị theo proxy bidding.
     * - winner: người có maxLimit cao nhất
     * - displayPrice: min(winnerMax, runnerUpMax + bidIncrement)
     */
    private ProxyBidResult resolveProxyBid(Auction auction, List<AutoBidding> contenders) {
        if (contenders == null || contenders.isEmpty()) {
            return null;
        }

        String currentLeaderId = auction.getCurrentHighestBidderId();
        BigDecimal currentPrice = auction.getCurrentPrice();
        BigDecimal increment = auction.getBidIncrement();

        // Loại người đã không còn đủ sức cạnh tranh, nhưng vẫn giữ leader hiện tại để tie-break nếu cần
        contenders.removeIf(cfg ->
                cfg.getMaxLimit().compareTo(currentPrice) < 0 &&
                        !cfg.getBidderId().equals(currentLeaderId)
        );

        if (contenders.isEmpty()) {
            return null;
        }
        // Sắp xếp:
        // 1. maxLimit cao hơn đứng trước
        contenders.sort((a, b) -> {
            int byMax = b.getMaxLimit().compareTo(a.getMaxLimit());
            if (byMax != 0) {
                return byMax;
            }
        // 2. nếu bằng nhau thì ưu tiên người đang dẫn đầu hiện tại
            boolean aIsLeader = a.getBidderId().equals(currentLeaderId);
            boolean bIsLeader = b.getBidderId().equals(currentLeaderId);

            if (aIsLeader && !bIsLeader) return -1;
            if (!aIsLeader && bIsLeader) return 1;
        // 3. nếu vẫn bằng thì ai tạo config sớm hơn được ưu tiên
            return a.getCreatedAt().compareTo(b.getCreatedAt());
        });

        // Nếu chỉ có 1 config thì giá hiển thị bằng giá hiện tại
        AutoBidding winner = contenders.get(0);
        BigDecimal displayPrice = currentPrice;
        // Nếu có ít nhất 2 config thì dùng công thức
        if (contenders.size() >= 2) {
            AutoBidding runnerUp = contenders.get(1);
            displayPrice = runnerUp.getMaxLimit()
                    .add(increment)
                    .min(winner.getMaxLimit());
            logger.debug("Proxy Bid calculation: winner={}, runnerUp={}, calculatedPrice={}",
                    winner.getBidderId(), runnerUp.getBidderId(), displayPrice);
        } else {
            logger.debug("Only one leading candidate: bidderId={}, currentPrice={}", winner.getBidderId(), displayPrice);
        }

        return new ProxyBidResult(winner.getBidderId(), displayPrice);
    }

    private record ProxyBidResult(String winnerBidderId, BigDecimal displayPrice) {}

    /**
     * Lấy toàn bộ lịch sử đặt giá của 1 phiên, dùng để hiển thị lịch sử cho người xem
     */
    public List<BidTransaction> getBidHistoryByAuction(String auctionId) {
        logger.debug("Loading bid history for auctionId={}", auctionId);
        return bidTransactionDAO.findByAuctionId(auctionId);
    }
    /**
     * Lấy lịch sử đặt giá của 1 bidder, dùng cho trang lịch sử cá nhân
     */
    public List<BidTransaction> getBidHistoryByBidder(String bidderId) {
        logger.debug("Loading bid history for bidderId={}", bidderId);
        return bidTransactionDAO.findByBidderId(bidderId);
    }
    /**
     * Lấy lần bid cao nhất hiện tại của phiên, dùng để xác định người thắng khi phiên kết thúc
     */
    public BidTransaction getBidHistoryByBidderAndAuction(String auctionId) {
        logger.debug("Loading highest bid for auctionId={}", auctionId);
        return bidTransactionDAO.getHighestBid(auctionId);
    }
}