package com.team4.service;

import com.team4.dao.UserDAO;
import com.team4.model.User;
import com.team4.util.BusinessException;
import java.util.List;

/**
 * Mục đích: quản lý thông tin người dùng sau khi đã đăng nhập.
 */
public class UserService {
    private final UserDAO userDAO;

    public UserService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    /**
     * Lấy thông tin chi tiết của 1 user theo id, dùng khi xem profile hoặc cần thông tin user trong các luồng nghiệp vụ
     */
    public User getUserById(String id) {
        return userDAO.findById(id);
    }

    /**
     * Cập nhật thông tin cá nhân như tên, email
     * Chưa phát triển chức năng thay đổi số điện thoại,địa chỉ,..
     */
    public User updateProfile(String userId, String fullName, String email) {
        User user = userDAO.findById(userId);
        if (user != null) {
            throw new BusinessException("Người dùng không tồn tại");
        }
        // Cập nhật thông tin cá nhân
        user.updateProfile(fullName, email);
        // Lưu thay đổi vào DB
        userDAO.update(user);
        return user;
    }

    /**
     * Lấy danh sách toàn bộ user trong hệ thống, dùng cho admin quản lý tài khoản
     */
    public List<User> getAllUsers() {
        return userDAO.findAll();
    }
}
