package com.team4.service;

import com.team4.model.Auction;
import com.team4.model.BidTransaction;
import com.team4.model.User;
import com.team4.observer.BidObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AuctionManager - Lớp dịch vụ quản lý Logic Đấu giá.
 * Áp dụng Singleton Pattern: Chỉ có 1 người điều phối duy nhất.
 */
public class AuctionManager {
    private static AuctionManager instance;

    // Quản lý danh sách các phiên (Trong thực tế sẽ dùng DAO để lấy từ DB)
    private List<Auction> auctions;
    private List<BidObserver> observers;

    private AuctionManager() {
        auctions = new ArrayList<>();
        observers = new ArrayList<>();
    }

    /**
     * Singleton - Thread-safe (Đảm bảo an toàn đa luồng)
     */
    public static synchronized AuctionManager getInstance() {
        if (instance == null) {
            instance = new AuctionManager();
        }
        return instance;
    }

    // --- QUẢN LÝ PHIÊN ---

    public void createAuction(Auction auction) {
        if (auction != null) {
            auctions.add(auction);
            System.out.println("[Manager] Tạo thành công phiên: " + auction.getId());
        }
    }

    public List<Auction> getActiveAuctions() {
        return auctions.stream()
                .filter(a -> "ACTIVE".equals(a.getStatus()))
                .collect(Collectors.toList());
    }

    // --- LOGIC ĐẤU GIÁ (TRÁI TIM HỆ THỐNG) ---

    /**
     * Xử lý Đặt Giá (Sử dụng synchronized để ngăn chặn 2 người cùng đặt 1 giá đồng thời)
     */
    public synchronized boolean placeBid(User bidder, Auction auction, double amount) {
        System.out.println("\n[PROCESS] Xử lý lượt trả giá từ: " + bidder.getUsername());

        // 1. Kiểm tra Quyền (Phải là BIDDER mới được đặt giá)
        if (!"BIDDER".equalsIgnoreCase(bidder.getRole())) {
            System.out.println("[REJECT] Lỗi: Chỉ người mua (Bidder) mới được đặt giá.");
            return false;
        }

        // 2. Kiểm tra Số dư (Phải có đủ tiền trong ví)
        if (bidder.getBalance() < amount) {
            System.out.println("[REJECT] Lỗi: Số dư không đủ! (Cần $" + amount + ", Có $" + bidder.getBalance() + ")");
            return false;
        }

        // 3. Kiểm tra Tính hợp lệ của phiên (Sử dụng logic từ lớp Auction)
        if (!auction.canBid()) {
            System.out.println("[REJECT] Lỗi: Phiên đấu giá này đã đóng hoặc chưa bắt đầu.");
            return false;
        }

        // 4. Kiểm tra Giá thầu mới so với Giá hiện tại
        if (amount <= auction.getCurrentPrice()) {
            System.out.println("[REJECT] Lỗi: Giá $" + amount + " phải cao hơn giá hiện tại $" + auction.getCurrentPrice());
            return false;
        }

        // --- CẬP NHẬT TRẠNG THÁI (GIAO DỊCH THÀNH CÔNG) ---

        // Cập nhật giá và người giữ kỷ lục
        auction.setCurrentPrice(amount);
        auction.setCurrentHighestBidderId(bidder.getId());

        // Tạo bản ghi lịch sử giao dịch (Sử dụng Constructor UUID tự động)
        BidTransaction transaction = new BidTransaction(auction.getId(), bidder.getId(), amount);

        // Phát tín hiệu thông báo cho toàn bộ hệ thống (Observer Pattern)
        notifyObservers(auction, transaction);

        System.out.println("[SUCCESS] Chấp nhận giá thầu! Người dẫn đầu: " + bidder.getUsername());
        return true;
    }

    // --- QUẢN LÝ OBSERVERS ---

    public void addObserver(BidObserver observer) {
        if (!observers.contains(observer)) observers.add(observer);
    }

    private void notifyObservers(Auction auction, BidTransaction transaction) {
        for (BidObserver observer : observers) {
            observer.updateNewBid(auction, transaction);
        }
    }
}