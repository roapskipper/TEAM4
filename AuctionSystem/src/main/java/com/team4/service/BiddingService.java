package com.team4.service;

import com.team4.dao.AuctionDAO;
import com.team4.dao.BidTransactionDAO;
import com.team4.model.Auction;
import com.team4.model.BidTransaction;
import com.team4.util.BusinessException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class BiddingService {
    private final AuctionDAO auctionDAO;
    private final BidTransactionDAO bidTransactionDAO;
    
    // Map to store locks per auction ID to ensure thread-safety without blocking other auctions
    private final ConcurrentHashMap<String, ReentrantLock> auctionLocks = new ConcurrentHashMap<>();

    public BiddingService(AuctionDAO auctionDAO, BidTransactionDAO bidTransactionDAO) {
        this.auctionDAO = auctionDAO;
        this.bidTransactionDAO = bidTransactionDAO;
    }

    private ReentrantLock getLockForAuction(String auctionId) {
        return auctionLocks.computeIfAbsent(auctionId, k -> new ReentrantLock());
    }

    public BidTransaction placeBid(String auctionId, String bidderId, BigDecimal bidAmount) {
        ReentrantLock lock = getLockForAuction(auctionId);
        lock.lock();
        try {
            // 1. Fetch current auction to ensure we have the latest state (avoiding lost updates)
            Auction auction = auctionDAO.findById(auctionId);
            if (auction == null) {
                throw new BusinessException("Cuộc đấu giá không tồn tại");
            }

            // 2. Apply new bid. This includes checking if the bid is valid/higher than the current bid + increment
            // It modifies currentPrice and currentHighestBidderId inside the Auction object
            auction.applyBid(bidderId, bidAmount);

            // 3. Create a new transaction log
            BidTransaction transaction = new BidTransaction(auctionId, bidderId, bidAmount);

            // 4. Save the transaction to DB/memory
            bidTransactionDAO.insert(transaction);

            // 5. Update the new highest bid and bidder to the DB
            auctionDAO.updateCurrentBid(auctionId, auction.getCurrentPrice(), auction.getCurrentHighestBidderId());

            // 6. Anti-sniping logic: if bid is placed within the last 30 seconds, extend time by 60 seconds
            long secondsLeft = ChronoUnit.SECONDS.between(LocalDateTime.now(), auction.getEndTime());
            if (secondsLeft >= 0 && secondsLeft <= 30) {
                auction.extendEndTime(60);
                auctionDAO.updateEndTime(auctionId, auction.getEndTime());
            }

            return transaction;
        } catch (IllegalArgumentException | IllegalStateException e) {
            // Convert domain-specific exceptions to BusinessException
            throw new BusinessException(e.getMessage());
        } finally {
            // Ensure the lock is ALWAYS released
            lock.unlock();
        }
    }
}
