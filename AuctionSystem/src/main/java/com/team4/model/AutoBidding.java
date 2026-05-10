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
    private static final long serialVersionUID = 1L;
    private final String auctionId;
    private final String bidderId;
    private BigDecimal maxLimit;        // giới hạn cao nhất bidder chấp nhận trả
    private BigDecimal incrementAmount; // bước giá tự động
    private boolean isActive;           // bật/tắt auto-bid
    // Tạo cấu hình mới
    public AutoBidding(String auctionId,
                       String bidderId,
                       BigDecimal maxLimit,
                       BigDecimal incrementAmount) {
        super();
        this.auctionId = Objects.requireNonNull(auctionId, "auctionId không được null").trim();
        this.bidderId = Objects.requireNonNull(bidderId, "bidderId không đợc null").trim();
        this.maxLimit = money(maxLimit);
        this.incrementAmount = money(incrementAmount);
        this.isActive = true;
        validateConfig();
    }
    // Nạp từ DB (id, createdAt do Entity)
    public AutoBidding(String id,
                       LocalDateTime createdAt,
                       String auctionId,
                       String bidderId,
                       BigDecimal maxLimit,
                       BigDecimal incrementAmount,
                       boolean isActive) {
        super(id, createdAt);
        this.auctionId = Objects.requireNonNull(auctionId, "auctionId không được null").trim();
        this.bidderId = Objects.requireNonNull(bidderId, "bidderId không được null").trim();
        this.maxLimit = money(maxLimit);
        this.incrementAmount = money(incrementAmount);
        this.isActive = isActive;
        validateConfig();
    }

    public void validateConfig() {
        if (incrementAmount == null || incrementAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("incrementAmount phải lớn hơn 0");
        }
        if (maxLimit == null || maxLimit.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("maxLimit phải lớn hơn 0");
        }
        if (this.auctionId.isEmpty()) throw new IllegalArgumentException("auctionId không được rỗng");
        if (this.bidderId.isEmpty()) throw new IllegalArgumentException("bidderId không được rỗng");
    }

    // maxLimit phải lớn hơn giá hiện tại
    public void validateConfig(BigDecimal currentAuctionPrice) {
        Objects.requireNonNull(currentAuctionPrice, "currentAuctionPrice không được null");
        validateConfig();
        if (maxLimit.compareTo(money(currentAuctionPrice)) <= 0) {
            throw new IllegalArgumentException("maxLimit phải lớn hơn giá hiện tại của phiên");
        }
    }
    /**
     * Tính giá đặt kế tiếp = currentAuctionPrice + incrementAmount.
     * - Nếu auto-bid không active hoặc giá mới vượt quá maxLimit -> trả Optional.empty()
     * - Trả Optional.of(nextBid) nếu hợp lệ (đã làm tròn scale = 2)
     * - Dùng Optional để tránh NPE, tốn bộ nhớ hơn 1 chút
     */
    public Optional<BigDecimal> calculateNextBid(BigDecimal currentAuctionPrice) {
        Objects.requireNonNull(currentAuctionPrice, "currentAuctionPrice không được null");
        if (!isActive) return Optional.empty(); // Trả về "không gì cả"
        BigDecimal current = money(currentAuctionPrice);
        BigDecimal next = money(current.add(incrementAmount));
        if (next.compareTo(maxLimit) > 0) {
            return Optional.empty();
        }
        return Optional.of(next);
    }
    // Bật/tắt auto-bid (synchronized để an toàn đơn giản đa luồng)
    public synchronized void activate() { this.isActive = true; }
    public synchronized void deactivate() { this.isActive = false; }

    private static BigDecimal money(BigDecimal amount) {
        if (amount == null) throw new IllegalArgumentException("Amount không được null");
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
    public BigDecimal getIncrementAmount() { return incrementAmount; }
    public void setIncrementAmount(BigDecimal incrementAmount) {
        this.incrementAmount = money(incrementAmount);
        validateConfig();
    }
    public boolean isActive() { return isActive; }
    @Override
    public String toString() {
        return "AutoBidding: " + getId() +
                " | auctionId : " + auctionId +
                " | bidderId : " + bidderId +
                " | maxLimit : " + maxLimit +
                " | incrementAmount : " + incrementAmount +
                " | isActive : " + isActive;
    }
}
