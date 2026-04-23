package com.team4.server;

import com.team4.db.DatabaseManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper {

    public static Connection getConnection() throws SQLException {
        return DatabaseManager.getInstance().getConnection();
    }

    public void insertUser(String username, String password, String role) {
        String query = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";
        try {
            PreparedStatement pstmt = getConnection().prepareStatement(query);
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setString(3, role);
            pstmt.executeUpdate();
            pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void insertCategory(String name) {
        String query = "INSERT INTO categories (name) VALUES (?)";
        try {
            PreparedStatement pstmt = getConnection().prepareStatement(query);
            pstmt.setString(1, name);
            pstmt.executeUpdate();
            pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void insertItem(String name, int categoryId, double startPrice, String endTime, int sellerId, String status) {
        String query = "INSERT INTO items (name, category_id, start_price, end_time, seller_id, status) VALUES (?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement pstmt = getConnection().prepareStatement(query);
            pstmt.setString(1, name);
            pstmt.setInt(2, categoryId);
            pstmt.setDouble(3, startPrice);
            pstmt.setString(4, endTime);
            pstmt.setInt(5, sellerId);
            pstmt.setString(6, status);
            pstmt.executeUpdate();
            pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void insertBid(int itemId, int bidderId, double bidAmount) {
        String query = "INSERT INTO bids (item_id, bidder_id, bid_amount) VALUES (?, ?, ?)";
        try {
            PreparedStatement pstmt = getConnection().prepareStatement(query);
            pstmt.setInt(1, itemId);
            pstmt.setInt(2, bidderId);
            pstmt.setDouble(3, bidAmount);
            pstmt.executeUpdate();
            pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String[] verifyLogin(String username, String password) {
        String query = "SELECT id, role FROM users WHERE username = ? AND password = ?";
        try {
            PreparedStatement pstmt = getConnection().prepareStatement(query);
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String id = String.valueOf(rs.getInt("id"));
                String role = rs.getString("role");
                rs.close();
                pstmt.close();
                return new String[]{id, role};
            }
            rs.close();
            pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<String[]> getAllUsers() {
        String query = "SELECT id, username, role FROM users";
        List<String[]> users = new ArrayList<>();
        try {
            PreparedStatement pstmt = getConnection().prepareStatement(query);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String id = String.valueOf(rs.getInt("id"));
                String username = rs.getString("username");
                String role = rs.getString("role");
                users.add(new String[]{id, username, role});
            }
            rs.close();
            pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }
}