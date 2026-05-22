package com.team4.dto.auction;

import java.math.BigDecimal;

public class AdminAuctionResponseDTO {
    private String id;
    private String itemName;
    private String sellerName;
    private BigDecimal startPrice;
    private String status;
    private int reportCount;

    public AdminAuctionResponseDTO() {}

    public AdminAuctionResponseDTO(String id, String itemName, String sellerName, BigDecimal startPrice, String status, int reportCount) {
        this.id = id;
        this.itemName = itemName;
        this.sellerName = sellerName;
        this.startPrice = startPrice;
        this.status = status;
        this.reportCount = reportCount;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getSellerName() {
        return sellerName;
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }

    public BigDecimal getStartPrice() {
        return startPrice;
    }

    public void setStartPrice(BigDecimal startPrice) {
        this.startPrice = startPrice;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getReportCount() {
        return reportCount;
    }

    public void setReportCount(int reportCount) {
        this.reportCount = reportCount;
    }
}
