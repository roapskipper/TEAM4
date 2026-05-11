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
        logger.info("Yêu cầu nạp tiền: userId={}, amount={}", userId, amount);
        User user = userDAO.findById(userId);
        if (user == null) {
            logger.warn("Nạp tiền thất bại: Người dùng không tồn tại. userId={}", userId);
            throw new BusinessException("Người dùng không tồn tại");
        }
        
        // Gọi phương thức của model User
        user.deposit(amount);
        
        // Nạp vào DB
        if (!userDAO.updateBalance(userId, user.getBalance())) {
            logger.error("Lỗi hệ thống khi cập nhật số dư sau khi nạp: userId={}, amount={}, currentBalance={}", 
                    userId, amount, user.getBalance());
            throw new BusinessException("Nạp tiền thất bại do lỗi cơ sở dữ liệu");
        }
        
        logger.info("Nạp tiền thành công: userId={}, amount={}, newBalance={}", userId, amount, user.getBalance());
        return user;
    }

    // Rút tiền
    public User withdraw(String userId, BigDecimal amount) {
        logger.info("Yêu cầu rút tiền: userId={}, amount={}", userId, amount);
        User user = userDAO.findById(userId);
        if (user == null) {
            logger.warn("Rút tiền thất bại: Người dùng không tồn tại. userId={}", userId);
            throw new BusinessException("Người dùng không tồn tại");
        }

        try {
            user.withdraw(amount);
        } catch (BusinessException e) {
            logger.warn("Rút tiền thất bại: Số dư không đủ hoặc số tiền không hợp lệ. userId={}, amount={}, currentBalance={}", 
                    userId, amount, user.getBalance());
            throw e;
        }

        if (!userDAO.updateBalance(userId, user.getBalance())) {
            logger.error("Lỗi hệ thống khi cập nhật số dư sau khi rút: userId={}, amount={}, currentBalance={}", 
                    userId, amount, user.getBalance());
            throw new BusinessException("Rút tiền thất bại do lỗi cơ sở dữ liệu");
        }
        
        logger.info("Rút tiền thành công: userId={}, amount={}, newBalance={}", userId, amount, user.getBalance());
        return user;
    }

    // Kiêm tra xem người dùng đủ tiền không
    public boolean hasSufficientBalance(String userId, BigDecimal amount) {
        logger.debug("Kiểm tra khả năng thanh toán: userId={}, requiredAmount={}", userId, amount);
        User user = userDAO.findById(userId);
        if (user == null) {
            logger.warn("Kiểm tra số dư thất bại: Người dùng không tồn tại. userId={}", userId);
            throw new BusinessException("Người dùng không tồn tại");
        }
        boolean sufficient = user.hasEnoughBalance(amount);
        logger.debug("Kết quả kiểm tra số dư: userId={}, sufficient={}", userId, sufficient);
        return sufficient;
    }

    // Lấy số dư hiện tại của user
    public BigDecimal balance(String userId) {
        logger.debug("Lấy số dư hiện tại của người dùng: userId={}", userId);
        User user = userDAO.findById(userId);
        if (user == null) {
            logger.warn("Lấy số dư thất bại: Người dùng không tồn tại. userId={}", userId);
            throw new BusinessException("Người dùng không tồn tại");
        }
        return user.getBalance();
    }
}
