package com.team4.dto.auction;

import com.team4.model.Auction;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AuctionResponseDTO {
    private String id;
    private String itemId;
    private String sellerId;
    private BigDecimal bidIncrement;
    private BigDecimal currentPrice;
    private String endTime;
    private Auction.AuctionStatus status;
    private String creatAt;

    public  AuctionResponseDTO() {}
    public AuctionResponseDTO(String id, String itemId, String sellerId,
                              BigDecimal bidIncrement, BigDecimal currentPrice,
                              String endTime, Auction.AuctionStatus status, String creatAt) {
        this.id = id;
        this.itemId = itemId;
        this.sellerId = sellerId;
        this.bidIncrement = bidIncrement;
        this.currentPrice = currentPrice;
        this.endTime = endTime;
        this.status = status;
        this.creatAt = creatAt;
    }

    public String getId() { return id; }
    public String getItemId() { return itemId; }
    public String getSellerId() { return sellerId; }
    public BigDecimal getBidIncrement() { return bidIncrement; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public String getEndTime() { return endTime; }
    public Auction.AuctionStatus getStatus() { return status; }
    public String getCreatAt() { return creatAt; }

    @Override
    public String toString() {
        return "AuctionResponseDTO{" +
                "id='" + id + '\'' +
                ", status=" + status +
                ", currentPrice=" + currentPrice +
                '}';
    }
}
