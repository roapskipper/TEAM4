package com.team4.dto.auction;

import com.team4.model.Auction;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AuctionResponseDTO {
    private final String id;
    private final String itemId;
    private final String sellerId;
    private final BigDecimal startingPrice;
    private final BigDecimal bidIncrement;
    private final String currentHighestBidderId;
    private final BigDecimal currentPrice;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final Auction.AuctionStatus status;
    private final LocalDateTime createdAt;

    public AuctionResponseDTO(String id, String itemId, String sellerId, BigDecimal startingPrice, 
                              BigDecimal bidIncrement, String currentHighestBidderId, BigDecimal currentPrice, 
                              LocalDateTime startTime, LocalDateTime endTime, Auction.AuctionStatus status, 
                              LocalDateTime createdAt) {
        this.id = id;
        this.itemId = itemId;
        this.sellerId = sellerId;
        this.startingPrice = startingPrice;
        this.bidIncrement = bidIncrement;
        this.currentHighestBidderId = currentHighestBidderId;
        this.currentPrice = currentPrice;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.createdAt = createdAt;
    }

    // Getters only (Immutable)
    public String getId() { return id; }
    public String getItemId() { return itemId; }
    public String getSellerId() { return sellerId; }
    public BigDecimal getStartingPrice() { return startingPrice; }
    public BigDecimal getBidIncrement() { return bidIncrement; }
    public String getCurrentHighestBidderId() { return currentHighestBidderId; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public Auction.AuctionStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    @Override
    public String toString() {
        return "AuctionResponseDTO{" +
                "id='" + id + '\'' +
                ", status=" + status +
                ", currentPrice=" + currentPrice +
                '}';
    }
}
