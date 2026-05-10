package com.team4.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;

public class BidTransaction extends Entity{
    private static final long serialVersionUID = 1L;
    private final String auctionId; // id cuộc đấu giá,tham chiếu đến id của model Auction
    private final String bidderId; // id người đặt giá, tham chiếu đến id của model User
    private final BigDecimal bidAmount; // mức đặt giá
    private LocalDateTime bidTime; // thời gian lúc đặt giá

    // Constructor khi tạo một giao dịch đặt giá mới
    public BidTransaction(String auctionId, String bidderId, BigDecimal bidAmount) {
        super();
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.bidAmount = normalizeMoney(bidAmount);
        this.bidTime = LocalDateTime.now();
        validateBidTransaction();
    }

    // Contructor khi lấy lịch sử từ DB
    public BidTransaction(String BidId, LocalDateTime creatTime, LocalDateTime bidTime, String auctionId, String bidderId, BigDecimal bidAmount) {
        super(BidId, creatTime);
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.bidAmount = normalizeMoney(bidAmount);
        this.bidTime = bidTime;
        validateBidTransaction();
    }

    private static BigDecimal normalizeMoney(BigDecimal amt) {
        return amt.setScale(2, RoundingMode.HALF_UP);
    }

    // Kiểm tra tính hợp lệ của giao dịch
    public void validateBidTransaction() {
        if (auctionId == null)
            throw new IllegalArgumentException("AuctionId không được null");
        if (bidderId == null)
            throw new IllegalArgumentException("BidderId không được null");
        if (bidAmount == null || bidAmount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("BidAmount phải lớn hơn 0");
    }

    @Override
    public String toString() {
        return "Bid: " + getId() + " | AuctionId: " + this.auctionId + " | BidderId: " + this.bidderId + " | BidAmount: " + this.bidAmount + " | BidTime: " + this.bidTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BidTransaction)) return false;
        BidTransaction that = (BidTransaction)o;
        return Objects.equals(getId(), that.getId());
    }
    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
    // Getter/Setter
    public String getAuctionId() {
        return auctionId;
    }
    public String getBidderId() {
        return bidderId;
    }
    public BigDecimal getBidAmount() {
        return bidAmount;
    }
    public LocalDateTime getBidTime() {
        return bidTime;
    }
}
