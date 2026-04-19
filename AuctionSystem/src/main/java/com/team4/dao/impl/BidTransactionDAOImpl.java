package com.team4.dao.impl;

import com.team4.dao.BidTransactionDAO;
import com.team4.db.DatabaseManager;
import com.team4.model.BidTransaction;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BidTransactionDAOImpl implements BidTransactionDAO {

    /**
     * Helper Method: Chuyển 1 dòng dữ liệu từ MySQL thành đối tượng Java
     */
    private BidTransaction mapRowToTransaction(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String auctionId = rs.getString("auction_id");
        String bidderId = rs.getString("bidder_id");
        double bidAmount = rs.getDouble("bid_amount");

        // Chuyển đổi Timestamp của DB sang LocalDateTime của Java
        Timestamp bidTimestamp = rs.getTimestamp("bid_time");

        return new BidTransaction(
                id,
                auctionId,
                bidderId,
                bidAmount,
                bidTimestamp != null ? bidTimestamp.toLocalDateTime() : null
        );
    }

    @Override
    public boolean insert(BidTransaction transaction) {
        String sql = "INSERT INTO bids (id, auction_id, bidder_id, bid_amount, bid_time) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, transaction.getId());
            stmt.setString(2, transaction.getAuctionId());
            stmt.setString(3, transaction.getBidderId());
            stmt.setDouble(4, transaction.getBidAmount());
            stmt.setTimestamp(5, Timestamp.valueOf(transaction.getBidTime()));

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * HÀM ĂN ĐIỂM: Dùng để lấy dữ liệu vẽ Biểu đồ đường (Line Chart) thời gian thực.
     * Dữ liệu được sắp xếp theo bid_time TĂNG DẦN (ASC) (từ cũ tới mới) để vẽ trục X.
     */
    @Override
    public List<BidTransaction> findByAuctionId(String auctionId) {
        String sql = "SELECT * FROM bids WHERE auction_id = ? ORDER BY bid_time ASC";
        List<BidTransaction> list = new ArrayList<>();

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, auctionId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToTransaction(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Dùng cho màn hình "Lịch sử của tôi" của User.
     * Dữ liệu sắp xếp theo bid_time GIẢM DẦN (DESC) để hiển thị giao dịch mới nhất lên đầu.
     */
    @Override
    public List<BidTransaction> findByBidderId(String bidderId) {
        String sql = "SELECT * FROM bids WHERE bidder_id = ? ORDER BY bid_time DESC";
        List<BidTransaction> list = new ArrayList<>();

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, bidderId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToTransaction(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public BidTransaction getHighestBid(String auctionId) {
        // Sắp xếp giá giảm dần và lấy 1 dòng đầu tiên (LIMIT 1)
        String sql = "SELECT * FROM bids WHERE auction_id = ? ORDER BY bid_amount DESC LIMIT 1";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, auctionId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToTransaction(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null; // Trả về null nếu phiên này chưa có ai đặt giá
    }
}