package com.team4.dao.impl;

import com.team4.dao.AuctionDAO;
import com.team4.db.DatabaseManager;
import com.team4.model.Auction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** AuctionDAOImpl
 * Nó là cầu nối giữa:
 * object Auction trong Java
 * bảng auctions trong MySQL
 *
 * Nó chỉ nên lo:
 * đọc 1 dòng SQL rồi dựng thành Auction
 * ghi Auction xuống DB
 * cập nhật vài field cần thiết như status, currentPrice, currentHighestBidderId
 *
 * Nó không nên lo:
 * placeBid()
 * kiểm tra đủ tiền
 * xử lý cạnh tranh nhiều người bid
 * Mấy thứ đó thuộc Service
 */
public class AuctionDAOImpl implements AuctionDAO {
    private static final Logger logger = LoggerFactory.getLogger(AuctionDAOImpl.class);

    /**
     * Chuyển đổi ResultSet thành Object Auction
     */
    private Auction mapRowToAuction(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();
        String itemId = rs.getString("item_id");
        String sellerId = rs.getString("seller_id");
        String currentHighestBidderId = rs.getString("current_highest_bidder_id");
        BigDecimal startingPrice = rs.getBigDecimal("starting_price");
        BigDecimal currentPrice = rs.getBigDecimal("current_price");
        BigDecimal bidIncrement = rs.getBigDecimal("bid_increment");
        String status = rs.getString("status");

        LocalDateTime startTime = rs.getTimestamp("start_time").toLocalDateTime() ;
        LocalDateTime endTime = rs.getTimestamp("end_time").toLocalDateTime() ;

        return new Auction(id, createdAt,
                itemId, sellerId,
                currentHighestBidderId,
                startingPrice, currentPrice, bidIncrement,
                startTime, endTime,
                Auction.AuctionStatus.valueOf(status));
    }

    @Override
    /**
     * 1. findById() - tìm theo id
     * Dùng khi: load chi tiết 1 phiên đấu giá
     * Ví dụ: user click vào phiên → load thông tin
     */
    public Auction findById(String id) {
        String sql = "SELECT * FROM auctions WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapRowToAuction(rs);
            }
        } catch (SQLException e) {
            logger.error("Unable to find auction with id={}", id, e);
        }
        return null;
    }

    @Override
    public Auction findById(Connection conn, String id) {
        String sql = "SELECT * FROM auctions WHERE id = ? FOR UPDATE";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapRowToAuction(rs);
            }
        } catch (SQLException e) {
            logger.error("Unable to find auction with id={} trong transaction", id, e);
        }
        return null;
    }


    @Override
    /**
     * 2. findByItemId() - tìm theo item
     * Dùng khi: kiểm tra item đã có phiên đấu giá chưa
     * Ví dụ: tránh tạo 2 phiên cho cùng 1 item
     */
    public Auction findByItemId(String itemId) {
        String sql = "SELECT * FROM auctions WHERE item_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, itemId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapRowToAuction(rs);
            }
        } catch (SQLException e) {
            logger.error("Unable to find auction with itemId={}", itemId, e);
        }
        return null;
    }

    @Override
    /**
     * 3. findAll() - lấy tất cả
     * Dùng khi: admin xem toàn bộ phiên đấu giá
     * Ví dụ: màn hình quản lý của admin
     */
    public List<Auction> findAll() {
        String sql ="SELECT * FROM auctions ORDER BY start_time DESC";
        return executeQueryList(sql);
    }

    @Override
    /**
     * 4. findByStatus() - tìm theo trạng thái
     * Dùng khi: lọc phiên theo trạng thái
     * Ví dụ: scheduler tìm phiên ACTIVE để kiểm tra hết giờ chưa
     */
    public List<Auction> findByStatus(Auction.AuctionStatus status) {
        // sắp xếp theo thời gian kết thúc tăng dần, kết thúc sớm nhất → lên đầu
        String sql = "SELECT * FROM auctions WHERE status = ? ORDER BY end_time ASC";
        List<Auction> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status.name());
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
    /**
     * 5. insert() - tạo mới
     * Dùng khi: seller tạo phiên đấu giá mới
     * Ví dụ: seller đăng sản phẩm → tạo phiên mới status=PENDING
     */
    public boolean insert(Auction auction) {
        try (Connection conn = DatabaseManager.getConnection()) {
            return insert(conn, auction);
        } catch (SQLException e) {
            logger.error("Unable to connect to create auction id={}", auction.getId(), e);
            return false;
        }
    }

    @Override
    public boolean insert(Connection conn, Auction auction) {
        String sql = "INSERT INTO auctions (id, created_at, item_id, seller_id, current_highest_bidder_id, " +
                "starting_price, bid_increment, current_price, start_time, end_time, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, auction.getId());
            stmt.setObject(2, auction.getCreatedAt());
            stmt.setString(3, auction.getItemId());
            stmt.setString(4, auction.getSellerId());
            if (auction.getCurrentHighestBidderId() != null) {
                stmt.setString(5, auction.getCurrentHighestBidderId());
            } else {
                stmt.setNull(5, Types.VARCHAR);
            }

            stmt.setBigDecimal(6, auction.getStartingPrice());
            stmt.setBigDecimal(7, auction.getBidIncrement());
            stmt.setBigDecimal(8, auction.getCurrentPrice());
            // Dùng Timestamp thay vì LocalDateTime để phù hợp với JDBC
            stmt.setTimestamp(9, Timestamp.valueOf(auction.getStartTime()));
            stmt.setTimestamp(10, Timestamp.valueOf(auction.getEndTime()));
            stmt.setString(11, auction.getStatus().name());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Unable to create auction id={}", auction.getId(), e);
            return false;
        }
    }
    @Override
    /**
     * 6. updateStatus() - cập nhật trạng thái
     * Dùng khi: chuyển trạng thái phiên
     * Ví dụ: admin duyệt → PENDING → ACTIVE
     */
    public boolean updateStatus(String auctionId, Auction.AuctionStatus newStatus) {
        String sql = "UPDATE auctions SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newStatus.name());
            stmt.setString(2, auctionId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Unable to update auction status auctionId={} newStatus={}", auctionId, newStatus, e);
            return false;
        }
    }

    @Override
    public boolean updateStatus(Connection conn, String auctionId, Auction.AuctionStatus newStatus) {
        String sql = "UPDATE auctions SET status = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newStatus.name());
            stmt.setString(2, auctionId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Unable to update auction status auctionId={} newStatus={} in transaction", auctionId, newStatus, e);
            return false;
        }
    }

    @Override
    /**
     * 7. updateCurrentBid() - cập nhật giá đấu
     * Dùng khi: có bid mới hợp lệ
     * Ví dụ: user bid 1500$ → cập nhật currentPrice + highestBidderId
     */
    public boolean updateCurrentBid(Connection conn, String id, BigDecimal currentPrice, String highestBidderId) {
        String sql = "UPDATE auctions SET current_price = ?, current_highest_bidder_id = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1,currentPrice);
            stmt.setString(2,highestBidderId);
            stmt.setString(3,id);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Unable to update current bid auctionId={} currentPrice={} highestBidderId={} in transaction", id, currentPrice, highestBidderId, e);
            return false;
        }
    }

    @Override
    // Trong AuctionDAOImpl
    public boolean updateEndTime(Connection conn, String auctionId, LocalDateTime newEndTime){
        String sql = "UPDATE auctions SET end_time = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(newEndTime));
            stmt.setString(2, auctionId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Unable to update endTime auctionId={} newEndTime={} in transaction", auctionId, newEndTime, e);
            return false;
        }
    }

    public boolean updateCurrentBid(String id, BigDecimal currentPrice, String highestBidderId) {
        String sql = "UPDATE auctions SET current_price = ?, current_highest_bidder_id = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1,currentPrice);
            stmt.setString(2,highestBidderId);
            stmt.setString(3,id);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Unable to update current bid auctionId={} currentPrice={} highestBidderId={}", id, currentPrice, highestBidderId, e);
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
            logger.error("Cannot execute auction list query", e);
        }
        return list;
    }
}
