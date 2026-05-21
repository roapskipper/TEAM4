package com.team4.service;

import com.team4.dao.UserDAO;
import com.team4.dto.auth.UserResponseDTO;
import com.team4.mapper.UserMapper;
import com.team4.model.User;
import com.team4.util.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

/**
 * Quản lý các nghiệp vụ liên quan đến ví tiền và số dư người dùng.
 */
public class WalletService {
    private static final Logger logger = LoggerFactory.getLogger(WalletService.class);
    private final UserDAO userDAO;

    public WalletService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    /**
     * Nạp tiền vào tài khoản người dùng.
     */
    public UserResponseDTO deposit(String userId, BigDecimal amount) {
        logger.info("Deposit requested: userId={}, amount={}", userId, amount);
        User user = userDAO.findById(userId);
        if (user == null) {
            logger.warn("Deposit failed: user not found. userId={}", userId);
            throw new BusinessException("User does not exist");
        }
        
        user.deposit(amount);
        
        if (!userDAO.updateBalance(userId, user.getBalance())) {
            logger.error("Database update failed for deposit: userId={}", userId);
            throw new BusinessException("Failed to update balance due to system error");
        }
        
        logger.info("Deposit successful: userId={}, newBalance={}", userId, user.getBalance());
        return UserMapper.toUserResponseDTO(user);
    }

    /**
     * Rút tiền từ tài khoản người dùng.
     */
    public UserResponseDTO withdraw(String userId, BigDecimal amount) {
        logger.info("Withdrawal requested: userId={}, amount={}", userId, amount);
        User user = userDAO.findById(userId);
        if (user == null) {
            logger.warn("Withdrawal failed: user not found. userId={}", userId);
            throw new BusinessException("User does not exist");
        }

        if (!user.withdraw(amount)) {
            logger.warn("Withdrawal failed: insufficient funds. userId={}, amount={}, balance={}", 
                    userId, amount, user.getBalance());
            throw new BusinessException("Insufficient balance or invalid amount for withdrawal");
        }

        if (!userDAO.updateBalance(userId, user.getBalance())) {
            logger.error("Database update failed for withdrawal: userId={}", userId);
            throw new BusinessException("Failed to update balance due to system error");
        }
        
        logger.info("Withdrawal successful: userId={}, newBalance={}", userId, user.getBalance());
        return UserMapper.toUserResponseDTO(user);
    }

    /**
     * Kiểm tra số dư người dùng có đủ cho một giao dịch hay không.
     */
    public boolean hasSufficientBalance(String userId, BigDecimal amount) {
        logger.debug("Checking balance sufficiency: userId={}, required={}", userId, amount);
        User user = userDAO.findById(userId);
        if (user == null) {
            throw new BusinessException("User does not exist");
        }
        return user.hasEnoughBalance(amount);
    }

    /**
     * Lấy số dư hiện tại của người dùng.
     */
    public BigDecimal getBalance(String userId) {
        logger.debug("Loading current balance: userId={}", userId);
        User user = userDAO.findById(userId);
        if (user == null) {
            throw new BusinessException("User does not exist");
        }
        return user.getBalance();
    }

    /**
     * Thực hiện thanh toán cho phiên đấu giá.
     */
    public UserResponseDTO payForAuction(String userId, BigDecimal amount) {
        logger.info("Processing auction payment: userId={}, amount={}", userId, amount);
        User user = userDAO.findById(userId);
        if (user == null) {
            throw new BusinessException("User does not exist");
        }
        if (!user.hasEnoughBalance(amount)) {
            logger.warn("Payment failed: insufficient balance. userId={}", userId);
            throw new BusinessException("Insufficient balance for auction payment");
        }

        user.withdraw(amount);
        if (!userDAO.updateBalance(userId, user.getBalance())) {
            logger.error("Database update failed for auction payment: userId={}", userId);
            throw new BusinessException("Payment failed due to system error");
        }
        
        logger.info("Auction payment successful: userId={}", userId);
        return UserMapper.toUserResponseDTO(user);
    }

    /**
     * Hoàn tiền cho người dùng (ví dụ: khi thua đấu giá).
     */
    public UserResponseDTO refund(String userId, BigDecimal amount) {
        logger.info("Processing refund: userId={}, amount={}", userId, amount);
        User user = userDAO.findById(userId);
        if (user == null) {
            throw new BusinessException("User does not exist");
        }

        user.deposit(amount);
        if (!userDAO.updateBalance(userId, user.getBalance())) {
            logger.error("Database update failed for refund: userId={}", userId);
            throw new BusinessException("Refund failed due to system error");
        }
        
        logger.info("Refund successful: userId={}", userId);
        return UserMapper.toUserResponseDTO(user);
    }
}
