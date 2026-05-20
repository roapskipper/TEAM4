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
    private int yearOfOrigin;       // năm xuất xứ
    private RarityLevel rarityLevel;     // độ hiếm
    private ConditionGrade conditionGrade; // tình trạng
    private boolean hasCertificate;  // có chứng chỉ không
    private String origin;           // xuất xứ (quốc gia/vùng)

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
        this.origin = (origin == null || origin.trim().isEmpty())
                ? "Unknown"
                : normalizeOptional(origin);
        validateYearOfOrigin(this.yearOfOrigin);
        validateRarityLevel(this.rarityLevel);
        validateConditionGrade(this.conditionGrade);
        validateOrigin(this.origin);
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
        this.origin = (origin == null || origin.trim().isEmpty())
                ? "Unknown"
                : normalizeOptional(origin);
        validateYearOfOrigin(this.yearOfOrigin);
        validateRarityLevel(this.rarityLevel);
        validateConditionGrade(this.conditionGrade);
        validateOrigin(this.origin);
    }

    // Validate
    private static void validateYearOfOrigin(int year) {
        if (year == 0) return; // unknown
        int current = LocalDate.now().getYear();
        final int MIN_YEAR = -3000;
        if (year < MIN_YEAR || year > current) {
            throw new IllegalArgumentException("Invalid production year. Valid range: " +
                    MIN_YEAR + " .. " + current + " (Use '0' if the production year is unknown).");
        }
    }
    private static void validateRarityLevel(RarityLevel rarity) {
        if (rarity == null)
           throw new IllegalArgumentException("Rarity level must not be blank.");
    }
    private static void validateConditionGrade(ConditionGrade grade) {
        if (grade == null)
            throw new IllegalArgumentException("Condition grade must not be blank.");
    }
    private static void validateOrigin(String origin) {
        if (origin == null) return;
        String o = origin.trim();
        if (o.length() > 120) throw new IllegalArgumentException("Origin is too long (maximum 120 characters).");
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
        this.origin = normalizeOptional(origin);
        validateOrigin(this.origin);
    }
}
