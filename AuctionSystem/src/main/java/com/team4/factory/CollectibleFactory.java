package com.team4.factory;

import com.team4.model.Collectible;
import com.team4.model.Item;
import java.math.BigDecimal;

public class CollectibleFactory implements ItemFactory {
    // Thuộc tính cơ bản
    private String name;
    private BigDecimal startingPrice;
    private String desc;
    private String ownerId;

    // Thuộc tính riêng
    private int yearOfOrigin;
    private Collectible.RarityLevel rarityLevel;
    private Collectible.ConditionGrade conditionGrade;
    private boolean hasCertificate;
    private String origin;

    public CollectibleFactory(String name, BigDecimal startingPrice, String desc, String ownerId,
                              int yearOfOrigin, Collectible.RarityLevel rarityLevel, Collectible.ConditionGrade conditionGrade,
                              boolean hasCertificate, String origin) {
        this.name = name;
        this.startingPrice = startingPrice;
        this.desc = desc;
        this.ownerId = ownerId;
        this.yearOfOrigin = yearOfOrigin;
        this.rarityLevel = rarityLevel;
        this.conditionGrade = conditionGrade;
        this.hasCertificate = hasCertificate;
        this.origin = origin;
    }

    @Override
    public Item createItem() {
        return new Collectible(
                name,
                startingPrice,
                desc,
                ownerId,
                yearOfOrigin,
                rarityLevel,
                conditionGrade,
                hasCertificate,
                origin
        );
    }
}