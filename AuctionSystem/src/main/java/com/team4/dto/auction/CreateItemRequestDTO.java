package com.team4.dto.auction;

import com.team4.model.Item;
import java.math.BigDecimal;

public class
CreateItemRequestDTO {
    private String name;
    private String description;
    private BigDecimal startingPrice;
    private Item.ItemCategory category;
    private static final int NAME_MAX = 255;
    private static final int DESC_MAX = 2000;
    private static final int OWNER_ID_MAX = 36;

    public CreateItemRequestDTO() {}
    public CreateItemRequestDTO(String name, BigDecimal startingPrice, String description, Item.ItemCategory category) {
        this.name = name;
        this.startingPrice = Item.money(startingPrice);
        this.description = description;
        this.category = category;
        validateItemDTO();
    }

    protected final void validateItemDTO() {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Item name must not be blank.");
        }
        if (name.length() > NAME_MAX) {
            throw new IllegalArgumentException("Item name must not exceed " + NAME_MAX + " characters.");
        }
        if (startingPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Starting price cannot be negative.");
        }
        if (category == null) {
            throw new IllegalArgumentException("Category must not be null.");
        }
        if (description == null || description.isEmpty()) {
            throw new IllegalArgumentException("Description must not be blank.");
        }
        if (description.length() > DESC_MAX) {
            throw new IllegalArgumentException("Description must not exceed " + DESC_MAX + " characters.");
        }
    }

    public String getName() {
        return name;
    }
    public String getDescription() {
        return description;
    }
    public BigDecimal getStartingPrice() {
        return startingPrice;
    }
    public Item.ItemCategory getCategory() {
        return category;
    }

    @Override
    public String toString() {
        return "ItemRequestDTO: " +
                " | name: " + name +
                " | startingPrice: " + (startingPrice==null ? "n/a" : startingPrice.toPlainString()) +
                " | category: " + category ;
    }
}
