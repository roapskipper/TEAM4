package com.team4.service;

import com.team4.dao.AuctionDAO;
import com.team4.dto.auction.AuctionResponseDTO;
import com.team4.dto.auth.UserResponseDTO;
import com.team4.model.Admin;
import com.team4.model.Auction;
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
 * Kiểm thử nghiệp vụ AdminService.
 * Đảm bảo các quyền quản trị (Super Admin, Moderator) được thực thi đúng và dữ liệu trả về là DTO.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests for AdminService")
public class AdminServiceTest {

    @Mock private UserService userService;
    @Mock private AuctionService auctionService;
    @Mock private AuctionDAO auctionDAO;

    @InjectMocks
    private AdminService adminService;

    // Helper tạo Super Admin thật
    private Admin createSuperAdmin(String id) {
        return new Admin(id, LocalDateTime.now(), "admin", "hash", "Super Admin", "a@t.com", BigDecimal.ZERO, Admin.AccessLevel.SUPER_ADMIN, "Code@123");
    }

    // Helper tạo Bidder thật
    private Bidder createBidder(String id) {
        return new Bidder(id, LocalDateTime.now(), "bidder", "hash", "Bidder", "b@t.com", BigDecimal.ZERO, "Addr", "0912345678");
    }

    @Nested
    @DisplayName("Nghiệp vụ Phê duyệt/Từ chối (Approve/Reject)")
    class ApprovalTests {

        @Test
        @DisplayName("Admin duyệt phiên đấu giá thành công")
        void testApproveAuction_Success() {
            String adminId = "admin-1";
            String auctionId = "auc-1";
            when(userService.getRawUserById(adminId)).thenReturn(createSuperAdmin(adminId));

            // WHEN: Admin phê duyệt
            adminService.approveAuction(adminId, auctionId);

            // THEN: Phải gọi xuống AuctionService
            verify(auctionService).approveAuction(auctionId);
        }

        @Test
        @DisplayName("Thất bại khi người thực hiện không có quyền Admin")
        void testApproveAuction_NoPermission() {
            String userId = "user-1";
            when(userService.getRawUserById(userId)).thenReturn(createBidder(userId));

            // WHEN & THEN: Lỗi thiếu quyền (User does not have admin privileges)
            BusinessException ex = assertThrows(BusinessException.class, () -> adminService.approveAuction(userId, "auc-1"));
            assertEquals("User does not have admin privileges", ex.getMessage());
            verify(auctionService, never()).approveAuction(anyString());
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ Hủy phiên (SUPER_ADMIN only)")
    class CancellationTests {

        @Test
        @DisplayName("Super Admin hủy phiên đấu giá thành công")
        void testCancelBySuperAdmin_Success() {
            String adminId = "super-1";
            String auctionId = "auc-1";
            when(userService.getRawUserById(adminId)).thenReturn(createSuperAdmin(adminId));

            // WHEN: Super Admin yêu cầu hủy
            adminService.cancelAuctionByAdmin(adminId, auctionId);

            // THEN: Thành công
            verify(auctionService).cancelAuction(auctionId);
        }

        @Test
        @DisplayName("Moderator không có quyền hủy phiên")
        void testCancelByModerator_Fail() {
            String adminId = "mod-1";
            Admin moderator = new Admin(adminId, LocalDateTime.now(), "mod", "h", "M", "m@t.com", BigDecimal.ZERO, Admin.AccessLevel.MODERATOR, "C");
            when(userService.getRawUserById(adminId)).thenReturn(moderator);

            // WHEN & THEN: Lỗi thiếu quyền Super Admin
            BusinessException ex = assertThrows(BusinessException.class, () -> adminService.cancelAuctionByAdmin(adminId, "auc-1"));
            assertTrue(ex.getMessage().contains("Only Super Admin can cancel"));
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ Truy vấn dữ liệu hệ thống")
    class ViewSystemDataTests {

        @Test
        @DisplayName("Xem danh sách người dùng (Trả về List DTO)")
        void testViewSystemUsers() {
            List<UserResponseDTO> mockList = List.of(new UserResponseDTO("1", "u", "F", "e", User.Role.BIDDER, BigDecimal.ZERO, "2024-05-21T00:00:00"));
            when(userService.getAllUsers()).thenReturn(mockList);

            List<UserResponseDTO> results = adminService.viewSystemUsers();

            assertEquals(1, results.size());
            verify(userService).getAllUsers();
        }

        @Test
        @DisplayName("Xem danh sách tất cả các phiên đấu giá (Trả về List DTO)")
        void testViewAllAuctions() {
            // GIVEN
            Auction auction = new Auction("i1", "s1", BigDecimal.TEN, BigDecimal.ONE, LocalDateTime.now());
            when(auctionDAO.findAll()).thenReturn(List.of(auction));

            // WHEN
            List<AuctionResponseDTO> results = adminService.viewAllAuctions();

            // THEN
            assertEquals(1, results.size());
            verify(auctionDAO).findAll();
        }
    }
}
