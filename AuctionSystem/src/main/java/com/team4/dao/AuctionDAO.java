package com.team4.dao;

import com.team4.model.Auction;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;

public interface AuctionDAO {
    Auction findById(String id);
    Auction findById(Connection conn, String id);
    Auction findByItemId(String itemId);
    List<Auction> findAll();
    public boolean updateEndTime(Connection conn, String auctionId, LocalDateTime newEndTime);
    List<Auction> findByStatus(Auction.AuctionStatus status);

    boolean insert(Auction auction);
    boolean updateStatus(String auctionId, Auction.AuctionStatus newStatus);
    boolean updateCurrentBid(Connection conn, String id, BigDecimal currentPrice, String highestBidderId);
}