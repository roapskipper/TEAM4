package com.team4.model;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/** Model cho tính năng đấu giá tự động
 *
 */
public class AutoBidding extends Entity {
    private final String auctionId;
    private final String bidderId;
    private BigDecimal maxLimit;        // giới hạn cao nhất bidder chấp nhận trả
    private boolean isActive;           // bật/tắt auto-bid
    // Tạo cấu hình mới
    public AutoBidding(String auctionId,
                       String bidderId,
                       BigDecimal maxLimit) {
        super();
        this.auctionId = Objects.requireNonNull(auctionId, "auctionId must not be null").trim();
        this.bidderId = Objects.requireNonNull(bidderId, "bidderId must not be null").trim();
        this.maxLimit = money(maxLimit);
        this.isActive = true;
        validateConfig();
    }
    // Nạp từ DB (id, createdAt do Entity)
    public AutoBidding(String id,
                       LocalDateTime createdAt,
                       String auctionId,
                       String bidderId,
                       BigDecimal maxLimit,
                       boolean isActive) {
        super(id, createdAt);
        this.auctionId = Objects.requireNonNull(auctionId, "auctionId must not be null").trim();
        this.bidderId = Objects.requireNonNull(bidderId, "bidderId must not be null").trim();
        this.maxLimit = money(maxLimit);
        this.isActive = isActive;
        validateConfig();
    }

    public void validateConfig() {
        if (maxLimit == null || maxLimit.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("maxLimit must be greater than 0");
        }
        if (this.auctionId.isEmpty()) throw new IllegalArgumentException("auctionId must not be blank");
        if (this.bidderId.isEmpty()) throw new IllegalArgumentException("bidderId must not be blank");
    }

    // maxLimit phải lớn hơn giá hiện tại
    public static void validateMaxLimit(BigDecimal maxLimit, BigDecimal currentAuctionPrice) {
        Objects.requireNonNull(currentAuctionPrice, "currentAuctionPrice must not be null");
        if (maxLimit.compareTo(money(currentAuctionPrice)) <= 0) {
            throw new IllegalArgumentException("maxLimit must be greater than the current auction price");
        }
    }

    // Bật/tắt auto-bid (synchronized để an toàn đơn giản đa luồng)
    public synchronized void activate() { this.isActive = true; }
    public synchronized void deactivate() { this.isActive = false; }

    private static BigDecimal money(BigDecimal amount) {
        if (amount == null) throw new IllegalArgumentException("Amount must not be null");
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
    // Getters / setters
    public String getAuctionId() { return auctionId; }
    public String getBidderId() { return bidderId; }
    public BigDecimal getMaxLimit() { return maxLimit; }
    public void setMaxLimit(BigDecimal maxLimit) {
        this.maxLimit = money(maxLimit);
        validateConfig();
    }
    public boolean isActive() { return isActive; }
    @Override
    public String toString() {
        return "AutoBidding: " + getId() +
                " | auctionId : " + auctionId +
                " | bidderId : " + bidderId +
                " | maxLimit : " + maxLimit +
                " | isActive : " + isActive;
    }
}
