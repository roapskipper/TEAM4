package com.team4.dto.socket;

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
            throw new IllegalArgumentException("maxLimit phải lớn hơn 0");
        }
        if (this.auctionId.isEmpty()) throw new IllegalArgumentException("auctionId không được rỗng");
        if (this.bidderId.isEmpty()) throw new IllegalArgumentException("bidderId không được rỗng");
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
