package com.team4.dto.auction;

import com.team4.model.Item;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ItemResponseDTO {
    private String id;
    private String name;
    private String description;
    private BigDecimal startingPrice;
    private Item.ItemCategory category;
    private String ownerId;
    private LocalDateTime createdAt;
    private String summary; // Chứa thông tin đặc thù của từng loại sản phẩm (từ model.summary())

    public ItemResponseDTO() {}

    // Getters and Setters
    public String getId() { return id; }

    public String getName() { return name; }

    public String getDescription() { return description; }

    public BigDecimal getStartingPrice() { return startingPrice; }

    public Item.ItemCategory getCategory() { return category; }

    public String getOwnerId() { return ownerId; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public String getSummary() { return summary; }
}
