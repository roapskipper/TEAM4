package com.team4.service;

import com.team4.dao.AuctionDAO;
import com.team4.dao.AutoBiddingDAO;
import com.team4.dao.BidTransactionDAO;
import com.team4.dao.UserDAO;
import com.team4.db.DatabaseManager;
import com.team4.dto.auction.BidTransactionResponseDTO;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Kiểm thử nghiệp vụ Đặt giá (BiddingService).
 * Đảm bảo các quy tắc Proxy Bidding và Anti-Sniping hoạt động đúng với cấu trúc DTO mới.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests for BiddingService")
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
    void setUp() {
        mockedDatabaseManager = mockStatic(DatabaseManager.class);
        mockedDatabaseManager.when(DatabaseManager::getConnection).thenReturn(mockConn);
    }

    @AfterEach
    void tearDown() {
        mockedDatabaseManager.close();
    }

    // Helper tạo Bidder thật
    private Bidder createRealBidder(String id, String balance) {
        return new Bidder(id, LocalDateTime.now(), "user_" + id, "hash", "Bidder", "b@t.com", new BigDecimal(balance), "Addr", "0912345678");
    }

    // Helper tạo Auction thật
    private Auction createRealAuction(String itemId, String sellerId, String currentPrice) {
        Auction auction = new Auction(itemId, sellerId, new BigDecimal("100.00"), new BigDecimal("10.00"), LocalDateTime.now().plusDays(1));
        auction.approve(); // Chuyển sang RUNNING
        return auction;
    }

    @Nested
    @DisplayName("Kiểm thử tính hợp lệ (Validation)")
    class ValidationTests {

        @Test
        @DisplayName("Thất bại khi đặt giá cho phiên đấu giá không tồn tại")
        void testPlaceBid_AuctionNotFound() {
            BidRequestDTO request = new BidRequestDTO("auc-none", "bidder-1", new BigDecimal("500"));
            when(auctionDAO.findById(eq(mockConn), eq("auc-none"))).thenReturn(null);

            assertThrows(BusinessException.class, () -> biddingService.placeBid(request));
            mockedDatabaseManager.verify(() -> DatabaseManager.rollbackTransaction(mockConn));
        }

        @Test
        @DisplayName("Thất bại khi người bán tự đấu giá mặt hàng của mình")
        void testPlaceBid_SellerBiddingOwnItem() {
            String sellerId = "seller-123";
            Auction auction = createRealAuction("item-1", sellerId, "100.00");
            Bidder bidderAsSeller = createRealBidder(sellerId, "1000.00");

            BidRequestDTO request = new BidRequestDTO("auc-1", sellerId, new BigDecimal("200"));
            when(auctionDAO.findById(mockConn, "auc-1")).thenReturn(auction);
            when(userDAO.findById(mockConn, sellerId)).thenReturn(bidderAsSeller);

            BusinessException ex = assertThrows(BusinessException.class, () -> biddingService.placeBid(request));
            assertEquals("Sellers are not allowed to bid on their own auctions.", ex.getMessage());
        }

        @Test
        @DisplayName("Thất bại khi số dư ví không đủ để đặt mức giá tối đa")
        void testPlaceBid_InsufficientBalance() {
            Auction auction = createRealAuction("item-1", "seller-1", "100.00");
            Bidder bidder = createRealBidder("bidder-1", "50.00"); // Chỉ có 50 đồng

            BidRequestDTO request = new BidRequestDTO("auc-1", "bidder-1", new BigDecimal("500"));
            when(auctionDAO.findById(mockConn, "auc-1")).thenReturn(auction);
            when(userDAO.findById(mockConn, "bidder-1")).thenReturn(bidder);

            BusinessException ex = assertThrows(BusinessException.class, () -> biddingService.placeBid(request));
            assertEquals("Insufficient balance to cover this bid.", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ Proxy Bidding")
    class ProxyBiddingLogicTests {

        @Test
        @DisplayName("Đặt giá lần đầu - Người đặt dẫn đầu với giá khởi điểm")
        void testPlaceBid_FirstBidderSuccess() {
            String auctionId = "auc-1";
            String bidderId = "bidder-1";
            Auction auction = createRealAuction("i1", "s1", "100.00");
            Bidder bidder = createRealBidder(bidderId, "1000.00");

            BidRequestDTO request = new BidRequestDTO(auctionId, bidderId, new BigDecimal("500.00"));

            when(auctionDAO.findById(mockConn, auctionId)).thenReturn(auction);
            when(userDAO.findById(mockConn, bidderId)).thenReturn(bidder);
            when(autoBiddingDAO.findByAuctionAndBidder(mockConn, auctionId, bidderId)).thenReturn(null);
            when(autoBiddingDAO.insert(eq(mockConn), any(AutoBidding.class))).thenReturn(true);

            AutoBidding config = new AutoBidding(auctionId, bidderId, new BigDecimal("500.00"));
            when(autoBiddingDAO.findActiveByAuctionId(mockConn, auctionId)).thenReturn(List.of(config));
            when(auctionDAO.updateCurrentBid(any(), any(), any(), any())).thenReturn(true);
            when(bidTransactionDAO.insert(any(), any())).thenReturn(true);

            // WHEN
            biddingService.placeBid(request);

            // THEN: Giá hiện tại vẫn là 100 vì chưa có ai cạnh tranh
            verify(auctionDAO).updateCurrentBid(mockConn, auctionId, new BigDecimal("100.00"), bidderId);
            mockedDatabaseManager.verify(() -> DatabaseManager.commitTransaction(mockConn));
        }

        @Test
        @DisplayName("Cạnh tranh Proxy Bidding - Người mới thắng người cũ")
        void testPlaceBid_OutbiddingExistingProxy() {
            String auctionId = "auc-1";
            String bidderA = "bidder-A"; // Đã đặt tối đa 200 trước đó
            String bidderB = "bidder-B"; // Đặt tối đa 500 bây giờ

            Auction auction = createRealAuction("i1", "s1", "100.00");
            // Giả lập A đang dẫn đầu với giá 100
            auction.applyBid(bidderA, new BigDecimal("100.00"));

            Bidder userB = createRealBidder(bidderB, "1000.00");
            BidRequestDTO request = new BidRequestDTO(auctionId, bidderB, new BigDecimal("500.00"));

            when(auctionDAO.findById(mockConn, auctionId)).thenReturn(auction);
            when(userDAO.findById(mockConn, bidderB)).thenReturn(userB);
            when(autoBiddingDAO.findByAuctionAndBidder(mockConn, auctionId, bidderB)).thenReturn(null);
            when(autoBiddingDAO.insert(eq(mockConn), any(AutoBidding.class))).thenReturn(true);

            AutoBidding configA = new AutoBidding(auctionId, bidderA, new BigDecimal("200.00"));
            AutoBidding configB = new AutoBidding(auctionId, bidderB, new BigDecimal("500.00"));

            when(autoBiddingDAO.findActiveByAuctionId(mockConn, auctionId)).thenReturn(List.of(configA, configB));
            when(auctionDAO.updateCurrentBid(any(), any(), any(), any())).thenReturn(true);
            when(bidTransactionDAO.insert(any(), any())).thenReturn(true);

            // WHEN
            biddingService.placeBid(request);

            // THEN: B dẫn đầu, giá hiển thị = max(A) + step(10) = 210
            verify(auctionDAO).updateCurrentBid(mockConn, auctionId, new BigDecimal("210.00"), bidderB);
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ Anti-Sniping")
    class AntiSnipingTests {

        @Test
        @DisplayName("Gia hạn thời gian khi đặt giá sát giờ kết thúc")
        void testAntiSniping_TriggersExtension() {
            String auctionId = "auc-1";
            // Kết thúc sau 30 giây
            LocalDateTime nearEnd = LocalDateTime.now().plusSeconds(30);
            Auction auction = new Auction("i1", "s1", new BigDecimal("100"), new BigDecimal("10"), nearEnd);
            auction.approve();

            Bidder bidder = createRealBidder("b1", "1000");
            BidRequestDTO request = new BidRequestDTO(auctionId, "b1", new BigDecimal("200"));

            when(auctionDAO.findById(mockConn, auctionId)).thenReturn(auction);
            when(userDAO.findById(mockConn, "b1")).thenReturn(bidder);
            when(autoBiddingDAO.findByAuctionAndBidder(mockConn, auctionId, "b1")).thenReturn(null);
            when(autoBiddingDAO.insert(eq(mockConn), any(AutoBidding.class))).thenReturn(true);

            AutoBidding config = new AutoBidding(auctionId, "b1", new BigDecimal("200"));
            when(autoBiddingDAO.findActiveByAuctionId(mockConn, auctionId)).thenReturn(List.of(config));
            when(auctionDAO.updateEndTime(any(), any(), any())).thenReturn(true);
            when(auctionDAO.updateCurrentBid(any(), any(), any(), any())).thenReturn(true);
            when(bidTransactionDAO.insert(any(), any())).thenReturn(true);

            // WHEN
            biddingService.placeBid(request);

            // THEN: Phải gọi lệnh cập nhật lại thời gian kết thúc
            verify(auctionDAO).updateEndTime(eq(mockConn), eq(auctionId), any());
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ Truy vấn lịch sử (Queries)")
    class HistoryTests {

        @Test
        @DisplayName("Lấy lịch sử đấu giá (Trả về DTO)")
        void testGetBidHistory() {
            String auctionId = "auc-1";
            BidTransaction bid = new BidTransaction(auctionId, "bidder-1", new BigDecimal("150"));
            when(bidTransactionDAO.findByAuctionId(auctionId)).thenReturn(List.of(bid));

            // WHEN
            List<BidTransactionResponseDTO> history = biddingService.getBidHistoryByAuction(auctionId);

            // THEN
            assertEquals(1, history.size());
            assertEquals("bidder-1", history.get(0).getBidderId());
        }
    }
}
