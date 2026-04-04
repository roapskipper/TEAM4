package com.team4.model;

import java.time.LocalDateTime;
import java.io.Serializable;
import java.util.UUID;

/**
 * Lớp Auction - Phiên đấu giá.
 * Kế thừa từ Entity để sử dụng UUID và đồng bộ với hệ thống.
 */
public class Auction extends Entity implements Serializable {
    private static final long serialVersionUID = 1L;

    private String itemId;                // Link tới vật phẩm đang đấu giá
    private String sellerId;              // Link tới người tạo (Seller)
    private String currentHighestBidderId;// Link tới người trả giá cao nhất (Bidder)
    private double currentPrice;          // Giá hiện tại của phiên
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;                // PENDING, ACTIVE, CLOSED, CANCELLED

    /**
     * CONSTRUCTOR 1: Dùng khi Seller tạo một phiên đấu giá mới.
     * Tự động sinh UUID, startTime là hiện tại và status mặc định là ACTIVE.
     */
    public Auction(String itemId, String sellerId, double startingPrice, LocalDateTime endTime) {
        super(UUID.randomUUID().toString()); // Tự sinh ID phiên đấu giá
        this.itemId = itemId;
        this.sellerId = sellerId;
        this.currentPrice = startingPrice;
        this.startTime = LocalDateTime.now();
        this.endTime = endTime;
        this.status = "ACTIVE";
    }

    /**
     * CONSTRUCTOR 2: Dùng cho DAO khi nạp dữ liệu từ Database lên Java.
     */
    public Auction(String id, String itemId, String sellerId, String currentHighestBidderId,
                   double currentPrice, LocalDateTime startTime, LocalDateTime endTime, String status) {
        super(id); // Sử dụng ID cũ từ MySQL
        this.itemId = itemId;
        this.sellerId = sellerId;
        this.currentHighestBidderId = currentHighestBidderId;
        this.currentPrice = currentPrice;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
    }

    // --- CÁC PHƯƠNG THỨC LOGIC NGHIỆP VỤ ---

    /**
     * Kiểm tra phiên đã kết thúc chưa dựa trên thời gian hiện tại
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(endTime);
    }

    /**
     * Kiểm tra xem phiên còn hiệu lực để đặt giá không
     */
    public boolean canBid() {
        return "ACTIVE".equalsIgnoreCase(status) && !isExpired();
    }

    /**
     * Phương thức hỗ trợ hiển thị thời gian còn lại (Dạng rút gọn cho Console)
     */
    public String getTimeRemaining() {
        if (isExpired()) return "Đã kết thúc";
        java.time.Duration duration = java.time.Duration.between(LocalDateTime.now(), endTime);
        return String.format("%d giờ, %d phút", duration.toHours(), duration.toMinutesPart());
    }

    // --- GETTERS & SETTERS (ENCAPSULATION) ---

    public String getItemId() { return itemId; }
    public String getSellerId() { return sellerId; }

    public double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(double currentPrice) {
        if (currentPrice > this.currentPrice) {
            this.currentPrice = currentPrice;
        }
    }

    public String getCurrentHighestBidderId() { return currentHighestBidderId; }
    public void setCurrentHighestBidderId(String bidderId) {
        this.currentHighestBidderId = bidderId;
    }

    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return "Auction [" + status + "] - ItemID: " + itemId +
                ", Highest Price: $" + currentPrice +
                ", WinnerID: " + (currentHighestBidderId != null ? currentHighestBidderId : "None") +
                ", Ends in: " + getTimeRemaining();
    }
}