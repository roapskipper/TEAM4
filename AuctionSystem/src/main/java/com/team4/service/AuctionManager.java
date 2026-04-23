package com.team4.service;

import com.team4.model.Auction;
import com.team4.model.BidTransaction;
import com.team4.observer.BidObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AuctionManager {
    // Áp dụng Singleton Pattern để chỉ có 1 Manager duy nhất quản lý đấu giá
    private static AuctionManager instance;

    private List<Auction> activeAuctions;
    private List<BidObserver> observers; // Danh sách những người đang "hóng" giá thay đổi

    private AuctionManager() {
        activeAuctions = new ArrayList<>();
        observers = new ArrayList<>();
    }

    public static synchronized AuctionManager getInstance() {
        if (instance == null) {
            instance = new AuctionManager();
        }
        return instance;
    }

    // 1. Logic thêm phiên đấu giá mới
    public void addAuction(Auction auction) {
        activeAuctions.add(auction);
    }

    // 2. Logic cho Client đăng ký nhận thông báo (Observer Pattern)
    public void registerObserver(BidObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    // 3. Logic xử lý khi có người đặt giá
    public boolean placeBid(Auction auction, String bidderId, double bidAmount) {
        // Kiểm tra xem giá đặt có lớn hơn giá hiện tại không và phiên còn hiệu lực không
        if (bidAmount > auction.getCurrentPrice() && !auction.isExpired() && auction.getStatus().equals("ACTIVE")) {

            // Cập nhật giá và người dẫn đầu mới
            auction.setCurrentPrice(bidAmount);
            auction.setCurrentHighestBidderId(bidderId);

            // Tạo lịch sử giao dịch
            BidTransaction newTransaction = new BidTransaction(
                    UUID.randomUUID().toString(),
                    auction.getId(),
                    bidderId,
                    bidAmount
            );

            // Thông báo (Broadcast) cho tất cả những người đang xem
            notifyObservers(auction, newTransaction);
            System.out.println("[AuctionManager] Bidder " + bidderId + " đã đặt giá thành công: " + bidAmount);
            return true;
        }
        System.out.println("[AuctionManager] Đặt giá thất bại! Giá quá thấp hoặc phiên đã kết thúc.");
        return false;
    }

    // 4. Bắn thông báo cho các Observers
    private void notifyObservers(Auction auction, BidTransaction transaction) {
        for (BidObserver obs : observers) {
            obs.updateNewBid(auction, transaction);
        }
    }

    // (Lộc) 5. Lấy danh sách các cuộc đấu giá đang hoạt động
    public List<Auction> getActiveAuctions() {
        return activeAuctions;
    }

    // (Lộc) 6. Hủy theo dõi cập nhật đấu giá
    public void removeObserver(BidObserver observer) {
        observers.remove(observer);
    }
}