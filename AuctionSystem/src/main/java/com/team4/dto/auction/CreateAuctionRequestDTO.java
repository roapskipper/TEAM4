package com.team4.dto.auction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CreateAuctionRequestDTO {
    private final String itemId;
    private final BigDecimal startingPrice;
    private final BigDecimal bidIncrement;
    private final LocalDateTime endTime;

    public CreateAuctionRequestDTO(String itemId, BigDecimal startingPrice, BigDecimal bidIncrement, LocalDateTime endTime) {
        this.itemId = itemId;
        this.startingPrice = startingPrice;
        this.bidIncrement = bidIncrement;
        this.endTime = endTime;
        validate();
    }

    public void validate() {
        if (itemId == null || itemId.trim().isEmpty()) {
            throw new IllegalArgumentException("ItemId không được để trống.");
        }
        if (startingPrice == null || startingPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Giá khởi điểm phải lớn hơn 0.");
        }
        if (bidIncrement == null || bidIncrement.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Bước giá phải lớn hơn 0.");
        }
        if (endTime == null) {
            throw new IllegalArgumentException("Thời gian kết thúc không được để trống.");
        }
        if (endTime.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Thời gian kết thúc phải ở trong tương lai.");
        }
    }

    public String getItemId() { return itemId; }
    public BigDecimal getStartingPrice() { return startingPrice; }
    public BigDecimal getBidIncrement() { return bidIncrement; }
    public LocalDateTime getEndTime() { return endTime; }

    @Override
    public String toString() {
        return "CreateAuction: itemId=" + itemId + " | startingPrice=" + startingPrice;
    }
}
