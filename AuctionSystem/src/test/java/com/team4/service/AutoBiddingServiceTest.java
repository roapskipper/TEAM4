package com.team4.service;

import com.team4.dao.AuctionDAO;
import com.team4.dao.AutoBiddingDAO;
import com.team4.dao.UserDAO;
import com.team4.model.Auction;
import com.team4.model.AutoBidding;
import com.team4.model.Bidder;
import com.team4.model.Seller;
import com.team4.util.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Lớp kiểm thử AutoBiddingServiceTest.
 * Môi trường: JDK 21, JUnit 5, Mockito.
 * 
 * TUÂN THỦ QUY TẮC:
 * 1. KHÔNG MOCK các class chứa dữ liệu (User, Auction, AutoBidding) -> Sử dụng 'new'.
 * 2. CHỈ MOCK các Interface phụ thuộc logic (DAO).
 * 3. Sử dụng @ExtendWith(MockitoExtension.class).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Kiểm thử nghiệp vụ Đấu giá tự động (AutoBiddingService)")
public class AutoBiddingServiceTest {

    @Mock private AutoBiddingDAO autoBiddingDAO;
    @Mock private AuctionDAO auctionDAO;
    @Mock private UserDAO userDAO;

    @InjectMocks
    private AutoBiddingService autoBiddingService;

    // Helper tạo Auction thật ở trạng thái RUNNING
    private Auction createRunningAuction(String auctionId, String sellerId, String currentPrice) {
        Auction auction = new Auction("item-1", sellerId, new BigDecimal("100.00"), new BigDecimal("10.00"), LocalDateTime.now().plusDays(1));
        auction.approve(); // Chuyển sang RUNNING
        if (currentPrice != null) {
            // Giả lập có người đã bid để nâng giá hiện tại
            auction.applyBid("other-bidder", new BigDecimal(currentPrice));
        }
        return auction;
    }

    // Helper tạo Bidder thật
    private Bidder createRealBidder(String id) {
        return new Bidder(id, LocalDateTime.now(), "bidder_" + id, "hash", "Bidder Name", "b@test.com", new BigDecimal("1000.00"), "Address", "09129999");
    }

    @Nested
    @DisplayName("Nghiệp vụ Bật Đấu giá tự động (Enable Auto-bid)")
    class EnableAutoBiddingTests {

        @Test
        @DisplayName("Bật lần đầu thành công - Tạo cấu hình mới")
        void testEnable_FirstTime_Success() {
            // GIVEN
            String bidderId = "bidder-1";
            String auctionId = "auc-1";
            BigDecimal maxLimit = new BigDecimal("500.00");

            Auction auction = createRunningAuction(auctionId, "seller-1", "100.00");
            Bidder bidder = createRealBidder(bidderId);

            when(auctionDAO.findById(auctionId)).thenReturn(auction);
            when(userDAO.findById(bidderId)).thenReturn(bidder);
            when(autoBiddingDAO.findByAuctionAndBidder(auctionId, bidderId)).thenReturn(null);
            when(autoBiddingDAO.insert(any(AutoBidding.class))).thenReturn(true);

            // WHEN
            AutoBidding result = autoBiddingService.enableAutoBidding(bidderId, auctionId, maxLimit);

            // THEN
            assertNotNull(result);
            assertEquals(maxLimit, result.getMaxLimit());
            assertTrue(result.isActive());
            verify(autoBiddingDAO).insert(any(AutoBidding.class));
        }

        @Test
        @DisplayName("Bật lại thành công - Kích hoạt lại cấu hình cũ đã tắt")
        void testEnable_Reactivate_Success() {
            // GIVEN
            String bidderId = "bidder-1";
            String auctionId = "auc-1";
            BigDecimal newLimit = new BigDecimal("600.00");

            Auction auction = createRunningAuction(auctionId, "seller-1", "100.00");
            Bidder bidder = createRealBidder(bidderId);
            // Cấu hình cũ đang inactive
            AutoBidding oldConfig = new AutoBidding(auctionId, bidderId, new BigDecimal("300.00"));
            oldConfig.deactivate();

            when(auctionDAO.findById(auctionId)).thenReturn(auction);
            when(userDAO.findById(bidderId)).thenReturn(bidder);
            when(autoBiddingDAO.findByAuctionAndBidder(auctionId, bidderId)).thenReturn(oldConfig);
            when(autoBiddingDAO.update(oldConfig)).thenReturn(true);

            // WHEN
            AutoBidding result = autoBiddingService.enableAutoBidding(bidderId, auctionId, newLimit);

            // THEN
            assertTrue(result.isActive());
            assertEquals(new BigDecimal("600.00"), result.getMaxLimit());
            verify(autoBiddingDAO).update(oldConfig);
        }

        @Test
        @DisplayName("Thất bại - Phiên đấu giá không ở trạng thái RUNNING")
        void testEnable_AuctionNotRunning() {
            String auctionId = "auc-1";
            // Phiên mới tạo, chưa được duyệt (PENDING)
            Auction pendingAuction = new Auction("i1", "s1", BigDecimal.TEN, BigDecimal.ONE, LocalDateTime.now());
            
            when(auctionDAO.findById(auctionId)).thenReturn(pendingAuction);

            assertThrows(BusinessException.class, () -> 
                autoBiddingService.enableAutoBidding("b1", auctionId, new BigDecimal("100.00"))
            );
        }

        @Test
        @DisplayName("Thất bại - Giới hạn tối đa không lớn hơn giá hiện tại")
        void testEnable_LimitTooLow() {
            String auctionId = "auc-1";
            Auction auction = createRunningAuction(auctionId, "s1", "200.00"); // Giá hiện tại 200
            
            when(auctionDAO.findById(auctionId)).thenReturn(auction);

            // Cố tình đặt giới hạn 150 (nhỏ hơn 200)
            assertThrows(BusinessException.class, () -> 
                autoBiddingService.enableAutoBidding("b1", auctionId, new BigDecimal("150.00"))
            );
        }

        @Test
        @DisplayName("Thất bại - Người bán không được dùng Auto-bid cho chính mình")
        void testEnable_SellerSelfBidding() {
            String sellerId = "seller-1";
            Auction auction = createRunningAuction("auc-1", sellerId, "100.00");
            
            when(auctionDAO.findById("auc-1")).thenReturn(auction);

            assertThrows(BusinessException.class, () -> 
                autoBiddingService.enableAutoBidding(sellerId, "auc-1", new BigDecimal("500.00"))
            );
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ Cập nhật giới hạn (Update Auto-bid)")
    class UpdateAutoBiddingTests {

        @Test
        @DisplayName("Cập nhật thành công")
        void testUpdate_Success() {
            String configId = "conf-123";
            BigDecimal newLimit = new BigDecimal("800.00");
            AutoBidding config = new AutoBidding("auc-1", "bid-1", new BigDecimal("500.00"));
            Auction auction = createRunningAuction("auc-1", "sel-1", "100.00");

            when(autoBiddingDAO.findById(configId)).thenReturn(config);
            when(auctionDAO.findById("auc-1")).thenReturn(auction);
            when(autoBiddingDAO.update(config)).thenReturn(true);

            // WHEN
            boolean updated = autoBiddingService.updateAutoBidding(configId, newLimit);

            // THEN
            assertTrue(updated);
            assertEquals(newLimit, config.getMaxLimit());
            verify(autoBiddingDAO).update(config);
        }

        @Test
        @DisplayName("Thất bại - Cấu hình không tồn tại")
        void testUpdate_NotFound() {
            when(autoBiddingDAO.findById("none")).thenReturn(null);

            assertThrows(BusinessException.class, () -> 
                autoBiddingService.updateAutoBidding("none", new BigDecimal("1000.00"))
            );
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ Tắt Đấu giá tự động (Disable Auto-bid)")
    class DisableAutoBiddingTests {

        @Test
        @DisplayName("Tắt thành công")
        void testDisable_Success() {
            String configId = "conf-1";
            AutoBidding config = new AutoBidding("auc-1", "bid-1", new BigDecimal("500.00"));
            // Mặc định là active = true

            when(autoBiddingDAO.findById(configId)).thenReturn(config);
            when(autoBiddingDAO.updateActive(configId, false)).thenReturn(true);

            // WHEN
            boolean disabled = autoBiddingService.disableAutoBidding(configId, "auc-1");

            // THEN
            assertTrue(disabled);
            assertFalse(config.isActive());
            verify(autoBiddingDAO).updateActive(configId, false);
        }

        @Test
        @DisplayName("Thất bại - Auto-bid vốn đã tắt từ trước")
        void testDisable_AlreadyDisabled() {
            String configId = "conf-1";
            AutoBidding config = new AutoBidding("auc-1", "bid-1", new BigDecimal("500.00"));
            config.deactivate(); // Đã tắt

            when(autoBiddingDAO.findById(configId)).thenReturn(config);

            assertThrows(BusinessException.class, () -> 
                autoBiddingService.disableAutoBidding(configId, "auc-1")
            );
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ Truy vấn (Queries)")
    class QueryTests {

        @Test
        @DisplayName("Tìm cấu hình cụ thể thành công")
        void testFindConfig_Success() {
            String bidderId = "b1";
            String auctionId = "a1";
            AutoBidding config = new AutoBidding(auctionId, bidderId, new BigDecimal("500.00"));

            when(autoBiddingDAO.findByAuctionAndBidder(auctionId, bidderId)).thenReturn(config);

            AutoBidding result = autoBiddingService.findConfig(bidderId, auctionId);

            assertNotNull(result);
            assertEquals(bidderId, result.getBidderId());
        }

        @Test
        @DisplayName("Thất bại khi tìm cấu hình chưa cài đặt")
        void testFindConfig_NotFound() {
            when(autoBiddingDAO.findByAuctionAndBidder(anyString(), anyString())).thenReturn(null);

            assertThrows(BusinessException.class, () -> 
                autoBiddingService.findConfig("b1", "a1")
            );
        }

        @Test
        @DisplayName("Lấy tất cả cấu hình đang hoạt động trong phiên")
        void testFindActiveConfigs() {
            String auctionId = "auc-1";
            List<AutoBidding> activeList = List.of(
                new AutoBidding(auctionId, "b1", new BigDecimal("500")),
                new AutoBidding(auctionId, "b2", new BigDecimal("600"))
            );

            when(autoBiddingDAO.findActiveByAuctionId(auctionId)).thenReturn(activeList);

            List<AutoBidding> results = autoBiddingService.findActiveConfigs(auctionId);

            assertEquals(2, results.size());
            verify(autoBiddingDAO).findActiveByAuctionId(auctionId);
        }
    }
}
