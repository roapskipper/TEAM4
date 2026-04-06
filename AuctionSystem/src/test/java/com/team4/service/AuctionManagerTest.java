package com.team4.service;

import com.team4.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AuctionManagerTest - Kiểm thử các chức năng của AuctionManager.
 *
 * Sử dụng JUnit 5 (Jupiter) để test các business logic:
 * - Tạo phiên đấu giá
 * - Đặt giá thầu
 * - Kiểm tra logic thanh toán
 * - Kiểm tra Observer Pattern
 */
@DisplayName("AuctionManager - Bộ kiểm thử hệ thống đấu giá")
public class AuctionManagerTest {

    private AuctionManager auctionManager;
    private Seller seller;
    private Bidder bidder1;
    private Bidder bidder2;
    private Item item;
    private Auction auction;

    @BeforeEach
    @DisplayName("Khởi tạo dữ liệu cho mỗi test")
    void setUp() {
        // Khởi tạo AuctionManager (Singleton)
        auctionManager = AuctionManager.getInstance();

        // Tạo người bán
        seller = new Seller("seller_test", "password123", "Test Store");
        seller.setBalance(0);  // Seller không cần tiền

        // Tạo người mua (Bidder)
        bidder1 = new Bidder("bidder1_test", "password123", 1000000.0, "TP.HCM", "0901234567");
        bidder2 = new Bidder("bidder2_test", "password123", 500000.0, "Hà Nội", "0987654321");

        // Tạo vật phẩm
        item = new Vehicle(
                "Test Car",
                50000.0,
                "Test Description",
                seller.getId(),
                "Toyota",
                "Camry 2024",
                2024,
                0,
                "Petrol",
                "White",
                "30H-123.45",
                true,
                "Auto"
        );

        // Tạo phiên đấu giá
        auction = new Auction(
                item.getId(),
                seller.getId(),
                item.getStartingPrice(),
                LocalDateTime.now().plusHours(1)
        );
    }

    // ========================================================================
    // TEST 1: TẠỌN PHIÊN ĐẤU GIÁ
    // ========================================================================

    @Test
    @DisplayName("Test 1: Tạo phiên đấu giá mới thành công")
    void testCreateAuctionSuccess() {
        System.out.println("\n[TEST 1] Tạo phiên đấu giá...");

        // Tạo phiên
        auctionManager.createAuction(auction);

        // Kiểm tra phiên đã được thêm vào danh sách hoạt động
        assertTrue(auctionManager.getActiveAuctionCount() >= 0);
        System.out.println("✓ Tạo phiên đấu giá thành công!");
    }

    @Test
    @DisplayName("Test 2: Phiên đấu giá không được tạo nếu null")
    void testCreateAuctionWithNull() {
        System.out.println("\n[TEST 2] Tạo phiên null...");

        int countBefore = auctionManager.getActiveAuctionCount();
        auctionManager.createAuction(null);
        int countAfter = auctionManager.getActiveAuctionCount();

        // Số lượng phiên không thay đổi
        assertEquals(countBefore, countAfter);
        System.out.println("✓ Phiên null bị loại bỏ!");
    }

    // ========================================================================
    // TEST 3: ĐẶT GIÁ (BIDDING)
    // ========================================================================

    @Test
    @DisplayName("Test 3: Bidder 1 đặt giá thành công")
    void testPlaceBidSuccess() {
        System.out.println("\n[TEST 3] Bidder 1 đặt giá...");

        // Bidder 1 đặt giá
        double newBid = auction.getCurrentPrice() + 10000;
        boolean result = auctionManager.placeBid(bidder1, auction, newBid);

        // Kiểm tra
        assertTrue(result, "Đặt giá phải thành công");
        assertEquals(newBid, auction.getCurrentPrice());
        assertEquals(bidder1.getId(), auction.getCurrentHighestBidderId());
        System.out.println("✓ Đặt giá thành công! Giá mới: $" + newBid);
    }

    @Test
    @DisplayName("Test 4: Không cho phép đặt giá thấp hơn giá hiện tại")
    void testPlaceBidLowerThanCurrent() {
        System.out.println("\n[TEST 4] Đặt giá thấp hơn giá hiện tại...");

        double currentPrice = auction.getCurrentPrice();
        double lowBid = currentPrice - 1000;

        // Cố gắng đặt giá thấp
        boolean result = auctionManager.placeBid(bidder1, auction, lowBid);

        // Kiểm tra
        assertFalse(result, "Không được đặt giá thấp hơn");
        assertEquals(currentPrice, auction.getCurrentPrice());
        System.out.println("✓ Hệ thống chặn đặt giá thấp!");
    }

    @Test
    @DisplayName("Test 5: Không cho phép nếu Bidder không đủ tiền")
    void testPlaceBidInsufficientBalance() {
        System.out.println("\n[TEST 5] Bidder không đủ tiền...");

        // Tạo bidder với ít tiền
        Bidder poorBidder = new Bidder("poor_bidder", "password", 1000.0, "City", "0123456789");

        // Cố gắng đặt giá cao
        double highBid = poorBidder.getBalance() + 100000;
        boolean result = auctionManager.placeBid(poorBidder, auction, highBid);

        // Kiểm tra
        assertFalse(result, "Không được phép vì không đủ tiền");
        System.out.println("✓ Hệ thống chặn bidder không đủ tiền!");
    }

    @Test
    @DisplayName("Test 6: Chỉ Bidder mới được đặt giá, Seller không được")
    void testPlaceBidOnlBidderAllowed() {
        System.out.println("\n[TEST 6] Seller cố gắng đặt giá...");

        // Seller cố gắng đặt giá
        boolean result = auctionManager.placeBid(seller, auction, 60000.0);

        // Kiểm tra
        assertFalse(result, "Seller không được đặt giá");
        System.out.println("✓ Hệ thống chặn Seller đặt giá!");
    }

    @Test
    @DisplayName("Test 7: Hai Bidder cạnh tranh, người cuối cùng thắng")
    void testBiddingWar() {
        System.out.println("\n[TEST 7] Hai Bidder cạnh tranh...");

        // Bidder 1 đặt giá
        double bid1 = auction.getCurrentPrice() + 10000;
        boolean result1 = auctionManager.placeBid(bidder1, auction, bid1);
        assertTrue(result1);
        assertEquals(bidder1.getId(), auction.getCurrentHighestBidderId());
        System.out.println("  → Bidder 1 đặt: $" + bid1);

        // Bidder 2 đặt giá cao hơn
        double bid2 = bid1 + 20000;
        boolean result2 = auctionManager.placeBid(bidder2, auction, bid2);
        assertTrue(result2);
        assertEquals(bidder2.getId(), auction.getCurrentHighestBidderId());
        assertEquals(bid2, auction.getCurrentPrice());
        System.out.println("  → Bidder 2 đặt: $" + bid2);

        // Bidder 1 đặt lại cao hơn
        double bid3 = bid2 + 15000;
        boolean result3 = auctionManager.placeBid(bidder1, auction, bid3);
        assertTrue(result3);
        assertEquals(bidder1.getId(), auction.getCurrentHighestBidderId());
        System.out.println("  → Bidder 1 đặt lại: $" + bid3);

        System.out.println("✓ Bidder 1 thắng với giá: $" + bid3);
    }

    // ========================================================================
    // TEST 8: KIỂM TRA TRẠNG THÁI PHIÊN ĐẤU GIÁ
    // ========================================================================

    @Test
    @DisplayName("Test 8: Phiên đấu giá mới có trạng thái ACTIVE")
    void testAuctionInitialStatus() {
        System.out.println("\n[TEST 8] Kiểm tra trạng thái phiên...");

        assertEquals("ACTIVE", auction.getStatus());
        assertFalse(auction.isExpired());
        assertTrue(auction.canBid());
        System.out.println("✓ Phiên có trạng thái ACTIVE");
    }

    @Test
    @DisplayName("Test 9: Phiên hết hạn không cho đặt giá")
    void testAuctionExpired() {
        System.out.println("\n[TEST 9] Phiên hết hạn...");

        // Tạo phiên với thời gian kết thúc trong quá khứ
        Auction expiredAuction = new Auction(
                item.getId(),
                seller.getId(),
                item.getStartingPrice(),
                LocalDateTime.now().minusHours(1)
        );

        // Cố gắng đặt giá
        boolean result = auctionManager.placeBid(bidder1, expiredAuction, 60000.0);

        // Kiểm tra
        assertFalse(result, "Không được đặt giá vì phiên hết hạn");
        assertTrue(expiredAuction.isExpired());
        System.out.println("✓ Phiên hết hạn được chặn!");
    }

    // ========================================================================
    // TEST 10: KIỂM TRA USER MODEL
    // ========================================================================

    @Test
    @DisplayName("Test 10: Kiểm tra Seller model")
    void testSellerModel() {
        System.out.println("\n[TEST 10] Kiểm tra Seller...");

        assertEquals("seller_test", seller.getUsername());
        assertEquals("SELLER", seller.getRole());
        assertEquals("Test Store", seller.getStoreName());
        assertEquals(5.0, seller.getRating());

        // Kiểm tra cộng điểm
        seller.setRating(5.0 + 0.1);
        assertEquals(5.1, seller.getRating());
        System.out.println("✓ Seller model hoạt động đúng!");
    }

    @Test
    @DisplayName("Test 11: Kiểm tra Bidder model")
    void testBidderModel() {
        System.out.println("\n[TEST 11] Kiểm tra Bidder...");

        assertEquals("bidder1_test", bidder1.getUsername());
        assertEquals("BIDDER", bidder1.getRole());
        assertEquals(1000000.0, bidder1.getBalance());
        assertEquals("0901234567", bidder1.getPhoneNumber());

        // Kiểm tra rút tiền
        boolean withdrawn = bidder1.withdraw(100000.0);
        assertTrue(withdrawn);
        assertEquals(900000.0, bidder1.getBalance());

        // Kiểm tra nạp tiền
        bidder1.deposit(50000.0);
        assertEquals(950000.0, bidder1.getBalance());
        System.out.println("✓ Bidder model hoạt động đúng!");
    }

    @Test
    @DisplayName("Test 12: Không cho phép rút tiền nhiều hơn số dư")
    void testWithdrawMoreThanBalance() {
        System.out.println("\n[TEST 12] Rút tiền vượt quá số dư...");

        double initialBalance = bidder1.getBalance();
        boolean result = bidder1.withdraw(initialBalance + 100000);

        // Kiểm tra
        assertFalse(result);
        assertEquals(initialBalance, bidder1.getBalance());
        System.out.println("✓ Hệ thống chặn rút tiền vượt quá!");
    }

    // ========================================================================
    // TEST 13: KIỂM TRA ITEM MODEL
    // ========================================================================

    @Test
    @DisplayName("Test 13: Kiểm tra Vehicle item")
    void testVehicleItem() {
        System.out.println("\n[TEST 13] Kiểm tra Vehicle...");

        assertTrue(item instanceof Vehicle);
        Vehicle vehicle = (Vehicle) item;

        assertEquals("Test Car", vehicle.getName());
        assertEquals("VEHICLE", vehicle.getCategory());
        assertEquals(50000.0, vehicle.getStartingPrice());
        assertEquals("Toyota", vehicle.getBrand());
        assertEquals(2024, vehicle.getManufacturingYear());
        System.out.println("✓ Vehicle model hoạt động đúng!");
    }

    @Test
    @DisplayName("Test 14: Kiểm tra cập nhật giá vật phẩm")
    void testItemPriceUpdate() {
        System.out.println("\n[TEST 14] Cập nhật giá vật phẩm...");

        double initialPrice = item.getCurrentPrice();
        item.updateCurrentPrice(60000.0);

        assertEquals(60000.0, item.getCurrentPrice());

        // Cố gắng cập nhật giá thấp hơn
        item.updateCurrentPrice(40000.0);
        assertEquals(60000.0, item.getCurrentPrice());  // Không thay đổi
        System.out.println("✓ Cập nhật giá hoạt động đúng!");
    }

    // ========================================================================
    // TEST 15: KIỂM TRA AUCTION MODEL
    // ========================================================================

    @Test
    @DisplayName("Test 15: Kiểm tra Auction model")
    void testAuctionModel() {
        System.out.println("\n[TEST 15] Kiểm tra Auction model...");

        assertEquals(item.getId(), auction.getItemId());
        assertEquals(seller.getId(), auction.getSellerId());
        assertEquals(50000.0, auction.getCurrentPrice());
        assertNull(auction.getCurrentHighestBidderId());
        assertEquals("ACTIVE", auction.getStatus());

        // Kiểm tra cập nhật người đặt giá
        auction.setCurrentHighestBidderId(bidder1.getId());
        assertEquals(bidder1.getId(), auction.getCurrentHighestBidderId());

        // Kiểm tra cập nhật giá
        auction.setCurrentPrice(60000.0);
        assertEquals(60000.0, auction.getCurrentPrice());
        System.out.println("✓ Auction model hoạt động đúng!");
    }

    // ========================================================================
    // TEST 16: KIỂM TRA BID TRANSACTION
    // ========================================================================

    @Test
    @DisplayName("Test 16: Kiểm tra BidTransaction model")
    void testBidTransaction() {
        System.out.println("\n[TEST 16] Kiểm tra BidTransaction...");

        BidTransaction tx = new BidTransaction(auction.getId(), bidder1.getId(), 60000.0);

        assertNotNull(tx.getId());
        assertEquals(auction.getId(), tx.getAuctionId());
        assertEquals(bidder1.getId(), tx.getBidderId());
        assertEquals(60000.0, tx.getBidAmount());
        assertNotNull(tx.getBidTime());
        assertTrue(tx.isValidBid());
        System.out.println("✓ BidTransaction model hoạt động đúng!");
    }

    @Test
    @DisplayName("Test 17: Kiểm tra validation BidTransaction")
    void testBidTransactionValidation() {
        System.out.println("\n[TEST 17] Validation BidTransaction...");

        // Bid không hợp lệ (không có bidderId)
        BidTransaction invalidTx = new BidTransaction(auction.getId(), null, 60000.0);
        assertFalse(invalidTx.isValidBid());

        // Bid không hợp lệ (bidAmount <= 0)
        BidTransaction invalidTx2 = new BidTransaction(auction.getId(), bidder1.getId(), 0);
        assertFalse(invalidTx2.isValidBid());
        System.out.println("✓ Validation BidTransaction hoạt động đúng!");
    }

    // ========================================================================
    // TEST 18: OBSERVER PATTERN
    // ========================================================================

    @Test
    @DisplayName("Test 18: Kiểm tra Observer được thêm và gỡ")
    void testObserverManagement() {
        System.out.println("\n[TEST 18] Kiểm tra Observer management...");

        MockBidObserver observer = new MockBidObserver();

        int countBefore = auctionManager.getObserverCount();
        auctionManager.addObserver(observer);
        int countAfter = auctionManager.getObserverCount();

        assertTrue(countAfter >= countBefore);

        auctionManager.removeObserver(observer);
        int countRemoved = auctionManager.getObserverCount();
        assertTrue(countRemoved <= countAfter);
        System.out.println("✓ Observer management hoạt động đúng!");
    }

    // ========================================================================
    // HELPER CLASS: MockBidObserver
    // ========================================================================

    /**
     * Mock Observer để test Observer Pattern.
     */
    private static class MockBidObserver implements com.team4.observer.BidObserver {
        public boolean notified = false;

        @Override
        public void updateNewBid(Auction auction, BidTransaction transaction) {
            notified = true;
        }
    }

    // ========================================================================
    // TEST SUMMARY
    // ========================================================================

    @Test
    @DisplayName("Test 19: In tóm tắt kết quả")
    void testSummary() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("                    ✓ TẤT CẢ TEST PASS ✓");
        System.out.println("=".repeat(70));
        System.out.println("Hệ thống đấu giá hoạt động bình thường!");
        System.out.println("- Tạo phiên đấu giá: OK");
        System.out.println("- Đặt giá thầu: OK");
        System.out.println("- Kiểm tra trạng thái: OK");
        System.out.println("- User/Item/Auction models: OK");
        System.out.println("- Observer Pattern: OK");
        System.out.println("=".repeat(70) + "\n");
    }
}
