package com.team4.service;

import com.team4.dao.UserDAO;
import com.team4.dto.auth.UserResponseDTO;
import com.team4.mapper.UserMapper;
import com.team4.model.User;
import com.team4.util.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Quản lý thông tin người dùng sau khi đã đăng nhập.
 */
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserDAO userDAO;

    public UserService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    /**
     * Lấy thông tin chi tiết của 1 user theo id (DTO).
     */
    public UserResponseDTO getUserById(String id) {
        logger.debug("Loading user details: userId={}", id);
        User user = getRawUserById(id);
        return UserMapper.toUserResponseDTO(user);
    }

    /**
     * Lấy model User gốc để sử dụng nội bộ.
     */
    public User getRawUserById(String id) {
        User user = userDAO.findById(id);
        if (user == null) {
            logger.warn("User not found: userId={}", id);
            throw new BusinessException("User does not exist");
        }
        return user;
    }

    /**
     * Cập nhật thông tin cá nhân.
     */
    public UserResponseDTO updateProfile(String userId, String fullName, String email, String phone) {
        logger.info("Updating user profile: userId={}, fullName={}, email={}, phone={}", userId, fullName, email,
                phone);
        User user = userDAO.findById(userId);
        if (user == null) {
            logger.warn("Profile update failed: user does not exist. userId={}", userId);
            throw new BusinessException("User does not exist");
        }

        // Cập nhật thông tin cá nhân
        user.updateProfile(fullName, email);
        if (user instanceof com.team4.model.Bidder bidder) {
            bidder.setPhoneNumber(phone);
        }

        // Lưu thay đổi vào DB
        if (!userDAO.update(user)) {
            logger.error("System error: unable to update user in database. userId={}", userId);
            throw new BusinessException("Failed to update profile due to system error.");
        }

        logger.info("User profile updated successfully: userId={}", userId);
        return UserMapper.toUserResponseDTO(user);
    }

    /**
     * Lấy danh sách toàn bộ user (Dành cho Admin).
     */
    public List<UserResponseDTO> getAllUsers() {
        logger.debug("Loading all system users");
        return userDAO.findAll().stream()
                .map(UserMapper::toUserResponseDTO)
                .collect(Collectors.toList());
    }
}
