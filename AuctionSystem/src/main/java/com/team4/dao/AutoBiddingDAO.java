package com.team4.dao;
import com.team4.model.AutoBidding;

import java.sql.Connection;
import java.util.List;

public interface AutoBiddingDAO {
    AutoBidding findById(String Id);
    /**
     * 1. findByAuctionAndBidder() - tìm config auto-bid
     * Dùng khi: kiểm tra bidder đã cài auto-bid chưa
     * Ví dụ: bidder vào phiên -> kiểm tra đã có auto-bid chưa
     * tránh tạo 2 config cho cùng 1 bidder trong 1 phiên
     */
    AutoBidding findByAuctionAndBidder(String auctionId, String bidderId);
    AutoBidding findByAuctionAndBidder(Connection conn, String auctionId, String bidderId);

    /**
     * 2. findActiveByAuctionId() - tìm tất cả auto-bid đang bật
     * Dùng khi: có bid mới -> kích hoạt auto-bid
     * Ví dụ: user A bid 1000$ -> hệ thống tìm tất cả auto-bid
     * đang active trong phiên -> tự động bid cho họ
     */
    List<AutoBidding> findActiveByAuctionId(String auctionId);
    List<AutoBidding> findActiveByAuctionId(Connection conn, String auctionId);

    /**
     * 3. insert() - tạo mới config auto-bid
     * Dùng khi: bidder bật tính năng auto-bid
     * Ví dụ: bidder đặt maxLimit=5000$, increment=100$
     */
    boolean insert(AutoBidding autoBidding);
    boolean insert(Connection conn, AutoBidding autoBidding);

    /**
     * 4. update() - cập nhật config auto-bid
     * Dùng khi: bidder muốn thay đổi cấu hình
     * Ví dụ: đổi maxLimit từ 5000$ -> 8000$
     * đổi increment từ 100$ -> 200$
     */
    boolean update(AutoBidding autoBidding);
    boolean update(Connection conn, AutoBidding autoBidding);

    /**
     * 5. updateActive() - bật/tắt auto-bid
     * Dùng khi:
     * - Bidder tự tắt auto-bid
     * - Auto-bid chạm maxLimit -> tự động tắt
     * - Phiên kết thúc -> tắt tất cả auto-bid
     */
    boolean updateActive(String id, boolean isActive);
    boolean updateActive(Connection conn, String id, boolean isActive);
}
