package com.team4.factory;

import com.team4.model.Electronics;
import com.team4.model.Item;

/**
 * ElectronicsFactory - Nhà máy sản xuất đồ điện tử (Laptops, Điện thoại, PC).
 * Thể hiện tính Abstraction thông qua Factory Pattern.
 */
public class ElectronicsFactory implements ItemFactory {
    // Thuộc tính cơ bản của Item
    private String name;
    private double startingPrice;
    private String desc;
    private String ownerId; // Link đến ID của Seller chuyên đồ công nghệ

    // Thuộc tính riêng của Electronics
    private String brand;
    private String model;
    private String serialNumber;
    private String color;
    private String condition; // Mới/99%/Cũ
    private int warrantyMonths;
    private boolean isFullyFunctional; // Còn hoạt động tốt không?
    private String technicalSpec; // Cấu hình chi tiết (RAM, CPU, Pin...)

    /**
     * CONSTRUCTOR: Nhận thông số kỹ thuật để chuẩn bị tạo sản phẩm công nghệ.
     */
    public ElectronicsFactory(String name, double startingPrice, String desc, String ownerId,
                              String brand, String model, String serialNumber, String color,
                              String condition, int warrantyMonths, boolean isFullyFunctional,
                              String technicalSpec) {
        this.name = name;
        this.startingPrice = startingPrice;
        this.desc = desc;
        this.ownerId = ownerId;
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
     * SẢN XUẤT: Tạo ra đối tượng Electronics thực tế.
     * Thể hiện tính Đa hình: Trả về kiểu Item, linh hồn là Electronics.
     */
    @Override
    public Item createItem() {
        // Sử dụng Constructor của Electronics (Sẽ tự sinh UUID và gán category = ELECTRONICS)
        return new Electronics(
                name,
                startingPrice,
                desc,
                ownerId,
                brand,
                model,
                serialNumber,
                color,
                condition,
                warrantyMonths,
                isFullyFunctional,
                technicalSpec
        );
    }
}