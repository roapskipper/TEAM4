package com.team4.dao.impl;

import com.team4.dao.UserDAO;
import com.team4.db.DatabaseManager;
import com.team4.model.Admin;
import com.team4.model.Bidder;
import com.team4.model.Seller;
import com.team4.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.List;
import java.util.ArrayList;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class UserDAOImpl implements UserDAO {
    private static final Logger logger = LoggerFactory.getLogger(UserDAOImpl.class);

    /** method mapRowToUser ánh xạ dữ liệu từ sql
     * Nhận một dòng dữ liệu từ ResultSet, rồi dựng đúng object con của User
     */
    private User mapRowToUser(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();
        String username = rs.getString("username");
        String passwordHash = rs.getString("password_hash");
        String fullName = rs.getString("full_name");
        String email = rs.getString("email");
        String role = rs.getString("role");
        BigDecimal balance = rs.getBigDecimal("balance");

        // Dựa vào ROLE để khởi tạo đúng đô tượng
        switch (User.Role.valueOf(role)) {
            case ADMIN:
                int level = rs.getInt("access_level");
                Admin.AccessLevel accessLevel = Admin.fromInt(level);
                String adminCodeHash = rs.getString("admin_code_hash");
                return new Admin(id, createdAt, username, passwordHash, fullName, email, balance, accessLevel, adminCodeHash);

            case BIDDER:
                String shippingAddress = rs.getString("shipping_address");
                String phoneNumber = rs.getString("phone_number");
                return new Bidder(id, createdAt, username, passwordHash, fullName, email, balance, shippingAddress, phoneNumber);

            case SELLER:
                String storeName = rs.getString("store_name");
                double rating = rs.getDouble("rating");
                return new Seller(id, createdAt, username, passwordHash, fullName, email, balance, storeName, rating);

            default:
                throw new SQLException("Error: Unrecognized role '" + role + "' for user ID: " + id);
        }
    }

    /**
     * PHẦN 2: CÁC PHƯƠNG THỨC CRUD
     */

    @Override
    /**
     * 1. findById() - tìm theo id
     * Dùng khi: load thông tin user sau khi đã biết id
     * Ví dụ: xem profile, update thông tin
     */
    public User findById(String id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        // Sử dụng try-with-resources để tự động đóng kết nối
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id); // Chống SQL Injection

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToUser(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Unable to find user with id={}", id, e);
        }
        return null; // Không tìm thấy
    }

    public User findById(Connection conn, String id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        // Sử dụng try-with-resources để tự động đóng kết nối
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id); // Chống SQL Injection

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToUser(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Unable to find user by id={} in transaction", id, e);
        }
        return null; // Không tìm thấy
    }

    @Override
    /**
     * 2. findByUsername() - tìm theo username
     * Dùng khi: user đăng nhập
     * Ví dụ: kiểm tra username + password
     */
    public User findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToUser(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Unable to find user with username={}", username, e);
        }
        return null;
    }

    @Override
    public User findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToUser(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Unable to find user with email={}", email, e);
        }
        return null;
    }

    @Override
    public boolean insert(User user) {
        String sql = "INSERT INTO users (id, created_at, username, password_hash, full_name, email, role, balance, " +
                "access_level, admin_code_hash, shipping_address, phone_number, store_name, rating) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Set các trường chung của User
            stmt.setString(1, user.getId());
            stmt.setTimestamp(2, Timestamp.valueOf(user.getCreatedAt()));
            stmt.setString(3, user.getUsername());
            stmt.setString(4, user.readPasswordHashForPersistence());
            stmt.setString(5, user.getFullName());
            stmt.setString(6, user.getEmail());
            stmt.setString(7, user.getRole().name());
            stmt.setBigDecimal(8, user.getBalance());

            // Set các trường riêng biệt
            if (user instanceof Admin) {
                Admin admin = (Admin) user;
                stmt.setInt(9, admin.getAccessLevel().getLevel());
                stmt.setString(10, admin.getAdminCodeHash());
                // Dù null nhưng DBMS vẫn yêu cầu cung cấp kiểu dữ liệu cụ thể để đảm bảo tính tương thích
                stmt.setNull(11, Types.VARCHAR);
                stmt.setNull(12, Types.VARCHAR);
                stmt.setNull(13, Types.VARCHAR);
                stmt.setNull(14, Types.DECIMAL);
            }
            else if (user instanceof Bidder) {
                Bidder bidder = (Bidder) user;
                stmt.setNull(9, Types.INTEGER);
                stmt.setNull(10, Types.VARCHAR);
                stmt.setString(11, bidder.getShippingAddress());
                stmt.setString(12, bidder.getPhoneNumber());
                stmt.setNull(13, Types.VARCHAR);
                stmt.setNull(14, Types.DECIMAL);
            }
            else if (user instanceof Seller) {
                Seller seller = (Seller) user;
                stmt.setNull(9, Types.INTEGER);
                stmt.setNull(10, Types.VARCHAR);
                stmt.setNull(11, Types.VARCHAR);
                stmt.setNull(12, Types.VARCHAR);
                stmt.setString(13, seller.getStoreName());
                stmt.setDouble(14, seller.getRating());
            }

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            logger.error("Unable to create user id={}", user.getId(), e);
            return false;
        }
    }

    @Override
    public boolean update(User user) {
        // Tương tự Insert, nhưng dùng câu lệnh UPDATE
        String sql = "UPDATE users SET full_name = ?, email = ?, password_hash = ?, balance = ?, " +
                "shipping_address = ?, phone_number = ?, store_name = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.getFullName());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.readPasswordHashForPersistence());
            stmt.setBigDecimal(4, user.getBalance());

            // Xử lý đa hình cho UPDATE
            if (user instanceof Bidder) {
                Bidder bidder = (Bidder) user;
                stmt.setString(5, bidder.getShippingAddress());
                stmt.setString(6, bidder.getPhoneNumber());
                stmt.setNull(7, Types.VARCHAR);
            } else if (user instanceof Seller) {
                Seller seller = (Seller) user;
                stmt.setNull(5, Types.VARCHAR);
                stmt.setNull(6, Types.VARCHAR);
                stmt.setString(7, seller.getStoreName());
            } else {
                // Nếu là Admin thì không cập nhật các trường này ở đây
                stmt.setNull(5, Types.VARCHAR);
                stmt.setNull(6, Types.VARCHAR);
                stmt.setNull(7, Types.VARCHAR);
            }

            stmt.setString(8, user.getId());

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            logger.error("Unable to update user id={}", user.getId(), e);
            return false;
        }
    }

    @Override
    /**
     * 3. findAll() - lấy tất cả
     * Dùng khi: admin quản lý danh sách user
     */
    public List<User> findAll() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapRowToUser(rs));
            }
        } catch (SQLException e) {
            logger.error("Unable to find all users", e);
        }
        return list;
    }
    // Dùng cho trường hợp bình thường, DAO tự mở & đóng connection
    @Override
    public boolean updateBalance(String id, BigDecimal newBalance) {
        String sql = "UPDATE users SET balance = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1,newBalance);
            stmt.setString(2,id);
            return stmt.executeUpdate() > 0;
        } catch  (SQLException e) {
            logger.error("Unable to update user balance id={} newBalance={}", id, newBalance, e);
            return false;
        }
    }
    // Dùng cho trường hợp cần transaction, connection được truyền vào từ bên ngoài
    @Override
    public boolean updateBalance(Connection conn, String id, BigDecimal newBalance) {
        String sql = "UPDATE users SET balance = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1,newBalance);
            stmt.setString(2,id);
            stmt.executeUpdate();
        } catch  (SQLException e) {
            logger.error("Unable to update user balance id={} newBalance={} in transaction", id, newBalance, e);
            return false;
        }
        return true;
    }
}
