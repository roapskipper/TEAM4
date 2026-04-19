package com.team4.dao;

import com.team4.model.Auction;
import java.util.List;

public interface AuctionDAO {
    Auction findById(String id);
    Auction findByItemId(String itemId);
    List<Auction> findAll();
    List<Auction> findByStatus(String status); // Tìm các phiên đang OPEN/ACTIVE

    boolean insert(Auction auction);
    boolean update(Auction auction);
    boolean updateStatus(String auctionId, String newStatus);

    /**
     * PHƯƠNG THỨC QUAN TRỌNG NHẤT: XỬ LÝ CONCURRENCY
     * Dùng khi một người dùng (Bidder) đặt giá mới.
     */
    boolean placeBid(String auctionId, String bidderId, double newBidPrice);
}