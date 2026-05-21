package com.team4.dto.auction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CreateAuctionRequestDTO {
    private String itemId;
    private String sellerId;
    private BigDecimal startingPrice;
    private BigDecimal bidIncrement;
    private LocalDateTime endTime;

    public CreateAuctionRequestDTO() {}
    public CreateAuctionRequestDTO(String itemId, String sellerId, BigDecimal startingPrice, BigDecimal bidIncrement, LocalDateTime endTime) {
        this.itemId = itemId;
        this.sellerId = sellerId;
        this.startingPrice = startingPrice;
        this.bidIncrement = bidIncrement;
        this.endTime = endTime;
        validateAuctionRequestDTO();
    }

    public void validateAuctionRequestDTO() {
        if (itemId == null || itemId.trim().isEmpty()) {
            throw new IllegalArgumentException("ItemId must not be blank.");
        }
        if (sellerId == null)
            throw new IllegalArgumentException("SellerId must not be null.");
        if (startingPrice == null || startingPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Starting price must be greater than 0.");
        }
        if (bidIncrement == null || bidIncrement.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Bid increment must be greater than 0.");
        }
        if (endTime == null) {
            throw new IllegalArgumentException("End time must not be null.");
        }
        if (endTime.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("End time must be in the future.");
        }
    }

    public String getItemId() { return itemId; }
    public BigDecimal getStartingPrice() { return startingPrice; }
    public BigDecimal getBidIncrement() { return bidIncrement; }
    public LocalDateTime getEndTime() { return endTime; }
    public String getSellerId() { return sellerId; }

    @Override
    public String toString() {
        return "CreateAuction: itemId=" + itemId + " , startingPrice=" + startingPrice + ", bidIncrement=" + bidIncrement;
    }
}
