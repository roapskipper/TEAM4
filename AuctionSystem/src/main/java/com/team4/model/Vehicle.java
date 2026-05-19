package com.team4.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Vehicle: model cho nhóm hàng phương tiện.
 */
public class Vehicle extends Item {
    private static final long serialVersionUID = 1L;
    // enum cho hộp số
    public enum Transmission {
        MANUAL,      // Số sàn
        AUTOMATIC,   // Số tự động
        CVT,         // Vô cấp
        DCT,         // Ly hợp kép
        OTHER        // Khác
    }
    public static Transmission fromNameTran(String name) {
        if (name == null) return null;
        try {
            return Transmission.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // enum cho động cơ
    public enum EngineType {
        // Động cơ đốt trong
        GASOLINE,       // Xăng
        DIESEL,         // Dầu diesel

        // Năng lượng mới
        ELECTRIC,       // Điện
        HYBRID,         // Hybrid (xăng + điện)
        PLUG_IN_HYBRID, // Hybrid sạc ngoài
        HYDROGEN,       // Hydro

        // Khác
        OTHER           // Khác
    }
    public static EngineType fromNameEng(String name) {
        if (name == null) return null;
        try {
            return EngineType.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    private String brand;           // thương hiệu
    private String model;           // tên model
    private int manufacturingYear;  // năm sản xuất
    private int odo;                // số km đã đi
    private EngineType engineType;      // loại động cơ
    private String color;           // màu sắc
    private boolean hasLegalPapers; // có giấy tờ pháp lý
    private Transmission transmission; // hộp số

    // Constructor dùng khi tạo mới (Seller đăng sản phẩm)
    public Vehicle(String name,
                   BigDecimal startingPrice,
                   String description,
                   String ownerId,
                   String brand,
                   String model,
                   int manufacturingYear,
                   int odo,
                   EngineType engineType,
                   String color,
                   boolean hasLegalPapers,
                   Transmission transmission) {
        super(name, startingPrice, description, ItemCategory.VEHICLE, ownerId);
        this.brand = (brand == null || brand.trim().isEmpty())
                ? "Unknown"
                : normalizeOptional(brand);
        this.model = (model == null || model.trim().isEmpty())
                ? "Unknown"
                : normalizeOptional(model);
        this.manufacturingYear = manufacturingYear;
        this.odo = odo;
        this.engineType = engineType;
        this.color = normalizeOptional(color);
        this.hasLegalPapers = hasLegalPapers;
        this.transmission = (transmission == null) ? Transmission.OTHER : transmission;
        validateBrand(this.brand);
        validateModel(this.model);
        validateManufacturingYear(this.manufacturingYear);
        validateOdo(this.odo);
        validateEngineType(this.engineType);
        validateColor(this.color);
        validateTransmission(this.transmission);
    }

    // Constructor dùng khi nạp từ DB (có id và createdAt)
    public Vehicle(String id,
                   LocalDateTime createdAt,
                   String name,
                   BigDecimal startingPrice,
                   String description,
                   String ownerId,
                   String brand,
                   String model,
                   int manufacturingYear,
                   int odo,
                   EngineType engineType,
                   String color,
                   boolean hasLegalPapers,
                   Transmission transmission) {
        super(id, createdAt, name, startingPrice, description, ItemCategory.VEHICLE, ownerId);
        this.brand = (brand == null || brand.trim().isEmpty())
                ? "Unknown"
                : normalizeOptional(brand);
        this.model = (model == null || model.trim().isEmpty())
                ? "Unknown"
                : normalizeOptional(model);
        this.manufacturingYear = manufacturingYear;
        this.odo = odo;
        this.engineType = engineType;
        this.color = normalizeOptional(color);
        this.hasLegalPapers = hasLegalPapers;
        this.transmission = (transmission == null) ? Transmission.OTHER : transmission;
        validateBrand(this.brand);
        validateModel(this.model);
        validateManufacturingYear(this.manufacturingYear);
        validateOdo(this.odo);
        validateEngineType(this.engineType);
        validateColor(this.color);
        validateTransmission(this.transmission);
    }
    // Validate
    private static void validateBrand(String b) {
        if (b == null) return;
        if (b.length() > 120) throw new IllegalArgumentException("Brand must not exceed 120 characters.");
    }
    private static void validateModel(String m) {
        if (m == null) return;
        if (m.length() > 120) throw new IllegalArgumentException("Model must not exceed 120 characters.");
    }
    private static void validateManufacturingYear(int year) {
        if (year == 0) return;
        int current = LocalDate.now().getYear();
        final int MIN_YEAR = 1886;
        if (year < MIN_YEAR || year > current) {
            throw new IllegalArgumentException("Invalid production year. Valid range: " +
                    MIN_YEAR + " .. " + (current) + " (Use '0' if unknown).");
        }
    }
    private static void validateOdo(int odo) {
        if (odo < 0) throw new IllegalArgumentException("Odometer (odo) must be >= 0.");
        if (odo > 1_000_000) throw new IllegalArgumentException("Odometer seems invalid (>1,000,000).");
    }
    private static void validateEngineType(EngineType t) {
        if (t == null)
            throw new IllegalArgumentException("EngineType must not be null. If the engine type is unknown, choose EngineType.OTHER.");
    }
    private static void validateColor(String c) {
        if (c == null) return; // optional
        if (c.length() > 50) throw new IllegalArgumentException("Color must not exceed 50 characters.");
    }
    private static void validateTransmission(Transmission t) {
        if (t == null) throw new IllegalArgumentException("Transmission must not be null.");
    }
    // Summary / toString
    @Override
    public String summary() {
        return brand + " " + model +
                " - " + manufacturingYear +
                " - " + odo + "km" +
                " - " + engineType.name() +
                (hasLegalPapers ? " (Legal papers available)" : " (Legal papers incomplete)");
    }
    @Override
    public String toString() {
        return super.toString()
                + " | brand: " + brand
                + " | model: " + model
                + " | manufacturingYear: " + manufacturingYear
                + " | odo: " + odo
                + " | engineType: " + engineType
                + " | color: " + color
                + " | hasLegalPapers: " + hasLegalPapers
                + " | transmission: " + transmission;
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
    public int getManufacturingYear() { return manufacturingYear; }
    public void setManufacturingYear(int manufacturingYear) {
        this.manufacturingYear = manufacturingYear;
        validateManufacturingYear(this.manufacturingYear);
    }
    public int getOdo() { return odo; }
    public void setOdo(int odo) {
        if (odo < this.odo)
            throw new IllegalArgumentException("Odo cannot decrease");
        this.odo = odo;
        validateOdo(this.odo);
    }
    public EngineType getEngineType() { return engineType; }
    public void setEngineType(EngineType engineType) {
        this.engineType = engineType;
        validateEngineType(this.engineType);
    }
    public String getColor() { return color; }
    public void setColor(String color) {
        this.color = normalizeOptional(color);
        validateColor(this.color);
    }
    public boolean hasLegalPapers() { return hasLegalPapers; }
    public void setHasLegalPapers(boolean hasLegalPapers) { this.hasLegalPapers = hasLegalPapers; }
    public Transmission getTransmission() { return transmission; }
    public void setTransmission(Transmission transmission) {
        this.transmission = (transmission == null ? Transmission.OTHER : transmission);
        validateTransmission(this.transmission);
    }
}
