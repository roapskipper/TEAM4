package com.team4.dao.impl;

import com.team4.dao.BaseDAO;
import com.team4.db.DatabaseManager;
import com.team4.model.BidTransaction;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BidTransactionDAOImpl implements BaseDAO<BidTransaction> {

    @Override
    public boolean save(BidTransaction bid) {
        String sql = "INSERT INTO bids (id, auction_id, bidder_id, bid_amount, bid_time) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, bid.getId());
            pstmt.setString(2, bid.getAuctionId());
            pstmt.setString(3, bid.getBidderId());
            pstmt.setDouble(4, bid.getBidAmount());
            pstmt.setTimestamp(5, Timestamp.valueOf(bid.getBidTime()));

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public Optional<BidTransaction> findById(String id) {
        String sql = "SELECT * FROM bids WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToBid(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public List<BidTransaction> findAll() {
        List<BidTransaction> bids = new ArrayList<>();
        String sql = "SELECT * FROM bids ORDER BY bid_time DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                bids.add(mapResultSetToBid(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return bids;
    }

    @Override
    public boolean update(BidTransaction entity) {
        // Giao dịch tiền bạc là bất biến (Immutable), không được sửa
        System.out.println("[DAO] Warning: Bid Transactions cannot be updated.");
        return false;
    }

    @Override
    public boolean delete(String id) {
        String sql = "DELETE FROM bids WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    // --- TÍNH NĂNG THÊM CHO TRUNG ---
    public List<BidTransaction> findByAuctionId(String auctionId) {
        List<BidTransaction> history = new ArrayList<>();
        String sql = "SELECT * FROM bids WHERE auction_id = ? ORDER BY bid_amount ASC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, auctionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    history.add(mapResultSetToBid(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return history;
    }

    // Helper method ánh xạ từ SQL Result sang Object Java
    private BidTransaction mapResultSetToBid(ResultSet rs) throws SQLException {
        return new BidTransaction(
                rs.getString("id"),
                rs.getString("auction_id"),
                rs.getString("bidder_id"),
                rs.getDouble("bid_amount"),
                rs.getTimestamp("bid_time").toLocalDateTime()
        );
    }
}