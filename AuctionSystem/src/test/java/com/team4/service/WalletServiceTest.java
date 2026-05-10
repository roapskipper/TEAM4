package com.team4.service;

import com.team4.dao.UserDAO;
import com.team4.model.Bidder;
import com.team4.model.User;
import com.team4.util.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WalletServiceTest {

    @Mock
    private UserDAO userDAO;

    private WalletService walletService;

    @BeforeEach
    void setUp() {
        walletService = new WalletService(userDAO);
    }

    @Test
    void testDeposit_Success() {
        // Arrange
        String userId = "user1";
        BigDecimal amount = new BigDecimal("500.00");
        User mockUser = new Bidder(userId, "Test", "e@test.com", "1", "A", "P");
        BigDecimal initialBalance = mockUser.getBalance();

        when(userDAO.findById(userId)).thenReturn(mockUser);
        when(userDAO.updateBalance(eq(userId), any(BigDecimal.class))).thenReturn(true);

        // Act
        User result = walletService.deposit(userId, amount);

        // Assert
        assertEquals(initialBalance.add(amount), result.getBalance());
        verify(userDAO).updateBalance(userId, result.getBalance());
    }

    @Test
    void testWithdraw_Success() {
        // Arrange
        String userId = "user1";
        BigDecimal amount = new BigDecimal("200.00");
        User mockUser = new Bidder(userId, "Test", "e@test.com", "1", "A", "P");
        mockUser.deposit(new BigDecimal("1000.00"));

        when(userDAO.findById(userId)).thenReturn(mockUser);
        when(userDAO.updateBalance(eq(userId), any(BigDecimal.class))).thenReturn(true);

        // Act
        User result = walletService.withdraw(userId, amount);

        // Assert
        assertEquals(new BigDecimal("800.00"), result.getBalance());
        verify(userDAO).updateBalance(userId, result.getBalance());
    }

    @Test
    void testWithdraw_Fail_InsufficientFunds() {
        // Arrange
        String userId = "user1";
        BigDecimal amount = new BigDecimal("2000.00");
        User mockUser = new Bidder(userId, "Test", "e@test.com", "1", "A", "P");
        mockUser.deposit(new BigDecimal("500.00"));

        when(userDAO.findById(userId)).thenReturn(mockUser);

        // Act & Assert
        assertThrows(BusinessException.class, () -> 
            walletService.withdraw(userId, amount)
        );
        verify(userDAO, never()).updateBalance(anyString(), any());
    }
}
