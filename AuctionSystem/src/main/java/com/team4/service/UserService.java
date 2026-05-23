package com.team4.service;

import com.team4.dao.UserDAO;
import com.team4.dto.auth.UserResponseDTO;
import com.team4.mapper.UserMapper;
import com.team4.model.Admin;
import com.team4.model.User;
import com.team4.util.BusinessException;
import com.team4.util.PasswordHasher;
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
        logger.info("Updating user profile: userId={}, fullName={}, email={}, phone={}", userId, fullName, email, phone);
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

    public UserResponseDTO grantAdminRole(String requesterId, String targetUserId, String adminCode) {
        requireSuperAdmin(requesterId);
        if (targetUserId == null || targetUserId.isBlank()) {
            throw new BusinessException("Target user id is required");
        }
        if (adminCode == null || adminCode.isBlank()) {
            throw new BusinessException("Admin code is required");
        }

        User target = getRawUserById(targetUserId);
        if (target.getRole() == User.Role.ADMIN) {
            throw new BusinessException("User is already an admin");
        }

        String adminCodeHash = PasswordHasher.hashPassword(adminCode.trim());
        if (!userDAO.grantAdminRole(targetUserId, adminCodeHash)) {
            throw new BusinessException("Unable to grant admin role");
        }
        return getUserById(targetUserId);
    }

    public UserResponseDTO revokeAdminRole(String requesterId, String targetUserId) {
        requireSuperAdmin(requesterId);
        if (targetUserId == null || targetUserId.isBlank()) {
            throw new BusinessException("Target user id is required");
        }
        if (requesterId.equals(targetUserId)) {
            throw new BusinessException("You cannot revoke your own admin role");
        }

        User target = getRawUserById(targetUserId);
        if (!(target instanceof Admin admin)) {
            throw new BusinessException("User is not an admin");
        }
        if (admin.getAccessLevel() == Admin.AccessLevel.SUPER_ADMIN) {
            throw new BusinessException("Super Admin role cannot be revoked");
        }

        if (!userDAO.revokeAdminRole(targetUserId)) {
            throw new BusinessException("Unable to revoke admin role");
        }
        return getUserById(targetUserId);
    }

    private void requireSuperAdmin(String requesterId) {
        if (requesterId == null || requesterId.isBlank()) {
            throw new BusinessException("Requester id is required");
        }
        User requester = getRawUserById(requesterId);
        if (!(requester instanceof Admin admin)
                || admin.getAccessLevel() != Admin.AccessLevel.SUPER_ADMIN) {
            throw new BusinessException("Only Super Admin can perform this action");
        }
    }

    /**
     * Thay đổi mật khẩu người dùng.
     */
    public void changePassword(String userId, String oldPassword, String newPassword) {
        logger.info("Changing password for user: userId={}", userId);
        User user = getRawUserById(userId);
        
        if (!user.verifyPassword(oldPassword)) {
            logger.warn("Password change failed: incorrect old password. userId={}", userId);
            throw new BusinessException("Old password is incorrect");
        }
        
        String newHash = com.team4.util.PasswordHasher.hashPassword(newPassword);
        user.changePasswordHash(newHash);
        
        if (!userDAO.update(user)) {
            logger.error("Failed to update password in database: userId={}", userId);
            throw new BusinessException("System error: unable to update password");
        }
        logger.info("Password updated successfully: userId={}", userId);
    }
}
