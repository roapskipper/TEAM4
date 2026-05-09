package com.team4.service;

<<<<<<< HEAD
public class BiddingService {
}
=======
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

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BiddingService {
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
        try (Connection conn = DatabaseManager.getConnection()) {
            try {
                // Tắt autocommit
                DatabaseManager.beginTransaction(conn);
                Auction auction = auctionDAO.findById(conn, auctionId);
                // Validate phiên đấu giá
                if (auction == null || !auction.canBid()) {
                    throw new BusinessException("Phiên đấu giá không tồn tại hoặc không thể nhận bid.");
                }

                User bidder = userDAO.findById(conn, bidderId);
                if (bidder == null || bidder.getRole() != User.Role.BIDDER) {
                    throw new BusinessException("Chỉ bidder hợp lệ mới được đặt giá.");
                }

                if (auction.getSellerId().equals(bidderId)) {
                    throw new BusinessException("Người bán không được đặt giá trên phiên của mình.");
                }

                if (maxAmount == null || maxAmount.compareTo(auction.getCurrentPrice().add(auction.getBidIncrement())) < 0) {
                    throw new BusinessException("Mức giá tối đa phải cao hơn giá hiện tại ít nhất một bước giá.");
                }

                if (!bidder.hasEnoughBalance(maxAmount)) {
                    throw new BusinessException("Số dư hiện tại không đủ để bảo đảm cho mức giá tối đa này.");
                }

                // 1. Upsert cấu hình auto-bid của bidder hiện tại
                AutoBidding existing = autoBiddingDAO.findByAuctionAndBidder(conn, auctionId, bidderId);
                // Nếu chưa có config thì tạo mới
                if (existing == null) {
                    AutoBidding newConfig = new AutoBidding(auctionId, bidderId, maxAmount);
                    if (!autoBiddingDAO.insert(conn, newConfig)) {
                        throw new BusinessException("Lỗi");
                    }

                    // Nếu đã có config rồi thì cập nhật maxLimit và bật nếu đang tắt
                } else {
                    existing.setMaxLimit(maxAmount);
                    if (!existing.isActive()) {
                        existing.activate();
                    }
                    if (!autoBiddingDAO.update(conn, existing)) {
                        throw new BusinessException("Lỗi");
                    }
                }

                // 2. Lấy lại toàn bộ contender đang active trong transaction này
                List<AutoBidding> contenders = new ArrayList<>(
                        autoBiddingDAO.findActiveByAuctionId(conn, auctionId)
                );

                ProxyBidResult result = resolveProxyBid(auction, contenders);
                if (result == null) {
                    DatabaseManager.commitTransaction(conn);
                    return;
                }
                // Kiểm tra nếu giá hiển thị mới không đổi so với giá hiện tại thì không cần update
                // tránh ghi log rác và vòng lặp vô tận
                boolean sameLeader = result.winnerBidderId().equals(auction.getCurrentHighestBidderId());
                boolean samePrice = result.displayPrice().compareTo(auction.getCurrentPrice()) == 0;

                if (!sameLeader || !samePrice) {
                    if (!auctionDAO.updateCurrentBid(conn, auctionId, result.displayPrice(), result.winnerBidderId()) ||
                            !bidTransactionDAO.insert(conn, new BidTransaction(auctionId, result.winnerBidderId(), result.displayPrice()))) {
                        throw new BusinessException("Lỗi");
                    }
                }

                // 3. Có thể tắt các config đã chạm trần và vẫn thua
                for (AutoBidding cfg : contenders) {
                    boolean losing = !cfg.getBidderId().equals(result.winnerBidderId());
                    boolean exhausted = cfg.getMaxLimit().compareTo(result.displayPrice()) <= 0;

                    if (cfg.isActive() && losing && exhausted) {
                        cfg.deactivate();
                        if (!autoBiddingDAO.updateActive(conn, cfg.getId(), false)) {
                            throw new BusinessException("Lỗi");
                        }
                    }
                }
                // Commit xuống DB
                DatabaseManager.commitTransaction(conn);
            } catch (Exception e) {
                DatabaseManager.rollbackTransaction(conn);
                throw new BusinessException("Đặt giá thất bại: " + e.getMessage());
            }
        } catch (SQLException e) {
            throw new BusinessException("Lỗi hệ thống khi xử lý bid: " + e.getMessage());
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
        }

        return new ProxyBidResult(winner.getBidderId(), displayPrice);
    }

    private record ProxyBidResult(String winnerBidderId, BigDecimal displayPrice) {}

    /**
     * Lấy toàn bộ lịch sử đặt giá của 1 phiên, dùng để hiển thị lịch sử cho người xem
     */
    public List<BidTransaction> getBidHistoryByAuction(String auctionId) {
        return bidTransactionDAO.findByAuctionId(auctionId);
    }
    /**
     * Lấy lịch sử đặt giá của 1 bidder, dùng cho trang lịch sử cá nhân
     */
    public List<BidTransaction> getBidHistoryByBidder(String bidderId) {
        return bidTransactionDAO.findByBidderId(bidderId);
    }
    /**
     * Lấy lần bid cao nhất hiện tại của phiên, dùng để xác định người thắng khi phiên kết thúc
     */
    public BidTransaction getBidHistoryByBidderAndAuction(String auctionId) {
        return bidTransactionDAO.getHighestBid(auctionId);
    }
}
>>>>>>> DAO-minhtrung
