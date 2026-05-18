package com.team4.service;

import com.team4.dao.UserDAO;
import com.team4.model.User;
import com.team4.util.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Mục đích: quản lý thông tin người dùng sau khi đã đăng nhập.
 */
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserDAO userDAO;

    public UserService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    /**
     * Lấy thông tin chi tiết của 1 user theo id, dùng khi xem profile hoặc cần thông tin user trong các luồng nghiệp vụ
     */
    public User getUserById(String id) {
        logger.debug("Đang lấy thông tin người dùng: id={}", id);
        return userDAO.findById(id);
    }

    /**
     * Cập nhật thông tin cá nhân như tên, email
     * Chưa phát triển chức năng thay đổi số điện thoại,địa chỉ,..
     */
    public User updateProfile(String userId, String fullName, String email, String phone) {
        logger.info("Đang cập nhật hồ sơ người dùng: userId={}, fullName={}, email={}, phone={}", userId, fullName, email, phone);
        User user = userDAO.findById(userId);
        if (user == null) {
            logger.warn("Cập nhật hồ sơ thất bại: Người dùng không tồn tại. userId={}", userId);
            throw new BusinessException("Người dùng không tồn tại");
        }
        // Cập nhật thông tin cá nhân
        user.updateProfile(fullName, email);
        if (user instanceof com.team4.model.Bidder) {
            ((com.team4.model.Bidder) user).setPhoneNumber(phone);
        }
        // Lưu thay đổi vào DB
        boolean updated = userDAO.update(user);
        if (updated) {
            logger.info("Đã cập nhật hồ sơ thành công cho người dùng: userId={}", userId);
        } else {
            logger.error("Lỗi hệ thống: Không thể cập nhật thông tin người dùng vào database. userId={}", userId);
        }
        return user;
    }

    /**
     * Lấy danh sách toàn bộ user trong hệ thống, dùng cho admin quản lý tài khoản
     */
    public List<User> getAllUsers() {
        logger.debug("Đang lấy danh sách tất cả người dùng");
        return userDAO.findAll();
    }
}
