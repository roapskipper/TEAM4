package com.team4.factory;

import com.team4.model.Electronics;
import com.team4.model.Item;

public class ElectronicsFactory implements ItemFactory {
    private final String id;
    private final String name;
    private final double price;
    private final String desc;
    private final String brand;
    private final String model;
    private final String serialNumber;
    private final String color;
    private final String condition;
    private final int warrantyMonths;
    private final boolean isFullyFunctional;
    private final String technicalSpec;

    public ElectronicsFactory(String id, String name, double price, String desc,
                              String brand, String model, String serialNumber,
                              String color, String condition,
                              int warrantyMonths, boolean isFullyFunctional,
                              String technicalSpec) {

        this.id = id;
        this.name = name;
        this.price = price;
        this.desc = desc;
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
    public Item createItem() {
        return new Electronics(
                id, name, price, desc,
                brand, model, serialNumber,
                color, condition,
                warrantyMonths, isFullyFunctional,
                technicalSpec
        );
    }
}