package com.team4.service;

import com.team4.dao.*;
import com.team4.dao.impl.*;
import com.team4.db.DatabaseManager;
import com.team4.model.*;
import com.team4.observer.BidObserver;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

/**
 * AuctionManager
 *
 * KIẾN TRÚC ÁP DỤNG :
 * 1. Singleton Pattern: Đảm bảo chỉ 1 bộ quản lý duy nhất.
 * 2. Observer Pattern: Cập nhật giá Realtime.
 * 3. Thread-Safe Collections: Dùng ConcurrentHashMap & CopyOnWriteArrayList chống crash.
 * 4. Database Transaction: Đảm bảo thanh toán không bị mất tiền (ACID).
 * 5. Concurrent Bidding: Không dùng 'synchronized' toàn cục gây nghẽn, đẩy việc khóa xuống DB.
 */
public class AuctionManager {
    private static volatile AuctionManager instance;

    private final ConcurrentHashMap<String, Auction> activeAuctionsMap;

    private final CopyOnWriteArrayList<BidObserver> observers;

    // Sử dụng Interface (Tính đa hình / Abstraction)
    private final AuctionDAO auctionDAO;
    private final UserDAO userDAO;
    private final ItemDAO itemDAO;
    private final BidTransactionDAO bidTransactionDAO;

    private ScheduledExecutorService endAuctionScheduler;

    private AuctionManager() {
        this.activeAuctionsMap = new ConcurrentHashMap<>();
        this.observers = new CopyOnWriteArrayList<>();

        this.auctionDAO = new AuctionDAOImpl();
        this.userDAO = new UserDAOImpl();
        this.itemDAO = new ItemDAOImpl();
        this.bidTransactionDAO = new BidTransactionDAOImpl();

        loadActiveAuctionsFromDB();
        startAuctionEndMonitor();
    }

    public static AuctionManager getInstance() {
        if (instance == null) {
            synchronized (AuctionManager.class) {
                if (instance == null) {
                    instance = new AuctionManager();
                }
            }
        }
        return instance;
    }

    // ========================================================================
    // CHỨC NĂNG 1: QUẢN LÝ PHIÊN ĐẤU GIÁ
    // ========================================================================

    public void createAuction(Auction auction) {
        if (auction != null && auctionDAO.insert(auction)) {
            activeAuctionsMap.put(auction.getId(), auction);
            System.out.println("[MANAGER] ✓ Đã kích hoạt phiên đấu giá ID: " + auction.getId().substring(0, 8));
        }
    }

    public List<Auction> getActiveAuctions() {
        return List.copyOf(activeAuctionsMap.values()); // Trả về bản sao an toàn (Immutable)
    }

    public Auction getAuctionById(String auctionId) {
        return activeAuctionsMap.get(auctionId);
    }

    // ========================================================================
    // CHỨC NĂNG 2: ĐẶT GIÁ (CONCURRENT BIDDING - RACE CONDITION SAFE)
    // ========================================================================

    /**
     * Xử lý đặt giá. KHÔNG DÙNG SYNCHRONIZED Ở ĐÂY ĐỂ TRÁNH NÚT THẮT CỔ CHAI.
     * Logic khóa (Lock) đã được đẩy xuống MySQL thông qua phương thức auctionDAO.placeBid().
     */
    public boolean placeBid(User bidder, String auctionId, double amount) {
        Auction auction = activeAuctionsMap.get(auctionId);

        if (auction == null || !auction.canBid()) {
            System.out.println("[BID] ✗ Phiên đấu giá không tồn tại hoặc đã kết thúc!");
            return false;
        }

        if (!"BIDDER".equalsIgnoreCase(bidder.getRole())) {
            System.out.println("[BID] ✗ Chỉ Bidder mới được tham gia đặt giá!");
            return false;
        }

        if (amount <= auction.getCurrentPrice()) {
            System.out.println("[BID] ✗ Giá thầu phải cao hơn giá hiện tại!");
            return false;
        }

        if (bidder.getBalance() < amount) {
            System.out.println("[BID] ✗ Số dư không đủ!");
            return false;
        }

        // Nếu 2 người cùng bid 1 giá tại cùng 1 mili-giây, DB sẽ chỉ cho 1 người thành công
        boolean dbSuccess = auctionDAO.placeBid(auctionId, bidder.getId(), amount);

        if (dbSuccess) {
            // Nếu DB lưu thành công, cập nhật đối tượng trên RAM
            auction.setCurrentPrice(amount);
            auction.setCurrentHighestBidderId(bidder.getId());

            // Lưu lịch sử giao dịch (Audit Log)
            BidTransaction tx = new BidTransaction(auctionId, bidder.getId(), amount);
            bidTransactionDAO.insert(tx);

            // Bắn Notify đến tất cả thiết bị (Observer)
            notifyObservers(auction, tx);

            System.out.println("[BID] ✓ " + bidder.getUsername() + " vươn lên dẫn đầu: $" + amount);

            // TODO: Ở đây có thể kích hoạt tính năng "Anti-Sniping" (Gia hạn nếu bid ở 5 giây cuối)
            // TODO: Ở đây có thể kích hoạt Bot "Auto-Bidding" (Trả giá tự động)

            return true;
        } else {
            System.out.println("[BID] ⚠ Đã có người đặt giá cao hơn vào phút chót. Vui lòng thử lại!");
            return false;
        }
    }

    // ========================================================================
    // CHỨC NĂNG 3: BACKGROUND THREAD ĐÓNG PHIÊN VÀ THANH TOÁN (TRANSACTION)
    // ========================================================================

    private void startAuctionEndMonitor() {
        endAuctionScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "AuctionEndMonitor");
            t.setDaemon(true);
            return t;
        });

        endAuctionScheduler.scheduleAtFixedRate(
                this::checkExpiredAuctions,
                0, 5, TimeUnit.SECONDS
        );
    }

    private void checkExpiredAuctions() {
        // Lặp qua Map an toàn
        for (Auction auction : activeAuctionsMap.values()) {
            if (auction.isExpired() && "ACTIVE".equalsIgnoreCase(auction.getStatus())) {
                closeAuction(auction);
            }
        }
    }

    private void closeAuction(Auction auction) {
        // Gỡ khỏi RAM ngay lập tức để không ai bid được nữa
        activeAuctionsMap.remove(auction.getId());

        if (auction.getCurrentHighestBidderId() != null) {
            // CÓ NGƯỜI THẮNG -> Gọi logic thanh toán có sử dụng Database Transaction
            processCheckoutTransaction(auction);
        } else {
            // KHÔNG CÓ AI MUA
            auction.setStatus("CLOSED");
            auctionDAO.updateStatus(auction.getId(), "CLOSED");
            System.out.println("[AUCTION] Phiên " + auction.getId().substring(0,8) + " kết thúc (Không có người mua).");
        }
    }

    /**
     * BẢO MẬT TÀI CHÍNH: Xử lý thanh toán thông qua SQL Transaction.
     * Tránh Lost Update (Trừ tiền người mua nhưng rớt mạng chưa kịp cộng tiền người bán).
     */
    private void processCheckoutTransaction(Auction auction) {
        System.out.println("\n[SYSTEM] Đang xử lý giao dịch cho phiên: " + auction.getId().substring(0,8));

        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            DatabaseManager.beginTransaction(conn); // TẮT AUTO-COMMIT

            double price = auction.getCurrentPrice();
            String winnerId = auction.getCurrentHighestBidderId();
            String sellerId = auction.getSellerId();

            // 1. Trừ tiền người Mua
            String deductSQL = "UPDATE users SET balance = balance - ? WHERE id = ? AND balance >= ?";
            try (PreparedStatement pst1 = conn.prepareStatement(deductSQL)) {
                pst1.setDouble(1, price);
                pst1.setString(2, winnerId);
                pst1.setDouble(3, price);
                if (pst1.executeUpdate() == 0) {
                    throw new SQLException("Người mua không đủ tiền lúc thanh toán!");
                }
            }

            // 2. Cộng tiền người Bán
            String addSQL = "UPDATE users SET balance = balance + ? WHERE id = ?";
            try (PreparedStatement pst2 = conn.prepareStatement(addSQL)) {
                pst2.setDouble(1, price);
                pst2.setString(2, sellerId);
                pst2.executeUpdate();
            }

            // 3. Sang tên đồ vật (Cập nhật owner_id trong bảng items)
            String transferSQL = "UPDATE items SET owner_id = ? WHERE id = ?";
            try (PreparedStatement pst3 = conn.prepareStatement(transferSQL)) {
                pst3.setString(1, winnerId);
                pst3.setString(2, auction.getItemId());
                pst3.executeUpdate();
            }

            // 4. Cập nhật trạng thái phiên thành PAID (Đã thanh toán)
            String updateAuctionSQL = "UPDATE auctions SET status = 'PAID' WHERE id = ?";
            try (PreparedStatement pst4 = conn.prepareStatement(updateAuctionSQL)) {
                pst4.setString(1, auction.getId());
                pst4.executeUpdate();
            }

            DatabaseManager.commitTransaction(conn); // HOÀN TẤT VÀ LƯU XUỐNG DB
            System.out.println("[SYSTEM] ✓ Thanh toán thành công! Tiền đã chuyển, Hàng đã sang tên.");

        } catch (SQLException e) {
            DatabaseManager.rollbackTransaction(conn); // NẾU LỖI -> HỦY BỎ TẤT CẢ
            System.err.println("[SYSTEM] ✗ Lỗi thanh toán! Đã Rollback toàn bộ. Chi tiết: " + e.getMessage());

            // Chuyển trạng thái phiên thành CANCELLED do lừa đảo/lỗi
            auctionDAO.updateStatus(auction.getId(), "CANCELLED");

        } finally {
            if (conn != null) {
                try { conn.close(); } catch (SQLException ex) {} // Trả kết nối về Pool
            }
        }
    }

    // ========================================================================
    // CHỨC NĂNG 4: OBSERVER PATTERN (THÔNG BÁO)
    // ========================================================================

    public void addObserver(BidObserver observer) {
        observers.addIfAbsent(observer);
    }

    public void removeObserver(BidObserver observer) {
        observers.remove(observer);
    }

    private void notifyObservers(Auction auction, BidTransaction transaction) {
        // Do dùng CopyOnWriteArrayList nên việc duyệt mảng cực kỳ an toàn
        for (BidObserver observer : observers) {
            observer.updateNewBid(auction, transaction);
        }
    }

    // ========================================================================
    // QUẢN LÝ VÒNG ĐỜI
    // ========================================================================

    private void loadActiveAuctionsFromDB() {
        List<Auction> dbAuctions = auctionDAO.findByStatus("ACTIVE");
        for (Auction a : dbAuctions) {
            activeAuctionsMap.put(a.getId(), a);
        }
        System.out.println("[MANAGER] ✓ Đã nạp " + dbAuctions.size() + " phiên đấu giá vào bộ nhớ.");
    }

    public void shutdownManager() {
        if (endAuctionScheduler != null && !endAuctionScheduler.isShutdown()) {
            endAuctionScheduler.shutdown();
        }
    }
}