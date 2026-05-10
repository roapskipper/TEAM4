package com.team4.service;

import com.team4.dao.UserDAO;
import com.team4.model.Bidder;
import com.team4.model.User;
import com.team4.util.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

public class WalletServiceTest {

    private UserDAO mockUserDAO;
    private WalletService walletService;

    @BeforeEach
    public void setUp() {
        // Tạo đối tượng giả (mock) cho UserDAO
        mockUserDAO = Mockito.mock(UserDAO.class);
        // Bơm mock DAO vào Service
        walletService = new WalletService(mockUserDAO);
    }

    @Test
    public void testDeposit_Success() {
        String userId = "u1";
        // Tạo User giả với số dư ban đầu là 100
        User mockUser = new Bidder("u1", java.time.LocalDateTime.now(), "u1_user", "hash", "Test User", "test@gmail.com", new BigDecimal("100.00"), "Addr", "0123456789");

        // Định nghĩa hành vi cho Mock DAO: Khi gọi findById("u1") thì trả về mockUser
        Mockito.when(mockUserDAO.findById(userId)).thenReturn(mockUser);
        // Khi gọi updateBalance thì luôn trả về true
        Mockito.when(mockUserDAO.updateBalance(eq(userId), any(BigDecimal.class))).thenReturn(true);

        // Thực thi hàm nạp 50
        User updatedUser = walletService.deposit(userId, new BigDecimal("50.00"));

        // Kiểm tra xem số dư có tăng lên 150 không
        assertEquals(new BigDecimal("150.00"), updatedUser.getBalance());
        
        // Đảm bảo hàm updateBalance trong DB đã được gọi chính xác 1 lần
        Mockito.verify(mockUserDAO, Mockito.times(1)).updateBalance(userId, new BigDecimal("150.00"));
    }

    @Test
    public void testWithdraw_InsufficientBalance_ThrowsException() {
        String userId = "u2";
        User mockUser = new Bidder("u2", java.time.LocalDateTime.now(), "u2_user", "hash", "Test 2", "test2@gmail.com", new BigDecimal("50.00"), "Addr", "0123456789"); // Chỉ có 50

        Mockito.when(mockUserDAO.findById(userId)).thenReturn(mockUser);

        // Thực thi hàm rút 100 và kỳ vọng sẽ ném ra BusinessException
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            walletService.withdraw(userId, new BigDecimal("100.00"));
        });

        // Hàm báo lỗi bên trong User.withdraw() hoặc WalletService
        assertTrue(exception.getMessage().contains("Số dư không đủ") || exception.getMessage().contains("thất bại"));
        
        // Đảm bảo updateBalance không bao giờ được gọi xuống DB vì đã lỗi từ trước
        Mockito.verify(mockUserDAO, Mockito.never()).updateBalance(any(), any());
    }
}
