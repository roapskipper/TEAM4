package com.team4.service;

import com.team4.dao.UserDAO;
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
 * Lớp kiểm thử WalletServiceTest.
 * Môi trường: JDK 21, JUnit 5, Mockito.
 * TUÂN THỦ QUY TẮC:
 * 1. KHÔNG MOCK các class dữ liệu (User, Bidder) -> Sử dụng 'new' để tạo đối tượng thật.
 * 2. CHỈ MOCK các Interface/Class phụ thuộc logic (UserDAO).
 * 3. Sử dụng @ExtendWith(MockitoExtension.class).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Kiểm thử nghiệp vụ Ví tiền (WalletService)")
public class WalletServiceTest {

    @Mock
    private UserDAO userDAO; // Chỉ mock thành phần truy xuất dữ liệu

    @InjectMocks
    private WalletService walletService; // Inject mock vào service cần test

    /**
     * Helper method để tạo đối tượng Bidder thật (Entity).
     * Tuân thủ quy tắc không mock Entity.
     */
    private Bidder createRealBidder(String id, String balance) {
        return new Bidder(
                id,
                LocalDateTime.now(),
                "testuser",
                "hashed_password",
                "Nguyễn Văn A",
                "test@example.com",
                new BigDecimal(balance),
                "123 Đường ABC, Hà Nội",
                "0987654321"
        );
    }

    @Nested
    @DisplayName("Nghiệp vụ Nạp tiền (Deposit)")
    class DepositTests {

        @Test
        @DisplayName("Nạp tiền thành công - Số dư phải tăng và lưu vào DB")
        void testDeposit_Success() {
            // GIVEN: Giả lập người dùng có 1000.00 trong ví
            String userId = "user-001";
            BigDecimal depositAmount = new BigDecimal("500.00");
            User realUser = createRealBidder(userId, "1000.00");

            when(userDAO.findById(userId)).thenReturn(realUser);
            when(userDAO.updateBalance(eq(userId), any(BigDecimal.class))).thenReturn(true);

            // WHEN: Thực hiện nạp thêm 500.00
            User result = walletService.deposit(userId, depositAmount);

            // THEN: Kiểm tra logic tính toán (1000 + 500 = 1500)
            assertEquals(new BigDecimal("1500.00"), result.getBalance(), "Số dư sau nạp phải là 1500.00");
            // Xác nhận Service có gọi xuống DAO để lưu dữ liệu
            verify(userDAO).updateBalance(userId, new BigDecimal("1500.00"));
        }

        @Test
        @DisplayName("Nạp tiền thất bại - Người dùng không tồn tại")
        void testDeposit_UserNotFound() {
            String userId = "invalid-id";
            when(userDAO.findById(userId)).thenReturn(null);

            // Kiểm tra ném ra đúng BusinessException
            BusinessException ex = assertThrows(BusinessException.class, () -> 
                walletService.deposit(userId, new BigDecimal("100.00"))
            );
            assertEquals("User does not exist", ex.getMessage());
        }

        @Test
        @DisplayName("Nạp tiền thất bại - Lỗi kết nối Database")
        void testDeposit_DatabaseError() {
            String userId = "user-001";
            User realUser = createRealBidder(userId, "100.00");

            when(userDAO.findById(userId)).thenReturn(realUser);
            // Giả lập DAO trả về false (lỗi cập nhật)
            when(userDAO.updateBalance(anyString(), any())).thenReturn(false);

            assertThrows(BusinessException.class, () -> 
                walletService.deposit(userId, new BigDecimal("50.00")),
                "Phải ném lỗi khi DB không cập nhật được"
            );
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ Rút tiền (Withdraw)")
    class WithdrawTests {

        @Test
        @DisplayName("Rút tiền thành công - Số dư phải trừ chính xác")
        void testWithdraw_Success() {
            // GIVEN: Có 1000.00, rút 200.00
            String userId = "user-001";
            BigDecimal withdrawAmount = new BigDecimal("200.00");
            User realUser = createRealBidder(userId, "1000.00");

            when(userDAO.findById(userId)).thenReturn(realUser);
            when(userDAO.updateBalance(eq(userId), any(BigDecimal.class))).thenReturn(true);

            // WHEN
            User result = walletService.withdraw(userId, withdrawAmount);

            // THEN: 1000 - 200 = 800
            assertEquals(new BigDecimal("800.00"), result.getBalance());
            verify(userDAO).updateBalance(userId, new BigDecimal("800.00"));
        }

        @Test
        @DisplayName("Rút tiền thất bại - Số dư không đủ")
        void testWithdraw_InsufficientBalance() {
            // GIVEN: Có 100.00, rút 500.00
            String userId = "user-001";
            User realUser = createRealBidder(userId, "100.00");
            when(userDAO.findById(userId)).thenReturn(realUser);

            // WHEN & THEN: Tùy vào logic WalletService xử lý return false hay ném Exception
            // Lưu ý: User.withdraw trả về false, Service cần kiểm tra điều này.
            assertThrows(Exception.class, () -> 
                walletService.withdraw(userId, new BigDecimal("500.00"))
            );
            
            // Quan trọng: Nếu không đủ tiền, tuyệt đối không được gọi updateBalance vào DB
            verify(userDAO, never()).updateBalance(anyString(), any());
        }
    }

    @Nested
    @DisplayName("Các tiện ích kiểm tra (Balance Inquiry)")
    class InquiryTests {

        @Test
        @DisplayName("Kiểm tra đủ số dư - Logic True/False")
        void testHasSufficientBalance() {
            String userId = "user-001";
            User realUser = createRealBidder(userId, "1000.00");
            when(userDAO.findById(userId)).thenReturn(realUser);

            // Kiểm tra các biên thanh toán
            assertTrue(walletService.hasSufficientBalance(userId, new BigDecimal("500.00")));
            assertTrue(walletService.hasSufficientBalance(userId, new BigDecimal("1000.00")));
            assertFalse(walletService.hasSufficientBalance(userId, new BigDecimal("1000.01")));
        }

        @Test
        @DisplayName("Lấy số dư hiện tại thành công")
        void testGetBalance_Success() {
            String userId = "user-001";
            User realUser = createRealBidder(userId, "750.25");
            when(userDAO.findById(userId)).thenReturn(realUser);

            BigDecimal currentBalance = walletService.balance(userId);
            
            assertEquals(new BigDecimal("750.25"), currentBalance);
        }
    }
}
