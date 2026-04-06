package com.team4.service;

import com.team4.dao.impl.AuctionDAOImpl;
import com.team4.dao.impl.ItemDAOImpl;
import com.team4.dao.impl.UserDAOImpl;
import com.team4.model.Auction;
import com.team4.model.BidTransaction;
import com.team4.model.Item;
import com.team4.model.Seller;
import com.team4.model.User;
import com.team4.observer.BidObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * AuctionManager - Lớp dịch vụ quản lý Logic Đấu giá toàn diện.
 * HOÀN THIỆN: Logic Đóng Phiên + Thanh Toán + Sang Tên Đổi Chủ + Tính Điểm
 */
public class AuctionManager {
    private static volatile AuctionManager instance;

    private final List<Auction> activeAuctions;
    private final List<BidObserver> observers;

    // Các DAO phục vụ cho quá trình Checkout & Sang tên
    private final AuctionDAOImpl auctionDAO;
    private final UserDAOImpl userDAO;
    private final ItemDAOImpl itemDAO;

    private ScheduledExecutorService endAuctionScheduler;

    private AuctionManager() {
        this.activeAuctions = new ArrayList<>();
        this.observers = new ArrayList<>();
        this.auctionDAO = new AuctionDAOImpl();
        this.userDAO = new UserDAOImpl();
        this.itemDAO = new ItemDAOImpl();

        loadActiveAuctionsFromDB();
        startAuctionEndMonitor(); // Bật Robot kiểm tra thời gian (Mỗi 5s)
    }

    public static synchronized AuctionManager getInstance() {
        if (instance == null) {
            synchronized (AuctionManager.class) {
                if (instance == null) {
                    instance = new AuctionManager();
                }
            }
        }
        return instance;
    }

    // --- LOGIC: CREATE VÀ ĐẶT GIÁ (GIỮ NGUYÊN NHƯ CHẶNG 1) ---

    public void createAuction(Auction auction) {
        if (auction != null && auctionDAO.save(auction)) {
            activeAuctions.add(auction);
            System.out.println("[MANAGER] Đã kích hoạt phiên đấu giá ID: " + auction.getId());
        }
    }

    public synchronized boolean placeBid(User bidder, Auction auction, double amount) {
        if (!auction.canBid()) return false;

        if (amount > auction.getCurrentPrice() && "BIDDER".equalsIgnoreCase(bidder.getRole()) && bidder.getBalance() >= amount) {
            auction.setCurrentPrice(amount);
            auction.setCurrentHighestBidderId(bidder.getId());

            auctionDAO.update(auction); // Save Realtime DB

            notifyObservers(auction, new BidTransaction(auction.getId(), bidder.getId(), amount));
            System.out.println("[BID] " + bidder.getUsername() + " đã trả giá vươn lên dẫn đầu: $" + amount);
            return true;
        }
        return false;
    }

    // =========================================================================
    // TASK 1.1 & 1.2: BACKGROUND THREAD ĐÓNG PHIÊN VÀ THANH TOÁN (GÕ BÚA)
    // =========================================================================

    private void startAuctionEndMonitor() {
        endAuctionScheduler = Executors.newSingleThreadScheduledExecutor();
        endAuctionScheduler.scheduleAtFixedRate(this::checkExpiredAuctions, 0, 5, TimeUnit.SECONDS);
    }

    private synchronized void checkExpiredAuctions() {
        List<Auction> expiredList = activeAuctions.stream()
                .filter(Auction::isExpired)
                .filter(a -> "ACTIVE".equals(a.getStatus()))
                .toList();

        for (Auction expAuction : expiredList) {
            closeAuction(expAuction);
        }
    }

    /**
     * HÀNH ĐỘNG GÕ BÚA: Chuyển Status, Trừ Tiền, Sang Tên và Cộng Điểm.
     */
    private void closeAuction(Auction auction) {
        System.out.println("\n================================================");
        System.out.println(" ⏰ PHIÊN ĐẤU GIÁ KẾT THÚC (Mã phiên: " + auction.getId().substring(0,8) + ")");

        auction.setStatus("CLOSED");

        if (auction.getCurrentHighestBidderId() != null) {
            // CÓ NGƯỜI THẮNG => Kích hoạt Logic TASK 1.2 (Thanh toán & Sang tên)
            processCheckoutAndScoring(auction);
        } else {
            System.out.println(" ❌ NO WINNER: Không có khách hàng nào trả giá.");
        }

        activeAuctions.remove(auction); // Gỡ khỏi RAM
        auctionDAO.update(auction);     // Cập nhật Database (status = CLOSED)
        System.out.println("================================================\n");
    }

    /**
     * HÀI CỐT KẾT TINH (TASK 1.2): Quản lý Dòng Tiền và Điểm Số
     */
    private void processCheckoutAndScoring(Auction auction) {
        System.out.println(" --- ĐANG XỬ LÝ THANH TOÁN & BÀN GIAO HÀNG HÓA ---");

        // 1. Truy xuất đối tượng gốc từ Database
        Optional<User> optWinner = userDAO.findById(auction.getCurrentHighestBidderId());
        Optional<User> optSeller = userDAO.findById(auction.getSellerId());
        Optional<Item> optItem   = itemDAO.findById(auction.getItemId());

        if (optWinner.isPresent() && optSeller.isPresent() && optItem.isPresent()) {
            User winner = optWinner.get();
            User seller = optSeller.get();
            Item item = optItem.get();
            double soldPrice = auction.getCurrentPrice();

            // 2. LOGIC THANH TOÁN (Trừ người mua, Cộng người bán)
            if (winner.withdraw(soldPrice)) {
                seller.deposit(soldPrice);

                // 3. SANG TÊN VẬT PHẨM (Quyền sở hữu thuộc về Người Thắng)
                item.setOwnerId(winner.getId());

                // 4. TÍNH ĐIỂM UY TÍN CHO SELLER (+0.1 Sao cho giao dịch thành công)
                if (seller instanceof Seller s) { // Sử dụng Pattern Matching Java 25
                    double newRating = Math.min(5.0, s.getRating() + 0.1);
                    s.setRating(newRating);
                }

                // 5. ĐỒNG BỘ TOÀN BỘ KẾT QUẢ XUỐNG DATABASE
                userDAO.update(winner);
                userDAO.update(seller);
                itemDAO.update(item);

                System.out.println(" 🏆 WINNER      : " + winner.getUsername());
                System.out.println(" 💸 TỔNG THANH TOÁN: $" + soldPrice);
                System.out.println(" 📦 CHỦ MỚI MÓN ĐỒ: Tài khoản " + winner.getUsername());
                if(seller instanceof Seller s) {
                    System.out.println(" ⭐ RATING CỦA SHOP ĐÃ TĂNG: " + s.getRating() + " / 5.0");
                }

            } else {
                // LOGIC LỖI: Trường hợp Bidder gian lận (Rút hết tiền trong lúc đấu giá)
                System.out.println(" ⚠ [LỪA ĐẢO] " + winner.getUsername() + " KHÔNG ĐỦ TIỀN THANH TOÁN! PHIÊN BỊ HỦY.");
                auction.setStatus("CANCELLED");
            }
        } else {
            System.err.println(" [LỖI HỆ THỐNG] Thiếu dữ liệu CSDL để hoàn tất thanh toán!");
        }
    }

    public void shutdownManager() {
        if (endAuctionScheduler != null) endAuctionScheduler.shutdown();
    }

    private void loadActiveAuctionsFromDB() {
        List<Auction> dbAuctions = auctionDAO.findAllActive();
        activeAuctions.addAll(dbAuctions);
    }

    public void addObserver(BidObserver observer) { if (!observers.contains(observer)) observers.add(observer); }
    private void notifyObservers(Auction auction, BidTransaction tx) { for (BidObserver o : observers) o.updateNewBid(auction, tx); }
}