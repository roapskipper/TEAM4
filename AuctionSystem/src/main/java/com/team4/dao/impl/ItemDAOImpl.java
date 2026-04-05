package com.team4.dao.impl;

import com.team4.dao.BaseDAO;
import com.team4.db.DatabaseManager;
import com.team4.model.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * ItemDAOImpl - Triển khai lưu trữ Đa hình cho 5 loại mặt hàng.
 * Áp dụng Design Pattern: Data Access Object (DAO).
 */
public class ItemDAOImpl implements BaseDAO<Item> {

    // --- 1. LƯU MỚI VẬT PHẨM ---
    @Override
    public boolean save(Item item) {
        String sql = "INSERT INTO items (id, name, description, starting_price, current_price, category, owner_id, " +
                "brand, model, manufacturing_year, odo, artist, creation_year, size, is_authentic, " +
                "serial_number, warranty_months, tech_spec, rarity_level, has_certificate, origin) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Tham số chung (1-7)
            setCommonParameters(pstmt, item);
            // Tham số riêng (8-21)
            setSpecificParameters(pstmt, item);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // --- 2. CẬP NHẬT VẬT PHẨM  ---
    @Override
    public boolean update(Item item) {
        // Cập nhật giá thầu hiện tại và thông tin chung là nghiệp vụ hay dùng nhất
        String sql = "UPDATE items SET name = ?, description = ?, current_price = ?, " +
                "odo = ?, is_authentic = ?, condition_status = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, item.getName());
            pstmt.setString(2, item.getDescription());
            pstmt.setDouble(3, item.getCurrentPrice());

            // Các trường có thể thay đổi trong quá trình đấu giá hoặc bảo quản
            if (item instanceof Vehicle v) pstmt.setInt(4, v.getOdo());
            else pstmt.setNull(4, Types.INTEGER);

            if (item instanceof Fashion f) {
                pstmt.setBoolean(5, f.isAuthentic());
                pstmt.setString(6, f.getCondition());
            } else {
                pstmt.setNull(5, Types.BOOLEAN);
                pstmt.setNull(6, Types.VARCHAR);
            }

            pstmt.setString(7, item.getId());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // --- 3. XÓA VẬT PHẨM ---
    @Override
    public boolean delete(String id) {
        String sql = "DELETE FROM items WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    // --- 4. TÌM KIẾM THEO ID (CÓ LẤY DỮ LIỆU ĐẶC THÙ) ---
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

    @Override
    public List<Item> findAll() {
        List<Item> items = new ArrayList<>();
        String sql = "SELECT * FROM items ORDER BY current_price DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) items.add(mapResultSetToItem(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return items;
    }

    // --- HÀM PHỤ TRỢ (HELPER METHODS) ---

    private void setCommonParameters(PreparedStatement pstmt, Item item) throws SQLException {
        pstmt.setString(1, item.getId());
        pstmt.setString(2, item.getName());
        pstmt.setString(3, item.getDescription());
        pstmt.setDouble(4, item.getStartingPrice());
        pstmt.setDouble(5, item.getCurrentPrice());
        pstmt.setString(6, item.getCategory());
        pstmt.setString(7, item.getOwnerId());
    }

    private void setSpecificParameters(PreparedStatement pstmt, Item item) throws SQLException {
        // Clear rác trước khi set
        for (int i = 8; i <= 21; i++) pstmt.setNull(i, Types.VARCHAR);

        if (item instanceof Vehicle v) {
            pstmt.setString(8, v.getBrand());
            pstmt.setString(9, v.getModel());
            pstmt.setInt(10, v.getManufacturingYear());
            pstmt.setInt(11, v.getOdo());
        } else if (item instanceof Art a) {
            pstmt.setString(12, a.getArtist());
            pstmt.setInt(13, a.getCreationYear());
        } else if (item instanceof Fashion f) {
            pstmt.setString(8, f.getBrand());
            pstmt.setString(14, f.getSize());
            pstmt.setBoolean(15, f.isAuthentic());
        } else if (item instanceof Electronics e) {
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

    private Item mapResultSetToItem(ResultSet rs) throws SQLException {
        String cat = rs.getString("category");
        String id = rs.getString("id");
        String name = rs.getString("name");
        double start = rs.getDouble("starting_price");
        double current = rs.getDouble("current_price");
        String desc = rs.getString("description");
        String owner = rs.getString("owner_id");

        return switch (cat) {
            case "VEHICLE" -> new Vehicle(id, name, start, current, desc, owner, rs.getString("brand"),
                    rs.getString("model"), rs.getInt("manufacturing_year"), rs.getInt("odo"), "EngineTypeDB", "ColorDB", "PlateDB", true, rs.getString("transmission"));
            case "ART" -> new Art(id, name, start, current, desc, owner, rs.getString("artist"),
                    rs.getInt("creation_year"), "MediumDB", "DimsDB", "StyleDB", true, "ExhibDB");
            case "FASHION" -> new Fashion(id, name, start, current, desc, owner, rs.getString("brand"),
                    rs.getString("size"), "MaterialDB", "ColorDB", "GenderDB", "ConditionDB", rs.getBoolean("is_authentic"));
            case "ELECTRONICS" -> new Electronics(id, name, start, current, desc, owner, "BrandDB",
                    "ModelDB", rs.getString("serial_number"), "ColorDB", "ConditionDB", rs.getInt("warranty_months"), true, rs.getString("tech_spec"));
            case "COLLECTIBLE" -> new Collectible(id, name, start, current, desc, owner,
                    rs.getInt("creation_year"), rs.getString("rarity_level"), "GradeDB", rs.getBoolean("has_certificate"), rs.getString("origin"), "SpecialDB");
            default -> throw new IllegalStateException("Unknown category: " + cat);
        };
    }
}