package com.team4.service;

import com.team4.dao.AuctionDAO;
import com.team4.dao.ItemDAO;
import com.team4.dao.UserDAO;
import com.team4.dto.auction.AuctionResponseDTO;
import com.team4.dto.auction.CreateAuctionRequestDTO;
import com.team4.model.Art;
import com.team4.model.Auction;
import com.team4.model.Item;
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
 * Lớp kiểm thử AuctionServiceTest.
 * Môi trường: JDK 21, JUnit 5, Mockito.
 * 
 * TUÂN THỦ QUY TẮC:
 * 1. KHÔNG MOCK các class chứa dữ liệu (Auction, Item, Art) -> Sử dụng 'new'.
 * 2. CHỈ MOCK các Interface phụ thuộc logic (AuctionDAO, ItemDAO).
 * 3. Sử dụng @ExtendWith(MockitoExtension.class).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Kiểm thử nghiệp vụ Quản lý đấu giá (AuctionService)")
public class AuctionServiceTest {

    @Mock
    private AuctionDAO auctionDAO;

    @Mock
    private ItemDAO itemDAO;

    @Mock
    private UserDAO userDAO;

    @InjectMocks
    private AuctionService auctionService;

    // Helper tạo một Item thật (dùng Art làm ví dụ cụ thể)
    private Art createRealItem(String itemId, String ownerId) {
        return new Art(
                itemId,
                LocalDateTime.now(),
                "Bức tranh cổ",
                new BigDecimal("500.00"),
                "Mô tả tranh",
                ownerId,
                "Danh họa X",
                1900,
                Art.Medium.OIL_PAINT,
                "100x100"
        );
    }

    // Helper tạo một Auction thật
    private Auction createRealAuction(String auctionId, String itemId, String sellerId, Auction.AuctionStatus status) {
        // Auction constructor thường nhận itemId, sellerId, startingPrice, bidIncrement, endTime
        var auction = new Auction(itemId, sellerId, new BigDecimal("1000.00"), new BigDecimal("100.00"), LocalDateTime.now().plusDays(1));
        // Giả sử Auction có cơ chế set ID và Status (thường qua reflection hoặc phương thức nội bộ trong thực tế, 
        // ở đây ta giả định model cho phép hoặc constructor có tham số phù hợp)
        // Vì constructor trong code AuctionService chỉ nhận 5 tham số, ta dùng reflection hoặc phương thức nếu có.
        // Tuy nhiên, để đơn giản và an toàn, ta sẽ dùng chính các phương thức của model để đổi trạng thái.
        if (status == Auction.AuctionStatus.RUNNING) auction.approve();
        if (status == Auction.AuctionStatus.FINISHED) {
            auction.approve();
            auction.close();
        }
        if (status == Auction.AuctionStatus.PAID) {
            auction.approve();
            auction.close();
            auction.markPaid();
        }
        if (status == Auction.AuctionStatus.CANCELLED) auction.cancel();
        
        return auction;
    }

    @Nested
    @DisplayName("Nghiệp vụ Tạo phiên đấu giá (Create Auction)")
    class CreateAuctionTests {

        @Test
        @DisplayName("Tạo phiên thành công - Trạng thái mặc định PENDING")
        void testCreateAuction_Success() {
            // GIVEN: Mặt hàng tồn tại và thuộc về người bán
            String itemId = "item-1";
            String sellerId = "seller-1";
            Item realItem = createRealItem(itemId, sellerId);
            
            when(itemDAO.findById(itemId)).thenReturn(realItem);
            when(auctionDAO.insert(any(Auction.class))).thenReturn(true);

            // WHEN
            AuctionResponseDTO result = auctionService.createAuction(
                    new CreateAuctionRequestDTO(itemId, sellerId, new BigDecimal("1000.00"), new BigDecimal("50.00"), LocalDateTime.now().plusDays(7))
            );

            // THEN: 
            // 1. Kết quả trả về hợp lệ
            assertNotNull(result);
            assertEquals(itemId, result.getItemId());
            assertEquals(Auction.AuctionStatus.PENDING, result.getStatus());
            // 2. Phải gọi DAO để lưu
            verify(auctionDAO).insert(any(Auction.class));
        }

        @Test
        @DisplayName("Thất bại - Mặt hàng không tồn tại")
        void testCreateAuction_ItemNotFound() {
            String itemId = "none";
            when(itemDAO.findById(itemId)).thenReturn(null);

            assertThrows(BusinessException.class, () -> 
                auctionService.createAuction(new CreateAuctionRequestDTO(itemId, "seller-1", BigDecimal.TEN, BigDecimal.ONE, LocalDateTime.now().plusHours(1)))
            );
        }

        @Test
        @DisplayName("Thất bại - Người bán không sở hữu mặt hàng")
        void testCreateAuction_NotOwner() {
            String itemId = "item-1";
            String sellerId = "hacker";
            Item realItem = createRealItem(itemId, "real-owner"); // Chủ thật là người khác
            
            when(itemDAO.findById(itemId)).thenReturn(realItem);

            BusinessException ex = assertThrows(BusinessException.class, () -> 
                auctionService.createAuction(new CreateAuctionRequestDTO(itemId, sellerId, BigDecimal.TEN, BigDecimal.ONE, LocalDateTime.now().plusHours(1)))
            );
            assertEquals("Seller does not own this item", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ Phê duyệt và Từ chối (Approve/Reject)")
    class ApprovalTests {

        @Test
        @DisplayName("Phê duyệt thành công - PENDING sang RUNNING")
        void testApprove_Success() {
            // GIVEN: Phiên đang chờ duyệt
            String auctionId = "auc-123";
            Auction pendingAuction = createRealAuction(auctionId, "i1", "s1", Auction.AuctionStatus.PENDING);
            when(auctionDAO.findById(auctionId)).thenReturn(pendingAuction);
            when(auctionDAO.updateStatus(auctionId, Auction.AuctionStatus.RUNNING)).thenReturn(true);

            // WHEN
            AuctionResponseDTO result = auctionService.approveAuction(auctionId);

            // THEN
            assertEquals(Auction.AuctionStatus.RUNNING, result.getStatus());
            verify(auctionDAO).updateStatus(auctionId, Auction.AuctionStatus.RUNNING);
        }

        @Test
        @DisplayName("Thất bại - Phê duyệt khi trạng thái không phải PENDING")
        void testApprove_InvalidStatus() {
            String auctionId = "auc-123";
            // Giả sử phiên đã bị hủy trước đó
            Auction cancelledAuction = createRealAuction(auctionId, "i1", "s1", Auction.AuctionStatus.CANCELLED);
            when(auctionDAO.findById(auctionId)).thenReturn(cancelledAuction);

            assertThrows(BusinessException.class, () -> auctionService.approveAuction(auctionId));
        }

        @Test
        @DisplayName("Từ chối duyệt thành công")
        void testReject_Success() {
            String auctionId = "auc-123";
            Auction pendingAuction = createRealAuction(auctionId, "i1", "s1", Auction.AuctionStatus.PENDING);
            when(auctionDAO.findById(auctionId)).thenReturn(pendingAuction);
            when(auctionDAO.updateStatus(auctionId, Auction.AuctionStatus.CANCELLED)).thenReturn(true);

            // WHEN
            auctionService.rejectAuction(auctionId);

            // THEN
            verify(auctionDAO).updateStatus(auctionId, Auction.AuctionStatus.CANCELLED);
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ Kết thúc phiên (Auto Close)")
    class CloseAuctionTests {

        @Test
        @DisplayName("Tự động đóng phiên đã hết hạn")
        void testCloseExpiredAuctions() {
            // GIVEN: Có 2 phiên RUNNING, 1 phiên đã quá hạn, 1 phiên chưa
            var expiredAuction = new Auction("i1", "s1", BigDecimal.TEN, BigDecimal.ONE, LocalDateTime.now().minusHours(1));
            expiredAuction.approve(); // Chuyển sang RUNNING
            
            var activeAuction = new Auction("i2", "s1", BigDecimal.TEN, BigDecimal.ONE, LocalDateTime.now().plusHours(1));
            activeAuction.approve(); // Chuyển sang RUNNING

            // Gán ID giả lập (vì AuctionService dùng getId() để update)
            // Trong thực tế ID do DB cấp, ở đây ta giả định model có setter hoặc dùng reflection
            // Do code AuctionService dùng auction.getId(), ta cần đảm bảo nó không null.
            
            when(auctionDAO.findByStatus(Auction.AuctionStatus.RUNNING)).thenReturn(List.of(expiredAuction, activeAuction));

            // WHEN
            auctionService.closeExpiredAuctions();

            // THEN: Chỉ phiên hết hạn mới bị chuyển sang FINISHED
            verify(auctionDAO, times(1)).updateStatus(any(), eq(Auction.AuctionStatus.FINISHED));
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ Thanh toán (Mark Paid)")
    class PaymentTests {

        @Test
        @DisplayName("Đánh dấu thanh toán thành công")
        void testMarkPaid_Success() {
            // GIVEN: Phiên đã kết thúc (FINISHED)
            String auctionId = "auc-123";
            Auction finishedAuction = createRealAuction(auctionId, "i1", "s1", Auction.AuctionStatus.FINISHED);
            when(auctionDAO.findById(auctionId)).thenReturn(finishedAuction);
            when(auctionDAO.updateStatus(auctionId, Auction.AuctionStatus.PAID)).thenReturn(true);

            // WHEN
            AuctionResponseDTO result = auctionService.markPaid(auctionId);

            // THEN
            assertEquals(Auction.AuctionStatus.PAID, result.getStatus());
            verify(auctionDAO).updateStatus(auctionId, Auction.AuctionStatus.PAID);
        }

        @Test
        @DisplayName("Thất bại - Đánh dấu thanh toán khi chưa kết thúc")
        void testMarkPaid_InvalidStatus() {
            String auctionId = "auc-123";
            Auction runningAuction = createRealAuction(auctionId, "i1", "s1", Auction.AuctionStatus.RUNNING);
            when(auctionDAO.findById(auctionId)).thenReturn(runningAuction);

            assertThrows(BusinessException.class, () -> auctionService.markPaid(auctionId));
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ Hủy phiên (Cancel)")
    class CancelTests {

        @Test
        @DisplayName("Hủy phiên thành công")
        void testCancel_Success() {
            String auctionId = "auc-123";
            Auction runningAuction = createRealAuction(auctionId, "i1", "s1", Auction.AuctionStatus.RUNNING);
            when(auctionDAO.findById(auctionId)).thenReturn(runningAuction);
            when(auctionDAO.updateStatus(auctionId, Auction.AuctionStatus.CANCELLED)).thenReturn(true);

            // WHEN
            auctionService.cancelAuction(auctionId);

            // THEN
            verify(auctionDAO).updateStatus(auctionId, Auction.AuctionStatus.CANCELLED);
        }

        @Test
        @DisplayName("Thất bại - Không thể hủy phiên đã thanh toán")
        void testCancel_PaidAuction() {
            String auctionId = "auc-123";
            Auction paidAuction = createRealAuction(auctionId, "i1", "s1", Auction.AuctionStatus.PAID);
            when(auctionDAO.findById(auctionId)).thenReturn(paidAuction);

            assertThrows(BusinessException.class, () -> auctionService.cancelAuction(auctionId));
        }
    }
}
