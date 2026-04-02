package com.team4.factory;

import com.team4.model.Vehicle;
import com.team4.model.Item;

public class VehicleFactory implements ItemFactory {
    private String id;
    private String name;
    private double startingPrice;
    private String desc;

    private String brand;
    private String model;
    private int manufacturingYear;
    private int odo;
    private String engineType;
    private String color;
    private String licensePlate;
    private boolean hasLegalPapers;
    private String transmission;

    public VehicleFactory(String id, String name, double startingPrice, String desc,
                          String brand, String model, int manufacturingYear, int odo,
                          String engineType, String color, String licensePlate,
                          boolean hasLegalPapers, String transmission) {
        this.id = id;
        this.name = name;
        this.startingPrice = startingPrice;
        this.desc = desc;

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

    @Override
    public Item createItem() {
        return new Vehicle(id, name, startingPrice, desc, brand, model,
                manufacturingYear, odo, engineType, color,
                licensePlate, hasLegalPapers, transmission);
    }
}