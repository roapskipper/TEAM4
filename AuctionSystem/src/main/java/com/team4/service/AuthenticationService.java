package com.team4.service;

import com.team4.dao.UserDAO;
import com.team4.model.Admin;
import com.team4.model.Bidder;
import com.team4.model.User;
import com.team4.model.Seller;
import com.team4.util.PasswordHasher;
import com.team4.util.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mục đích: xử lý đăng ký, đăng nhập, đổi mật khẩu.
 */
public class AuthenticationService {
    private UserDAO userDAO;
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);

    public AuthenticationService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    // Tạo tài khoản Bidder mới
    public void registerBidder(String username, String rawPassword, String fullName, String email, String shippingAddress, String phoneNumber) {
        logger.info("Registering bidder account username={} email={}", username, email);
        // Kiểm tra xem username đã tồn tại chưa
        if (userDAO.findByUsername(username) != null) {
            logger.warn("Bidder registration failed because username already exists username={}", username);
            throw new BusinessException("Username already exists");
        }
        // Nếu username hợp lệ thì tạo bidder mới
        // model Bidder sẽ tự động validate fields
        String hashedPassword = PasswordHasher.hashPassword(rawPassword);
        Bidder bidder = new Bidder(username, hashedPassword, fullName, email, shippingAddress, phoneNumber);
        // Lưu vào DB
        boolean saved = userDAO.insert(bidder);
        if (!saved) {
            logger.error("Bidder registration failed due to database save error username={}", username);
            throw new BusinessException("Unable to create account, please try again");
        }
        logger.info("Bidder account registered userId={} username={}", bidder.getId(), username);
    }

    // Tạo tài khoản Seller mới
    public void registerSeller(String username, String rawPassword, String fullName, String email, String storeName) {
        logger.info("Registering seller account username={} email={} storeName={}", username, email, storeName);
        if (userDAO.findByUsername(username) != null) {
            logger.warn("Seller registration failed because username already exists username={}", username);
            throw new BusinessException("Username already exists");
        }
        String hashedPassword = PasswordHasher.hashPassword(rawPassword);
        Seller seller = new Seller(username, hashedPassword, fullName, email, storeName);
        boolean saved = userDAO.insert(seller);
        if (!saved) {
            logger.error("Seller registration failed due to database save error username={}", username);
            throw new BusinessException("Unable to create account, please try again");
        }
        logger.info("Seller account registered userId={} username={}", seller.getId(), username);
    }

    // Đăng nhập cho Bidder & Seller
    public User login(String username, String rawPassword) {
        logger.info("Signing in username={}", username);
        User user = userDAO.findByUsername(username);
        if (user == null) {
            logger.warn("Sign-in failed because username does not exist username={}", username);
            throw new BusinessException("Username does not exist");
        }
        if (!user.verifyPassword(rawPassword)) {
            logger.warn("Sign-in failed because password is incorrect username={} userId={}", username, user.getId());
            throw new BusinessException("Incorrect password");
        }
        logger.info("Sign-in successful userId={} username={} role={}", user.getId(), username, user.getRole());
        return user;
    }

    // Đăng nhập cho Admin
    public Admin loginAdmin(String username, String rawPassword, String rawAdminCode) {
        logger.info("Signing in admin username={}", username);
        User user = userDAO.findByUsername(username);

        if (user == null) {
            logger.warn("Admin sign-in failed because username does not exist username={}", username);
            throw new BusinessException("Username does not exist");

        }

        if (!(user instanceof Admin admin)) {
            logger.warn("Admin sign-in failed because account does not have admin privileges username={} userId={} role={}",
                    username, user.getId(), user.getRole());
            throw new BusinessException("Account does not have admin privileges");

        }

        if (!admin.verifyPassword(rawPassword)) {
            logger.warn("Admin sign-in failed because password is incorrect username={} userId={}", username, admin.getId());
            throw new BusinessException("Incorrect password");

        }

        if (!admin.verifyAdminCode(rawAdminCode)) {
            logger.warn("Admin sign-in failed because admin code is incorrect username={} userId={}", username, admin.getId());
            throw new BusinessException("Incorrect admin code");

        }

        logger.info("Admin sign-in successful userId={} username={} accessLevel={}",
                admin.getId(), username, admin.getAccessLevel());
        return admin;
    }

    // Đỏi mật khẩu
    public void changePassword(String userId, String oldRawPassword, String newRawPassword) {
        logger.info("Changing password userId={}", userId);
        User user = userDAO.findById(userId);
        if (user == null) {
            logger.warn("Password change failed because user does not exist userId={}", userId);
            throw new BusinessException("User does not exist");
        }
        if (!user.verifyPassword(oldRawPassword)) {
            logger.warn("Password change failed because old password is incorrect userId={}", userId);
            throw new BusinessException("Old password is incorrect");
        }
        user.changePasswordHash(PasswordHasher.hashPassword(newRawPassword));
        userDAO.update(user);
        logger.info("Password changed userId={} username={}", userId, user.getUsername());
    }
}
