package com.team4.dao.impl;

import com.team4.dao.ItemDAO;
import com.team4.db.DatabaseManager;
import com.team4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;
import java.math.BigDecimal;

public class ItemDAOImpl implements ItemDAO {
    private static final Logger logger = LoggerFactory.getLogger(ItemDAOImpl.class);

    /**
     * Hàm này đọc 1 dòng ResultSet và quyết định tạo ra Object gì.
     */
    private Item mapRowToItem(ResultSet rs) throws SQLException {
        // Lấy các trường chung của Item
        String id = rs.getString("id");
        LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();
        String name = rs.getString("name");
        String description = rs.getString("description");
        BigDecimal startingPrice = rs.getBigDecimal("starting_price");
        String category = rs.getString("category");
        String ownerId = rs.getString("owner_id");

        // Dùng Switch-Case kết hợp Factory để trả về đúng Class con
        switch (Item.ItemCategory.valueOf(category)) {
            case ART:
                return new Art(id, createdAt, name, startingPrice, description, ownerId,
                        rs.getString("artist"), rs.getInt("creation_year"), Art.fromName(rs.getString("medium")),
                        rs.getString("dimensions"));

            case COLLECTIBLE:
                return new Collectible(id, createdAt, name, startingPrice, description, ownerId,
                        rs.getInt("year_of_origin"), Collectible.fromNameR(rs.getString("rarity_level")),Collectible.fromNameCon(rs.getString("condition_grade")),
                        rs.getBoolean("has_certificate"),
                        rs.getString("origin"));

            case ELECTRONICS:
                return new Electronics(id, createdAt, name, startingPrice, description, ownerId,
                        rs.getString("brand"), rs.getString("model"),
                        Electronics.fromNameCon(rs.getString("condition_grade")), rs.getInt("warranty_months"),
                        rs.getBoolean("fully_functional"));

            case FASHION:
                return new Fashion(id, createdAt, name, startingPrice, description, ownerId,
                        rs.getString("brand"), Fashion.fromNameSize(rs.getString("size")), rs.getString("material"),
                        rs.getString("color"), Fashion.fromNameGender(rs.getString("gender")), Fashion.fromNameCon(rs.getString("condition_grade")),
                        rs.getBoolean("authentic"));

            case VEHICLE:
                return new Vehicle(id, createdAt, name, startingPrice, description, ownerId,
                        rs.getString("brand"), rs.getString("model"), rs.getInt("manufacturing_year"),
                        rs.getInt("odo"), Vehicle.fromNameEng(rs.getString("engine_type")), rs.getString("color")
                        , rs.getBoolean("has_legal_papers"), Vehicle.fromNameTran(rs.getString("transmission")));

            default:
                throw new SQLException("Lỗi hệ thống: Category không hợp lệ -> " + category);
        }
    }

    /**
     * CRUD
     */
    @Override
    /**
     * 1. findById() - tìm theo id
     * Dùng khi: xem chi tiết sản phẩm
     * Ví dụ: click vào item → load thông tin
     */
    public Item findById(String id) {
        String sql = "SELECT * FROM items WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapRowToItem(rs);
            }
        } catch (SQLException e) {
            logger.error("Không thể tìm item với id={}", id, e);
        } catch (Exception e) {
            // Bắt IllegalArgumentException từ constructor subclass (VD: condition trống)
            logger.warn("Item id={} có dữ liệu không hợp lệ, bỏ qua: {}", id, e.getMessage());
        }
        return null;
    }

    @Override
    /**
     * 2. findAll() - lấy tất cả
     * Dùng khi: hiển thị trang chủ, admin quản lý
     * Ví dụ: danh sách tất cả sản phẩm đang đấu giá
     */
    public List<Item> findAll() {
        String sql = "SELECT * FROM items ORDER BY created_at DESC";
        return executeQueryList(sql);
    }

    @Override
    /**
     * 3. findByCategory() - tìm theo danh mục
     * Dùng khi: người mua lọc theo loại
     * Ví dụ: chỉ xem Electronics, Fashion...
     */
    public List<Item> findByCategory(String category) {
        String sql = "SELECT * FROM items WHERE category = ? ORDER BY created_at DESC";
        return executeQueryListWithParam(sql, category);
    }

    @Override
    /**
     * 4. findByOwnerId() - tìm theo người bán
     * Dùng khi: seller xem sản phẩm của mình
     * Ví dụ: màn hình quản lý của Seller
     */
    public List<Item> findByOwnerId(String ownerId) {
        // sắp xếp theo thời gian tạo giảm dần (mới nhất lên trước)
        String sql = "SELECT * FROM items WHERE owner_id = ? ORDER BY created_at DESC";
        return executeQueryListWithParam(sql, ownerId);
    }

    @Override
    public boolean insert(Item item) {
        // Lưu ý: Trong Single Table Inheritance, bảng sẽ có rất nhiều cột.
        // Ta set các cột không liên quan thành NULL.
        String sql = "INSERT INTO items (id, created_at, name, description, starting_price, category, owner_id, " +
                "brand, model, color, condition_grade, " +
                "artist, creation_year, medium, dimensions, " +
                "year_of_origin, rarity_level, has_certificate, origin, " +
                "warranty_months, fully_functional, " +
                "size, material, gender, authentic, " +
                "manufacturing_year, odo, engine_type, has_legal_papers, transmission) " +
                "VALUES (?,?,?,?,?,?,?, ?,?,?,?, ?,?,?,?,?,?, ?,?,?,?,?,?,?, ?,?,?,?, ?,?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // 1-7: Cột chung của Item
            stmt.setString(1, item.getId());
            stmt.setObject(2, item.getCreatedAt());
            stmt.setString(3, item.getName());
            stmt.setString(4, item.getDescription());
            stmt.setBigDecimal(5, item.getStartingPrice());
            stmt.setString(6, item.getCategory().name()); // từ enum về String
            stmt.setString(7, item.getOwnerId());

            for (int i = 8; i <= 30; i++) {
                stmt.setNull(i, Types.VARCHAR); // Dùng Varchar đại diện, DB sẽ tự ép kiểu sang NULL hợp lệ
            }

            // Ghi đè các cột dựa theo Class con (instanceof)
            if (item instanceof Art) {
                Art art = (Art) item;
                stmt.setString(12, art.getArtist());
                stmt.setInt(13, art.getCreationYear());
                stmt.setString(14, art.getMedium().name());
                stmt.setString(15, art.getDimensions());
            }
            else if (item instanceof Collectible) {
                Collectible c = (Collectible) item;
                stmt.setInt(16, c.getYearOfOrigin());
                stmt.setString(17, c.getRarityLevel().name());
                stmt.setBoolean(18, c.isHasCertificate());
                stmt.setString(19, c.getOrigin());
                stmt.setString(11,c.getConditionGrade().name());
            }
            else if (item instanceof Electronics) {
                Electronics e = (Electronics) item;
                stmt.setString(8, e.getBrand());
                stmt.setString(9, e.getModel());
                stmt.setInt(20, e.getWarrantyMonths());
                stmt.setBoolean(21, e.isFullyFunctional());
                stmt.setString(11,e.getItemCondition().name());
            }
            else if (item instanceof Fashion) {
                Fashion f = (Fashion) item;
                stmt.setString(8, f.getBrand());
                stmt.setString(10, f.getColor());
                stmt.setString(11, f.getCondition().name());
                stmt.setString(22, f.getSize().name());
                stmt.setString(23, f.getMaterial());
                stmt.setString(24, f.getGender().name());
                stmt.setBoolean(25, f.isAuthentic());
            }
            else if (item instanceof Vehicle) {
                Vehicle v = (Vehicle) item;
                stmt.setString(8, v.getBrand());
                stmt.setString(9, v.getModel());
                stmt.setString(10, v.getColor());
                stmt.setInt(26, v.getManufacturingYear());
                stmt.setInt(27, v.getOdo());
                stmt.setString(28, v.getEngineType().name());
                stmt.setBoolean(29, v.hasLegalPapers());
                stmt.setString(30, v.getTransmission().name());
            }

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            logger.error("Không thể tạo item id={}", item.getId(), e);
            return false;
        }
    }

    @Override
    public boolean update(Item item) {
        //  Thường người ta chỉ cho phép update thông tin chung
        String sql = "UPDATE items SET name = ?, description = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, item.getName());
            stmt.setString(2, item.getDescription());
            stmt.setString(3, item.getId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Không thể update item id={}", item.getId(), e);
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
            logger.error("Không thể xóa item id={}", id, e);
            return false;
        }
    }

    /** HELPER METHOD
     * Tránh lặp code khi có nhiều method query trả về List
     */

    private List<Item> executeQueryList(String sql) {
        List<Item> items = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                try {
                    items.add(mapRowToItem(rs));
                } catch (Exception ex) {
                    logger.warn("Skipping malformed item in DB: {}", ex.getMessage());
                }
            }
        } catch (SQLException e) {
            logger.error("Cannot execute item list query", e);
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
                    try {
                        items.add(mapRowToItem(rs));
                    } catch (Exception ex) {
                        logger.warn("Skipping malformed item in DB: {}", ex.getMessage());
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Cannot execute item list query with param={}", param, e);
        }
        return items;
    }
}
