package com.team4.service;

import com.team4.dao.AuctionDAO;
import com.team4.dao.AutoBiddingDAO;
import com.team4.dao.BidTransactionDAO;
import com.team4.dao.UserDAO;
import com.team4.db.DatabaseManager;
import com.team4.dto.bidding.BidRequestDTO;
import com.team4.model.*;
import com.team4.util.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Lớp kiểm thử BiddingServiceTest.
 * Môi trường: JDK 21, JUnit 5, Mockito.
 * 
 * TUÂN THỦ QUY TẮC:
 * 1. KHÔNG MOCK các class chứa dữ liệu (User, Auction, AutoBidding, BidTransaction) -> Sử dụng 'new'.
 * 2. CHỈ MOCK các Interface phụ thuộc logic (DAO).
 * 3. Sử dụng @ExtendWith(MockitoExtension.class).
 * 4. Sử dụng mockStatic cho DatabaseManager để quản lý Connection và Transaction.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Kiểm thử nghiệp vụ Đặt giá và Đấu giá ủy nhiệm (BiddingService)")
public class BiddingServiceTest {

    @Mock private AuctionDAO auctionDAO;
    @Mock private BidTransactionDAO bidTransactionDAO;
    @Mock private UserDAO userDAO;
    @Mock private AutoBiddingDAO autoBiddingDAO;
    @Mock private Connection mockConn;

    @InjectMocks
    private BiddingService biddingService;

    private MockedStatic<DatabaseManager> mockedDatabaseManager;

    @BeforeEach
    void setUp() throws SQLException {
        // Cần mock static vì BiddingService lấy connection từ DatabaseManager.getConnection()
        mockedDatabaseManager = mockStatic(DatabaseManager.class);
        mockedDatabaseManager.when(() -> DatabaseManager.getConnection()).thenReturn(mockConn);
    }

    @AfterEach
    void tearDown() {
        mockedDatabaseManager.close();
    }

    // Helper tạo Bidder thật
    private Bidder createRealBidder(String id, String balance) {
        return new Bidder(id, LocalDateTime.now(), "user_" + id, "hash", "Bidder " + id, "b@test.com", new BigDecimal(balance), "Address", "09129999");
    }

    // Helper tạo Auction thật
    private Auction createRealAuction(String itemId, String sellerId, String currentHighestBidderId, String currentPrice) {
        Auction auction = new Auction(itemId, sellerId, new BigDecimal("100.00"), new BigDecimal("10.00"), LocalDateTime.now().plusDays(1));
        // Chuyển sang RUNNING để có thể bid
        auction.approve();
        // Giả lập giá hiện tại (nếu cần)
        if (currentHighestBidderId != null) {
            auction.applyBid(currentHighestBidderId, new BigDecimal(currentPrice));
        }
        return auction;
    }

    @Nested
    @DisplayName("Kiểm thử tính hợp lệ khi Đặt giá (Validation)")
    class ValidationTests {

        @Test
        @DisplayName("Thất bại - Phiên đấu giá không tồn tại hoặc đã kết thúc")
        void testPlaceBid_AuctionNotAvailable() throws SQLException {
            String auctionId = "auc-1";
            when(auctionDAO.findById(eq(mockConn), eq(auctionId))).thenReturn(null);

            assertThrows(BusinessException.class, () -> 
                biddingService.placeBid(new BidRequestDTO(auctionId, "bidder-1", new BigDecimal("500.00")))
            );
            
            // Đảm bảo có rollback khi lỗi
            mockedDatabaseManager.verify(() -> DatabaseManager.rollbackTransaction(mockConn));
        }

        @Test
        @DisplayName("Thất bại - Người bán không được đặt giá trên phiên của chính mình")
        void testPlaceBid_SellerBiddingOwnAuction() throws SQLException {
            String sellerId = "seller-1";
            Auction auction = createRealAuction("item-1", sellerId, null, "100.00");
            User bidder = createRealBidder(sellerId, "1000.00"); // Cùng ID với seller

            when(auctionDAO.findById(mockConn, "auc-1")).thenReturn(auction);
            when(userDAO.findById(mockConn, sellerId)).thenReturn(bidder);

            BusinessException ex = assertThrows(BusinessException.class, () -> 
                biddingService.placeBid(new BidRequestDTO("auc-1", sellerId, new BigDecimal("500.00")))
            );
            assertTrue(ex.getMessage().contains("Sellers are not allowed"));
        }

        @Test
        @DisplayName("Thất bại - Giá đặt không cao hơn giá hiện tại ít nhất một bước giá")
        void testPlaceBid_BidTooLow() throws SQLException {
            // Giá hiện tại 100, bước giá 10 -> Ít nhất phải 110
            Auction auction = createRealAuction("item-1", "seller-1", "bidder-old", "100.00");
            User bidder = createRealBidder("bidder-new", "1000.00");

            when(auctionDAO.findById(mockConn, "auc-1")).thenReturn(auction);
            when(userDAO.findById(mockConn, "bidder-new")).thenReturn(bidder);

            assertThrows(BusinessException.class, () -> 
                biddingService.placeBid(new BidRequestDTO("auc-1", "bidder-new", new BigDecimal("105.00")))
            );
        }

        @Test
        @DisplayName("Thất bại - Số dư ví không đủ để đảm bảo mức giá tối đa")
        void testPlaceBid_InsufficientBalance() throws SQLException {
            Auction auction = createRealAuction("item-1", "seller-1", null, "100.00");
            User bidder = createRealBidder("bidder-1", "50.00"); // Chỉ có 50, đòi bid 200

            when(auctionDAO.findById(mockConn, "auc-1")).thenReturn(auction);
            when(userDAO.findById(mockConn, "bidder-1")).thenReturn(bidder);

            assertThrows(BusinessException.class, () -> 
                biddingService.placeBid(new BidRequestDTO("auc-1", "bidder-1", new BigDecimal("200.00")))
            );
        }

        @Test
        @DisplayName("Thất bại - Giá đặt vượt quá giới hạn multiplier của chính sách")
        void testPlaceBid_BidExceedsMultiplierPolicy() throws SQLException {
            // currentPrice = 1,000,000 (1M) -> multiplier = 4 -> allowedMax = 4,000,000 (4M)
            Auction auction = createRealAuction("item-1", "seller-1", "bidder-old", "1000000.00");
            User bidder = createRealBidder("bidder-new", "10000000.00");

            when(auctionDAO.findById(mockConn, "auc-1")).thenReturn(auction);
            when(userDAO.findById(mockConn, "bidder-new")).thenReturn(bidder);

            BusinessException ex = assertThrows(BusinessException.class, () -> 
                biddingService.placeBid(new BidRequestDTO("auc-1", "bidder-new", new BigDecimal("4000001.00")))
            );
            assertTrue(ex.getMessage().contains("maximum allowed limit"));
        }

        @Test
        @DisplayName("Thất bại - Giá đặt vượt quá giới hạn tuyệt đối ABSOLUTE_MAX")
        void testPlaceBid_BidExceedsAbsoluteMax() throws SQLException {
            // currentPrice = 400,000,000 (400M) -> allowedMax would be 400M * 1.3 = 520M, but clamped to ABSOLUTE_MAX (500M)
            Auction auction = createRealAuction("item-1", "seller-1", "bidder-old", "400000000.00");
            User bidder = createRealBidder("bidder-new", "600000000.00");

            when(auctionDAO.findById(mockConn, "auc-1")).thenReturn(auction);
            when(userDAO.findById(mockConn, "bidder-new")).thenReturn(bidder);

            BusinessException ex = assertThrows(BusinessException.class, () -> 
                biddingService.placeBid(new BidRequestDTO("auc-1", "bidder-new", new BigDecimal("500000001.00")))
            );
            assertTrue(ex.getMessage().contains("maximum allowed limit"));
        }

        @Test
        @DisplayName("Thành công - Giá đặt nằm trong giới hạn cho phép (bằng allowedMax)")
        void testPlaceBid_BidWithinAllowedMax() throws SQLException {
            // currentPrice = 1,000,000 (1M) -> multiplier = 4 -> allowedMax = 4,000,000 (4M)
            Auction auction = createRealAuction("item-1", "seller-1", "bidder-old", "1000000.00");
            User bidder = createRealBidder("bidder-new", "10000000.00");

            when(auctionDAO.findById(mockConn, "auc-1")).thenReturn(auction);
            when(userDAO.findById(mockConn, "bidder-new")).thenReturn(bidder);
            when(autoBiddingDAO.findByAuctionAndBidder(mockConn, "auc-1", "bidder-new")).thenReturn(null);
            when(autoBiddingDAO.insert(eq(mockConn), any(AutoBidding.class))).thenReturn(true);
            
            AutoBidding config = new AutoBidding("auc-1", "bidder-new", new BigDecimal("4000000.00"));
            when(autoBiddingDAO.findActiveByAuctionId(mockConn, "auc-1")).thenReturn(List.of(config));

            when(auctionDAO.updateCurrentBid(eq(mockConn), eq("auc-1"), any(), eq("bidder-new"))).thenReturn(true);
            when(bidTransactionDAO.insert(eq(mockConn), any())).thenReturn(true);

            assertDoesNotThrow(() -> 
                biddingService.placeBid(new BidRequestDTO("auc-1", "bidder-new", new BigDecimal("4000000.00")))
            );
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ Đấu giá ủy nhiệm (Proxy Bidding Logic)")
    class ProxyBiddingTests {

        @Test
        @DisplayName("Đặt giá lần đầu - Người đặt dẫn đầu với giá khởi điểm")
        void testPlaceBid_FirstBidder() throws SQLException {
            String auctionId = "auc-1";
            String bidderId = "bidder-1";
            BigDecimal maxAmount = new BigDecimal("500.00");

            Auction auction = createRealAuction("item-1", "seller-1", null, "100.00");
            User bidder = createRealBidder(bidderId, "1000.00");

            when(auctionDAO.findById(mockConn, auctionId)).thenReturn(auction);
            when(userDAO.findById(mockConn, bidderId)).thenReturn(bidder);
            // Chưa có config nào
            when(autoBiddingDAO.findByAuctionAndBidder(mockConn, auctionId, bidderId)).thenReturn(null);
            when(autoBiddingDAO.insert(eq(mockConn), any(AutoBidding.class))).thenReturn(true);
            
            // Sau khi insert, tìm thấy 1 contender duy nhất
            AutoBidding config = new AutoBidding(auctionId, bidderId, maxAmount);
            when(autoBiddingDAO.findActiveByAuctionId(mockConn, auctionId)).thenReturn(List.of(config));

            when(auctionDAO.updateCurrentBid(eq(mockConn), eq(auctionId), any(), eq(bidderId))).thenReturn(true);
            when(bidTransactionDAO.insert(eq(mockConn), any())).thenReturn(true);

            // WHEN
            biddingService.placeBid(new BidRequestDTO(auctionId, bidderId, maxAmount));

            // THEN: Người đầu tiên bid thì giá hiển thị vẫn là giá hiện tại (khởi điểm)
            verify(auctionDAO).updateCurrentBid(mockConn, auctionId, new BigDecimal("100.00"), bidderId);
            mockedDatabaseManager.verify(() -> DatabaseManager.commitTransaction(mockConn));
        }

        @Test
        @DisplayName("Cạnh tranh Proxy Bidding - Người mới đặt cao hơn người cũ")
        void testPlaceBid_OutbiddingExistingProxy() throws SQLException {
            String auctionId = "auc-1";
            String bidderA = "bidder-A"; // Người cũ, max 200
            String bidderB = "bidder-B"; // Người mới, max 500
            BigDecimal increment = new BigDecimal("10.00");

            Auction auction = createRealAuction("item-1", "seller-1", bidderA, "100.00");
            User userB = createRealBidder(bidderB, "1000.00");

            when(auctionDAO.findById(mockConn, auctionId)).thenReturn(auction);
            when(userDAO.findById(mockConn, bidderB)).thenReturn(userB);

            // Config của B chưa có
            when(autoBiddingDAO.findByAuctionAndBidder(mockConn, auctionId, bidderB)).thenReturn(null);
            when(autoBiddingDAO.insert(eq(mockConn), any())).thenReturn(true);

            // Danh sách contender lúc này có cả A (max 200) và B (max 500)
            AutoBidding configA = new AutoBidding(auctionId, bidderA, new BigDecimal("200.00"));
            AutoBidding configB = new AutoBidding(auctionId, bidderB, new BigDecimal("500.00"));
            when(autoBiddingDAO.findActiveByAuctionId(mockConn, auctionId)).thenReturn(List.of(configA, configB));

            when(auctionDAO.updateCurrentBid(eq(mockConn), anyString(), any(), anyString())).thenReturn(true);
            when(bidTransactionDAO.insert(eq(mockConn), any())).thenReturn(true);

            // WHEN
            when(autoBiddingDAO.updateActive(any(), anyString(), anyBoolean())).thenReturn(true);
            biddingService.placeBid(new BidRequestDTO(auctionId, bidderB, new BigDecimal("500.00")));

            // THEN: B dẫn đầu, giá hiển thị = maxLimit của A (200) + increment (10) = 210
            verify(auctionDAO).updateCurrentBid(mockConn, auctionId, new BigDecimal("210.00"), bidderB);
        }

        @Test
        @DisplayName("Cạnh tranh Proxy Bidding - Hai cấu hình nằm trong allowedMax, tắt cấu hình đã hết hạn")
        void testPlaceBid_TwoAutoBidders_ExhaustedDeactivated() throws SQLException {
            String auctionId = "auc-1";
            String bidderA = "bidder-A";
            String bidderB = "bidder-B";

            // Price = 100,000 (100k) -> allowedMax = 500k
            Auction auction = createRealAuction("item-1", "seller-1", bidderA, "100000.00");
            User userB = createRealBidder(bidderB, "1000000.00");

            when(auctionDAO.findById(mockConn, auctionId)).thenReturn(auction);
            when(userDAO.findById(mockConn, bidderB)).thenReturn(userB);

            when(autoBiddingDAO.findByAuctionAndBidder(mockConn, auctionId, bidderB)).thenReturn(null);
            when(autoBiddingDAO.insert(eq(mockConn), any())).thenReturn(true);

            AutoBidding configA = new AutoBidding("conf-A", LocalDateTime.now(), auctionId, bidderA, new BigDecimal("200000.00"), true); // max 200k
            AutoBidding configB = new AutoBidding("conf-B", LocalDateTime.now(), auctionId, bidderB, new BigDecimal("400000.00"), true); // max 400k

            when(autoBiddingDAO.findActiveByAuctionId(mockConn, auctionId)).thenReturn(List.of(configA, configB));
            when(auctionDAO.updateCurrentBid(eq(mockConn), anyString(), any(), anyString())).thenReturn(true);
            when(bidTransactionDAO.insert(eq(mockConn), any())).thenReturn(true);
            when(autoBiddingDAO.updateActive(eq(mockConn), eq("conf-A"), eq(false))).thenReturn(true);

            // WHEN
            biddingService.placeBid(new BidRequestDTO(auctionId, bidderB, new BigDecimal("400000.00")));

            // THEN: Winner is B, displayPrice = A's max (200k) + increment (10k) = 210k
            // A (max 200k) is exhausted because 200k <= 210k. A must be deactivated.
            verify(autoBiddingDAO).updateActive(mockConn, "conf-A", false);
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ Gia hạn thời gian (Anti-Sniping)")
    class AntiSnipingTests {

        @Test
        @DisplayName("Kích hoạt Anti-Sniping khi bid gần thời điểm kết thúc")
        void testAntiSniping_Activation() throws SQLException {
            String auctionId = "auc-1";
            // Đặt thời gian kết thúc chỉ còn 30 giây nữa
            LocalDateTime nearEnd = LocalDateTime.now().plusSeconds(30);
            Auction auction = new Auction("item-1", "seller-1", new BigDecimal("100.00"), new BigDecimal("10.00"), nearEnd);
            auction.approve();

            User bidder = createRealBidder("bidder-1", "1000.00");

            when(auctionDAO.findById(mockConn, auctionId)).thenReturn(auction);
            when(userDAO.findById(mockConn, "bidder-1")).thenReturn(bidder);
            when(autoBiddingDAO.insert(any(), any())).thenReturn(true);
            
            AutoBidding config = new AutoBidding(auctionId, "bidder-1", new BigDecimal("500.00"));
            when(autoBiddingDAO.findActiveByAuctionId(mockConn, auctionId)).thenReturn(List.of(config));
            
            when(auctionDAO.updateEndTime(eq(mockConn), eq(auctionId), any())).thenReturn(true);
            when(auctionDAO.updateCurrentBid(any(), any(), any(), any())).thenReturn(true);
            when(bidTransactionDAO.insert(any(), any())).thenReturn(true);

            // WHEN
            biddingService.placeBid(new BidRequestDTO(auctionId, "bidder-1", new BigDecimal("500.00")));

            // THEN: Phải có gọi updateEndTime lên DB
            verify(auctionDAO).updateEndTime(eq(mockConn), eq(auctionId), any());
        }
    }

    @Nested
    @DisplayName("Quản lý giao dịch (Transaction Management)")
    class TransactionTests {

        @Test
        @DisplayName("Rollback khi xảy ra lỗi bất ngờ (SQLException)")
        void testPlaceBid_RollbackOnFailure() throws SQLException {
            String auctionId = "auc-1";
            when(auctionDAO.findById(mockConn, auctionId)).thenThrow(new RuntimeException("Lỗi kết nối bất ngờ"));

            assertThrows(BusinessException.class, () -> 
                biddingService.placeBid(new BidRequestDTO(auctionId, "bidder-1", new BigDecimal("500.00")))
            );

            // Kiểm tra xem rollback đã được gọi chưa
            mockedDatabaseManager.verify(() -> DatabaseManager.rollbackTransaction(mockConn));
            // Không được commit
            mockedDatabaseManager.verify(() -> DatabaseManager.commitTransaction(mockConn), never());
        }
    }
}
