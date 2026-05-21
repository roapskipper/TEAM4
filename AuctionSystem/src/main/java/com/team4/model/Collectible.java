package com.team4.model;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
/**
 * Collectible: Đại diện cho đồ sưu tập (Collectible).
 */
public class Collectible extends Item {
    public enum RarityLevel {
        COMMON,         // Phổ biến
        UNCOMMON,       // Ít phổ biến
        RARE,           // Hiếm
        VERY_RARE,      // Rất hiếm
        ULTRA_RARE,     // Cực hiếm
    }
    public static RarityLevel fromNameR(String name) {
        if (name == null) return null;
        try {
            return RarityLevel.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    public enum ConditionGrade {
        POOR,           // Kém
        FAIR,           // Trung bình
        GOOD,           // Tốt
        VERY_GOOD,      // Rất tốt
        EXCELLENT,      // Xuất sắc
        MINT            // Hoàn hảo (như mới)
    }
    public static ConditionGrade fromNameCon(String name) {
        if (name == null) return null;
        try {
            return ConditionGrade.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    private static final int ORIGIN_MAX_LENGTH = 120;
    private static final int YEAR_MIN = -3000;

    /** Shared validation messages for Collectible entity and ItemService pre-checks. */
    public static final class ValidationMessages {
        public static final String RARITY_REQUIRED = "Rarity level is required.";
        public static final String CONDITION_REQUIRED = "Condition grade is required.";
        public static final String YEAR_OF_ORIGIN_INVALID =
                "Year of origin must be between " + YEAR_MIN + " and the current year (use 0 if unknown).";
        public static final String ORIGIN_TOO_LONG =
                "Origin must not exceed " + ORIGIN_MAX_LENGTH + " characters.";

        private ValidationMessages() {}
    }

    private int yearOfOrigin;       // năm xuất xứ
    private RarityLevel rarityLevel;     // độ hiếm
    private ConditionGrade conditionGrade; // tình trạng
    private boolean hasCertificate;  // có chứng chỉ không
    private String origin;           // xuất xứ (quốc gia/vùng)

    /**
     * Validates required Collectible fields (rarity, condition) and optional year rules.
     */
    public static void validateCollectibleFields(
            RarityLevel rarityLevel,
            ConditionGrade conditionGrade,
            int yearOfOrigin,
            String origin) {
        validateRarityLevel(rarityLevel);
        validateConditionGrade(conditionGrade);
        validateYearOfOrigin(yearOfOrigin);
        if (origin != null) {
            validateOrigin(origin);
        }
    }

    private void validateCollectibleCategory() {
        validateCollectibleFields(rarityLevel, conditionGrade, yearOfOrigin, origin);
    }

    // Constructor dùng khi tạo Collectible mới (Seller đăng sản phẩm)
    public Collectible(String name,
                       BigDecimal startingPrice,
                       String description,
                       String ownerId,
                       int yearOfOrigin,
                       RarityLevel rarityLevel,
                       ConditionGrade conditionGrade,
                       boolean hasCertificate,
                       String origin) {
        super(name, startingPrice, description, ItemCategory.COLLECTIBLE, ownerId);
        this.yearOfOrigin = yearOfOrigin;
        this.rarityLevel = rarityLevel;
        this.conditionGrade = conditionGrade;
        this.hasCertificate = hasCertificate;
        this.origin = resolveOrigin(origin);
        validateCollectibleCategory();
    }

    // Constructor dùng khi nạp từ DB
    public Collectible(String id,
                       LocalDateTime createdAt,
                       String name,
                       BigDecimal startingPrice,
                       String description,
                       String ownerId,
                       int yearOfOrigin,
                       RarityLevel rarityLevel,
                       ConditionGrade conditionGrade,
                       boolean hasCertificate,
                       String origin) {
        super(id, createdAt, name, startingPrice, description, ItemCategory.COLLECTIBLE, ownerId);
        this.yearOfOrigin = yearOfOrigin;
        this.rarityLevel = rarityLevel;
        this.conditionGrade = conditionGrade;
        this.hasCertificate = hasCertificate;
        this.origin = resolveOrigin(origin);
        validateCollectibleCategory();
    }

    /**
     * Optional origin: blank values default to {@code Unknown}.
     */
    public static String resolveOrigin(String origin) {
        if (origin == null || origin.trim().isEmpty()) {
            return "Unknown";
        }
        return normalizeOptional(origin);
    }

    private static void validateYearOfOrigin(int year) {
        if (year == 0) return; // unknown
        int current = LocalDate.now().getYear();
        if (year < YEAR_MIN || year > current) {
            throw new IllegalArgumentException(ValidationMessages.YEAR_OF_ORIGIN_INVALID);
        }
    }
    private static void validateRarityLevel(RarityLevel rarity) {
        if (rarity == null) {
            throw new IllegalArgumentException(ValidationMessages.RARITY_REQUIRED);
        }
    }
    private static void validateConditionGrade(ConditionGrade grade) {
        if (grade == null) {
            throw new IllegalArgumentException(ValidationMessages.CONDITION_REQUIRED);
        }
    }
    private static void validateOrigin(String origin) {
        if (origin == null) return;
        String o = origin.trim();
        if (o.length() > ORIGIN_MAX_LENGTH) {
            throw new IllegalArgumentException(ValidationMessages.ORIGIN_TOO_LONG);
        }
    }
    // Summary / toString
    @Override
    public String summary() {
        return rarityLevel.name() + " - " + conditionGrade.name() +
                (hasCertificate ? " (Certified)" : "");
    }
    @Override
    public String toString() {
        return super.toString()
                + " | yearOfOrigin: " + yearOfOrigin
                + " | rarityLevel: " + rarityLevel
                + " | conditionGrade: " + conditionGrade
                + " | hasCertificate: " + hasCertificate
                + " | origin: " + origin;
    }
    // Getters / Setters
    public int getYearOfOrigin() { return yearOfOrigin; }
    public void setYearOfOrigin(int yearOfOrigin) {
        this.yearOfOrigin = yearOfOrigin;
        validateYearOfOrigin(this.yearOfOrigin);
    }
    public RarityLevel getRarityLevel() { return rarityLevel; }
    public void setRarityLevel(RarityLevel rarityLevel) {
        this.rarityLevel = rarityLevel;
        validateRarityLevel(this.rarityLevel);
    }
    public ConditionGrade getConditionGrade() { return conditionGrade; }
    public void setConditionGrade(ConditionGrade conditionGrade) {
        this.conditionGrade = conditionGrade;
        validateConditionGrade(this.conditionGrade);
    }
    public boolean isHasCertificate() { return hasCertificate; }
    public void setHasCertificate(boolean hasCertificate) { this.hasCertificate = hasCertificate; }
    public String getOrigin() { return origin; }
    public void setOrigin(String origin) {
        this.origin = resolveOrigin(origin);
        validateOrigin(this.origin);
    }
}
