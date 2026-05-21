package com.team4.dto.item;

import com.team4.dto.auction.CreateItemRequestDTO;
import com.team4.model.Collectible;
import com.team4.model.Item;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CreateCollectibleRequestDTO extends CreateItemRequestDTO {
    private int yearOfOrigin;       // năm xuất xứ
    private Collectible.RarityLevel rarityLevel;     // độ hiếm
    private Collectible.ConditionGrade conditionGrade; // tình trạng
    private boolean hasCertificate;  // có chứng chỉ không
    private String origin;           // xuất xứ (quốc gia/vùng)

    public CreateCollectibleRequestDTO() {}
    public CreateCollectibleRequestDTO(String name, BigDecimal startingPrice, String description, Item.ItemCategory category, int yearOfOrigin, Collectible.RarityLevel rarityLevel, Collectible.ConditionGrade conditionGrade, boolean hasCertificate, String origin) {
        super(name,startingPrice,description,category);
        this.yearOfOrigin = yearOfOrigin;
        this.rarityLevel = rarityLevel;
        this.conditionGrade = conditionGrade;
        this.hasCertificate = hasCertificate;
        this.origin = origin;
        validateCollectibleDTO();
    }

    protected final void validateCollectibleDTO() {
        if (yearOfOrigin == 0) return; // unknown
        int current = LocalDate.now().getYear();
        final int MIN_YEAR = -3000;
        if (yearOfOrigin < MIN_YEAR || yearOfOrigin > current) {
            throw new IllegalArgumentException("Invalid production year. Valid range: " +
                    MIN_YEAR + " .. " + current + " (Use '0' if unknown).");
        }
        if (rarityLevel == null)
            throw new IllegalArgumentException("Rarity level must not be blank.");
        if (conditionGrade == null)
            throw new IllegalArgumentException("Condition grade must not be blank.");
        if (origin == null) return;
        String o = origin.trim();
        if (o.length() > 120) throw new IllegalArgumentException("Origin is too long (maximum 120 characters).");
    }

    public void validate() {
        validateCollectibleDTO();
        validateItemDTO();
    }

    public  int getYearOfOrigin() {
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
