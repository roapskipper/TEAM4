package com.team4.service;

import com.team4.dao.AuctionDAO;
import com.team4.dao.AutoBiddingDAO;
import com.team4.dao.UserDAO;
import com.team4.dto.bidding.AutoBidRequestDTO;
import com.team4.dto.bidding.AutoBidResponseDTO;
import com.team4.model.Auction;
import com.team4.model.AutoBidding;
import com.team4.model.Bidder;
import com.team4.model.User;
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
 * Kiểm thử nghiệp vụ Đấu giá tự động (AutoBiddingService).
 * Đảm bảo quản lý cấu hình tự động đặt giá chính xác với cấu trúc DTO mới.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests for AutoBiddingService")
public class AutoBiddingServiceTest {

    @Mock private AutoBiddingDAO autoBiddingDAO;
    @Mock private AuctionDAO auctionDAO;
    @Mock private UserDAO userDAO;

    @InjectMocks
    private AutoBiddingService autoBiddingService;

    // Helper tạo Auction đang chạy
    private Auction createRunningAuction(String auctionId, String sellerId) {
        Auction auction = new Auction("item-1", sellerId, new BigDecimal("100.00"), new BigDecimal("10.00"), LocalDateTime.now().plusDays(1));
        auction.approve(); // RUNNING
        return auction;
    }

    // Helper tạo Bidder thật
    private Bidder createRealBidder(String id) {
        return new Bidder(id, LocalDateTime.now(), "bidder_" + id, "hash", "Bidder", "b@t.com", new BigDecimal("1000"), "Addr", "0912345678");
    }

    @Nested
    @DisplayName("Nghiệp vụ Bật Đấu giá tự động (enableAutoBidding)")
    class EnableAutoBiddingTests {

        @Test
        @DisplayName("Bật đấu giá tự động lần đầu thành công")
        void testEnable_FirstTime_Success() {
            // GIVEN: Yêu cầu hợp lệ
            String bidderId = "bidder-1";
            String auctionId = "auc-1";
            BigDecimal maxLimit = new BigDecimal("500.00");
            AutoBidRequestDTO request = new AutoBidRequestDTO(auctionId, bidderId, maxLimit);

            Auction auction = createRunningAuction(auctionId, "seller-1");
            Bidder bidder = createRealBidder(bidderId);

            when(auctionDAO.findById(auctionId)).thenReturn(auction);
            when(userDAO.findById(bidderId)).thenReturn(bidder);
            when(autoBiddingDAO.findByAuctionAndBidder(auctionId, bidderId)).thenReturn(null);
            when(autoBiddingDAO.insert(any(AutoBidding.class))).thenReturn(true);

            // WHEN: Kích hoạt auto-bid
            AutoBidResponseDTO result = autoBiddingService.enableAutoBidding(request);

            // THEN: Trả về DTO cấu hình và đã lưu vào DB
            assertNotNull(result);
            assertEquals(maxLimit, result.getMaxLimit());
            assertTrue(result.isActive());
            verify(autoBiddingDAO).insert(any(AutoBidding.class));
        }

        @Test
        @DisplayName("Thất bại khi đấu giá đã kết thúc")
        void testEnable_AuctionNotRunning() {
            String auctionId = "auc-1";
            Auction finished = new Auction("i1", "s1", BigDecimal.TEN, BigDecimal.ONE, LocalDateTime.now());
            // Mặc định là PENDING, không phải RUNNING

            when(auctionDAO.findById(auctionId)).thenReturn(finished);

            assertThrows(BusinessException.class, () ->
                    autoBiddingService.enableAutoBidding(new AutoBidRequestDTO(auctionId, "b1", new BigDecimal("100")))
            );
        }

        @Test
        @DisplayName("Thất bại khi giới hạn tối đa quá thấp")
        void testEnable_LimitTooLow() {
            String auctionId = "auc-1";
            Auction auction = createRunningAuction(auctionId, "s1"); // Giá hiện tại 100

            when(auctionDAO.findById(auctionId)).thenReturn(auction);

            // Đặt giới hạn 50 (thấp hơn giá hiện tại 100)
            assertThrows(BusinessException.class, () ->
                    autoBiddingService.enableAutoBidding(new AutoBidRequestDTO(auctionId, "b1", new BigDecimal("50")))
            );
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ Cập nhật giới hạn (updateAutoBidding)")
    class UpdateAutoBiddingTests {

        @Test
        @DisplayName("Cập nhật giới hạn tối đa thành công")
        void testUpdate_Success() {
            String configId = "conf-123";
            BigDecimal newLimit = new BigDecimal("400.00");
            AutoBidding config = new AutoBidding("auc-1", "bid-1", new BigDecimal("500.00"));
            Auction auction = createRunningAuction("auc-1", "sel-1");

            when(autoBiddingDAO.findById(configId)).thenReturn(config);
            when(auctionDAO.findById("auc-1")).thenReturn(auction);
            when(autoBiddingDAO.update(config)).thenReturn(true);

            // WHEN
            AutoBidResponseDTO result = autoBiddingService.updateAutoBidding(configId, newLimit);

            // THEN
            assertEquals(0, newLimit.compareTo(result.getMaxLimit()));
            verify(autoBiddingDAO).update(config);
        }

        @Test
        @DisplayName("Thất bại khi cấu hình không tồn tại")
        void testUpdate_NotFound() {
            when(autoBiddingDAO.findById("none")).thenReturn(null);

            assertThrows(BusinessException.class, () ->
                    autoBiddingService.updateAutoBidding("none", new BigDecimal("1000"))
            );
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ Truy vấn (Queries)")
    class QueryTests {

        @Test
        @DisplayName("Tìm cấu hình hiện tại (Trả về DTO)")
        void testFindConfig_Success() {
            String bidderId = "b1";
            String auctionId = "a1";
            AutoBidding config = new AutoBidding(auctionId, bidderId, new BigDecimal("500"));

            when(autoBiddingDAO.findByAuctionAndBidder(auctionId, bidderId)).thenReturn(config);

            AutoBidResponseDTO result = autoBiddingService.findConfig(bidderId, auctionId);

            assertNotNull(result);
            assertEquals(bidderId, result.getBidderId());
        }

        @Test
        @DisplayName("Lấy danh sách cấu hình đang hoạt động (DTO)")
        void testFindActiveConfigs() {
            String auctionId = "auc-1";
            when(autoBiddingDAO.findActiveByAuctionId(auctionId)).thenReturn(List.of(
                    new AutoBidding(auctionId, "b1", new BigDecimal("500")),
                    new AutoBidding(auctionId, "b2", new BigDecimal("600"))
            ));

            List<AutoBidResponseDTO> results = autoBiddingService.findActiveConfigs(auctionId);

            assertEquals(2, results.size());
            verify(autoBiddingDAO).findActiveByAuctionId(auctionId);
        }
    }
}
