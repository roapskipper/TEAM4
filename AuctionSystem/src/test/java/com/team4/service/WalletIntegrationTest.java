package com.team4.service;

import com.team4.dao.UserDAO;
import com.team4.dao.impl.UserDAOImpl;
import com.team4.dto.auth.UserResponseDTO;
import com.team4.model.Bidder;
import com.team4.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử tích hợp nghiệp vụ Ví tiền (WalletService).
 * Kiểm tra tính chính xác của việc tính toán và cập nhật số dư vào Database.
 */
@DisplayName("Integration Tests for WalletService")
public class WalletIntegrationTest extends BaseServiceIntegrationTest {

    private final UserDAO userDAO = new UserDAOImpl();
    private final WalletService walletService = new WalletService(userDAO);

    @Test
    @DisplayName("Nạp tiền và Rút tiền: Kiểm tra luồng thay đổi số dư liên tục trong DB")
    void testWalletFlow_Integration() {
        // 1. CHUẨN BỊ (GIVEN)
        String userId = "wallet_user";
        Bidder user = new Bidder(userId, "pass", "Wallet User", "wallet@test.com", "Addr", "0123");
        userDAO.insert(user); // Số dư ban đầu là 0.00 (theo logic Model/DB)

        // 2. THỰC THI & KIỂM CHỨNG (WHEN & THEN)
        
        // BƯỚC 1: Nạp 1.000.000 đồng
        UserResponseDTO depositRes = walletService.deposit(userId, new BigDecimal("1000000.00"));
        assertEquals(0, new BigDecimal("1000000.00").compareTo(depositRes.getBalance()));
        
        // Truy vấn ngược lại DB để chắc chắn nó đã nằm ở đó
        User dbUser1 = userDAO.findById(userId);
        assertEquals(0, new BigDecimal("1000000.00").compareTo(dbUser1.getBalance()));

        // BƯỚC 2: Rút 400.000 đồng
        UserResponseDTO withdrawRes = walletService.withdraw(userId, new BigDecimal("400000.00"));
        assertEquals(0, new BigDecimal("600000.00").compareTo(withdrawRes.getBalance()));

        // BƯỚC 3: Thanh toán đấu giá 550.000 đồng
        UserResponseDTO payRes = walletService.payForAuction(userId, new BigDecimal("550000.00"));
        assertEquals(0, new BigDecimal("50000.00").compareTo(payRes.getBalance()));

        // BƯỚC 4: Hoàn tiền (Refund) 100.000 đồng
        UserResponseDTO refundRes = walletService.refund(userId, new BigDecimal("100000.00"));
        assertEquals(0, new BigDecimal("150000.00").compareTo(refundRes.getBalance()));

        // KIỂM TRA CUỐI CÙNG TRONG DATABASE
        User finalDbUser = userDAO.findById(userId);
        assertEquals(0, new BigDecimal("150000.00").compareTo(finalDbUser.getBalance()), "Final database balance must be exact");
    }
}
