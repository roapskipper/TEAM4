package com.team4.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

public class Auction extends Entity {
    // Giả sử luật: Nếu bid trong 1 phút cuối -> Cộng thêm 1 phút
    private static final int SNIPING_THRESHOLD_MINUTES = 5;
    private static final int EXTENSION_MINUTES = 30;
    // enum các trạng thái của phiên đấu giá
    public enum AuctionStatus {
        PENDING, RUNNING, FINISHED, PAID ,CANCELLED
    }
    private String itemId;
    private String sellerId;
    private BigDecimal startingPrice;
    private BigDecimal bidIncrement; // bước giá tối thiểu mỗi lần bid
    private String currentHighestBidderId;
    private BigDecimal currentPrice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AuctionStatus status;

    // Constructor khi tạo một cuộc đấu giá mới
    public Auction(String itemId, String sellerId, BigDecimal startingPrice,BigDecimal bidIncrement, LocalDateTime endTime) {
        super();
        this.itemId = itemId;
        this.sellerId = sellerId;
        this.startingPrice = money(startingPrice);
        this.bidIncrement = money(bidIncrement);
        this.currentPrice = money(startingPrice);
        this.startTime = LocalDateTime.now();
        this.endTime = endTime;
        this.status = AuctionStatus.PENDING; // Mă định là "Pending approval"
        validateAuctionInfo();
    }
    // Constructor khi nạp cuộc đấu giá lên từ DB
    public Auction(String id, LocalDateTime createdAt,
                   String itemId, String sellerId,
                   String currentHighestBidderId,
                   BigDecimal startingPrice, BigDecimal currentPrice,
                   BigDecimal bidIncrement,
                   LocalDateTime startTime, LocalDateTime endTime,
                   AuctionStatus status) {
        super(id, createdAt);
        this.itemId = itemId;
        this.sellerId = sellerId;
        this.startingPrice = money(startingPrice);
        this.bidIncrement = money(bidIncrement);
        this.currentHighestBidderId = currentHighestBidderId;
        this.currentPrice = money(currentPrice);
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        validateAuctionInfo();
    }

    // Validate và chuẩn hóa các field
    private void validateAuctionInfo() {
        if (itemId == null)
            throw new IllegalArgumentException("ItemId must not be null");
        if (sellerId == null)
            throw new IllegalArgumentException("SellerId must not be null");
        if (startingPrice == null || startingPrice.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Starting price must be greater than 0");
        if (bidIncrement == null || bidIncrement.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Bid increment must be greater than 0");
        if (startTime == null)
            throw new IllegalArgumentException("Start time must not be null");
        if (endTime == null)
            throw new IllegalArgumentException("End time must not be null");
        if (currentPrice.compareTo(startingPrice) < 0)
            throw new IllegalArgumentException("Current price cannot be lower than the starting price");
        if (status == null)
            throw new IllegalArgumentException("Auction status must not be null");
    }
    // Làm tròn tiền đến phần trăm
    private static BigDecimal money(BigDecimal amount) {
        if (amount == null) throw new IllegalArgumentException("Amount must not be null."); // Tránh NPE
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
    /** Các method dưới đây đều public,do service sẽ cần gọi tới*/
    // Kiểm tra phiên đã hết thời gian chưa
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(endTime);
    }
    // Kiểm tra có thể đặt giá không
    public boolean canBid() {
        return status == AuctionStatus.RUNNING && !isExpired();
    }
    // Cập nhật giá mới và người dẫn đầu
    public void applyBid(String bidderId, BigDecimal amount) {
        if (!canBid())
            throw new IllegalStateException("This auction cannot accept bids at this time");
        if (bidderId == null)
            throw new IllegalArgumentException("BidderId must not be null");
        if (amount == null)
            throw new IllegalArgumentException("Amount must not be null");

        this.currentPrice = money(amount);
        this.currentHighestBidderId = bidderId;
    }
    // RUNNING -> CLOSED
    public void close() {
        if (status != AuctionStatus.RUNNING)
            throw new IllegalStateException("Can only close from RUNNING");
        this.status = AuctionStatus.FINISHED;
    }
    // CLOSED -> PAID, Dùng khi: bidder thanh toán thành công
    public void markPaid() {
        if (status != AuctionStatus.FINISHED)
            throw new IllegalStateException("Can only mark paid from FINISHED");
        this.status = AuctionStatus.PAID;
    }
    //  CANCELLED (từ PENDING hoặc ACTIVE), Dùng khi: admin từ chối hoặc seller hủy
    public void cancel() {
        if (status == AuctionStatus.PAID)
            throw new IllegalStateException("Cannot cancel an auction that has already been paid");
        this.status = AuctionStatus.CANCELLED;
    }
    // Chuyển từ PENDING sang ACTIVE
    public void approve() {
        this.status = AuctionStatus.RUNNING;
    }

    public boolean applyAntiSniping() {
        LocalDateTime now = LocalDateTime.now();
        // Nếu thời gian hiện tại cộng thêm THRESHOLD mà vượt quá endTime
        if (now.plusMinutes(SNIPING_THRESHOLD_MINUTES).isAfter(this.endTime)
                && now.isBefore(this.endTime)) {
            // Dời endTime lên thêm EXTENSION_MINUTES
            this.endTime = this.endTime.plusMinutes(EXTENSION_MINUTES);
            return true;
        }
        return false;
    }

    // Getter/ Setter
    // Setter chỉ cho phép thay đổi bidIncrement
    public String getItemId() {
        return itemId;
    }
    public String getSellerId() {
        return sellerId;
    }
    public String getCurrentHighestBidderId() {
        return currentHighestBidderId;
    }
    public BigDecimal getStartingPrice() {
        return startingPrice;
    }
    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }
    public BigDecimal getBidIncrement() {
        return bidIncrement;
    }
    public LocalDateTime getStartTime() {
        return startTime;
    }
    public LocalDateTime getEndTime() {
        return endTime;
    }
    public AuctionStatus getStatus() {
        return status;
    }
    public void setBidIncrement(BigDecimal bidIncrement) {
        if (bidIncrement.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Bid increment must be greater than 0");
        this.bidIncrement = money(bidIncrement);
    }
     @Override
     public String toString() {
         return "Item: " + itemId +
                 " | Seller: " + sellerId +
                 " | Current Price: $" + (currentPrice == null ? "n/a" : currentPrice.toPlainString()) + // tránh gọi toString của currentPrice
                 " | Bid: " + bidIncrement +
                 " | Status: " + status;
     }
}