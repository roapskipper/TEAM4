package com.team4.dao.impl;

import com.team4.dao.BaseDAO;
import com.team4.db.DatabaseManager;
import com.team4.model.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDAOImpl implements BaseDAO<User> {

    @Override
    public boolean save(User user) {
        // SQL lưu tất cả các trường, kể cả các trường đặc thù của Seller/Bidder
        String sql = "INSERT INTO users (id, username, password, full_name, email, role, balance, " +
                "store_name, rating, shipping_address, phone_number, access_level, admin_code) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, user.getId());
            pstmt.setString(2, user.getUsername());
            pstmt.setString(3, user.getPassword());
            pstmt.setString(4, user.getFullName());
            pstmt.setString(5, user.getEmail());
            pstmt.setString(6, user.getRole());
            pstmt.setDouble(7, user.getBalance());

            // Xử lý các trường đặc thù bằng cách kiểm tra kiểu đối tượng (instanceof)
            if (user instanceof Seller) {
                Seller s = (Seller) user;
                pstmt.setString(8, s.getStoreName());
                pstmt.setDouble(9, s.getRating());
                pstmt.setNull(10, Types.VARCHAR); // shipping_address
                pstmt.setNull(11, Types.VARCHAR); // phone_number
                pstmt.setNull(12, Types.INTEGER); // access_level
                pstmt.setNull(13, Types.VARCHAR); // admin_code
            } else if (user instanceof Bidder) {
                Bidder b = (Bidder) user;
                pstmt.setNull(8, Types.VARCHAR); // store_name
                pstmt.setNull(9, Types.DOUBLE);  // rating
                pstmt.setString(10, b.getShippingAddress());
                pstmt.setString(11, b.getPhoneNumber());
                pstmt.setNull(12, Types.INTEGER);
                pstmt.setNull(13, Types.VARCHAR);
            } else if (user instanceof Admin) {
                Admin a = (Admin) user;
                pstmt.setNull(8, Types.VARCHAR);
                pstmt.setNull(9, Types.DOUBLE);
                pstmt.setNull(10, Types.VARCHAR);
                pstmt.setNull(11, Types.VARCHAR);
                pstmt.setInt(12, a.getAccessLevel());
                pstmt.setString(13, a.getAdminCode());
            }

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public Optional<User> findById(String id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    @Override
    public boolean update(User user) {
        String sql = "UPDATE users SET full_name = ?, email = ?, balance = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.getFullName());
            pstmt.setString(2, user.getEmail());
            pstmt.setDouble(3, user.getBalance());
            pstmt.setString(4, user.getId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean delete(String id) {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * GIẢI PHÁP CHO LỖI ABSTRACT: Hàm ánh xạ từ DB sang Object thực tế.
     * Sử dụng đa hình để tạo đúng lớp con.
     */
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        String role = rs.getString("role");
        String id = rs.getString("id");
        String username = rs.getString("username");
        String password = rs.getString("password");
        String fullName = rs.getString("full_name");
        String email = rs.getString("email");
        double balance = rs.getDouble("balance");

        // Dựa vào Role để tạo đối tượng cụ thể (Polymorphism)
        if ("ADMIN".equalsIgnoreCase(role)) {
            return new Admin(id, username, password, fullName, email, balance,
                    rs.getInt("access_level"), rs.getString("admin_code"));
        } else if ("SELLER".equalsIgnoreCase(role)) {
            return new Seller(id, username, password, fullName, email, balance,
                    rs.getString("store_name"), rs.getDouble("rating"));
        } else {
            // Mặc định là BIDDER
            return new Bidder(id, username, password, fullName, email, balance,
                    rs.getString("shipping_address"), rs.getString("phone_number"));
        }
    }
}