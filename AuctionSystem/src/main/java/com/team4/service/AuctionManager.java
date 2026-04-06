package com.team4.service;

import com.team4.dao.impl.AuctionDAOImpl;
import com.team4.dao.impl.BidTransactionDAOImpl;
import com.team4.dao.impl.ItemDAOImpl;
import com.team4.dao.impl.UserDAOImpl;
import com.team4.model.*;
import com.team4.observer.BidObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * AuctionManager - Lớp dịch vụ quản lý Logic Đấu giá toàn diện.
 * HOÀN THIỆN: Logic Đóng Phiên + Thanh Toán + Sang Tên Đổi Chủ + Tính Điểm
 *
 * Singleton Pattern: Đảm bảo chỉ có một instance quản lý toàn bộ hệ thống.
 */
public class AuctionManager {
    private static volatile AuctionManager instance;

    private final List<Auction> activeAuctions;
    private final List<BidObserver> observers;
    private final BidTransactionDAOImpl bidTransactionDAO;

    // Các DAO phục vụ cho quá trình Checkout & Sang tên
    private final AuctionDAOImpl auctionDAO;
    private final UserDAOImpl userDAO;
    private final ItemDAOImpl itemDAO;

    private ScheduledExecutorService endAuctionScheduler;

    /**
     * Private Constructor - Chỉ được gọi một lần duy nhất.
     * Khởi tạo tất cả DAO và bắt đầu monitor tự động.
     */
    private AuctionManager() {
        this.activeAuctions = new ArrayList<>();
        this.observers = new ArrayList<>();
        this.auctionDAO = new AuctionDAOImpl();
        this.userDAO = new UserDAOImpl();
        this.itemDAO = new ItemDAOImpl();
        this.bidTransactionDAO = new BidTransactionDAOImpl();

        // Tải các phiên đấu giá đang hoạt động từ Database
        loadActiveAuctionsFromDB();

        // Khởi động thread theo dõi thời gian kết thúc phiên đấu giá (mỗi 5 giây)
        startAuctionEndMonitor();
    }

    /**
     * Singleton Pattern: Lấy instance duy nhất của AuctionManager.
     * Thread-safe bằng double-checked locking.
     */
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

    /**
     * Tạo một phiên đấu giá mới và lưu vào Database + RAM.
     *
     * @param auction Phiên đấu giá cần tạo
     * @return true nếu tạo thành công, false nếu thất bại
     */
    public void createAuction(Auction auction) {
        if (auction != null && auctionDAO.save(auction)) {
            activeAuctions.add(auction);
            System.out.println("[MANAGER] ✓ Đã kích hoạt phiên đấu giá ID: " + auction.getId().substring(0, 8) + "...");
        } else {
            System.err.println("[MANAGER] ✗ Tạo phiên đấu giá thất bại!");
        }
    }

    /**
     * Lấy tất cả phiên đấu giá đang hoạt động từ Database.
     */
    public List<Auction> getActiveAuctions() {
        return new ArrayList<>(activeAuctions);
    }

    /**
     * Lấy chi tiết một phiên đấu giá.
     */
    public Optional<Auction> getAuctionById(String auctionId) {
        return auctionDAO.findById(auctionId);
    }

    // ========================================================================
    // CHỨC NĂNG 2: ĐẶT GIÁ (BIDDING LOGIC)
    // ========================================================================

    /**
     * Xử lý logic đặt giá từ một Bidder.
     * Kiểm tra:
     * - Phiên đấu giá còn hiệu lực chưa?
     * - Người đặt giá có phải là Bidder không?
     * - Số tiền đặt có cao hơn giá hiện tại không?
     * - Người đặt giá có đủ tiền không?
     *
     * @param bidder Người đặt giá
     * @param auction Phiên đấu giá
     * @param amount Số tiền đặt
     * @return true nếu đặt giá thành công, false nếu không
     */
    public synchronized boolean placeBid(User bidder, Auction auction, double amount) {
        // Kiểm tra phiên có còn tiếp tục không
        if (!auction.canBid()) {
            System.out.println("[BID] ✗ Phiên đấu giá đã kết thúc!");
            return false;
        }

        // Kiểm tra người đặt giá phải là Bidder
        if (!"BIDDER".equalsIgnoreCase(bidder.getRole())) {
            System.out.println("[BID] ✗ Chỉ Bidder mới được đặt giá!");
            return false;
        }

        // Kiểm tra số tiền phải cao hơn giá hiện tại
        if (amount <= auction.getCurrentPrice()) {
            System.out.println("[BID] ✗ Giá thầu phải cao hơn $" + auction.getCurrentPrice());
            return false;
        }

        // Kiểm tra Bidder có đủ tiền không
        if (bidder.getBalance() < amount) {
            System.out.println("[BID] ✗ " + bidder.getUsername() + " không đủ tiền! (Cần: $" + amount + ", Có: $" + bidder.getBalance() + ")");
            return false;
        }

        // Cập nhật phiên đấu giá với giá mới
        auction.setCurrentPrice(amount);
        auction.setCurrentHighestBidderId(bidder.getId());

        // Lưu lại vào Database
        auctionDAO.update(auction);

        // Ghi lại lịch sử đặt giá
        BidTransaction bidTransaction = new BidTransaction(auction.getId(), bidder.getId(), amount);
        bidTransactionDAO.save(bidTransaction);

        // Thông báo tới các Observer
        notifyObservers(auction, bidTransaction);

        System.out.println("[BID] ✓ " + bidder.getUsername() + " đã trả giá vươn lên dẫn đầu: $" + amount);
        return true;
    }

    // ========================================================================
    // CHỨC NĂNG 3: BACKGROUND THREAD ĐÓNG PHIÊN VÀ THANH TOÁN
    // ========================================================================

    /**
     * Khởi động thread theo dõi các phiên đấu giá hết hạn.
     * Mỗi 5 giây sẽ kiểm tra một lần.
     */
    private void startAuctionEndMonitor() {
        endAuctionScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "AuctionEndMonitor");
            t.setDaemon(true);
            return t;
        });

        endAuctionScheduler.scheduleAtFixedRate(
                this::checkExpiredAuctions,
                0,           // Initial delay
                5,           // Period
                TimeUnit.SECONDS
        );
    }

    /**
     * Kiểm tra các phiên đấu giá đã hết hạn và đóng chúng.
     */
    private synchronized void checkExpiredAuctions() {
        List<Auction> expiredList = activeAuctions.stream()
                .filter(Auction::isExpired)
                .filter(a -> "ACTIVE".equalsIgnoreCase(a.getStatus()))
                .toList();

        for (Auction expiredAuction : expiredList) {
            closeAuction(expiredAuction);
        }
    }

    /**
     * Đóng một phiên đấu giá và xử lý thanh toán.
     * TASK 1.1: Chuyển trạng thái từ ACTIVE -> CLOSED
     * TASK 1.2: Xử lý thanh toán và sang tên vật phẩm
     *
     * @param auction Phiên đấu giá cần đóng
     */
    private void closeAuction(Auction auction) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println(" ⏰ PHIÊN ĐẤU GIÁ KẾT THÚC (Mã phiên: " + auction.getId().substring(0, 8) + ")");

        // Bước 1: Chuyển trạng thái
        auction.setStatus("CLOSED");

        // Bước 2: Kiểm tra xem có người thắng không
        if (auction.getCurrentHighestBidderId() != null) {
            // CÓ NGƯỜI THẮNG => Kích hoạt Logic TASK 1.2 (Thanh toán & Sang tên)
            processCheckoutAndScoring(auction);
        } else {
            System.out.println(" ❌ NO WINNER: Không có khách hàng nào trả giá.");
        }

        // Bước 3: Cập nhật Database
        activeAuctions.remove(auction);  // Gỡ khỏi RAM
        auctionDAO.update(auction);      // Cập nhật Database (status = CLOSED)
        System.out.println("=".repeat(60) + "\n");
    }

    /**
     * TASK 1.2: Xử lý Thanh Toán + Sang Tên + Tính Điểm.
     *
     * Quy trình:
     * 1. Lấy thông tin Winner, Seller, Item từ Database
     * 2. Trừ tiền từ Winner, Cộng tiền cho Seller
     * 3. Sang tên vật phẩm cho Winner
     * 4. Cộng điểm uy tín cho Seller
     * 5. Đồng bộ tất cả thay đổi xuống Database
     *
     * @param auction Phiên đấu giá vừa kết thúc
     */
    private void processCheckoutAndScoring(Auction auction) {
        System.out.println(" --- ĐANG XỬ LÝ THANH TOÁN & BÀN GIAO HÀNG HÓA ---");

        // Bước 1: Truy xuất đối tượng gốc từ Database
        Optional<User> optWinner = userDAO.findById(auction.getCurrentHighestBidderId());
        Optional<User> optSeller = userDAO.findById(auction.getSellerId());
        Optional<Item> optItem = itemDAO.findById(auction.getItemId());

        // Kiểm tra tất cả đối tượng phải tồn tại
        if (optWinner.isEmpty() || optSeller.isEmpty() || optItem.isEmpty()) {
            System.err.println(" [LỖI HỆ THỐNG] Thiếu dữ liệu CSDL để hoàn tất thanh toán!");
            return;
        }

        User winner = optWinner.get();
        User seller = optSeller.get();
        Item item = optItem.get();
        double soldPrice = auction.getCurrentPrice();

        // Bước 2: LOGIC THANH TOÁN (Trừ người mua, Cộng người bán)
        if (!winner.withdraw(soldPrice)) {
            // Trường hợp gian lận: Bidder rút hết tiền trong lúc đấu giá
            System.out.println(" ⚠️ [LỪA ĐẢO] " + winner.getUsername() + " KHÔNG ĐỦ TIỀN THANH TOÁN! PHIÊN BỊ HỦY.");
            auction.setStatus("CANCELLED");
            auctionDAO.update(auction);
            return;
        }

        seller.deposit(soldPrice);

        // Bước 3: SANG TÊN VẬT PHẨM (Quyền sở hữu thuộc về Người Thắng)
        item.setOwnerId(winner.getId());

        // Bước 4: TÍNH ĐIỂM UY TÍN CHO SELLER (+0.1 Sao cho giao dịch thành công)
        if (seller instanceof Seller sellerObj) {
            double newRating = Math.min(5.0, sellerObj.getRating() + 0.1);
            sellerObj.setRating(newRating);
        }

        // Bước 5: ĐỒNG BỘ TOÀN BỘ KẾT QUẢ XUỐNG DATABASE
        userDAO.update(winner);
        userDAO.update(seller);
        itemDAO.update(item);

        // In kết quả thanh toán
        System.out.println(" 🏆 NGƯỜI THẮNG          : " + winner.getUsername());
        System.out.println(" 💸 TỔNG THANH TOÁN      : $" + soldPrice);
        System.out.println(" 📦 CHỦ MỚI CỦA MÓN ĐỒ : " + winner.getUsername());
        if (seller instanceof Seller sellerObj) {
            System.out.println(" ⭐ RATING CỬA SHOP TĂNG : " + sellerObj.getRating() + " / 5.0");
        }
        System.out.println(" ✅ THANH TOÁN HOÀN TẤT !");
    }

    // ========================================================================
    // CHỨC NĂNG 4: OBSERVER PATTERN (THÔNG BÁO)
    // ========================================================================

    /**
     * Đăng ký một Observer để nhận thông báo mỗi khi có đặt giá mới.
     */
    public void addObserver(BidObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    /**
     * Gỡ đăng ký Observer.
     */
    public void removeObserver(BidObserver observer) {
        observers.remove(observer);
    }

    /**
     * Thông báo tới tất cả Observer về một đặt giá mới.
     */
    private void notifyObservers(Auction auction, BidTransaction transaction) {
        for (BidObserver observer : observers) {
            observer.updateNewBid(auction, transaction);
        }
    }

    // ========================================================================
    // CHỨC NĂNG 5: QUẢN LÝ VÒNG ĐỜI MANAGER
    // ========================================================================

    /**
     * Tải các phiên đấu giá đang hoạt động từ Database vào RAM.
     */
    private void loadActiveAuctionsFromDB() {
        try {
            List<Auction> dbAuctions = auctionDAO.findAllActive();
            activeAuctions.addAll(dbAuctions);
            System.out.println("[MANAGER] ✓ Tải được " + dbAuctions.size() + " phiên đấu giá từ Database.");
        } catch (Exception e) {
            System.err.println("[MANAGER] ✗ Lỗi khi tải dữ liệu từ Database: " + e.getMessage());
        }
    }

    /**
     * Tắt AuctionManager (dừng thread monitor).
     * Nên gọi trước khi đóng ứng dụng.
     */
    public void shutdownManager() {
        if (endAuctionScheduler != null && !endAuctionScheduler.isShutdown()) {
            endAuctionScheduler.shutdown();
            try {
                if (!endAuctionScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    endAuctionScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                endAuctionScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
            System.out.println("[MANAGER] ✓ AuctionManager đã tắt.");
        }
    }

    // ========================================================================
    // PHƯƠNG THỨC HỖ TRỢ
    // ========================================================================

    /**
     * Lấy số phiên đấu giá đang hoạt động.
     */
    public int getActiveAuctionCount() {
        return activeAuctions.size();
    }

    /**
     * Lấy số lượng Observer đang theo dõi.
     */
    public int getObserverCount() {
        return observers.size();
    }
}
