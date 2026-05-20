package com.team4.dto.bidding;

import java.math.BigDecimal;

/**
 * Phản hồi thông tin chi tiết về cấu hình tự động đặt giá (Auto-bid).
 */
public class AutoBidResponseDTO {
    private String id;
    private String auctionId;
    private String bidderId;
    private BigDecimal maxLimit;
    private boolean isActive;

    public AutoBidResponseDTO() {}

    public AutoBidResponseDTO(String id, String auctionId, String bidderId, BigDecimal maxLimit, boolean isActive) {
        this.id = id;
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.maxLimit = maxLimit;
        this.isActive = isActive;
    }

    public String getId() { return id; }
    public String getAuctionId() { return auctionId; }
    public String getBidderId() { return bidderId; }
    public BigDecimal getMaxLimit() { return maxLimit; }
    public boolean isActive() { return isActive; }

    @Override
    public String toString() {
        return "AutoBidResponseDTO{" +
                "id='" + id + '\'' +
                ", auctionId='" + auctionId + '\'' +
                ", maxLimit=" + maxLimit +
                ", active=" + isActive +
                '}';
    }
}
