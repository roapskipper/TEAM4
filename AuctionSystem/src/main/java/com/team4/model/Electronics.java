package com.team4.model;

import java.io.Serializable;

public class Electronics extends Item implements Serializable {
    private String brand;           // Thương hiệu (ví dụ: Apple, Samsung)
    private String model;           // Dòng máy (ví dụ: iPhone 15 Pro, Galaxy S24)
    private String serialNumber;    // Số Serial (để định danh duy nhất máy này, rất quan trọng khi bàn giao)
    private String color;           // Màu sắc
    private String condition;       // Tình trạng (ví dụ: Mới 100%, Likenew 99%, Cũ)
    private int warrantyMonths;   // Thời gian bảo hành còn lại (tháng)
    private boolean isFullyFunctional; // Có hoạt động hoàn hảo không? (true/false)
    private String technicalSpec;   // Thông số kỹ thuật tóm tắt (ví dụ: RAM 8GB, SSD 256GB)

    public Electronics(String id, String name, double startingPrice, String desc,
                       String brand, String model, String serialNumber, String color,
                       String condition, int warrantyMonths, boolean isFullyFunctional,
                       String technicalSpec) {

        super(id, name, startingPrice, desc);
        this.brand = brand;
        this.model = model;
        this.serialNumber = serialNumber;
        this.color = color;
        this.condition = condition;
        this.warrantyMonths = warrantyMonths;
        this.isFullyFunctional = isFullyFunctional;
        this.technicalSpec = technicalSpec;
    }

    public String getBrand() { return brand; }
    public String getModel() { return model; }
    public String getSerialNumber() { return serialNumber; }
    public String getColor() { return color; }
    public String getCondition() { return condition; }
    public int getWarrantyMonths() { return warrantyMonths; }
    public boolean isFullyFunctional() { return isFullyFunctional; }
    public String getTechnicalSpec() { return technicalSpec; }

    @Override
    public void showInfo() {
        System.out.println("--- [ĐỒ ĐIỆN TỬ] ---");
        System.out.println("Tên: " + name + " (" + brand + " " + model + ")");
        System.out.println("Màu: " + color + " | Tình trạng: " + condition);
        System.out.println("Bảo hành: " + warrantyMonths + " tháng | Hoạt động: " + (isFullyFunctional ? "Hoàn hảo" : "Có lỗi"));
        System.out.println("Thông số: " + technicalSpec);
        System.out.println("Giá hiện tại: " + currentPrice);
    }
}