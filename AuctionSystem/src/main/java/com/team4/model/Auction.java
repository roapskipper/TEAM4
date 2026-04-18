package com.team4.model;

import java.time.LocalDateTime;
import java.io.Serializable;

public class Auction extends Entity implements Serializable {
    private String itemId;             // Khớp với ID của Item
    private String sellerId;           // Khớp với ID của Seller tạo ra phiên này
    private String currentHighestBidderId; // Khớp với ID của Bidder đang trả giá cao nhất
    private double currentPrice;       // Giá cao nhất hiện tại
    private LocalDateTime startTime;   // Thời gian bắt đầu
    private LocalDateTime endTime;     // Thời gian kết thúc
    private String status;             // PENDING, ACTIVE, FINISHED, CANCELLED

    public Auction(String id, String itemId, String sellerId, double startingPrice, LocalDateTime endTime) {
        super(id);
        this.itemId = itemId;
        this.sellerId = sellerId;
        this.currentPrice = startingPrice;
        this.startTime = LocalDateTime.now();
        this.endTime = endTime;
        this.status = "ACTIVE";
    }

    public String getItemId() { return itemId; }
    public String getSellerId() { return sellerId; }
    public double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }

    public String getCurrentHighestBidderId() { return currentHighestBidderId; }
    public void setCurrentHighestBidderId(String bidderId) { this.currentHighestBidderId = bidderId; }

    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    // Logic kiểm tra xem phiên đấu giá đã hết hạn chưa
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(endTime);
    }
}