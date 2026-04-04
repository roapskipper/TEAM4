package com.team4.model;

import java.io.Serializable;

public class Electronics extends Item implements Serializable {
    private String brand;
    private String model;
    private String serialNumber;
    private String color;
    private String condition;
    private int warrantyMonths;
    private boolean isFullyFunctional;
    private String technicalSpec;

    /**
     * CONSTRUCTOR 1: Khớp với ElectronicsFactory (Sửa lỗi bạn đang gặp)
     */
    public Electronics(String name, double startingPrice, String desc, String ownerId,
                       String brand, String model, String serialNumber, String color,
                       String condition, int warrantyMonths, boolean isFullyFunctional,
                       String technicalSpec) {

        // Gọi lên Item để xử lý các thuộc tính chung và sinh UUID
        super(name, startingPrice, desc, "ELECTRONICS", ownerId);

        this.brand = brand;
        this.model = model;
        this.serialNumber = serialNumber;
        this.color = color;
        this.condition = condition;
        this.warrantyMonths = warrantyMonths;
        this.isFullyFunctional = isFullyFunctional;
        this.technicalSpec = technicalSpec;
    }

    /**
     * CONSTRUCTOR 2: Dùng cho Database (Dành cho Chặng sau)
     */
    public Electronics(String id, String name, double startingPrice, double currentPrice,
                       String desc, String ownerId, String brand, String model,
                       String serialNumber, String color, String condition,
                       int warrantyMonths, boolean isFullyFunctional, String technicalSpec) {

        super(id, name, startingPrice, currentPrice, desc, "ELECTRONICS", ownerId);
        this.brand = brand;
        this.model = model;
        this.serialNumber = serialNumber;
        this.color = color;
        this.condition = condition;
        this.warrantyMonths = warrantyMonths;
        this.isFullyFunctional = isFullyFunctional;
        this.technicalSpec = technicalSpec;
    }

    @Override
    public void showInfo() {
        System.out.println("\n----------- [ ĐỒ ĐIỆN TỬ ] -----------");
        System.out.println("Sản phẩm     : " + getName() + " (ID: " + getId() + ")");
        System.out.println("Thương hiệu  : " + brand + " | Model: " + model);
        System.out.println("Serial No    : " + serialNumber);
        System.out.println("Tình trạng   : " + condition + " | Hoạt động: " + (isFullyFunctional ? "Tốt" : "Cần sửa chữa"));
        System.out.println("Bảo hành     : " + warrantyMonths + " tháng");
        System.out.println("Cấu hình     : " + technicalSpec);
        System.out.println("--------------------------------------");
        System.out.println("GIÁ HIỆN TẠI : $" + getCurrentPrice());
        System.out.println("Người đăng   : Seller-" + getOwnerId());
        System.out.println("--------------------------------------\n");
    }

    // Các Getter/Setter bạn tự thêm để phục vụ logic sau này nhé...
}