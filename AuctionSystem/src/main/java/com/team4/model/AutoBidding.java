package com.team4.model;

import java.io.Serializable;

public class AutoBidding implements Serializable {
    private String autoBidId;
    private String auctionId;        // Phiên đấu giá nào?
    private String bidderId;         // Ai cài đặt lệnh này?
    private double maxLimit;         // Số tiền tối đa họ sẵn sàng trả (ví dụ: 10 triệu)
    private double incrementAmount;  // Mỗi lần máy tự động tăng thêm bao nhiêu? (ví dụ: +50k)

    public AutoBidding(String autoBidId, String auctionId, String bidderId, double maxLimit, double incrementAmount) {
        this.autoBidId = autoBidId;
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.maxLimit = maxLimit;
        this.incrementAmount = incrementAmount;
    }

    public String getAutoBidId() { return autoBidId; }
    public String getAuctionId() { return auctionId; }
    public String getBidderId() { return bidderId; }
    public double getMaxLimit() { return maxLimit; }
    public double getIncrementAmount() { return incrementAmount; }
}