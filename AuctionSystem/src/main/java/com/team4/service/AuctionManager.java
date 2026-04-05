package com.team4.service;

import com.team4.dao.impl.AuctionDAOImpl;
import com.team4.model.Auction;
import com.team4.model.BidTransaction;
import com.team4.model.User;
import com.team4.observer.BidObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * AuctionManager - "Bộ não" quản lý Logic Đấu giá (Dành cho TEAM 4).
 * Cải tiến: Tích hợp DAO và Robot tự động đóng phiên khi hết giờ.
 */
public class AuctionManager {
    // Sử dụng volatile kết hợp double-checked locking cho Singleton chuẩn xác
    private static volatile AuctionManager instance;

    private List<Auction> activeAuctions;
    private List<BidObserver> observers;

    // DAO để lưu thẳng giá tiền và trạng thái xuống MySQL
    private final AuctionDAOImpl auctionDAO;

    // Robot (Thread) tuần tra kiểm tra phiên đấu giá hết hạn
    private ScheduledExecutorService endAuctionScheduler;

    private AuctionManager() {
        this.activeAuctions = new ArrayList<>();
        this.observers = new ArrayList<>();
        this.auctionDAO = new AuctionDAOImpl();

        // 1. Tự động móc các phiên đấu giá "đang diễn ra" từ SQL lên RAM
        loadActiveAuctionsFromDB();

        // 2. Bật Robot tuần tra (Mỗi 5 giây nó đi kiểm tra đồng hồ 1 lần)
        startAuctionEndMonitor();
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

    // ==============================================================
    // PHẦN 1: QUẢN LÝ PHIÊN (MANAGERMENT)
    // ==============================================================

    public void createAuction(Auction auction) {
        if (auction != null) {
            activeAuctions.add(auction);
            auctionDAO.save(auction); // Lưu thẳng xuống MySQL khi tạo mới
            System.out.println("[Manager] Đã kích hoạt & Lưu Database phiên: " + auction.getId());
        }
    }

    public List<Auction> getActiveAuctions() {
        return activeAuctions.stream()
                .filter(a -> "ACTIVE".equals(a.getStatus()))
                .collect(Collectors.toList());
    }

    // ==============================================================
    // PHẦN 2: LOGIC ĐẤU GIÁ & ĐẶT TIỀN (TRÁI TIM HỆ THỐNG CỦA TRUNG)
    // ==============================================================

    public synchronized boolean placeBid(User bidder, Auction auction, double amount) {
        System.out.println("\n[PROCESS] Xử lý lượt trả giá từ: " + bidder.getUsername());

        // 1. Kiểm tra Quyền
        if (!"BIDDER".equalsIgnoreCase(bidder.getRole())) {
            System.out.println("[REJECT] Lỗi: Chỉ người mua (Bidder) mới được đặt giá.");
            return false;
        }

        // 2. Kiểm tra Số dư
        if (bidder.getBalance() < amount) {
            System.out.println("[REJECT] Lỗi: Số dư không đủ! (Cần $" + amount + ", Có $" + bidder.getBalance() + ")");
            return false;
        }

        // 3. Kiểm tra Thời gian và Trạng thái phiên
        if (!auction.canBid()) {
            System.out.println("[REJECT] Lỗi: Phiên đấu giá này đã ĐÓNG CỬA hoặc HẾT HẠN!");
            return false;
        }

        // 4. Kiểm tra Mức giá mới
        if (amount <= auction.getCurrentPrice()) {
            System.out.println("[REJECT] Lỗi: Giá $" + amount + " phải CAO HƠN giá hiện tại $" + auction.getCurrentPrice());
            return false;
        }

        // --- CẬP NHẬT KHI THÀNH CÔNG ---
        auction.setCurrentPrice(amount);
        auction.setCurrentHighestBidderId(bidder.getId());

        // **Quan trọng:** Update MySQL Real-time ngay lập tức!
        auctionDAO.update(auction);

        BidTransaction transaction = new BidTransaction(auction.getId(), bidder.getId(), amount);
        notifyObservers(auction, transaction);

        System.out.println("[SUCCESS] Chấp nhận giá thầu! Người dẫn đầu: " + bidder.getUsername());
        return true;
    }

    // ==============================================================
    // PHẦN 3: LOGIC ĐÓNG PHIÊN & KIỂM TRA HẾT HẠN (BACKGROUND ROBOT)
    // ==============================================================

    /**
     * Bật một Thread chạy ngầm ngầm dưới hệ thống mỗi 5 giây
     */
    private void startAuctionEndMonitor() {
        endAuctionScheduler = Executors.newSingleThreadScheduledExecutor();
        endAuctionScheduler.scheduleAtFixedRate(this::checkExpiredAuctions, 0, 5, TimeUnit.SECONDS);
        System.out.println("[SERVICE-BOOT] Robot giám sát phiên hết hạn đã khởi động (Delay 5s/lần)...");
    }

    /**
     * Tự động quét xem có ông nào lố thời gian (EndTime) chưa
     */
    private synchronized void checkExpiredAuctions() {
        // Tìm ra danh sách các ông đã "quá đát" (Expired) nhưng vẫn chưa bị gõ búa
        List<Auction> expiredList = activeAuctions.stream()
                .filter(Auction::isExpired)
                .filter(a -> "ACTIVE".equals(a.getStatus()))
                .toList();

        // Tiến hành khóa và thanh toán
        for (Auction expAuction : expiredList) {
            closeAuction(expAuction);
        }
    }

    /**
     * HÀNH ĐỘNG "GÕ BÚA" KHI PHIÊN HẾT THỜI GIAN
     */
    private void closeAuction(Auction auction) {
        System.out.println("\n================================================");
        System.out.println(" ⏰ BONGGG! PHIÊN ĐẤU GIÁ " + auction.getItemId() + " ĐÃ KẾT THÚC!");

        // 1. Chuyển trạng thái Khóa
        auction.setStatus("CLOSED");

        // 2. Trao giải hoặc Hủy bỏ nếu không có ai thầu
        if (auction.getCurrentHighestBidderId() != null) {
            System.out.println(" 🏆 WINNER: Khách hàng (ID " + auction.getCurrentHighestBidderId() + ")");
            System.out.println(" 💰 GIÁ CHỐT BÁN: $" + auction.getCurrentPrice());
            // => TASK TIẾP THEO (Thanh toán tự động) SẼ ĐẶT Ở ĐÂY.
        } else {
            System.out.println(" ❌ NO WINNER: Không có khách hàng nào trả giá.");
        }

        // 3. Đá phiên này ra khỏi RAM và lưu "Án chung thân" xuống Database
        activeAuctions.remove(auction);
        auctionDAO.update(auction);

        System.out.println(" [DATABASE] Đã chốt khóa cửa phiên xuống MySQL.");
        System.out.println("================================================\n");
    }

    /**
     * Tắt ứng dụng dọn dẹp Robot
     */
    public void shutdownManager() {
        if (endAuctionScheduler != null && !endAuctionScheduler.isShutdown()) {
            endAuctionScheduler.shutdown();
            System.out.println("[SERVICE-OFF] Đã tắt Robot giám sát an toàn.");
        }
    }

    private void loadActiveAuctionsFromDB() {
        List<Auction> dbAuctions = auctionDAO.findAllActive();
        activeAuctions.addAll(dbAuctions);
        System.out.println("[SERVICE-BOOT] Tải " + dbAuctions.size() + " phiên còn dở dang từ DB lên thành công.");
    }

    // ==============================================================
    // PHẦN 4: OBSERVER PATTERN (MỞ RỘNG MÀN HÌNH TƯƠNG TÁC)
    // ==============================================================

    public void addObserver(BidObserver observer) {
        if (!observers.contains(observer)) observers.add(observer);
    }

    private void notifyObservers(Auction auction, BidTransaction transaction) {
        for (BidObserver observer : observers) {
            observer.updateNewBid(auction, transaction);
        }
    }
}