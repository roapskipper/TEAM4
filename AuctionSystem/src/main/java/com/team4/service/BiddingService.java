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
    
    private final AutoBiddingDAO autoBiddingDAO;

    // Map to store locks per auction ID to ensure thread-safety without blocking other auctions
    private final ConcurrentHashMap<String, ReentrantLock> auctionLocks = new ConcurrentHashMap<>();

    public BiddingService(AuctionDAO auctionDAO, BidTransactionDAO bidTransactionDAO, AutoBiddingDAO autoBiddingDAO) {
        this.auctionDAO = auctionDAO;
        this.bidTransactionDAO = bidTransactionDAO;
        this.autoBiddingDAO = autoBiddingDAO;
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

            // 6. Anti-sniping logic: if bid is placed within the last X=30 seconds, extend time by Y=60 seconds (Verified with BTL)
            long secondsLeft = ChronoUnit.SECONDS.between(LocalDateTime.now(), auction.getEndTime());
            if (secondsLeft >= 0 && secondsLeft <= 30) {
                auction.extendEndTime(60);
                // Đảm bảo sau khi gia hạn, endTime mới phải được update xuống database
                auctionDAO.updateEndTime(auctionId, auction.getEndTime());
            }

            // 7. Kích hoạt auto-bidding
            processAutoBids(auctionId, bidderId);

            return transaction;
        } catch (IllegalArgumentException | IllegalStateException e) {
            // Convert domain-specific exceptions to BusinessException
            throw new BusinessException(e.getMessage());
        } finally {
            // Ensure the lock is ALWAYS released
            lock.unlock();
        }
    }

    private void processAutoBids(String auctionId, String currentHighestBidderId) {
        java.util.List<com.team4.model.AutoBidding> activeAutoBids = autoBiddingDAO.findActiveByAuctionId(auctionId);
        if (activeAutoBids == null || activeAutoBids.isEmpty()) return;

        // Ưu tiên người đăng ký auto-bid trước (Timestamp-based priority)
        activeAutoBids.sort(java.util.Comparator.comparing(com.team4.model.Entity::getCreatedAt));

        boolean hasNewBid = true;
        while (hasNewBid) {
            hasNewBid = false;
            Auction auction = auctionDAO.findById(auctionId);
            
            for (com.team4.model.AutoBidding ab : activeAutoBids) {
                if (ab.getBidderId().equals(auction.getCurrentHighestBidderId())) continue;

                java.util.Optional<BigDecimal> nextBidOpt = ab.calculateNextBid(auction.getCurrentPrice());
                if (nextBidOpt.isPresent() && nextBidOpt.get().compareTo(ab.getMaxLimit()) <= 0) {
                    // Cập nhật giá mới theo increment
                    auction.applyBid(ab.getBidderId(), nextBidOpt.get());
                    bidTransactionDAO.insert(new BidTransaction(auctionId, ab.getBidderId(), nextBidOpt.get()));
                    auctionDAO.updateCurrentBid(auctionId, auction.getCurrentPrice(), auction.getCurrentHighestBidderId());
                    hasNewBid = true;
                } else {
                    // Nếu không thể bid tiếp (ví dụ chạm max limit), tắt auto-bid này
                    autoBiddingDAO.updateActive(ab.getId(), false);
                    ab.deactivate();
                }
            }
        }
    }
}
