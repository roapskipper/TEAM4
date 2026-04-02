package com.team4.model;

import java.io.Serializable;

public class Fashion extends Item implements Serializable {
    private String brand;      // Thương hiệu (Nike, Adidas, Gucci...)
    private String size;       // Kích cỡ (S, M, L, XL hoặc size giày 39, 40...)
    private String material;   // Chất liệu (Cotton, Da, Lụa...)
    private String color;      // Màu sắc
    private String gender;     // Giới tính (Nam, Nữ, Unisex)
    private String condition;  // Tình trạng (Mới nguyên tag, Đã qua sử dụng...)
    private boolean isAuthentic; // Có phải hàng chính hãng không? (Cực kỳ quan trọng khi đấu giá đồ hiệu)

    // 2. Constructor đầy đủ
    public Fashion(String id, String name, double startingPrice, String desc,
                   String brand, String size, String material, String color,
                   String gender, String condition, boolean isAuthentic) {

        super(id, name, startingPrice, desc);
        this.brand = brand;
        this.size = size;
        this.material = material;
        this.color = color;
        this.gender = gender;
        this.condition = condition;
        this.isAuthentic = isAuthentic;
    }

    public String getBrand() { return brand; }
    public String getSize() { return size; }
    public String getMaterial() { return material; }
    public String getColor() { return color; }
    public String getGender() { return gender; }
    public String getCondition() { return condition; }
    public boolean isAuthentic() { return isAuthentic; }

    @Override
    public void showInfo() {
        System.out.println("--- [THỜI TRANG] ---");
        System.out.println("Sản phẩm: " + name + " | Thương hiệu: " + brand);
        System.out.println("Size: " + size + " | Màu: " + color + " | Giới tính: " + gender);
        System.out.println("Chất liệu: " + material + " | Tình trạng: " + condition);
        System.out.println("Xác minh: " + (isAuthentic ? "Hàng chính hãng (Authentic)" : "Hàng chưa xác minh"));
        System.out.println("Giá khởi điểm: " + startingPrice + " | Giá hiện tại: " + currentPrice);
    }
}