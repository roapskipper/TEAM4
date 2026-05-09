package com.team4.dao;

import com.team4.model.Auction;
import java.util.List;
import java.math.BigDecimal;

public interface AuctionDAO {
    Auction findById(String id);
    Auction findByItemId(String itemId);
    List<Auction> findAll();

    List<Auction> findByStatus(Auction.AuctionStatus status);

    boolean insert(Auction auction);
    boolean updateStatus(String auctionId, Auction.AuctionStatus newStatus);
    boolean updateCurrentBid(String id, BigDecimal currentPrice, String highestBidderId);
    boolean updateEndTime(String id, java.time.LocalDateTime newEndTime);
}