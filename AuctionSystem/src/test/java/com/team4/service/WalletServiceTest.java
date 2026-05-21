package com.team4.service;

import com.team4.dao.UserDAO;
import com.team4.dto.auth.UserResponseDTO;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Kiểm thử nghiệp vụ Ví tiền (WalletService).
 * Đảm bảo các giao dịch nạp, rút, thanh toán và hoàn tiền hoạt động đúng với cấu trúc DTO mới.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests for WalletService")
public class WalletServiceTest {

    @Mock
    private UserDAO userDAO;

    @InjectMocks
    private WalletService walletService;

    // Helper tạo Bidder thật
    private Bidder createRealBidder(String id, String balance) {
        return new Bidder(id, LocalDateTime.now(), "user", "pass", "Test User", "test@example.com", new BigDecimal(balance), "Addr", "0912345678");
    }

    @Nested
    @DisplayName("Nghiệp vụ Nạp tiền (deposit)")
    class DepositTests {

        @Test
        @DisplayName("Nạp tiền thành công và trả về DTO số dư mới")
        void testDeposit_Success() {
            // GIVEN: Người dùng có 1000 đồng
            String userId = "user-1";
            BigDecimal amount = new BigDecimal("500.00");
            User user = createRealBidder(userId, "1000.00");

            when(userDAO.findById(userId)).thenReturn(user);
            when(userDAO.updateBalance(eq(userId), any())).thenReturn(true);

            // WHEN: Thực hiện nạp 500 đồng
            UserResponseDTO result = walletService.deposit(userId, amount);

            // THEN: Số dư mới là 1500
            assertEquals(new BigDecimal("1500.00"), result.getBalance());
            verify(userDAO).updateBalance(userId, new BigDecimal("1500.00"));
        }

        @Test
        @DisplayName("Thất bại khi nạp tiền cho người dùng không tồn tại")
        void testDeposit_UserNotFound() {
            when(userDAO.findById("none")).thenReturn(null);

            assertThrows(BusinessException.class, () -> walletService.deposit("none", BigDecimal.TEN));
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ Rút tiền (withdraw)")
    class WithdrawTests {

        @Test
        @DisplayName("Rút tiền thành công")
        void testWithdraw_Success() {
            // GIVEN: Có 1000 đồng, rút 200 đồng
            String userId = "user-1";
            User user = createRealBidder(userId, "1000.00");

            when(userDAO.findById(userId)).thenReturn(user);
            when(userDAO.updateBalance(eq(userId), any())).thenReturn(true);

            // WHEN
            UserResponseDTO result = walletService.withdraw(userId, new BigDecimal("200.00"));

            // THEN: Còn 800 đồng
            assertEquals(new BigDecimal("800.00"), result.getBalance());
            verify(userDAO).updateBalance(userId, new BigDecimal("800.00"));
        }

        @Test
        @DisplayName("Thất bại khi rút quá số dư khả dụng")
        void testWithdraw_InsufficientFunds() {
            String userId = "user-1";
            User user = createRealBidder(userId, "100.00");
            when(userDAO.findById(userId)).thenReturn(user);

            // WHEN & THEN: Lỗi không đủ tiền
            assertThrows(BusinessException.class, () -> walletService.withdraw(userId, new BigDecimal("500.00")));
            verify(userDAO, never()).updateBalance(anyString(), any());
        }
    }

    @Nested
    @DisplayName("Các tiện ích số dư")
    class InquiryTests {

        @Test
        @DisplayName("Lấy số dư hiện tại thành công")
        void testGetBalance_Success() {
            String userId = "user-1";
            User user = createRealBidder(userId, "750.00");
            when(userDAO.findById(userId)).thenReturn(user);

            BigDecimal balance = walletService.getBalance(userId);

            assertEquals(new BigDecimal("750.00"), balance);
        }
    }
}
