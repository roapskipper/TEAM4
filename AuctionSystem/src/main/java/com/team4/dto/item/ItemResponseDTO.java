package com.team4.dto.item;

import com.team4.model.Item;
import java.math.BigDecimal;

public class ItemResponseDTO {
    private String id;
    private String name;
    private BigDecimal startingPrice;
    private Item.ItemCategory category;
    private String ownerId;
    private String createdAt;

    public ItemResponseDTO() {}
    public ItemResponseDTO(String id, String name, BigDecimal startingPrice, Item.ItemCategory category, String ownerId, String createdAt) {
        this.id = id;
        this.name = name;
        this.startingPrice = startingPrice;
        this.category = category;
        this.ownerId = ownerId;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public String getId() { return id; }

    public String getName() { return name; }

    public BigDecimal getStartingPrice() { return startingPrice; }

    public Item.ItemCategory getCategory() { return category; }

    public String getOwnerId() { return ownerId; }

    public String getCreatedAt() { return createdAt; }

}
