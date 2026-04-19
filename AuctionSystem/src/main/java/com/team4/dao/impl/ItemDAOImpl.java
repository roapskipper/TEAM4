package com.team4.dao.impl;

import com.team4.dao.ItemDAO;
import com.team4.db.DatabaseManager;
import com.team4.model.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ItemDAOImpl implements ItemDAO {

    /**
     * PHẦN 1: FACTORY METHOD (Rất quan trọng để lấy điểm Design Pattern)
     * Hàm này đọc 1 dòng ResultSet và quyết định tạo ra Object gì.
     */
    private Item mapRowToItem(ResultSet rs) throws SQLException {
        // Lấy các trường chung của class Item
        String id = rs.getString("id");
        String name = rs.getString("name");
        String description = rs.getString("description");
        double startingPrice = rs.getDouble("starting_price");
        double currentPrice = rs.getDouble("current_price");
        String category = rs.getString("category");
        String ownerId = rs.getString("owner_id");

        // Dùng Switch-Case kết hợp Factory để trả về đúng Class con
        switch (category.toUpperCase()) {
            case "ART":
                return new Art(id, name, startingPrice, currentPrice, description, ownerId,
                        rs.getString("artist"), rs.getInt("creation_year"), rs.getString("medium"),
                        rs.getString("dimensions"), rs.getString("style"), rs.getBoolean("is_original"),
                        rs.getString("exhibition_history"));

            case "COLLECTIBLE":
                return new Collectible(id, name, startingPrice, currentPrice, description, ownerId,
                        rs.getInt("year_of_origin"), rs.getString("rarity_level"), rs.getString("condition_grade"),
                        rs.getString("category_specific"), rs.getBoolean("has_certificate"),
                        rs.getString("origin"), rs.getString("special_features"));

            case "ELECTRONICS":
                return new Electronics(id, name, startingPrice, currentPrice, description, ownerId,
                        rs.getString("brand"), rs.getString("model"), rs.getString("serial_number"),
                        rs.getString("color"), rs.getString("item_condition"), rs.getInt("warranty_months"),
                        rs.getBoolean("is_fully_functional"), rs.getString("technical_spec"));

            case "FASHION":
                return new Fashion(id, name, startingPrice, currentPrice, description, ownerId,
                        rs.getString("brand"), rs.getString("size"), rs.getString("material"),
                        rs.getString("color"), rs.getString("gender"), rs.getString("item_condition"),
                        rs.getBoolean("is_authentic"));

            case "VEHICLE":
                return new Vehicle(id, name, startingPrice, currentPrice, description, ownerId,
                        rs.getString("brand"), rs.getString("model"), rs.getInt("manufacturing_year"),
                        rs.getInt("odo"), rs.getString("engine_type"), rs.getString("color"),
                        rs.getString("license_plate"), rs.getBoolean("has_legal_papers"), rs.getString("transmission"));

            default:
                throw new SQLException("Lỗi hệ thống: Category không hợp lệ -> " + category);
        }
    }

    /**
     * PHẦN 2: CÁC HÀM SELECT (TÌM KIẾM)
     */
    @Override
    public Item findById(String id) {
        String sql = "SELECT * FROM items WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapRowToItem(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Item> findAll() {
        return executeQueryList("SELECT * FROM items ORDER BY created_at DESC");
    }

    @Override
    public List<Item> findByCategory(String category) {
        String sql = "SELECT * FROM items WHERE category = ? ORDER BY created_at DESC";
        return executeQueryListWithParam(sql, category);
    }

    @Override
    public List<Item> findByOwnerId(String ownerId) {
        String sql = "SELECT * FROM items WHERE owner_id = ? ORDER BY created_at DESC";
        return executeQueryListWithParam(sql, ownerId);
    }

    /**
     * PHẦN 3: HÀM THÊM MỚI (INSERT) - Áp dụng Đa hình (Polymorphism)
     */
    @Override
    public boolean insert(Item item) {
        // Lưu ý: Trong Single Table Inheritance, bảng sẽ có rất nhiều cột.
        // Ta set các cột không liên quan thành NULL.
        String sql = "INSERT INTO items (id, name, description, starting_price, current_price, category, owner_id, " +
                "brand, model, color, item_condition, " +
                "artist, creation_year, medium, dimensions, style, is_original, exhibition_history, " +
                "year_of_origin, rarity_level, condition_grade, category_specific, has_certificate, origin, special_features, " +
                "serial_number, warranty_months, is_fully_functional, technical_spec, " +
                "size, material, gender, is_authentic, " +
                "manufacturing_year, odo, engine_type, license_plate, has_legal_papers, transmission) " +
                "VALUES (?,?,?,?,?,?,?, ?,?,?,?, ?,?,?,?,?,?, ?,?,?,?,?,?,?, ?,?,?,?, ?,?,?,?, ?,?,?,?,?,?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // 1-7: Cột chung của Item
            stmt.setString(1, item.getId());
            stmt.setString(2, item.getName());
            stmt.setString(3, item.getDescription());
            stmt.setDouble(4, item.getStartingPrice());
            stmt.setDouble(5, item.getCurrentPrice());
            stmt.setString(6, item.getCategory());
            stmt.setString(7, item.getOwnerId());

            // Đặt toàn bộ 32 cột đặc thù thành NULL trước
            for (int i = 8; i <= 39; i++) {
                stmt.setNull(i, Types.VARCHAR); // Dùng Varchar đại diện, DB sẽ tự ép kiểu sang NULL hợp lệ
            }

            // Ghi đè các cột dựa theo Class con (instanceof)
            if (item instanceof Art) {
                Art art = (Art) item;
                stmt.setString(12, art.getArtist());
                stmt.setInt(13, art.getCreationYear());
                stmt.setString(14, art.getMedium());
                stmt.setString(15, art.getDimensions());
                stmt.setString(16, art.getStyle());
                stmt.setBoolean(17, art.isOriginal());
                stmt.setString(18, art.getExhibitionHistory());
            }
            else if (item instanceof Collectible) {
                Collectible c = (Collectible) item;
                stmt.setInt(19, c.getYearOfOrigin());
                stmt.setString(20, c.getRarityLevel());
                stmt.setString(21, c.getConditionGrade());
                stmt.setString(22, c.getCategorySpecific());
                stmt.setBoolean(23, c.isHasCertificate());
                stmt.setString(24, c.getOrigin());
                stmt.setString(25, c.getSpecialFeatures());
            }
            else if (item instanceof Electronics) {
                Electronics e = (Electronics) item;
                stmt.setString(8, e.getBrand());
                stmt.setString(9, e.getModel());
                stmt.setString(10, e.getColor());
                stmt.setString(11, e.getCondition());
                stmt.setString(26, e.getSerialNumber());
                stmt.setInt(27, e.getWarrantyMonths());
                stmt.setBoolean(28, e.isFullyFunctional());
                stmt.setString(29, e.getTechnicalSpec());
            }
            else if (item instanceof Fashion) {
                Fashion f = (Fashion) item;
                stmt.setString(8, f.getBrand());
                stmt.setString(10, f.getColor());
                stmt.setString(11, f.getCondition());
                stmt.setString(30, f.getSize());
                stmt.setString(31, f.getMaterial());
                stmt.setString(32, f.getGender());
                stmt.setBoolean(33, f.isAuthentic());
            }
            else if (item instanceof Vehicle) {
                Vehicle v = (Vehicle) item;
                stmt.setString(8, v.getBrand());
                stmt.setString(9, v.getModel());
                stmt.setString(10, v.getColor());
                stmt.setInt(34, v.getManufacturingYear());
                stmt.setInt(35, v.getOdo());
                stmt.setString(36, v.getEngineType());
                stmt.setString(37, v.getLicensePlate());
                stmt.setBoolean(38, v.isHasLegalPapers());
                stmt.setString(39, v.getTransmission());
            }

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean update(Item item) {
        // Đơn giản hóa: Thường người ta chỉ cho phép update thông tin chung hoặc giá hiện tại
        String sql = "UPDATE items SET name = ?, description = ?, current_price = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, item.getName());
            stmt.setString(2, item.getDescription());
            stmt.setDouble(3, item.getCurrentPrice());
            stmt.setString(4, item.getId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean delete(String id) {
        String sql = "DELETE FROM items WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // --- CÁC HÀM TIỆN ÍCH (HELPER METHODS) ---

    private List<Item> executeQueryList(String sql) {
        List<Item> items = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                items.add(mapRowToItem(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }

    private List<Item> executeQueryListWithParam(String sql, String param) {
        List<Item> items = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, param);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    items.add(mapRowToItem(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }
}