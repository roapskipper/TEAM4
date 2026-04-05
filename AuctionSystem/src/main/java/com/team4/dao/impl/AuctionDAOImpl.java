package com.team4.dao.impl;

import com.team4.dao.BaseDAO;
import com.team4.db.DatabaseManager;
import com.team4.model.Auction;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AuctionDAOImpl implements BaseDAO<Auction> {

    @Override
    public boolean save(Auction auction) {
        String sql = "INSERT INTO auctions (id, item_id, seller_id, current_highest_bidder_id, " +
                "current_price, start_time, end_time, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, auction.getId());
            pstmt.setString(2, auction.getItemId());
            pstmt.setString(3, auction.getSellerId());
            pstmt.setString(4, auction.getCurrentHighestBidderId());
            pstmt.setDouble(5, auction.getCurrentPrice());
            pstmt.setTimestamp(6, Timestamp.valueOf(auction.getStartTime()));
            pstmt.setTimestamp(7, Timestamp.valueOf(auction.getEndTime()));
            pstmt.setString(8, auction.getStatus());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public Optional<Auction> findById(String id) {
        String sql = "SELECT * FROM auctions WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToAuction(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public List<Auction> findAll() {
        List<Auction> auctions = new ArrayList<>();
        String sql = "SELECT * FROM auctions";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                auctions.add(mapResultSetToAuction(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return auctions;
    }

    /**
     * CẬP NHẬT PHIÊN ĐẤU GIÁ (Cực kỳ quan trọng để cập nhật giá mới)
     */
    @Override
    public boolean update(Auction auction) {
        String sql = "UPDATE auctions SET current_highest_bidder_id = ?, " +
                "current_price = ?, status = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, auction.getCurrentHighestBidderId());
            pstmt.setDouble(2, auction.getCurrentPrice());
            pstmt.setString(3, auction.getStatus());
            pstmt.setString(4, auction.getId());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean delete(String id) {
        String sql = "DELETE FROM auctions WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * LẤY CÁC PHIÊN ĐANG HOẠT ĐỘNG (Bổ sung để Service sử dụng)
     */
    public List<Auction> findAllActive() {
        List<Auction> activeAuctions = new ArrayList<>();
        String sql = "SELECT * FROM auctions WHERE status = 'ACTIVE' AND end_time > NOW()";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                activeAuctions.add(mapResultSetToAuction(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return activeAuctions;
    }

    // Helper mapping ResultSet to Object
    private Auction mapResultSetToAuction(ResultSet rs) throws SQLException {
        return new Auction(
                rs.getString("id"),
                rs.getString("item_id"),
                rs.getString("seller_id"),
                rs.getString("current_highest_bidder_id"),
                rs.getDouble("current_price"),
                rs.getTimestamp("start_time").toLocalDateTime(),
                rs.getTimestamp("end_time").toLocalDateTime(),
                rs.getString("status")
        );
    }
}