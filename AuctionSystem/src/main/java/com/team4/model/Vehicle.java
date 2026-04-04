package com.team4.model;

import java.io.Serializable;

/**
 * Lớp Vehicle - Đại diện cho phương tiện giao thông đấu giá.
 * Kế thừa từ Item (Tính Inheritance và Polymorphism)
 */
public class Vehicle extends Item implements Serializable {
    private static final long serialVersionUID = 1L;

    // Các thuộc tính riêng của Phương tiện
    private String brand;
    private String model;
    private int manufacturingYear;
    private int odo;              // Số km đã đi
    private String engineType;    // Xăng, Dầu, Điện...
    private String color;
    private String licensePlate;
    private boolean hasLegalPapers;
    private String transmission;  // Số sàn/Tự động

    /**
     * CONSTRUCTOR 1: Dùng khi Seller đăng ký xe mới để đấu giá.
     * Tự động gán category là "VEHICLE".
     */
    public Vehicle(String name, double startingPrice, String desc, String ownerId,
                   String brand, String model, int manufacturingYear, int odo,
                   String engineType, String color, String licensePlate,
                   boolean hasLegalPapers, String transmission) {

        // Gọi constructor 1 của Item (Tự sinh UUID, category = VEHICLE)
        super(name, startingPrice, desc, "VEHICLE", ownerId);

        this.brand = brand;
        this.model = model;
        this.manufacturingYear = manufacturingYear;
        this.odo = odo;
        this.engineType = engineType;
        this.color = color;
        this.licensePlate = licensePlate;
        this.hasLegalPapers = hasLegalPapers;
        this.transmission = transmission;
    }

    /**
     * CONSTRUCTOR 2: Dùng cho ItemDAO khi nạp dữ liệu xe từ MySQL lên.
     */
    public Vehicle(String id, String name, double startingPrice, double currentPrice,
                   String desc, String ownerId, String brand, String model,
                   int manufacturingYear, int odo, String engineType, String color,
                   String licensePlate, boolean hasLegalPapers, String transmission) {

        // Gọi constructor 2 của Item (Giữ nguyên ID và CurrentPrice từ DB)
        super(id, name, startingPrice, currentPrice, desc, "VEHICLE", ownerId);

        this.brand = brand;
        this.model = model;
        this.manufacturingYear = manufacturingYear;
        this.odo = odo;
        this.engineType = engineType;
        this.color = color;
        this.licensePlate = licensePlate;
        this.hasLegalPapers = hasLegalPapers;
        this.transmission = transmission;
    }

    // --- TRIỂN KHAI ĐA HÌNH (POLYMORPHISM) ---

    @Override
    public void showInfo() {
        System.out.println("\n----------- [ THÔNG TIN PHƯƠNG TIỆN ] -----------");
        System.out.println("Sản phẩm     : " + getName() + " (ID: " + getId() + ")");
        System.out.println("Thương hiệu  : " + brand + " " + model);
        System.out.println("Năm sản xuất : " + manufacturingYear);
        System.out.println("Số KM (ODO)  : " + odo + " km");
        System.out.println("Động cơ      : " + engineType + " | Hộp số: " + transmission);
        System.out.println("Màu sắc      : " + color + " | Biển số: " + (licensePlate != null ? licensePlate : "N/A"));
        System.out.println("Pháp lý      : " + (hasLegalPapers ? "GIẤY TỜ ĐẦY ĐỦ" : "Chưa đủ giấy tờ"));
        System.out.println("-------------------------------------------------");
        System.out.println("GIÁ KHỞI ĐIỂM: $" + getStartingPrice());
        System.out.println(">>> GIÁ HIỆN TẠI: $" + getCurrentPrice() + " <<<");
        System.out.println("Người đăng bán: ID-" + getOwnerId());
        System.out.println("-------------------------------------------------\n");
    }

    // --- GETTERS & SETTERS ---
    public String getBrand() { return brand; }
    public String getModel() { return model; }
    public int getManufacturingYear() { return manufacturingYear; }
    public int getOdo() { return odo; }
    public String getEngineType() { return engineType; }
    public String getColor() { return color; }
    public String getLicensePlate() { return licensePlate; }
    public boolean isHasLegalPapers() { return hasLegalPapers; }
    public String getTransmission() { return transmission; }

    public void setOdo(int odo) { this.odo = odo; }
    public void setHasLegalPapers(boolean hasLegalPapers) { this.hasLegalPapers = hasLegalPapers; }
}