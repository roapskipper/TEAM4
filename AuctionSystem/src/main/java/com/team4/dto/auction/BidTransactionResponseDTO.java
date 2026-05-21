package com.team4.dto.auction;

import java.math.BigDecimal;

/**
 * DTO hiển thị lịch sử đặt giá của một phiên đấu giá.
 */
public class BidTransactionResponseDTO {
    private String id;
    private String auctionId;
    private String bidderId;
    private BigDecimal bidAmount;
    private String bidTime;

    public BidTransactionResponseDTO() {}

    public BidTransactionResponseDTO(String id, String auctionId, String bidderId, BigDecimal bidAmount, String bidTime) {
        this.id = id;
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.bidAmount = bidAmount;
        this.bidTime = bidTime;
    }

    public String getId() { return id; }
    public String getAuctionId() { return auctionId; }
    public String getBidderId() { return bidderId; }
    public BigDecimal getBidAmount() { return bidAmount; }
    public String getBidTime() { return bidTime; }

    @Override
    public String toString() {
        return "BidTransactionResponseDTO{" +
                "bidderId='" + bidderId + '\'' +
                ", bidAmount=" + bidAmount +
                ", bidTime='" + bidTime + '\'' +
                '}';
    }
}
