package com.team4.dao.impl;

import com.team4.dao.BidTransactionDAO;
import com.team4.db.DatabaseManager;
import com.team4.model.BidTransaction;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;
import java.math.BigDecimal;

public class BidTransactionDAOImpl implements BidTransactionDAO {

    private BidTransaction mapRowToTransaction(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();
        String auctionId = rs.getString("auction_id");
        String bidderId = rs.getString("bidder_id");
        BigDecimal bidAmount = rs.getBigDecimal("bid_amount");
        LocalDateTime bidTime = rs.getTimestamp("bid_time").toLocalDateTime();

        return new BidTransaction(id, createdAt, bidTime, auctionId, bidderId, bidAmount);
    }

    @Override
    public boolean insert(BidTransaction transaction) {
        String sql = "INSERT INTO bid_transactions (id, created_at, auction_id, bidder_id, bid_amount, bid_time) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, transaction.getId());
            stmt.setTimestamp(2, Timestamp.valueOf(transaction.getCreatedAt()));
            stmt.setString(3, transaction.getAuctionId());
            stmt.setString(4, transaction.getBidderId());
            stmt.setBigDecimal(5, transaction.getBidAmount());
            stmt.setTimestamp(6, Timestamp.valueOf(transaction.getBidTime()));

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public List<BidTransaction> findByAuctionId(String auctionId) {
        String sql = "SELECT * FROM bid_transactions WHERE auction_id = ? ORDER BY bid_time ASC";
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

    @Override
    public List<BidTransaction> findByBidderId(String bidderId) {
        String sql = "SELECT * FROM bid_transactions WHERE bidder_id = ? ORDER BY bid_time DESC";
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
        // Dùng bid_time DESC để xử lý 2 bid cùng giá
        String sql = "SELECT * FROM bid_transactions WHERE auction_id = ? ORDER BY bid_amount DESC, bid_time DESC LIMIT 1";

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