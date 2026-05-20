package com.team4.service;
import com.team4.dao.UserDAO;
import com.team4.model.User;
import com.team4.util.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

/**
 * Mục đích: Tách riêng nghiệp vụ tiền ra khỏi UserService vì tiền là phần nhạy cảm
 */
public class WalletService {
    private static final Logger logger = LoggerFactory.getLogger(WalletService.class);
    private final UserDAO userDAO;

    public WalletService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    // Nạp tiền
    public User deposit(String userId, BigDecimal amount) {
        logger.info("Deposit request: userId={}, amount={}", userId, amount);
        User user = userDAO.findById(userId);
        if (user == null) {
            logger.warn("Deposit failed: user does not exist. userId={}", userId);
            throw new BusinessException("User does not exist");
        }
        
        // Gọi phương thức của model User
        user.deposit(amount);
        
        // Nạp vào DB
        if (!userDAO.updateBalance(userId, user.getBalance())) {
            logger.error("System error while updating balance after deposit: userId={}, amount={}, currentBalance={}", 
                    userId, amount, user.getBalance());
            throw new BusinessException("Deposit failed due to database error");
        }
        
        logger.info("Deposit successful: userId={}, amount={}, newBalance={}", userId, amount, user.getBalance());
        return user;
    }

    // Rút tiền
    public User withdraw(String userId, BigDecimal amount) {
        logger.info("Withdrawal request: userId={}, amount={}", userId, amount);
        User user = userDAO.findById(userId);
        if (user == null) {
            logger.warn("Withdrawal failed: user does not exist. userId={}", userId);
            throw new BusinessException("User does not exist");
        }
        if (!user.withdraw(amount)) {
            logger.warn("Withdrawal failed: insufficient balance or invalid amount. userId={}, amount={}, currentBalance={}", 
                    userId, amount, user.getBalance());
            throw new BusinessException("Withdrawal failed: insufficient balance or invalid amount");
        }
        if (!userDAO.updateBalance(userId, user.getBalance())) {
            logger.error("System error while updating balance after withdrawal: userId={}, amount={}, currentBalance={}", 
                    userId, amount, user.getBalance());
            throw new BusinessException("Withdrawal failed due to database error");
        }
        
        logger.info("Withdrawal successful: userId={}, amount={}, newBalance={}", userId, amount, user.getBalance());
        return user;
    }

    // Kiêm tra xem người dùng đủ tiền không
    public boolean hasSufficientBalance(String userId, BigDecimal amount) {
        logger.debug("Checking payment capability: userId={}, requiredAmount={}", userId, amount);
        User user = userDAO.findById(userId);
        if (user == null) {
            logger.warn("Balance check failed: user does not exist. userId={}", userId);
            throw new BusinessException("User does not exist");
        }
        boolean sufficient = user.hasEnoughBalance(amount);
        logger.debug("Balance check result: userId={}, sufficient={}", userId, sufficient);
        return sufficient;
    }

    // Lấy số dư hiện tại của user
    public BigDecimal balance(String userId) {
        logger.debug("Loading current user balance: userId={}", userId);
        User user = userDAO.findById(userId);
        if (user == null) {
            logger.warn("Balance load failed: user does not exist. userId={}", userId);
            throw new BusinessException("User does not exist");
        }
        return user.getBalance();
    }
    // Thanh toan cho phien dau gia
    public User payForAuction(String userId, BigDecimal amount) {
        User user = userDAO.findById(userId);
        if (user == null) {
            throw new BusinessException("User does not exist");
        }
        if (!user.hasEnoughBalance(amount)) {
            throw new BusinessException("Insufficient balance for payment");
        }
        user.withdraw(amount);
        if (!userDAO.updateBalance(userId, user.getBalance())) {
            throw new BusinessException("Payment failed due to database error");
        }
        return user;
    }

    // Hoan tien cho nguoi thua cuoc
    public User refund(String userId, BigDecimal amount) {
        User user = userDAO.findById(userId);
        if (user == null) {
            throw new BusinessException("User does not exist");
        }
        user.deposit(amount);
        if (!userDAO.updateBalance(userId, user.getBalance())) {
            throw new BusinessException("Refund failed due to database error");
        }
        return user;
    }
}
