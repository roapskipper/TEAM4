package com.team4.dto.item;

import com.team4.dto.auction.CreateItemRequestDTO;
import com.team4.model.Collectible;
import com.team4.model.Item;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CreateCollectibleRequestDTO extends CreateItemRequestDTO {
    private int yearOfOrigin;
    private Collectible.RarityLevel rarityLevel;
    private Collectible.ConditionGrade conditionGrade;
    private boolean hasCertificate;
    private String origin;

    public CreateCollectibleRequestDTO() {}
    public CreateCollectibleRequestDTO(String name, BigDecimal startingPrice, String description, Item.ItemCategory category, int yearOfOrigin, Collectible.RarityLevel rarityLevel, Collectible.ConditionGrade conditionGrade, boolean hasCertificate, String origin) {
        super(name, startingPrice, description, category);
        this.yearOfOrigin = yearOfOrigin;
        this.rarityLevel = rarityLevel;
        this.conditionGrade = conditionGrade;
        this.hasCertificate = hasCertificate;
        this.origin = origin;
        validateCollectibleDTO();
    }

    protected final void validateCollectibleDTO() {
        if (yearOfOrigin == 0) return;
        int current = LocalDate.now().getYear();
        final int MIN_YEAR = -3000;
        if (yearOfOrigin < MIN_YEAR || yearOfOrigin > current) {
            throw new IllegalArgumentException("Year of origin is invalid. Valid range: " +
                    MIN_YEAR + " .. " + current + " (enter '0' if unknown).");
        }
        if (rarityLevel == null) {
            throw new IllegalArgumentException("Rarity level is required.");
        }
        if (conditionGrade == null) {
            throw new IllegalArgumentException("Condition grade is required.");
        }
        if (origin == null) return;
        String o = origin.trim();
        if (o.length() > 120) {
            throw new IllegalArgumentException("Origin must not exceed 120 characters.");
        }
    }

    public void validate() {
        validateCollectibleDTO();
        validateItemDTO();
    }

    public int getYearOfOrigin() {
        return yearOfOrigin;
    }
    public Collectible.RarityLevel getRarityLevel() {
        return rarityLevel;
    }
    public Collectible.ConditionGrade getConditionGrade() {
        return conditionGrade;
    }
    public boolean isHasCertificate() {
        return hasCertificate;
    }
    public String getOrigin() {
        return origin;
    }

    @Override
    public String toString() {
        return super.toString()
                + " | yearOfOrigin: " + yearOfOrigin
                + " | rarityLevel: " + rarityLevel
                + " | conditionGrade: " + conditionGrade
                + " | hasCertificate: " + hasCertificate
                + " | origin: " + origin;
    }
}
