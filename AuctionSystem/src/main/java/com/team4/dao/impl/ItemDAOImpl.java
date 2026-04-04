package com.team4.dao.impl;

import com.team4.dao.BaseDAO;
import com.team4.db.DatabaseManager;
import com.team4.model.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ItemDAOImpl implements BaseDAO<Item> {

    @Override
    public boolean save(Item item) {
        String sql = "INSERT INTO items (id, name, description, starting_price, current_price, category, owner_id, " +
                "brand, model, manufacturing_year, odo, artist, creation_year, size, is_authentic, " +
                "serial_number, warranty_months, tech_spec, rarity_level, has_certificate, origin) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // 1. Gán các trường chung
            pstmt.setString(1, item.getId());
            pstmt.setString(2, item.getName());
            pstmt.setString(3, item.getDescription());
            pstmt.setDouble(4, item.getStartingPrice());
            pstmt.setDouble(5, item.getCurrentPrice());
            pstmt.setString(6, item.getCategory());
            pstmt.setString(7, item.getOwnerId());

            // 2. Sử dụng Pattern Matching (instanceof) để gán các trường đặc thù
            setSpecificParameters(pstmt, item);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void setSpecificParameters(PreparedStatement pstmt, Item item) throws SQLException {
        // Đặt mặc định tất cả là null từ vị trí số 8 đến 21
        for (int i = 8; i <= 21; i++) pstmt.setNull(i, Types.VARCHAR);

        if (item instanceof Vehicle v) {
            pstmt.setString(8, v.getBrand());
            pstmt.setString(9, v.getModel());
            pstmt.setInt(10, v.getManufacturingYear());
            pstmt.setInt(11, v.getOdo());
            pstmt.setString(21, "Vehicle Transmission: " + v.getTransmission());
        } else if (item instanceof Art a) {
            pstmt.setString(12, a.getArtist());
            pstmt.setInt(13, a.getCreationYear());
            pstmt.setString(21, a.getMedium());
        } else if (item instanceof Fashion f) {
            pstmt.setString(8, f.getBrand());
            pstmt.setString(14, f.getSize());
            pstmt.setBoolean(15, f.isAuthentic());
        } else if (item instanceof Electronics e) {
            pstmt.setString(8, e.getBrand());
            pstmt.setString(9, e.getModel());
            pstmt.setString(16, e.getSerialNumber());
            pstmt.setInt(17, e.getWarrantyMonths());
            pstmt.setString(18, e.getTechnicalSpec());
        } else if (item instanceof Collectible c) {
            pstmt.setInt(13, c.getYearOfOrigin());
            pstmt.setString(19, c.getRarityLevel());
            pstmt.setBoolean(20, c.isHasCertificate());
            pstmt.setString(21, c.getOrigin());
        }
    }

    @Override
    public List<Item> findAll() {
        List<Item> items = new ArrayList<>();
        String sql = "SELECT * FROM items";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                items.add(mapResultSetToItem(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return items;
    }

    @Override
    public Optional<Item> findById(String id) {
        String sql = "SELECT * FROM items WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapResultSetToItem(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return Optional.empty();
    }

    private Item mapResultSetToItem(ResultSet rs) throws SQLException {
        String category = rs.getString("category");
        String id = rs.getString("id");
        String name = rs.getString("name");
        double startP = rs.getDouble("starting_price");
        double currP = rs.getDouble("current_price");
        String desc = rs.getString("description");
        String owner = rs.getString("owner_id");

        return switch (category) {
            case "ART" -> new Art(id, name, startP, currP, desc, owner, rs.getString("artist"), rs.getInt("creation_year"), "Medium from DB", "Dims from DB", "Style DB", true, "Gallery");
            case "VEHICLE" -> new Vehicle(id, name, startP, currP, desc, owner, rs.getString("brand"), rs.getString("model"), rs.getInt("manufacturing_year"), rs.getInt("odo"), "Fuel", "Color", "Plate", true, "Auto");
            case "FASHION" -> new Fashion(id, name, startP, currP, desc, owner, rs.getString("brand"), rs.getString("size"), "Material", "Color", "Gender", "Condition", rs.getBoolean("is_authentic"));
            case "ELECTRONICS" -> new Electronics(id, name, startP, currP, desc, owner, rs.getString("brand"), rs.getString("model"), rs.getString("serial_number"), "Color", "Condition", rs.getInt("warranty_months"), true, rs.getString("tech_spec"));
            case "COLLECTIBLE" -> new Collectible(id, name, startP, currP, desc, owner, rs.getInt("creation_year"), rs.getString("rarity_level"), "Grade", rs.getBoolean("has_certificate"), rs.getString("origin"), "Special features");
            default -> throw new IllegalStateException("Unknown category: " + category);
        };
    }

    @Override public boolean update(Item item) { return false; /* Implement similar to UserDAO if needed */ }
    @Override public boolean delete(String id) {
        String sql = "DELETE FROM items WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }
}