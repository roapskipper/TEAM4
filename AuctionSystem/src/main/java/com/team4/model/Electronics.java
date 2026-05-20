package com.team4.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * Electronics: model cho nhóm hàng điện tử.
 */
public class Electronics extends Item {
    private static final long serialVersionUID = 1L;
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

    private String brand;           // thương hiệu
    private String model;           // tên model
    private ConditionGrade itemCondition;       // tình trạng
    private int warrantyMonths;     // bảo hành (tháng)
    private boolean fullyFunctional; // hoạt động đầy đủ
    // Constructor dùng khi tạo mới (Seller đăng sản phẩm)
    public Electronics(String name,
                       BigDecimal startingPrice,
                       String description,
                       String ownerId,
                       String brand,
                       String model,
                       ConditionGrade itemCondition,
                       int warrantyMonths,
                       boolean fullyFunctional) {
        super(name, startingPrice, description, ItemCategory.ELECTRONICS, ownerId);
        // Default blank brand to Unknown
        this.brand = (brand == null || brand.trim().isEmpty())
                ? "Unknown"
                : normalizeOptional(brand);
        // Default blank model to Unknown
        this.model = (model == null || model.trim().isEmpty())
                ? "Unknown"
                : normalizeOptional(model);
        // Require itemCondition
        this.itemCondition = itemCondition;
        // Require warrantyMonths >= 0
        this.warrantyMonths = warrantyMonths;
        // Keep fullyFunctional optional
        this.fullyFunctional = fullyFunctional;
        validateBrand(this.brand);
        validateModel(this.model);
        validateItemCondition(this.itemCondition);
        validateWarrantyMonths(this.warrantyMonths);
    }
    // Constructor dùng khi nạp từ DB
    public Electronics(String id,
                       LocalDateTime createdAt,
                       String name,
                       BigDecimal startingPrice,
                       String description,
                       String ownerId,
                       String brand,
                       String model,
                       ConditionGrade itemCondition,
                       int warrantyMonths,
                       boolean fullyFunctional) {
        super(id, createdAt, name, startingPrice, description, ItemCategory.ELECTRONICS, ownerId);
        // Default blank brand to Unknown
        this.brand = (brand == null || brand.trim().isEmpty())
                ? "Unknown"
                : normalizeOptional(brand);
        // Default blank model to Unknown
        this.model = (model == null || model.trim().isEmpty())
                ? "Unknown"
                : normalizeOptional(model);
        // Require itemCondition
        this.itemCondition = itemCondition;
        // Require warrantyMonths >= 0
        this.warrantyMonths = warrantyMonths;
        // Keep fullyFunctional optional
        this.fullyFunctional = fullyFunctional;
        validateBrand(this.brand);
        validateModel(this.model);
        validateItemCondition(this.itemCondition);
        validateWarrantyMonths(this.warrantyMonths);
    }

    // Validate
    private static void validateBrand(String brand) {
        if (brand == null || brand.isEmpty()) return;
        if (brand.length() > 50) {
            throw new IllegalArgumentException("Brand must not exceed 50 characters.");
        }
    }
    private static void validateModel(String model) {
        if (model == null || model.isEmpty()) return;
        if (model.length() > 50) {
            throw new IllegalArgumentException("Model must not exceed 50 characters.");
        }
    }
    private static void validateItemCondition(ConditionGrade cond) {
        if (cond == null) {
            throw new IllegalArgumentException("Item condition must not be blank.");
        }
    }
    private static void validateWarrantyMonths(int months) {
        if (months < 0) {
            throw new IllegalArgumentException("Warranty months must be >= 0.");
        }
    }
    // Summary / toString
    @Override
    public String summary() {
        return brand + " " + model +
                " - " + itemCondition.name() +
                " - Warranty: " + warrantyMonths + " months" +
                (fullyFunctional ? "" : " (Not fully functional)");
    }
    @Override
    public String toString() {
        return super.toString()
                + " | brand: " + brand
                + " | model: " + model
                + " | condition: " + itemCondition
                + " | warrantyMonths: " + warrantyMonths
                + " | fullyFunctional: " + fullyFunctional;
    }
    // Getters / Setters
    public String getBrand() { return brand; }
    public void setBrand(String brand) {
        this.brand = normalizeOptional(brand);
        validateBrand(this.brand);
    }
    public String getModel() { return model; }
    public void setModel(String model) {
        this.model = normalizeOptional(model);
        validateModel(this.model);
    }
    public ConditionGrade getItemCondition() { return itemCondition; }
    public void setItemCondition(ConditionGrade itemCondition) {
        this.itemCondition = itemCondition;
        validateItemCondition(this.itemCondition);
    }
    public int getWarrantyMonths() { return warrantyMonths; }
    public void setWarrantyMonths(int warrantyMonths) {
        this.warrantyMonths = warrantyMonths;
        validateWarrantyMonths(this.warrantyMonths);
    }
    public boolean isFullyFunctional() { return fullyFunctional; }
    public void setFullyFunctional(boolean fullyFunctional) { this.fullyFunctional = fullyFunctional; }
    }
