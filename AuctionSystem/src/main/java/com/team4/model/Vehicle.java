package com.team4.model;

import java.io.Serializable;

public class Vehicle extends Item implements Serializable {
    private String brand;         // Hãng xe (Toyota, BMW, Tesla...)
    private String model;         // Dòng xe (Camry, i8, Model 3...)
    private int manufacturingYear; // Năm sản xuất
    private int odo;              // Số km đã đi (Odometer)
    private String engineType;    // Loại động cơ (Xăng, Dầu, Điện, Hybrid)
    private String color;         // Màu sắc ngoại thất
    private String licensePlate;  // Biển số xe (nếu có)
    private boolean hasLegalPapers; // Có đầy đủ giấy tờ pháp lý/sổ đăng kiểm không?
    private String transmission;  // Hộp số (Số sàn, Số tự động)

    public Vehicle(String id, String name, double startingPrice, String desc,
                   String brand, String model, int manufacturingYear, int odo,
                   String engineType, String color, String licensePlate,
                   boolean hasLegalPapers, String transmission) {

        super(id, name, startingPrice, desc);
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

    public String getBrand() { return brand; }
    public String getModel() { return model; }
    public int getManufacturingYear() { return manufacturingYear; }
    public int getOdo() { return odo; }
    public String getEngineType() { return engineType; }
    public String getColor() { return color; }
    public String getLicensePlate() { return licensePlate; }
    public boolean isHasLegalPapers() { return hasLegalPapers; }
    public String getTransmission() { return transmission; }

    @Override
    public void showInfo() {
        System.out.println("--- [PHƯƠNG TIỆN] ---");
        System.out.println("Xe: " + brand + " " + model + " (" + manufacturingYear + ")");
        System.out.println("ODO: " + odo + " km | Động cơ: " + engineType + " | Hộp số: " + transmission);
        System.out.println("Màu sắc: " + color + " | Biển số: " + (licensePlate != null ? licensePlate : "Chưa đăng ký"));
        System.out.println("Pháp lý: " + (hasLegalPapers ? "Chính chủ, đủ giấy tờ" : "Đang chờ hoàn thiện giấy tờ"));
        System.out.println("Giá hiện tại: " + currentPrice);
    }
}