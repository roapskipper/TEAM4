package com.team4.model;

import java.io.Serializable;
import java.util.UUID;

/**
 * Lớp AutoBidding - Lệnh đấu giá tự động.
 * Kế thừa từ Entity để sử dụng hệ thống UUID chuẩn TEAM4.
 */
public class AutoBidding extends Entity implements Serializable {
    private static final long serialVersionUID = 1L;

    private String auctionId;        // ID của phiên đấu giá đang theo dõi
    private String bidderId;         // ID của người đặt lệnh tự động
    private double maxLimit;         // Số tiền cao nhất họ có thể trả
    private double incrementAmount;  // Bước giá tự động tăng thêm mỗi lần bị vượt mặt
    private boolean isActive;        // Trạng thái lệnh (Bật/Tắt)

    /**
     * CONSTRUCTOR 1: Dùng khi người dùng cài đặt lệnh đấu giá tự động mới.
     * Tự động sinh UUID và đặt trạng thái hoạt động là true.
     */
    public AutoBidding(String auctionId, String bidderId, double maxLimit, double incrementAmount) {
        super(UUID.randomUUID().toString()); // Sinh ID cho lệnh tự động
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.maxLimit = maxLimit;
        this.incrementAmount = incrementAmount;
        this.isActive = true;
    }

    /**
     * CONSTRUCTOR 2: Dùng cho DAO nạp dữ liệu từ MySQL lên.
     */
    public AutoBidding(String id, String auctionId, String bidderId, double maxLimit, double incrementAmount, boolean isActive) {
        super(id);
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.maxLimit = maxLimit;
        this.incrementAmount = incrementAmount;
        this.isActive = isActive;
    }

    // --- CÁC PHƯƠNG THỨC LOGIC ---

    /**
     * Tính toán giá thầu tiếp theo nếu có người khác trả giá cao hơn.
     * Trả về -1 nếu giá thầu tiếp theo vượt quá giới hạn maxLimit.
     */
    public double calculateNextBid(double currentAuctionPrice) {
        double nextBid = currentAuctionPrice + incrementAmount;
        if (nextBid <= maxLimit) {
            return nextBid;
        }
        return -1; // Không thể đặt giá thêm vì đã chạm trần
    }

    // --- GETTERS & SETTERS (ENCAPSULATION) ---

    public String getAuctionId() { return auctionId; }
    public String getBidderId() { return bidderId; }

    public double getMaxLimit() { return maxLimit; }
    public void setMaxLimit(double maxLimit) { this.maxLimit = maxLimit; }

    public double getIncrementAmount() { return incrementAmount; }
    public void setIncrementAmount(double incrementAmount) { this.incrementAmount = incrementAmount; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    @Override
    public String toString() {
        return "AutoBidding [ID: " + getId() + "] - Auction: " + auctionId +
                ", Bidder: " + bidderId + ", MaxLimit: $" + maxLimit +
                ", Step: +$" + incrementAmount + ", Status: " + (isActive ? "ACTIVE" : "DISABLED");
    }
}