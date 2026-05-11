package com.team4.dao.impl;

import com.team4.model.AutoBidding;
import com.team4.db.DatabaseManager;
import com.team4.dao.AutoBiddingDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

public class AutoBiddingDAOImpl implements AutoBiddingDAO {
    private static final Logger logger = LoggerFactory.getLogger(AutoBiddingDAOImpl.class);

    private AutoBidding mapRowToAutoBidding(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();
        String auctionId = rs.getString("auction_id");
        String bidderId = rs.getString("bidder_id");
        BigDecimal maxLimit = rs.getBigDecimal("max_limit");
        BigDecimal increment = rs.getBigDecimal("increment_amount");
        Boolean isActive = rs.getBoolean("is_active");

        return new AutoBidding(id, createdAt, auctionId, bidderId, maxLimit, isActive);
    }
    @Override
    public AutoBidding findById(String id) {
        String sql = "SELECT * FROM auto_biddings WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1,id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapRowToAutoBidding(rs);
            }
        } catch (SQLException e) {
            logger.error("Không thể tìm cấu hình có id={}", id, e); }
    return null;}
    @Override
    public AutoBidding findByAuctionAndBidder(String auctionId, String bidderId) {
        String sql = "SELECT * FROM auto_biddings WHERE auction_id = ? AND bidder_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1,auctionId);
            stmt.setString(2,bidderId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapRowToAutoBidding(rs);
            }
        } catch (SQLException e) {
            logger.error("Không thể tìm cấu hình với auctionId={} bidderId={}", auctionId, bidderId, e);
        }
        return null;
    }

    @Override
    public AutoBidding findByAuctionAndBidder(Connection conn, String auctionId, String bidderId) {
        String sql = "SELECT * FROM auto_biddings WHERE auction_id = ? AND bidder_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1,auctionId);
            stmt.setString(2,bidderId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapRowToAutoBidding(rs);
            }
        } catch (SQLException e) {
            logger.error("Không thể tìm cấu hình với auctionId={} bidderId={} trong transaction", auctionId, bidderId, e);
        }
        return null;
    }

    @Override
    public List<AutoBidding> findActiveByAuctionId(Connection conn, String auctionId) {
        String sql = "SELECT * FROM auto_biddings WHERE auction_id = ? AND is_active = ?";
        List<AutoBidding> list = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1,auctionId);
            stmt.setBoolean(2,true);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    AutoBidding autoBidding = mapRowToAutoBidding(rs);
                    list.add(autoBidding);
                }
            }
        } catch (SQLException e) {
            logger.error("Không thể tìm danh sách cấu hình với auctionId={} trong transaction", auctionId, e);
        }
        return list;
    }

    @Override
    public List<AutoBidding> findActiveByAuctionId(String auctionId) {
        String sql = "SELECT * FROM auto_biddings WHERE auction_id = ? AND is_active = ?";
        List<AutoBidding> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1,auctionId);
            stmt.setBoolean(2,true);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    AutoBidding autoBidding = mapRowToAutoBidding(rs);
                    list.add(autoBidding);
                }
            }
        } catch (SQLException e) {
            logger.error("Không thể tìm danh sách cấu hình với auctionId={}", auctionId, e);
        }
        return list;
    }

    @Override
    public boolean insert(AutoBidding autoBidding) {
        String sql = "INSERT INTO auto_biddings (id, created_at, auction_id, bidder_id, max_limit, is_active) VALUES (?, ?, ?, ?, ?, ?) ";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1,autoBidding.getId());
            stmt.setObject(2, autoBidding.getCreatedAt());
            stmt.setString(3,autoBidding.getAuctionId());
            stmt.setString(4,autoBidding.getBidderId());
            stmt.setBigDecimal(5,autoBidding.getMaxLimit());
            stmt.setBoolean(6,autoBidding.isActive());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Không thể tạo cấu hình có id={}", autoBidding.getId(), e);
            return false;
        }
    }

    @Override
    public boolean insert(Connection conn, AutoBidding autoBidding) {
        String sql = "INSERT INTO auto_biddings (id, created_at, auction_id, bidder_id, max_limit, is_active) VALUES (?, ?, ?, ?, ?, ?) ";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1,autoBidding.getId());
            stmt.setObject(2, autoBidding.getCreatedAt());
            stmt.setString(3,autoBidding.getAuctionId());
            stmt.setString(4,autoBidding.getBidderId());
            stmt.setBigDecimal(5,autoBidding.getMaxLimit());
            stmt.setBoolean(6,autoBidding.isActive());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Không thể tạo cấu hình có id={} trong transaction", autoBidding.getId(), e);
            return false;
        }
    }

    @Override
    public boolean update(Connection conn, AutoBidding autoBidding) {
        String sql ="UPDATE auto_biddings SET max_limit = ?, is_active = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1,autoBidding.getMaxLimit());
            stmt.setBoolean(2,autoBidding.isActive());
            stmt.setString(3,autoBidding.getId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Khong thể update auto-bid id={} trong transaction", autoBidding.getId(), e);
            return false;
        }
    }

    @Override
    public boolean update(AutoBidding autoBidding) {
        String sql ="UPDATE auto_biddings SET max_limit = ?, is_active = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1,autoBidding.getMaxLimit());
            stmt.setBoolean(2,autoBidding.isActive());
            stmt.setString(3,autoBidding.getId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Không thể update auto-bid id={}", autoBidding.getId(), e);
            return false;
        }
    }

    @Override
    public boolean updateActive(String id, boolean isActive) {
        String sql = "UPDATE auto_biddings SET is_active = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBoolean(1,isActive);
            stmt.setString(2,id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Không thể update trạng thái auto-bid id={} isActive={}", id, isActive, e);
            return false;
        }
    }

    @Override
    public boolean updateActive(Connection conn,String id, boolean isActive) {
        String sql = "UPDATE auto_biddings SET is_active = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBoolean(1,isActive);
            stmt.setString(2,id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Không thể update trạng thái auto-bid id={} isActive={} trong transaction", id, isActive, e);
            return false;
        }
    }
}
