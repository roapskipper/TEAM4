package com.team4.dto.bidding;

import java.math.BigDecimal;

public class BidRequestDTO {
    private String auctionId;
    private String bidderId;
    private BigDecimal amount;

    public BidRequestDTO() {
    }

    public BidRequestDTO(String auctionId, String bidderId, BigDecimal amount) {
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.amount = amount;
        validateBidRequest();
    }

    public void validateBidRequest() {
        if (auctionId == null) {
            throw new IllegalArgumentException("AuctionId must not be null.");
        }
        if (bidderId == null) {
            throw new IllegalArgumentException("BidderId must not be null.");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("BidAmount must be greater than 0.");
        }
    }

    public String getAuctionId() {
        return auctionId;
    }

    public String getBidderId() {
        return bidderId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    @Override
    public String toString() {
        return "BidRequestDTO: auctionbId=" + auctionId + ", bidderid=" + bidderId + ", amount=" + amount;
    }
}
