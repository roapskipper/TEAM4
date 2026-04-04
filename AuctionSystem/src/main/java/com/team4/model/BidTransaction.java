package com.team4.model;

import java.time.LocalDateTime;
import java.io.Serializable;
import java.util.UUID;

/**
 * Lớp BidTransaction - Bản ghi lịch sử đặt giá.
 * Kế thừa từ Entity để đồng bộ hệ thống String ID (UUID).
 */
public class BidTransaction extends Entity implements Serializable {
    private static final long serialVersionUID = 1L;

    private String auctionId;     // ID phiên đấu giá tham gia
    private String bidderId;      // ID người thực hiện đặt giá
    private double bidAmount;     // Số tiền đặt
    private LocalDateTime bidTime;// Thời điểm máy chủ ghi nhận giao dịch

    /**
     * CONSTRUCTOR 1: Dùng khi người mua vừa bấm nút đặt giá.
     * Tự động sinh mã giao dịch (UUID) và ghi nhận thời gian hiện tại.
     */
    public BidTransaction(String auctionId, String bidderId, double bidAmount) {
        // Tự động cấp ID giao dịch duy nhất toàn cầu
        super(UUID.randomUUID().toString());
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.bidAmount = bidAmount;
        this.bidTime = LocalDateTime.now();
    }

    /**
     * CONSTRUCTOR 2: Dùng cho DAO nạp dữ liệu lịch sử từ Database lên.
     */
    public BidTransaction(String id, String auctionId, String bidderId, double bidAmount, LocalDateTime bidTime) {
        super(id); // Sử dụng mã giao dịch cũ từ MySQL
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.bidAmount = bidAmount;
        this.bidTime = bidTime;
    }

    // --- CÁC PHƯƠNG THỨC LOGIC ---

    public boolean isValidBid() {
        return bidAmount > 0 && auctionId != null && bidderId != null;
    }

    // --- GETTERS (ENCAPSULATION) ---
    // Giao dịch là dữ liệu quá khứ nên chỉ có Getters (Dữ liệu bất biến)

    public String getAuctionId() { return auctionId; }
    public String getBidderId() { return bidderId; }
    public double getBidAmount() { return bidAmount; }
    public LocalDateTime getBidTime() { return bidTime; }

    @Override
    public String toString() {
        return "Transaction [ID: " + getId().substring(0,8) + "...] " +
                "Bidder: " + bidderId + " trả $" + bidAmount +
                " lúc: " + bidTime.toLocalTime();
    }
}