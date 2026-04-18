package com.team4.factory;

import com.team4.model.Collectible;
import com.team4.model.Item;

public class CollectibleFactory implements ItemFactory {
    private String id;
    private String name;
    private double startingPrice;
    private String desc;

    private int yearOfOrigin;
    private String rarityLevel;
    private String conditionGrade;
    private String category;
    private boolean hasCertificate;
    private String origin;
    private String specialFeatures;

    // Constructor của Factory nhận vào toàn bộ dữ liệu được căn lề gọn gàng
    public CollectibleFactory(String id, String name, double startingPrice, String desc,
                              int yearOfOrigin, String rarityLevel, String conditionGrade,
                              String category, boolean hasCertificate, String origin,
                              String specialFeatures) {
        this.id = id;
        this.name = name;
        this.startingPrice = startingPrice;
        this.desc = desc;

        this.yearOfOrigin = yearOfOrigin;
        this.rarityLevel = rarityLevel;
        this.conditionGrade = conditionGrade;
        this.category = category;
        this.hasCertificate = hasCertificate;
        this.origin = origin;
        this.specialFeatures = specialFeatures;
    }

    @Override
    public Item createItem() {
        return new Collectible(id, name, startingPrice, desc, yearOfOrigin,
                rarityLevel, conditionGrade, category, hasCertificate,
                origin, specialFeatures);
    }
}