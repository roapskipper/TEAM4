package com.team4.service;

import com.team4.dao.UserDAO;
import com.team4.model.Bidder;
import com.team4.model.User;
import com.team4.model.Seller;
import com.team4.util.PasswordHasher;

/**
 * Mục đích: xử lý đăng ký, đăng nhập, đổi mật khẩu.
 */
public class AuthenticationService {
    private UserDAO userDAO;

    public AuthenticationService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    // Tạo tài khoản Bidder mới
    public void registerBidder(String username, String rawPassword, String fullName, String email, String shippingAddress, String phoneNumber) {
        // Kiểm tra xem username đã tồn tại chưa
        if (userDAO.findByUsername(username) != null) {
            throw new BusinessException("Tên đăng nhập đã tồn tại");
        }
        // Nếu username hợp lệ thì tạo bidder mới
        // model Bidder sẽ tự động validate fields
        String hashedPassword = PasswordHasher.hashPassword(rawPassword);
        Bidder bidder = new Bidder(username, hashedPassword, fullName, email, shippingAddress, phoneNumber);
        // Lưu vào DB
        userDAO.insert(bidder);
    }

    // Tạo tài khoản Seller mới
    public void registerSeller(String username, String rawPassword, String fullName, String email, String storeName) {
        if (userDAO.findByUsername(username) != null) {
            throw new BusinessException("Tên đăng nhập đã tồn tại");
        }
        String hashedPassword = PasswordHasher.hashPassword(rawPassword);
        Seller seller = new Seller(username, hashedPassword, fullName, email, storeName);
        userDAO.insert(seller);
    }

    // Đăng nhập
    public User login(String username, String rawPassword) {
        User user = userDAO.findByUsername(username);
        if (user == null) {
            throw new BusinessException("Tên đăng nhập không tồn tại");
        }
        if (!user.verifyPassword(rawPassword)) {
            throw new BusinessException("Mật khẩu không đúng");
        }
        return user;
    }

    // Đỏi mật khẩu
    public void changePassword(String userId, String oldRawPassword, String newRawPassword) {
        User user = userDAO.findById(userId);
        if (user == null) {
            throw new BusinessException("Người dùng không tồn tại");
        }
        if (!user.verifyPassword(oldRawPassword)) {
            throw new BusinessException("Mật khẩu cũ không đúng");
        }
        user.changePasswordHash(PasswordHasher.hashPassword(newRawPassword));
        userDAO.update(user);
    }
}
class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
