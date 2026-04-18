package com.team4.factory;

import com.team4.model.Electronics;
import com.team4.model.Item;

public class ElectronicsFactory implements ItemFactory {
    private String id;
    private String name;
    private double startingPrice;
    private String desc;

    private String brand;
    private String model;
    private String serialNumber;
    private String color;
    private String condition;
    private int warrantyMonths;
    private boolean isFullyFunctional;
    private String technicalSpec;

    public ElectronicsFactory(String id, String name, double startingPrice, String desc,
                              String brand, String model, String serialNumber, String color,
                              String condition, int warrantyMonths, boolean isFullyFunctional,
                              String technicalSpec) {
        this.id = id;
        this.name = name;
        this.startingPrice = startingPrice;
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
        return new Electronics(id, name, startingPrice, desc, brand, model,
                serialNumber, color, condition, warrantyMonths,
                isFullyFunctional, technicalSpec);
    }
}