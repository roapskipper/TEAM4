package com.team4.dto.bidding;

import java.math.BigDecimal;

public class AutoBidRequestDTO {
    private String auctionId;
    private String bidderId;
    private BigDecimal maxAmount;

    public AutoBidRequestDTO() {
    }

    public AutoBidRequestDTO(String auctionId, String bidderId, BigDecimal maxAmount) {
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.maxAmount = maxAmount;
    }

    public void validateAutoBidRequestDTO() {
        if (maxAmount == null || maxAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("maxLimit must be greater than 0.");
        }
        if (auctionId == null || auctionId.isEmpty()) {
            throw new IllegalArgumentException("auctionId is required.");
        }
        if (bidderId == null || bidderId.isEmpty()) {
            throw new IllegalArgumentException("bidderId is required.");
        }
    }

    public String getAuctionId() {
        return auctionId;
    }

    public String getBidderId() {
        return bidderId;
    }

    public BigDecimal getMaxAmount() {
        return maxAmount;
    }

    @Override
    public String toString() {
        return "AutoBidRequestDTO: auctionId = " + auctionId + ", bidderId = " + bidderId + ", maxAmount = " + maxAmount;
    }
}
