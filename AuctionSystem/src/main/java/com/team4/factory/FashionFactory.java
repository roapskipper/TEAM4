package com.team4.factory;

import com.team4.model.Fashion;
import com.team4.model.Item;
import java.math.BigDecimal;

public class FashionFactory implements ItemFactory {
    // Thuộc tính cơ bản của Item
    private String name;
    private BigDecimal startingPrice;
    private String desc;
    private String ownerId;

    // Thuộc tính riêng của Fashion
    private String brand;
    private Fashion.Size size;
    private String material;
    private String color;
    private Fashion.Gender gender;
    private Fashion.ConditionGrade condition;
    private boolean isAuthentic;

    public FashionFactory(String name, BigDecimal startingPrice, String desc, String ownerId,
                          String brand, Fashion.Size size, String material, String color,
                          Fashion.Gender gender, Fashion.ConditionGrade condition, boolean isAuthentic) {
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

    @Override
    public Item createItem() {
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