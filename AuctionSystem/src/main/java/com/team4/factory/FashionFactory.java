package com.team4.factory;

import com.team4.model.Fashion;
import com.team4.model.Item;

public class FashionFactory implements ItemFactory {
    private String id;
    private String name;
    private double startingPrice;
    private String desc;

    private String brand;
    private String size;
    private String material;
    private String color;
    private String gender;
    private String condition;
    private boolean isAuthentic;

    public FashionFactory(String id, String name, double startingPrice, String desc,
                          String brand, String size, String material, String color,
                          String gender, String condition, boolean isAuthentic) {
        this.id = id;
        this.name = name;
        this.startingPrice = startingPrice;
        this.desc = desc;

        this.brand = brand;
        this.size = size;
        this.material = material;
        this.color = color;
        this.gender = gender;
        this.condition = condition;
        this.isAuthentic = isAuthentic;
    }

    @Override
    public Item createItem() {
        return new Fashion(id, name, startingPrice, desc, brand, size,
                material, color, gender, condition, isAuthentic);
    }
}