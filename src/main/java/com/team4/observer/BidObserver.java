package com.team4.observer;

import com.team4.model.Auction;
import com.team4.model.BidTransaction;

public interface BidObserver {
    // Hàm này sẽ tự động được gọi khi có người đặt giá mới thành công
    void updateNewBid(Auction auction, BidTransaction latestBid);
}