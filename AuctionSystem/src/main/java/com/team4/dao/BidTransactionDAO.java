package com.team4.dao;

import com.team4.model.BidTransaction;

import java.sql.Connection;
import java.util.List;

public interface BidTransactionDAO {
    // 1. Thêm một lượt đặt giá mới vào lịch sử
    boolean insert(Connection conn,BidTransaction transaction);

    // 2. Lấy toàn bộ lịch sử đặt giá của một phiên đấu giá (Phục vụ vẽ biểu đồ Line Chart)
    List<BidTransaction> findByAuctionId(String auctionId);

    // 3. Lấy toàn bộ lịch sử những lần đặt giá của một người dùng cụ thể
    List<BidTransaction> findByBidderId(String bidderId);

    // 4. Lấy giao dịch có mức giá cao nhất của một phiên (Hàm tiện ích)
    BidTransaction getHighestBid(String auctionId);
}
