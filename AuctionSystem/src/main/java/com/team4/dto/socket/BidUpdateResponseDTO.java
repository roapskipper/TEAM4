package com.team4.dto.socket;

import java.math.BigDecimal;

public class BidUpdateResponseDTO {
    private String auctionId;
    private BigDecimal currentPrice;
    private String currentHighestBidderId;
    private String endTime;

    public BidUpdateResponseDTO() {
    }

    public BidUpdateResponseDTO(String auctionId, BigDecimal currentPrice, String currentHighestBidderId, String endTime) {
        this.auctionId = auctionId;
        this.currentPrice = currentPrice;
        this.currentHighestBidderId = currentHighestBidderId;
        this.endTime = endTime;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public String getCurrentHighestBidderId() {
        return currentHighestBidderId;
    }

    public String getEndTime() {
        return endTime;
    }
}
