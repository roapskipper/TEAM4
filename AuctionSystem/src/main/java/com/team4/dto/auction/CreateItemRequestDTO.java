package com.team4.dto.auction;

import com.team4.model.Item;
import java.math.BigDecimal;

public class CreateItemRequestDTO {
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
            throw new IllegalArgumentException("Tên mặt hàng không được để trống.");
        }
        if (name.length() > NAME_MAX) {
            throw new IllegalArgumentException("Tên mặt hàng không được vượt quá " + NAME_MAX + " ký tự.");
        }
        if (startingPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Giá khởi điểm không được âm.");
        }
        if (category == null) {
            throw new IllegalArgumentException("Category không được để trống.");
        }
        if (description == null || description.isEmpty()) {
            throw new IllegalArgumentException("Mô tả không được để trống.");
        }
        if (description.length() > DESC_MAX) {
            throw new IllegalArgumentException("Mô tả không được vượt quá " + DESC_MAX + " ký tự.");
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
        return "Item: " +
                " | name: " + name +
                " | startingPrice: " + (startingPrice==null ? "n/a" : startingPrice.toPlainString()) +
                " | category: " + category ;
    }
}
