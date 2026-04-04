package com.team4.factory;

import com.team4.model.Fashion;
import com.team4.model.Item;

/**
 * FashionFactory - Nhà máy chuyên sản xuất mặt hàng Thời trang.
 * Thực thi giao diện ItemFactory (Design Pattern: Factory Method).
 */
public class FashionFactory implements ItemFactory {
    // Thuộc tính cơ bản của Item
    private String name;
    private double startingPrice;
    private String desc;
    private String ownerId; // ID của Seller đăng bán

    // Thuộc tính riêng của Fashion
    private String brand;
    private String size;
    private String material;
    private String color;
    private String gender;
    private String condition;
    private boolean isAuthentic;

    /**
     * CONSTRUCTOR: Chuẩn bị nguyên liệu để tạo món hàng thời trang.
     * ID không cần truyền vào vì sẽ được sinh tự động (UUID).
     */
    public FashionFactory(String name, double startingPrice, String desc, String ownerId,
                          String brand, String size, String material, String color,
                          String gender, String condition, boolean isAuthentic) {
        this.name = name;
        this.startingPrice = startingPrice;
        this.desc = desc;
        this.ownerId = ownerId;
        this.brand = brand;
        this.size = size;
        this.material = material;
        this.color = color;
        this.gender = gender;
        this.condition = condition;
        this.isAuthentic = isAuthentic;
    }

    /**
     * SẢN XUẤT: Trả về đối tượng Fashion thực tế.
     * Thể hiện tính Đa hình (Polymorphism): Trả về kiểu Item nhưng bên trong là Fashion.
     */
    @Override
    public Item createItem() {
        // Gọi Constructor 1 của Fashion (Tự sinh UUID, category = FASHION)
        return new Fashion(
                name,
                startingPrice,
                desc,
                ownerId,
                brand,
                size,
                material,
                color,
                gender,
                condition,
                isAuthentic
        );
    }
}