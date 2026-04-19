package com.team4.dao.impl;

import com.team4.dao.UserDAO;
import com.team4.db.DatabaseManager;
import com.team4.model.Admin;
import com.team4.model.Bidder;
import com.team4.model.Seller;
import com.team4.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAOImpl implements UserDAO {

    /**
     * PHẦN 1: FACTORY METHOD (Ánh xạ ResultSet thành Object đa hình)
     * Đọc 1 dòng từ DB và trả về đúng kiểu Admin, Bidder hoặc Seller
     */
    private User mapRowToUser(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String username = rs.getString("username");
        String password = rs.getString("password");
        String fullName = rs.getString("full_name");
        String email = rs.getString("email");
        String role = rs.getString("role");
        double balance = rs.getDouble("balance");

        // Dựa vào ROLE để khởi tạo đúng Class con (Polymorphism)
        switch (role.toUpperCase()) {
            case "ADMIN":
                int accessLevel = rs.getInt("access_level");
                String adminCode = rs.getString("admin_code");
                return new Admin(id, username, password, fullName, email, balance, accessLevel, adminCode);

            case "BIDDER":
                String shippingAddress = rs.getString("shipping_address");
                String phoneNumber = rs.getString("phone_number");
                return new Bidder(id, username, password, fullName, email, balance, shippingAddress, phoneNumber);

            case "SELLER":
                String storeName = rs.getString("store_name");
                double rating = rs.getDouble("rating");
                return new Seller(id, username, password, fullName, email, balance, storeName, rating);

            default:
                throw new SQLException("Lỗi: Không nhận diện được role '" + role + "' của user ID: " + id);
        }
    }

    /**
     * PHẦN 2: CÁC PHƯƠNG THỨC CRUD
     */

    @Override
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
            e.printStackTrace();
        }
        return null; // Không tìm thấy
    }

    @Override
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
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean insert(User user) {
        String sql = "INSERT INTO users (id, username, password, full_name, email, role, balance, " +
                "access_level, admin_code, shipping_address, phone_number, store_name, rating) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Set các trường chung của User
            stmt.setString(1, user.getId());
            stmt.setString(2, user.getUsername());
            stmt.setString(3, user.getPassword());
            stmt.setString(4, user.getFullName());
            stmt.setString(5, user.getEmail());
            stmt.setString(6, user.getRole());
            stmt.setDouble(7, user.getBalance());

            // Set các trường riêng biệt (áp dụng tính đa hình - instanceOf)
            if (user instanceof Admin) {
                Admin admin = (Admin) user;
                stmt.setInt(8, admin.getAccessLevel());
                stmt.setString(9, admin.getAdminCode());
                stmt.setNull(10, Types.VARCHAR); // Các cột của role khác để NULL
                stmt.setNull(11, Types.VARCHAR);
                stmt.setNull(12, Types.VARCHAR);
                stmt.setNull(13, Types.DECIMAL);
            }
            else if (user instanceof Bidder) {
                Bidder bidder = (Bidder) user;
                stmt.setNull(8, Types.INTEGER);
                stmt.setNull(9, Types.VARCHAR);
                stmt.setString(10, bidder.getShippingAddress());
                stmt.setString(11, bidder.getPhoneNumber());
                stmt.setNull(12, Types.VARCHAR);
                stmt.setNull(13, Types.DECIMAL);
            }
            else if (user instanceof Seller) {
                Seller seller = (Seller) user;
                stmt.setNull(8, Types.INTEGER);
                stmt.setNull(9, Types.VARCHAR);
                stmt.setNull(10, Types.VARCHAR);
                stmt.setNull(11, Types.VARCHAR);
                stmt.setString(12, seller.getStoreName());
                stmt.setDouble(13, seller.getRating());
            }

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean update(User user) {
        // Tương tự Insert, nhưng dùng câu lệnh UPDATE
        String sql = "UPDATE users SET full_name = ?, email = ?, password = ?, balance = ?, " +
                "shipping_address = ?, phone_number = ?, store_name = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.getFullName());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getPassword());
            stmt.setDouble(4, user.getBalance());

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
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public List<User> findAll() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapRowToUser(rs)); // Gọi lại Factory Method
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}