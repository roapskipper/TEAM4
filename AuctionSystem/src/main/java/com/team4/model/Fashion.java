package com.team4.model;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Fashion: model cho nhóm hàng thời trang.
 * <p>
 * Backend Validation Notes:
 * <ul>
 * <li><b>size</b>: Required. Cannot be null.</li>
 * <li><b>condition</b>: Required. Cannot be null.</li>
 * <li><b>gender</b>: Required. Defaults to UNISEX if blank or missing.</li>
 * <li><b>brand, material, color</b>: Optional. Defaults to "Unknown" if blank.</li>
 * <li><b>authentic</b>: Optional boolean flag.</li>
 * </ul>
 */
public class Fashion extends Item {
    private static final long serialVersionUID = 1L;
    public enum Size {
        XS, S, M, L, XL, XXL, XXXL, OTHER
    }
    public static Size fromNameSize(String name) {
        if (name == null) return null;
        try {
            return Size.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    public enum Gender {
        UNISEX, MALE, FEMALE
    }
    public static Gender fromNameGender(String name) {
        if (name == null) return null;
        try {
            return Gender.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    public enum ConditionGrade {
        POOR, FAIR, GOOD, VERY_GOOD, EXCELLENT, MINT
    }
    public static ConditionGrade fromNameCon(String name) {
        if (name == null) return null;
        try {
            return ConditionGrade.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String brand;
    private Size size;
    private String material;
    private String color;
    private Gender gender;           // giới tính/đối tượng
    private ConditionGrade condition; // tình trạng
    private boolean authentic;       // is_authentic

    // Constructor khi Seller đăng sản phẩm mới
    public Fashion(String name,
                   BigDecimal startingPrice,
                   String description,
                   String ownerId,
                   String brand,
                   Size size,
                   String material,
                   String color,
                   Gender gender,
                   ConditionGrade condition,
                   boolean authentic) {
        super(name, startingPrice, description, ItemCategory.FASHION, ownerId);
        this.brand = (brand == null || brand.trim().isEmpty())
                ? "Unknown"
                : normalizeOptional(brand);
        // Require size
        this.size = size;
        this.material = (material == null || material.trim().isEmpty())
                ? "Unknown"
                : normalizeOptional(material);
        this.color = (color == null || color.trim().isEmpty())
                ? "Unknown"
                : normalizeOptional(color);
        // Default blank/missing gender to UNISEX
        this.gender = (gender == null ? Gender.UNISEX : gender);
        // Require condition
        this.condition = condition;
        // Keep authentic optional
        this.authentic = authentic;
        validateBrand(this.brand);
        validateSize(this.size);
        validateMaterial(this.material);
        validateColor(this.color);
        validateGender(this.gender);
        validateCondition(this.condition);
    }
    // Constructor khi nạp từ DB
    public Fashion(String id,
                   LocalDateTime createdAt,
                   String name,
                   BigDecimal startingPrice,
                   String description,
                   String ownerId,
                   String brand,
                   Size size,
                   String material,
                   String color,
                   Gender gender,
                   ConditionGrade condition,
                   boolean authentic) {
        super(id, createdAt, name, startingPrice, description, ItemCategory.FASHION, ownerId);
        this.brand = (brand == null || brand.trim().isEmpty())
                ? "Unknown"
                : normalizeOptional(brand);
        // Require size
        this.size = size;
        this.material = (material == null || material.trim().isEmpty())
                ? "Unknown"
                : normalizeOptional(material);
        this.color = (color == null || color.trim().isEmpty())
                ? "Unknown"
                : normalizeOptional(color);
        // Default blank/missing gender to UNISEX
        this.gender = (gender == null ? Gender.UNISEX : gender);
        // Require condition
        this.condition = condition;
        // Keep authentic optional
        this.authentic = authentic;
        validateBrand(this.brand);
        validateSize(this.size);
        validateMaterial(this.material);
        validateColor(this.color);
        validateGender(this.gender);
        validateCondition(this.condition);
    }

    // Validate
    private static void validateBrand(String brand) {
        if (brand == null) return; // optional
        if (brand.length() > 120) {
            throw new IllegalArgumentException("Brand must not exceed 120 characters.");
        }
    }
    private static void validateSize(Size size) {
        if  (size == null)
            throw new IllegalArgumentException("Fashion size is required.");
    }
    private static void validateMaterial(String material) {
        if (material == null) return;
        if (material.length() > 120) {
            throw new IllegalArgumentException("Material must not exceed 120 characters.");
        }
    }
    private static void validateColor(String color) {
        if (color == null) return;
        if (color.length() > 50) {
            throw new IllegalArgumentException("Color must not exceed 50 characters.");
        }
    }
    private static void validateGender(Gender gender) {
        if (gender == null) throw new IllegalArgumentException("Gender must not be null.");
    }
    private static void validateCondition(ConditionGrade condition) {
        if (condition == null) throw new IllegalArgumentException("Fashion condition grade is required.");
    }
    // Summary / toString
    @Override
    public String summary() {
        return brand + " - " + size +
                " - " + gender.name() +
                " - " + condition.name() +
                (authentic ? " (Authentic)" : " (Not authenticated)");
    }
    @Override
    public String toString() {
        return super.toString()
                + " | brand: " + brand
                + " | size: " + size
                + " | material: " + material
                + " | color: " + color
                + " | gender: " + gender
                + " | condition: " + condition
                + " | authentic: " + authentic;
    }
    // Getters / Setters
    public String getBrand() { return brand; }
    public void setBrand(String brand) {
        this.brand = normalizeOptional(brand);
        validateBrand(this.brand);
    }
    public Size getSize() { return size; }
    public void setSize(Size size) {
        this.size = size;
        validateSize(this.size);
    }
    public String getMaterial() { return material; }
    public void setMaterial(String material) {
        this.material = normalizeOptional(material);
        validateMaterial(this.material);
    }
    public String getColor() { return color; }
    public void setColor(String color) {
        this.color = normalizeOptional(color);
        validateColor(this.color);
    }
    public Gender getGender() { return gender; }
    public void setGender(Gender gender) {
        this.gender = (gender == null ? Gender.UNISEX : gender);
        validateGender(this.gender);
    }
    public ConditionGrade getCondition() { return condition; }
    public void setCondition(ConditionGrade condition) {
        this.condition = condition;
        validateCondition(this.condition);
    }
    public boolean isAuthentic() { return authentic; }
    public void setAuthentic(boolean authentic) { this.authentic = authentic; }
}
