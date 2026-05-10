package com.team4.service;

import com.team4.dao.AuctionDAO;
import com.team4.model.Admin;
import com.team4.model.User;
import com.team4.util.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdminServiceTest {

    @Mock
    private UserService userService;
    @Mock
    private AuctionService auctionService;
    @Mock
    private AuctionDAO auctionDAO;

    private AdminService adminService;

    @BeforeEach
    void setUp() {
        adminService = new AdminService(userService, auctionService, auctionDAO);
    }

    @Test
    void testApproveAuction_Success() {
        String adminId = "admin1";
        String auctionId = "auc1";
        Admin mockAdmin = mock(Admin.class);
        when(mockAdmin.getRole()).thenReturn(User.Role.ADMIN);
        when(userService.getUserById(adminId)).thenReturn(mockAdmin);

        adminService.approveAuction(adminId, auctionId);

        verify(auctionService).approveAuction(auctionId);
    }

    @Test
    void testCancelAuctionByAdmin_SuperAdminOnly() {
        String adminId = "admin1";
        String auctionId = "auc1";
        Admin mockAdmin = mock(Admin.class);
        when(mockAdmin.getRole()).thenReturn(User.Role.ADMIN);
        when(mockAdmin.getAccessLevel()).thenReturn(Admin.AccessLevel.REGULAR_ADMIN);
        when(userService.getUserById(adminId)).thenReturn(mockAdmin);

        BusinessException ex = assertThrows(BusinessException.class, () -> 
            adminService.cancelAuctionByAdmin(auctionId, adminId)
        );
        assertEquals("Chỉ Super Admin mới có quyền hủy phiên đấu giá", ex.getMessage());
    }
}
