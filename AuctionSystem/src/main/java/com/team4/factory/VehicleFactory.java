package com.team4.factory;

import com.team4.model.Vehicle;
import com.team4.model.Item;
import java.math.BigDecimal;

public class VehicleFactory implements ItemFactory {
    // Thuộc tính cơ bản của Item
    private String name;
    private BigDecimal startingPrice;
    private String desc;
    private String ownerId;

    // Thuộc tính riêng của Phương tiện
    private String brand;
    private String model;
    private int manufacturingYear;
    private int odo;
    private Vehicle.EngineType engineType;
    private String color;
    private boolean hasLegalPapers;
    private Vehicle.Transmission transmission;

    public VehicleFactory(String name, BigDecimal startingPrice, String desc, String ownerId,
                          String brand, String model, int manufacturingYear, int odo,
                          Vehicle.EngineType engineType, String color, boolean hasLegalPapers, Vehicle.Transmission transmission) {
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
        this.hasLegalPapers = hasLegalPapers;
        this.transmission = transmission;
    }

    @Override
    public Item createItem() {
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
                hasLegalPapers,
                transmission
        );
    }
}