package com.team4.model;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
/**
 * Abstract Item entity: fields, validation and light helpers only.
 * - Dùng BigDecimal cho tiền (scale = 2)
 * - Không chứa nghiệp vụ cập nhật nhiều bảng (AuctionService chịu trách nhiệm đó)
 */
public abstract class Item extends Entity {
    // Dùng enum cho category
    public enum ItemCategory {
        ART,
        ELECTRONICS,
        FASHION,
        VEHICLE,
        COLLECTIBLE
    }
    private static final int NAME_MAX = 255;
    private static final int DESC_MAX = 2000;
    private static final int OWNER_ID_MAX = 36;
    private String name;
    private String description;
    private BigDecimal startingPrice;
    private ItemCategory category;
    private String ownerId;

    // Constructor dùng cho đối tượng mới
    protected Item(String name, BigDecimal startingPrice, String description, ItemCategory category, String ownerId) {
        super(); // Entity() sinh id và createdAt
        this.name = normalizeName(name);
        this.description = normalizeDescription(description);
        this.startingPrice = money(startingPrice);
        this.category = category;
        this.ownerId = normalizeOwnerId(ownerId);
        validateBaseItem();
    }
    // Constructor dùng cho đối tượng đã tồn tại (đọc từ DB)
    protected Item(String id, LocalDateTime createdAt, String name,
                   BigDecimal startingPrice,
                   String description, ItemCategory category, String ownerId) {
        super(id, createdAt);
        this.name = normalizeName(name);
        this.description = normalizeDescription(description);
        this.startingPrice = money(startingPrice);
        this.category = category;
        this.ownerId = normalizeOwnerId(ownerId);
        validateBaseItem();
    }
    // Validate Item
    protected final void validateBaseItem() {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Tên mặt hàng không được để trống.");
        }
        if (name.length() > NAME_MAX) {
            throw new IllegalArgumentException("Tên mặt hàng không được vượt quá " + NAME_MAX + " ký tự.");
        }
        if (startingPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Giá khởi điểm không được âm.");
        }
        if (category == null) {
            throw new IllegalArgumentException("Category không được để trống.");
        }
        if (ownerId == null || ownerId.isEmpty()) {
            throw new IllegalArgumentException("OwnerId không được để trống.");
        }
        if (ownerId.length() > OWNER_ID_MAX) {
            throw new IllegalArgumentException("OwnerId không được dài quá " + OWNER_ID_MAX + " ký tự.");
        }
        if (description == null || description.isEmpty()) {
            throw new IllegalArgumentException("Mô tả không được để trống.");
        }
        if (description.length() > DESC_MAX) {
            throw new IllegalArgumentException("Mô tả không được vượt quá " + DESC_MAX + " ký tự.");
        }
    }

    // Getter/setter
    public String getName() { return name; }
    // Đổi tên sp
    public void setName(String name) {
        this.name = normalizeName(name);
        validateBaseItem();
    }
    public String getDescription() { return description; }
    // Đổi mô tả
    public void setDescription(String description) {
        this.description = normalizeDescription(description);
        validateBaseItem();
    }
    public BigDecimal getStartingPrice() { return startingPrice; }
    public ItemCategory getCategory() { return category; }
    public void setCategory(ItemCategory category) {
        this.category = category;
        validateBaseItem();
    }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) {
        this.ownerId = normalizeOwnerId(ownerId);
        validateBaseItem();
    }
    // Phương để mô tả, không gắn UI Console
    // Item này là gì về mặt nghiệp vụ
    public abstract String summary();

    // Các phương thức chuẩn hóa
    private static BigDecimal money(BigDecimal amount) {
        if (amount == null) throw new IllegalArgumentException("Amount không được null.");
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
    private static String normalizeName(String name) {
        if (name == null) throw new IllegalArgumentException("Tên không được null.");
        String t = name.trim();
        return t.isEmpty() ? null : t;
    }
    private static String normalizeDescription(String desc) {
        if (desc == null) return null;
        String t = desc.trim();
        return t.isEmpty() ? null : t;
    }
    private static String normalizeOwnerId(String ownerId) {
        if (ownerId == null) throw new IllegalArgumentException("OwnerId không được null.");
        return ownerId.trim();
    }
    // Item này là gì về mặt kỹ thuật
    @Override
    public String toString() {
        return "Item: " +
                " | id: " + getId() +
                " | name: " + name +
                " | startingPrice: " + startingPrice +
                " | category: " + category +
                " | ownerId: " + ownerId +
                " | createdAt: " + getCreatedAt();
    }
    protected static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}