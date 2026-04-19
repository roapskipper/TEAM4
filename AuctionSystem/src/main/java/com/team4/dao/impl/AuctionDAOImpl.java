package com.team4.dao.impl;

import com.team4.dao.AuctionDAO;
import com.team4.db.DatabaseManager;
import com.team4.model.Auction;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AuctionDAOImpl implements AuctionDAO {

    /**
     * Helper Method: Chuyển đổi ResultSet thành Object Auction
     */
    private Auction mapRowToAuction(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String itemId = rs.getString("item_id");
        String sellerId = rs.getString("seller_id");
        String currentHighestBidderId = rs.getString("current_highest_bidder_id");
        double currentPrice = rs.getDouble("current_price");

        // Chuyển java.sql.Timestamp từ DB sang java.time.LocalDateTime của Java
        Timestamp startTimestamp = rs.getTimestamp("start_time");
        Timestamp endTimestamp = rs.getTimestamp("end_time");

        return new Auction(
                id,
                itemId,
                sellerId,
                currentHighestBidderId,
                currentPrice,
                startTimestamp != null ? startTimestamp.toLocalDateTime() : null,
                endTimestamp != null ? endTimestamp.toLocalDateTime() : null,
                rs.getString("status")
        );
    }

    @Override
    public Auction findById(String id) {
        String sql = "SELECT * FROM auctions WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapRowToAuction(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Auction findByItemId(String itemId) {
        String sql = "SELECT * FROM auctions WHERE item_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, itemId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapRowToAuction(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Auction> findAll() {
        return executeQueryList("SELECT * FROM auctions ORDER BY start_time DESC");
    }

    @Override
    public List<Auction> findByStatus(String status) {
        String sql = "SELECT * FROM auctions WHERE status = ? ORDER BY end_time ASC";
        List<Auction> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToAuction(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public boolean insert(Auction auction) {
        String sql = "INSERT INTO auctions (id, item_id, seller_id, current_highest_bidder_id, " +
                "current_price, start_time, end_time, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, auction.getId());
            stmt.setString(2, auction.getItemId());
            stmt.setString(3, auction.getSellerId());

            if (auction.getCurrentHighestBidderId() != null) {
                stmt.setString(4, auction.getCurrentHighestBidderId());
            } else {
                stmt.setNull(4, Types.VARCHAR);
            }

            stmt.setDouble(5, auction.getCurrentPrice());
            stmt.setTimestamp(6, Timestamp.valueOf(auction.getStartTime()));
            stmt.setTimestamp(7, Timestamp.valueOf(auction.getEndTime()));
            stmt.setString(8, auction.getStatus());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean update(Auction auction) {
        String sql = "UPDATE auctions SET current_highest_bidder_id = ?, current_price = ?, " +
                "end_time = ?, status = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (auction.getCurrentHighestBidderId() != null) {
                stmt.setString(1, auction.getCurrentHighestBidderId());
            } else {
                stmt.setNull(1, Types.VARCHAR);
            }

            stmt.setDouble(2, auction.getCurrentPrice());
            stmt.setTimestamp(3, Timestamp.valueOf(auction.getEndTime()));
            stmt.setString(4, auction.getStatus());
            stmt.setString(5, auction.getId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean updateStatus(String auctionId, String newStatus) {
        String sql = "UPDATE auctions SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newStatus);
            stmt.setString(2, auctionId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // =======================================================================
    //  XỬ LÝ CONCURRENT BIDDING
    // =======================================================================
    @Override
    public boolean placeBid(String auctionId, String bidderId, double newBidPrice) {
        /*
         * Câu lệnh SQL này sử dụng điều kiện "AND current_price < ?" để áp dụng Optimistic Locking.
         * Nếu Thread A và Thread B cùng gọi hàm này với giá 100$ và 105$.
         * Nếu Thread B (105$) chạy xong trước, giá trong DB = 105$.
         * Khi Thread A (100$) chạy tới, điều kiện `current_price < 100` sẽ bị SAI.
         * Hàm executeUpdate() sẽ trả về 0 (0 rows affected). Ngăn chặn thành công Lost Update!
         */
        String sql = "UPDATE auctions " +
                "SET current_price = ?, current_highest_bidder_id = ? " +
                "WHERE id = ? AND status = 'ACTIVE' AND current_price < ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDouble(1, newBidPrice);
            stmt.setString(2, bidderId);
            stmt.setString(3, auctionId);
            stmt.setDouble(4, newBidPrice); // Khóa logic ở đây

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0; // Trả về true nếu đặt giá thành công, false nếu bị tranh chấp hoặc quá hạn

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private List<Auction> executeQueryList(String sql) {
        List<Auction> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapRowToAuction(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}