package com.team4.service;

import com.team4.dao.AuctionDAO;
import com.team4.dao.ItemDAO;
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

import com.team4.db.DatabaseManager;
import org.mockito.MockedStatic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import java.sql.Connection;
import java.sql.SQLException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Kiểm thử nghiệp vụ AuctionService.
 * Đảm bảo quản lý vòng đời phiên đấu giá chính xác với cấu trúc DTO mới.
 */
import com.team4.dao.UserDAO;
import com.team4.model.User;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests for AuctionService")
public class AuctionServiceTest {

    @Mock
    private AuctionDAO auctionDAO;

    @Mock
    private ItemDAO itemDAO;

    @Mock
    private UserDAO userDAO;

    @Mock
    private Connection mockConn;

    @InjectMocks
    private AuctionService auctionService;

    // Helper tạo Item thật
    private Art createRealItem(String itemId, String ownerId) {
        return new Art(itemId, LocalDateTime.now(), "Antique Painting", new BigDecimal("500.00"),
                "Description", ownerId, "Artist X", 1900, Art.Medium.OIL_PAINT, "100x100");
    }

    // Helper tạo Auction thật và điều chỉnh trạng thái
    private Auction createRealAuction(String itemId, String sellerId, Auction.AuctionStatus status) {
        var auction = new Auction(itemId, sellerId, new BigDecimal("1000.00"), new BigDecimal("100.00"), LocalDateTime.now().plusDays(1));
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
    @DisplayName("Nghiệp vụ Tạo phiên đấu giá (createAuction)")
    class CreateAuctionTests {

        @Test
        @DisplayName("Tạo phiên đấu giá thành công")
        void testCreateAuction_Success() {
            // GIVEN: Item tồn tại và seller là chủ sở hữu
            String itemId = "item-1";
            String sellerId = "seller-1";
            Item item = createRealItem(itemId, sellerId);
            CreateAuctionRequestDTO request = new CreateAuctionRequestDTO(itemId, sellerId, new BigDecimal("1000"), new BigDecimal("100"), LocalDateTime.now().plusDays(1));

            when(itemDAO.findById(itemId)).thenReturn(item);
            when(auctionDAO.insert(any(Auction.class))).thenReturn(true);

            // WHEN: Thực hiện tạo phiên
            AuctionResponseDTO result = auctionService.createAuction(request);

            // THEN: Kết quả phải là DTO và trạng thái mặc định là PENDING
            assertNotNull(result);
            assertEquals(itemId, result.getItemId());
            assertEquals(Auction.AuctionStatus.PENDING, result.getStatus());
            verify(auctionDAO).insert(any(Auction.class));
        }

        @Test
        @DisplayName("Thất bại khi Item không tồn tại")
        void testCreateAuction_ItemNotFound() {
            CreateAuctionRequestDTO request = new CreateAuctionRequestDTO("none", "s1", BigDecimal.TEN, BigDecimal.ONE, LocalDateTime.now().plusDays(1));
            when(itemDAO.findById("none")).thenReturn(null);

            assertThrows(BusinessException.class, () -> auctionService.createAuction(request));
        }

        @Test
        @DisplayName("Thất bại khi người tạo không phải chủ sở hữu Item")
        void testCreateAuction_NotOwner() {
            String itemId = "item-1";
            Item item = createRealItem(itemId, "real-owner");
            CreateAuctionRequestDTO request = new CreateAuctionRequestDTO(itemId, "hacker", BigDecimal.TEN, BigDecimal.ONE, LocalDateTime.now().plusDays(1));

            when(itemDAO.findById(itemId)).thenReturn(item);

            BusinessException ex = assertThrows(BusinessException.class, () -> auctionService.createAuction(request));
            assertEquals("Seller does not own this item", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ Phê duyệt (approveAuction)")
    class ApprovalTests {

        @Test
        @DisplayName("Admin phê duyệt phiên đấu giá thành công")
        void testApprove_Success() {
            // GIVEN: Phiên đấu giá đang ở trạng thái PENDING
            String auctionId = "auc-1";
            Auction auction = createRealAuction("i1", "s1", Auction.AuctionStatus.PENDING);
            when(auctionDAO.findById(auctionId)).thenReturn(auction);
            when(auctionDAO.updateStatus(eq(auctionId), any())).thenReturn(true);

            // WHEN: Phê duyệt
            AuctionResponseDTO result = auctionService.approveAuction(auctionId);

            // THEN: Trạng thái phải chuyển sang RUNNING
            assertEquals(Auction.AuctionStatus.RUNNING, result.getStatus());
            verify(auctionDAO).updateStatus(auctionId, Auction.AuctionStatus.RUNNING);
        }

        @Test
        @DisplayName("Thất bại khi phê duyệt phiên không phải PENDING")
        void testApprove_InvalidStatus() {
            String auctionId = "auc-1";
            Auction auction = createRealAuction("i1", "s1", Auction.AuctionStatus.CANCELLED);
            when(auctionDAO.findById(auctionId)).thenReturn(auction);

            BusinessException ex = assertThrows(BusinessException.class, () -> auctionService.approveAuction(auctionId));
            assertEquals("Only pending auctions can be approved", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ Hủy phiên (cancelAuction)")
    class CancelTests {

        @Test
        @DisplayName("Hủy phiên đấu giá thành công")
        void testCancel_Success() {
            String auctionId = "auc-1";
            Auction auction = createRealAuction("i1", "s1", Auction.AuctionStatus.RUNNING);
            when(auctionDAO.findById(auctionId)).thenReturn(auction);
            when(auctionDAO.updateStatus(eq(auctionId), any())).thenReturn(true);

            // WHEN: Thực hiện hủy
            AuctionResponseDTO result = auctionService.cancelAuction(auctionId);

            // THEN: Trạng thái là CANCELLED
            assertEquals(Auction.AuctionStatus.CANCELLED, result.getStatus());
            verify(auctionDAO).updateStatus(auctionId, Auction.AuctionStatus.CANCELLED);
        }

        @Test
        @DisplayName("Thất bại khi hủy phiên đã thanh toán")
        void testCancel_PaidAuction() {
            String auctionId = "auc-1";
            Auction auction = createRealAuction("i1", "s1", Auction.AuctionStatus.PAID);
            when(auctionDAO.findById(auctionId)).thenReturn(auction);

            assertThrows(BusinessException.class, () -> auctionService.cancelAuction(auctionId));
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ Đóng phiên tự động (closeExpiredAuctions)")
    class AutoCloseTests {

        @Test
        @DisplayName("Tự động đóng các phiên đã hết thời gian")
        void testCloseExpiredAuctions() {
            // GIVEN: Một phiên đã hết hạn và một phiên còn hạn
            Auction expired = new Auction("i1", "s1", BigDecimal.TEN, BigDecimal.ONE, LocalDateTime.now().minusMinutes(1));
            expired.approve(); // RUNNING

            Auction active = new Auction("i2", "s1", BigDecimal.TEN, BigDecimal.ONE, LocalDateTime.now().plusHours(1));
            active.approve(); // RUNNING

            when(auctionDAO.findByStatus(Auction.AuctionStatus.RUNNING)).thenReturn(List.of(expired, active));
            when(auctionDAO.updateStatus(any(), eq(Auction.AuctionStatus.FINISHED))).thenReturn(true);

            // WHEN: Chạy logic quét phiên hết hạn
            auctionService.closeExpiredAuctions();

            // THEN: Chỉ gọi update cho phiên đã hết hạn
            verify(auctionDAO, times(1)).updateStatus(any(), eq(Auction.AuctionStatus.FINISHED));
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ Truy vấn (Queries)")
    class QueryTests {

        @Test
        @DisplayName("Lấy danh sách đấu giá theo trạng thái (DTO)")
        void testGetAuctionsByStatus() {
            // GIVEN
            Auction auction = createRealAuction("i1", "s1", Auction.AuctionStatus.RUNNING);
            when(auctionDAO.findByStatus(Auction.AuctionStatus.RUNNING)).thenReturn(List.of(auction));

            // WHEN
            List<AuctionResponseDTO> results = auctionService.getAuctionsByStatus(Auction.AuctionStatus.RUNNING);

            // THEN
            assertEquals(1, results.size());
            verify(auctionDAO).findByStatus(Auction.AuctionStatus.RUNNING);
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ Thanh toán (markPaid)")
    class MarkPaidTests {
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

        @Test
        @DisplayName("Thanh toán thành công và có người thắng cuộc")
        void testMarkPaid_Success_WithWinner() {
            String auctionId = "auc-1";
            String itemId = "item-1";
            String winnerId = "winner-123";
            String sellerId = "seller-1";
            Auction auction = new Auction(
                auctionId, LocalDateTime.now(), itemId, sellerId,
                winnerId, new BigDecimal("1000.00"), new BigDecimal("1500.00"),
                new BigDecimal("100.00"), LocalDateTime.now().minusDays(1), LocalDateTime.now().minusMinutes(5),
                Auction.AuctionStatus.FINISHED
            );

            var bidder = new com.team4.model.Bidder(winnerId, LocalDateTime.now(), "bidder", "pass", "Bidder Name", "bidder@test.com", new BigDecimal("5000.00"), "Hanoi", "0912123456");
            var seller = new com.team4.model.Seller(sellerId, LocalDateTime.now(), "seller", "pass", "Seller Name", "seller@test.com", new BigDecimal("100.00"), "My Store", 5.0);

            when(auctionDAO.findById(eq(mockConn), eq(auctionId))).thenReturn(auction);
            when(auctionDAO.updateStatus(eq(mockConn), eq(auctionId), eq(Auction.AuctionStatus.PAID))).thenReturn(true);
            when(userDAO.findById(eq(mockConn), eq(winnerId))).thenReturn(bidder);
            when(userDAO.updateBalance(eq(mockConn), eq(winnerId), any(BigDecimal.class))).thenReturn(true);
            when(userDAO.findById(eq(mockConn), eq(sellerId))).thenReturn(seller);
            when(userDAO.updateBalance(eq(mockConn), eq(sellerId), any(BigDecimal.class))).thenReturn(true);
            when(itemDAO.updateOwner(eq(mockConn), eq(itemId), eq(winnerId))).thenReturn(true);

            // WHEN
            AuctionResponseDTO result = auctionService.markPaid(auctionId);

            // THEN
            assertNotNull(result);
            assertEquals(Auction.AuctionStatus.PAID, result.getStatus());
            assertEquals(0, new BigDecimal("3500.00").compareTo(bidder.getBalance())); // 5000 - 1500 = 3500
            assertEquals(0, new BigDecimal("1600.00").compareTo(seller.getBalance())); // 100 + 1500 = 1600
            verify(auctionDAO).updateStatus(mockConn, auctionId, Auction.AuctionStatus.PAID);
            verify(itemDAO).updateOwner(mockConn, itemId, winnerId);
            mockedDatabaseManager.verify(() -> DatabaseManager.commitTransaction(mockConn));
        }

        @Test
        @DisplayName("Thanh toán thành công nhưng không có người thắng cuộc")
        void testMarkPaid_Success_NoWinner() {
            String auctionId = "auc-1";
            String itemId = "item-1";
            Auction auction = new Auction(
                auctionId, LocalDateTime.now(), itemId, "seller-1",
                null, new BigDecimal("1000.00"), new BigDecimal("1000.00"),
                new BigDecimal("100.00"), LocalDateTime.now().minusDays(1), LocalDateTime.now().minusMinutes(5),
                Auction.AuctionStatus.FINISHED
            );

            when(auctionDAO.findById(eq(mockConn), eq(auctionId))).thenReturn(auction);
            when(auctionDAO.updateStatus(eq(mockConn), eq(auctionId), eq(Auction.AuctionStatus.PAID))).thenReturn(true);

            // WHEN
            AuctionResponseDTO result = auctionService.markPaid(auctionId);

            // THEN
            assertNotNull(result);
            assertEquals(Auction.AuctionStatus.PAID, result.getStatus());
            verify(auctionDAO).updateStatus(mockConn, auctionId, Auction.AuctionStatus.PAID);
            verify(itemDAO, never()).updateOwner(any(), any(), any());
            mockedDatabaseManager.verify(() -> DatabaseManager.commitTransaction(mockConn));
        }

        @Test
        @DisplayName("Thanh toán thất bại khi cập nhật trạng thái lỗi - Rollback")
        void testMarkPaid_Fail_UpdateStatusError() {
            String auctionId = "auc-1";
            String itemId = "item-1";
            Auction auction = new Auction(
                auctionId, LocalDateTime.now(), itemId, "seller-1",
                null, new BigDecimal("1000.00"), new BigDecimal("1000.00"),
                new BigDecimal("100.00"), LocalDateTime.now().minusDays(1), LocalDateTime.now().minusMinutes(5),
                Auction.AuctionStatus.FINISHED
            );

            when(auctionDAO.findById(eq(mockConn), eq(auctionId))).thenReturn(auction);
            when(auctionDAO.updateStatus(eq(mockConn), eq(auctionId), eq(Auction.AuctionStatus.PAID))).thenReturn(false);

            // WHEN & THEN
            assertThrows(BusinessException.class, () -> auctionService.markPaid(auctionId));
            mockedDatabaseManager.verify(() -> DatabaseManager.rollbackTransaction(mockConn));
            mockedDatabaseManager.verify(() -> DatabaseManager.commitTransaction(mockConn), never());
        }

        @Test
        @DisplayName("Thanh toán thất bại khi cập nhật chủ sở hữu lỗi - Rollback")
        void testMarkPaid_Fail_UpdateOwnerError() {
            String auctionId = "auc-1";
            String itemId = "item-1";
            String winnerId = "winner-123";
            String sellerId = "seller-1";
            Auction auction = new Auction(
                auctionId, LocalDateTime.now(), itemId, sellerId,
                winnerId, new BigDecimal("1000.00"), new BigDecimal("1500.00"),
                new BigDecimal("100.00"), LocalDateTime.now().minusDays(1), LocalDateTime.now().minusMinutes(5),
                Auction.AuctionStatus.FINISHED
            );

            var bidder = new com.team4.model.Bidder(winnerId, LocalDateTime.now(), "bidder", "pass", "Bidder Name", "bidder@test.com", new BigDecimal("5000.00"), "Hanoi", "0912123456");
            var seller = new com.team4.model.Seller(sellerId, LocalDateTime.now(), "seller", "pass", "Seller Name", "seller@test.com", new BigDecimal("100.00"), "My Store", 5.0);

            when(auctionDAO.findById(eq(mockConn), eq(auctionId))).thenReturn(auction);
            when(auctionDAO.updateStatus(eq(mockConn), eq(auctionId), eq(Auction.AuctionStatus.PAID))).thenReturn(true);
            when(userDAO.findById(eq(mockConn), eq(winnerId))).thenReturn(bidder);
            when(userDAO.updateBalance(eq(mockConn), eq(winnerId), any(BigDecimal.class))).thenReturn(true);
            when(userDAO.findById(eq(mockConn), eq(sellerId))).thenReturn(seller);
            when(userDAO.updateBalance(eq(mockConn), eq(sellerId), any(BigDecimal.class))).thenReturn(true);
            when(itemDAO.updateOwner(eq(mockConn), eq(itemId), eq(winnerId))).thenReturn(false);

            // WHEN & THEN
            assertThrows(BusinessException.class, () -> auctionService.markPaid(auctionId));
            mockedDatabaseManager.verify(() -> DatabaseManager.rollbackTransaction(mockConn));
            mockedDatabaseManager.verify(() -> DatabaseManager.commitTransaction(mockConn), never());
        }
    }
}
