package com.team4.dto.socket;

import java.math.BigDecimal;

public class AuctionEndResponseDTO {
    private String auctionId;
    private BigDecimal finalPrice;
    private String winnerId;

    public AuctionEndResponseDTO() {
    }

    public AuctionEndResponseDTO(String auctionId, BigDecimal finalPrice, String winnerId) {
        this.auctionId = auctionId;
        this.finalPrice = finalPrice;
        this.winnerId = winnerId;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public BigDecimal getFinalPrice() {
        return finalPrice;
    }

    public String getWinnerId() {
        return winnerId;
    }
}
