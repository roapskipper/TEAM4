package com.team4.model;

import java.time.LocalDateTime;
import java.io.Serializable;

public class BidTransaction extends Entity implements Serializable {
    private String auctionId;    // Khớp với ID của Auction
    private String bidderId;     // Khớp với ID của Bidder vừa đặt giá
    private double bidAmount;    // Số tiền đặt
    private LocalDateTime bidTime; // Thời điểm bấm nút đặt giá

    public BidTransaction(String id, String auctionId, String bidderId, double bidAmount) {
        super(id);
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.bidAmount = bidAmount;
        this.bidTime = LocalDateTime.now();
    }

    public String getAuctionId() { return auctionId; }
    public String getBidderId() { return bidderId; }
    public double getBidAmount() { return bidAmount; }
    public LocalDateTime getBidTime() { return bidTime; }
}