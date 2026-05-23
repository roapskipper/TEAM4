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
    private static final long serialVersionUID = 1L;
    // Dùng enum cho category
    public enum ItemCategory {
        ART,
        ELECTRONICS,
        FASHION,
        VEHICLE,
        COLLECTIBLE
    }
    public static final int NAME_MAX_LENGTH = 255;
    public static final int DESCRIPTION_MAX_LENGTH = 2000;
    private static final int OWNER_ID_MAX = 36;

    /** Shared validation messages for Item entity and ItemService pre-checks. */
    public static final class ValidationMessages {
        public static final String NAME_REQUIRED = "Item name is required.";
        public static final String NAME_TOO_LONG =
                "Item name must not exceed " + NAME_MAX_LENGTH + " characters.";
        public static final String STARTING_PRICE_REQUIRED = "Starting price is required.";
        public static final String STARTING_PRICE_NON_NEGATIVE =
                "Starting price must be zero or greater.";
        public static final String CATEGORY_REQUIRED = "Category is required.";
        public static final String DESCRIPTION_REQUIRED = "Description is required.";
        public static final String DESCRIPTION_TOO_LONG =
                "Description must not exceed " + DESCRIPTION_MAX_LENGTH + " characters.";
        public static final String OWNER_ID_REQUIRED = "Owner id is required.";
        public static final String OWNER_ID_TOO_LONG =
                "Owner id must not exceed " + OWNER_ID_MAX + " characters.";

        private ValidationMessages() {}
    }
    private String name;
    private String description;
    private BigDecimal startingPrice;
    private ItemCategory category;
    private String ownerId;
    private String status = "PENDING";

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

    /**
     * Validates common item fields from API/request payloads before entity construction.
     */
    public static void validateCommonFields(
            String name,
            BigDecimal startingPrice,
            String description,
            ItemCategory category,
            String ownerId) {
        String trimmedName = name == null ? null : name.trim();
        if (trimmedName == null || trimmedName.isEmpty()) {
            throw new IllegalArgumentException(ValidationMessages.NAME_REQUIRED);
        }
        if (trimmedName.length() > NAME_MAX_LENGTH) {
            throw new IllegalArgumentException(ValidationMessages.NAME_TOO_LONG);
        }
        if (startingPrice == null) {
            throw new IllegalArgumentException(ValidationMessages.STARTING_PRICE_REQUIRED);
        }
        if (startingPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(ValidationMessages.STARTING_PRICE_NON_NEGATIVE);
        }
        if (category == null) {
            throw new IllegalArgumentException(ValidationMessages.CATEGORY_REQUIRED);
        }
        String trimmedOwnerId = ownerId == null ? null : ownerId.trim();
        if (trimmedOwnerId == null || trimmedOwnerId.isEmpty()) {
            throw new IllegalArgumentException(ValidationMessages.OWNER_ID_REQUIRED);
        }
        if (trimmedOwnerId.length() > OWNER_ID_MAX) {
            throw new IllegalArgumentException(ValidationMessages.OWNER_ID_TOO_LONG);
        }
        String trimmedDescription = description == null ? null : description.trim();
        if (trimmedDescription == null || trimmedDescription.isEmpty()) {
            throw new IllegalArgumentException(ValidationMessages.DESCRIPTION_REQUIRED);
        }
        if (trimmedDescription.length() > DESCRIPTION_MAX_LENGTH) {
            throw new IllegalArgumentException(ValidationMessages.DESCRIPTION_TOO_LONG);
        }
    }

    // Validate Item
    protected final void validateBaseItem() {
        validateCommonFields(name, startingPrice, description, category, ownerId);
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
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    // Phương để mô tả, không gắn UI Console
    // Item này là gì về mặt nghiệp vụ
    public abstract String summary();

    // Các phương thức chuẩn hóa
    public static BigDecimal money(BigDecimal amount) {
        if (amount == null) throw new IllegalArgumentException("Amount must not be null.");
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
    private static String normalizeName(String name) {
        if (name == null) throw new IllegalArgumentException(ValidationMessages.NAME_REQUIRED);
        String t = name.trim();
        return t.isEmpty() ? null : t;
    }
    private static String normalizeDescription(String desc) {
        if (desc == null) return null;
        String t = desc.trim();
        return t.isEmpty() ? null : t;
    }
    private static String normalizeOwnerId(String ownerId) {
        if (ownerId == null) throw new IllegalArgumentException(ValidationMessages.OWNER_ID_REQUIRED);
        String t = ownerId.trim();
        if (t.isEmpty()) throw new IllegalArgumentException(ValidationMessages.OWNER_ID_REQUIRED);
        return t;
    }
    // Item này là gì về mặt kỹ thuật
    @Override
    public String toString() {
        return "Item: " +
                " | id: " + getId() +
                " | name: " + name +
                " | startingPrice: " + (startingPrice==null ? "n/a" : startingPrice.toPlainString()) +
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

    /**
     * Normalizes a string value. If the string is null or blank, returns the default value.
     */
    protected static String normalizeDefaultString(String value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? defaultValue : trimmed;
    }
}
