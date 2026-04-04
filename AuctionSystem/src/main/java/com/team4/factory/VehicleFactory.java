package com.team4.factory;

import com.team4.model.Vehicle;
import com.team4.model.Item;

/**
 * VehicleFactory - Nhà máy sản xuất các phương tiện đấu giá (Ô tô, Xe máy).
 * Áp dụng Design Pattern: Factory Method.
 */
public class VehicleFactory implements ItemFactory {
    // Thuộc tính cơ bản của Item
    private String name;
    private double startingPrice;
    private String desc;
    private String ownerId; // Liên kết tới Showroom/Seller qua UUID

    // Thuộc tính riêng của Phương tiện
    private String brand;
    private String model;
    private int manufacturingYear;
    private int odo;
    private String engineType;
    private String color;
    private String licensePlate;
    private boolean hasLegalPapers;
    private String transmission;

    /**
     * CONSTRUCTOR: Chuẩn bị thông số để khởi tạo một phương tiện.
     * Lưu ý: ID xe không cần truyền vào đây vì lớp Vehicle sẽ tự sinh UUID.
     */
    public VehicleFactory(String name, double startingPrice, String desc, String ownerId,
                          String brand, String model, int manufacturingYear, int odo,
                          String engineType, String color, String licensePlate,
                          boolean hasLegalPapers, String transmission) {
        this.name = name;
        this.startingPrice = startingPrice;
        this.desc = desc;
        this.ownerId = ownerId;
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
     * SẢN XUẤT: Tạo ra đối tượng Vehicle thực tế.
     */
    @Override
    public Item createItem() {
        // Sử dụng Constructor 1 của lớp Vehicle (Tự sinh UUID, category = VEHICLE)
        return new Vehicle(
                name,
                startingPrice,
                desc,
                ownerId,
                brand,
                model,
                manufacturingYear,
                odo,
                engineType,
                color,
                licensePlate,
                hasLegalPapers,
                transmission
        );
    }
}