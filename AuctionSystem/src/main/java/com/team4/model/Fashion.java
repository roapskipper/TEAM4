package com.team4.model;

import java.io.Serializable;

/**
 * Lớp Fashion - Đại diện cho các mặt hàng thời trang (Quần áo, giày dép, phụ kiện).
 * Kế thừa từ Item (Thể hiện tính Inheritance & Polymorphism)
 */
public class Fashion extends Item implements Serializable {
    private static final long serialVersionUID = 1L;

    // Các thuộc tính riêng của Thời trang
    private String brand;
    private String size;
    private String material;
    private String color;
    private String gender;
    private String condition; // New with tags, Used...
    private boolean isAuthentic; // Kiểm định chính hãng

    /**
     * CONSTRUCTOR 1: Dùng khi Seller đăng sản phẩm thời trang mới.
     * Category mặc định là "FASHION".
     */
    public Fashion(String name, double startingPrice, String desc, String ownerId,
                   String brand, String size, String material, String color,
                   String gender, String condition, boolean isAuthentic) {

        // Gọi constructor 1 của Item (Tự sinh UUID, category = FASHION)
        super(name, startingPrice, desc, "FASHION", ownerId);

        this.brand = brand;
        this.size = size;
        this.material = material;
        this.color = color;
        this.gender = gender;
        this.condition = condition;
        this.isAuthentic = isAuthentic;
    }

    /**
     * CONSTRUCTOR 2: Dùng cho ItemDAO nạp dữ liệu từ MySQL lên.
     */
    public Fashion(String id, String name, double startingPrice, double currentPrice,
                   String desc, String ownerId, String brand, String size,
                   String material, String color, String gender, String condition,
                   boolean isAuthentic) {

        // Gọi constructor 2 của Item (Giữ nguyên ID và giá hiện tại từ DB)
        super(id, name, startingPrice, currentPrice, desc, "FASHION", ownerId);

        this.brand = brand;
        this.size = size;
        this.material = material;
        this.color = color;
        this.gender = gender;
        this.condition = condition;
        this.isAuthentic = isAuthentic;
    }

    // --- TRIỂN KHAI ĐA HÌNH (POLYMORPHISM) ---

    @Override
    public void showInfo() {
        System.out.println("\n----------- [ THÔNG TIN THỜI TRANG ] -----------");
        System.out.println("Sản phẩm     : " + getName() + " (ID: " + getId() + ")");
        System.out.println("Thương hiệu  : " + brand);
        System.out.println("Size/Màu     : " + size + " / " + color);
        System.out.println("Chất liệu    : " + material + " | Dành cho: " + gender);
        System.out.println("Tình trạng   : " + condition);
        System.out.println("Xác thực Auth: " + (isAuthentic ? "ĐÃ KIỂM ĐỊNH CHÍNH HÃNG ✔" : "Chưa kiểm định ⚠"));
        System.out.println("-------------------------------------------------");
        System.out.println("GIÁ KHỞI ĐIỂM: $" + getStartingPrice());
        System.out.println(">>> GIÁ HIỆN TẠI: $" + getCurrentPrice() + " <<<");
        System.out.println("Người sở hữu : Seller-" + getOwnerId());
        System.out.println("-------------------------------------------------\n");
    }

    // --- GETTERS & SETTERS (ENCAPSULATION) ---
    public String getBrand() { return brand; }
    public String getSize() { return size; }
    public String getMaterial() { return material; }
    public String getColor() { return color; }
    public String getGender() { return gender; }
    public String getCondition() { return condition; }
    public boolean isAuthentic() { return isAuthentic; }

    public void setCondition(String condition) { this.condition = condition; }
    public void setAuthentic(boolean authentic) { isAuthentic = authentic; }
}