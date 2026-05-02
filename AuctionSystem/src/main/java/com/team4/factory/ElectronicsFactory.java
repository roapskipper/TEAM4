package com.team4.factory;

import com.team4.model.Electronics;
import com.team4.model.Item;
import java.math.BigDecimal;

public class ElectronicsFactory implements ItemFactory {
    // Thuộc tính cơ bản của Item
    private String name;
    private BigDecimal startingPrice;
    private String desc;
    private String ownerId;

    // Thuộc tính riêng của Electronics
    private String brand;
    private String model;
    private Electronics.ConditionGrade condition;
    private int warrantyMonths;
    private boolean isFullyFunctional;

    public ElectronicsFactory(String name, BigDecimal startingPrice, String desc, String ownerId,
                              String brand, String model, Electronics.ConditionGrade condition, int warrantyMonths, boolean isFullyFunctional) {
        this.name = name;
        this.startingPrice = startingPrice;
        this.desc = desc;
        this.ownerId = ownerId;
        this.brand = brand;
        this.model = model;
        this.condition = condition;
        this.warrantyMonths = warrantyMonths;
        this.isFullyFunctional = isFullyFunctional;
    }

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
                condition,
                warrantyMonths,
                isFullyFunctional
        );
    }
}