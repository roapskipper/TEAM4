package com.team4.service;

import com.team4.dao.AuctionDAO;
import com.team4.dto.auction.AuctionResponseDTO;
import com.team4.dto.auth.UserResponseDTO;
import com.team4.mapper.UserMapper;
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
 * Unit tests cho AdminService.
 *
 * Quy tắc:
 * 1. KHÔNG mock model (Admin, Bidder, Auction) → dùng 'new'.
 * 2. CHỈ mock các interface/dependency (UserService, AuctionService, AuctionDAO).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Kiểm thử nghiệp vụ Quản trị (AdminService)")
public class AdminServiceTest {

    @Mock private UserService userService;
    @Mock private AuctionService auctionService;
    @Mock private AuctionDAO auctionDAO;

    @InjectMocks
    private AdminService adminService;

    // -------------------------------------------------------------------------
    // Helpers tạo entity thật
    // -------------------------------------------------------------------------
    private Admin realSuperAdmin(String id) {
        return new Admin(
                id, LocalDateTime.now(),
                "superadmin",
                "$2a$12$dummyhashvaluefortest123456789xx",
                "Super Admin",
                "admin@test.com",
                BigDecimal.ZERO,
                Admin.AccessLevel.SUPER_ADMIN,
                "Admin@12345"   // đúng pattern regex adminCode
        );
    }

    private Admin realModerator(String id) {
        return new Admin(
                id, LocalDateTime.now(),
                "moderator1",
                "$2a$12$dummyhashvaluefortest123456789xx",
                "Moderator",
                "mod@test.com",
                BigDecimal.ZERO,
                Admin.AccessLevel.MODERATOR,
                "Admin@12345"
        );
    }

    private Bidder realBidder(String id) {
        return new Bidder(
                id, LocalDateTime.now(),
                "bidder001",
                "$2a$12$dummyhashvaluefortest123456789xx",
                "Nguyen Van A",
                "bidder@test.com",
                BigDecimal.ZERO,
                "123 ABC St",
                "0912345678"
        );
    }

    private Auction realAuction(Auction.AuctionStatus status) {
        Auction a = new Auction("item-1", "seller-1",
                new BigDecimal("1000.00"), new BigDecimal("100.00"),
                LocalDateTime.now().plusDays(1));
        if (status == Auction.AuctionStatus.RUNNING) a.approve();
        if (status == Auction.AuctionStatus.FINISHED) { a.approve(); a.close(); }
        if (status == Auction.AuctionStatus.CANCELLED) a.cancel();
        return a;
    }

    // =========================================================================
    // approveAuction
    // =========================================================================
    @Nested
    @DisplayName("Duyệt phiên đấu giá (approveAuction)")
    class ApproveAuctionTests {

        @Test
        @DisplayName("Thành công – Admin hợp lệ duyệt phiên")
        void approveAuction_success() {
            String adminId = "admin-1";
            String auctionId = "auction-1";
            when(userService.getRawUserById(adminId)).thenReturn(realSuperAdmin(adminId));

            adminService.approveAuction(adminId, auctionId);

            verify(auctionService).approveAuction(auctionId);
        }

        @Test
        @DisplayName("Thất bại – Người dùng không phải Admin")
        void approveAuction_notAdmin_throwsException() {
            String userId = "user-1";
            when(userService.getRawUserById(userId)).thenReturn(realBidder(userId));

            assertThrows(BusinessException.class,
                    () -> adminService.approveAuction(userId, "auction-1"));
            verifyNoInteractions(auctionService);
        }

        @Test
        @DisplayName("Thất bại – Người dùng không tồn tại")
        void approveAuction_userNotFound_throwsException() {
            when(userService.getRawUserById("ghost")).thenThrow(new BusinessException("User does not exist"));

            assertThrows(BusinessException.class,
                    () -> adminService.approveAuction("ghost", "auction-1"));
            verifyNoInteractions(auctionService);
        }

        @Test
        @DisplayName("Thành công – Moderator cũng có quyền duyệt")
        void approveAuction_moderator_success() {
            String adminId = "mod-1";
            when(userService.getRawUserById(adminId)).thenReturn(realModerator(adminId));

            adminService.approveAuction(adminId, "auction-1");

            verify(auctionService).approveAuction("auction-1");
        }
    }

    // =========================================================================
    // rejectAuction
    // =========================================================================
    @Nested
    @DisplayName("Từ chối phiên đấu giá (rejectAuction)")
    class RejectAuctionTests {

        @Test
        @DisplayName("Thành công – Admin từ chối phiên")
        void rejectAuction_success() {
            String adminId = "admin-1";
            when(userService.getRawUserById(adminId)).thenReturn(realSuperAdmin(adminId));

            adminService.rejectAuction(adminId, "auction-1");

            verify(auctionService).rejectAuction("auction-1");
        }

        @Test
        @DisplayName("Thất bại – Không phải Admin")
        void rejectAuction_notAdmin_throwsException() {
            String userId = "user-1";
            when(userService.getRawUserById(userId)).thenReturn(realBidder(userId));

            assertThrows(BusinessException.class,
                    () -> adminService.rejectAuction(userId, "auction-1"));
            verifyNoInteractions(auctionService);
        }
    }

    // =========================================================================
    // cancelAuctionByAdmin
    // =========================================================================
    @Nested
    @DisplayName("Hủy phiên đấu giá (cancelAuctionByAdmin)")
    class CancelAuctionTests {

        @Test
        @DisplayName("Thành công – SUPER_ADMIN hủy phiên")
        void cancelAuction_superAdmin_success() {
            String adminId = "admin-1";
            when(userService.getRawUserById(adminId)).thenReturn(realSuperAdmin(adminId));

            adminService.cancelAuctionByAdmin(adminId, "auction-1");

            verify(auctionService).cancelAuction("auction-1");
        }

        @Test
        @DisplayName("Thất bại – MODERATOR không có quyền hủy")
        void cancelAuction_moderator_throwsBusinessException() {
            String adminId = "mod-1";
            when(userService.getRawUserById(adminId)).thenReturn(realModerator(adminId));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> adminService.cancelAuctionByAdmin(adminId, "auction-1"));
            assertTrue(ex.getMessage().contains("Super Admin"));
            verifyNoInteractions(auctionService);
        }

        @Test
        @DisplayName("Thất bại – Không phải Admin")
        void cancelAuction_notAdmin_throwsException() {
            String userId = "user-1";
            when(userService.getRawUserById(userId)).thenReturn(realBidder(userId));

            assertThrows(BusinessException.class,
                    () -> adminService.cancelAuctionByAdmin(userId, "auction-1"));
            verifyNoInteractions(auctionService);
        }

        @Test
        @DisplayName("Thất bại – User không tồn tại")
        void cancelAuction_userNotFound_throwsException() {
            when(userService.getRawUserById("ghost")).thenThrow(new BusinessException("User does not exist"));

            assertThrows(BusinessException.class,
                    () -> adminService.cancelAuctionByAdmin("ghost", "auction-1"));
        }
    }

    // =========================================================================
    // viewSystemUsers / viewAllAuctions
    // =========================================================================
    @Nested
    @DisplayName("Xem dữ liệu hệ thống")
    class ViewTests {

        @Test
        @DisplayName("viewSystemUsers – delegate sang UserService.getAllUsers()")
        void viewSystemUsers_delegatesToUserService() {
            List<UserResponseDTO> expected = List.of(UserMapper.toUserResponseDTO(realBidder("u1")), UserMapper.toUserResponseDTO(realBidder("u2")));
            when(userService.getAllUsers()).thenReturn(expected);

            List<UserResponseDTO> result = adminService.viewSystemUsers();

            assertSame(expected, result);
            verify(userService).getAllUsers();
        }

        @Test
        @DisplayName("viewAllAuctions – delegate sang AuctionDAO.findAll()")
        void viewAllAuctions_delegatesToAuctionDAO() {
            List<Auction> expected = List.of(realAuction(Auction.AuctionStatus.RUNNING));
            when(auctionDAO.findAll()).thenReturn(expected);

            List<AuctionResponseDTO> result = adminService.viewAllAuctions();

            assertEquals(expected.size(), result.size());
            verify(auctionDAO).findAll();
        }
    }
}
