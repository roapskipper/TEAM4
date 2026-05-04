package com.team4.service;
import java.math.BigDecimal;
import com.team4.dao.UserDAO;
import com.team4.model.Bidder;
import com.team4.model.User;
import com.team4.util.BusinessException;

/**
 * Mục đích: Tách riêng nghiệp vụ tiền ra khỏi UserService vì tiền là phần nhạy cảm
 */
public class WalletService {
    private final UserDAO userDAO;

    public WalletService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    // Nạp tiền
    public User deposit(String userId, BigDecimal amount) {
        User user = userDAO.findById(userId);
        if (user == null) {
            throw new BusinessException("Người dùng không tồn tại");
        }
        // Gọi phương thức của model User
        user.deposit(amount);
        // Nạp vào DB
        if (!userDAO.updateBalance(userId, user.getBalance())) {
            throw new BusinessException("Nạp tiền thất bại do lỗi cơ sở dữ liệu");
        }
        return user;
    }
    // Rút tiền
    public User withdraw(String userId, BigDecimal amount) {
        User user = userDAO.findById(userId);
        if (user == null) {
            throw new BusinessException("Người dùng không tồn tại");
        }
        user.withdraw(amount);
        if (!userDAO.updateBalance(userId, user.getBalance())) {
            throw new BusinessException("Rút tiền thất bại do lỗi cơ sở dữ liệu");
        }
        return user;
    }
    // Kiêm tra xem người dùng đủ tiền không
    public boolean hasSufficientBalance(String userId, BigDecimal amount) {
        User user = userDAO.findById(userId);
        if (user == null) {
            throw new BusinessException("Người dùng không tồn tại");
        }
        return user.hasEnoughBalance(amount);
    }
    // Lấy số dư hiện tại của user
    public BigDecimal balance(String userId) {
        User user = userDAO.findById(userId);
        if (user == null) {
            throw new BusinessException("Người dùng không tồn tại");
        }
        return user.getBalance();
    }
}
