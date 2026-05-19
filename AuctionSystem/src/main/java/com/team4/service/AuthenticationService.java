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
        logger.info("Đang đăng ký tài khoản bidder username={} email={}", username, email);
        // Kiểm tra xem username đã tồn tại chưa
        if (userDAO.findByUsername(username) != null) {
            logger.warn("Đăng ký bidder thất bại vì username đã tồn tại username={}", username);
            throw new BusinessException("Tên đăng nhập đã tồn tại");
        }
        // Nếu username hợp lệ thì tạo bidder mới
        // model Bidder sẽ tự động validate fields
        String hashedPassword = PasswordHasher.hashPassword(rawPassword);
        Bidder bidder = new Bidder(username, hashedPassword, fullName, email, shippingAddress, phoneNumber);
        // Lưu vào DB
        boolean saved = userDAO.insert(bidder);
        if (!saved) {
            logger.error("Đăng ký bidder thất bại do lỗi lưu DB username={}", username);
            throw new BusinessException("Không thể tạo tài khoản, vui lòng thử lại");
        }
        logger.info("Đã đăng ký tài khoản bidder userId={} username={}", bidder.getId(), username);
    }

    // Tạo tài khoản Seller mới
    public void registerSeller(String username, String rawPassword, String fullName, String email, String storeName) {
        logger.info("Đang đăng ký tài khoản seller username={} email={} storeName={}", username, email, storeName);
        if (userDAO.findByUsername(username) != null) {
            logger.warn("Đăng ký seller thất bại vì username đã tồn tại username={}", username);
            throw new BusinessException("Tên đăng nhập đã tồn tại");
        }
        String hashedPassword = PasswordHasher.hashPassword(rawPassword);
        Seller seller = new Seller(username, hashedPassword, fullName, email, storeName);
        boolean saved = userDAO.insert(seller);
        if (!saved) {
            logger.error("Đăng ký seller thất bại do lỗi lưu DB username={}", username);
            throw new BusinessException("Không thể tạo tài khoản, vui lòng thử lại");
        }
        logger.info("Đã đăng ký tài khoản seller userId={} username={}", seller.getId(), username);
    }

    // Đăng nhập cho Bidder & Seller
    public User login(String username, String rawPassword) {
        logger.info("Đang đăng nhập username={}", username);
        User user = userDAO.findByUsername(username);
        if (user == null) {
            logger.warn("Đăng nhập thất bại vì username không tồn tại username={}", username);
            throw new BusinessException("Tên đăng nhập không tồn tại");
        }
        if (!user.verifyPassword(rawPassword)) {
            logger.warn("Đăng nhập thất bại vì mật khẩu không đúng username={} userId={}", username, user.getId());
            throw new BusinessException("Mật khẩu không đúng");
        }
        logger.info("Đăng nhập thành công userId={} username={} role={}", user.getId(), username, user.getRole());
        return user;
    }

    // Đăng nhập cho Admin
    public Admin loginAdmin(String username, String rawPassword, String rawAdminCode) {
        logger.info("Đang đăng nhập admin username={}", username);
        User user = userDAO.findByUsername(username);

        if (user == null) {
            logger.warn("Đăng nhập admin thất bại vì username không tồn tại username={}", username);
            throw new BusinessException("Tên đăng nhập không tồn tại");

        }

        if (!(user instanceof Admin admin)) {
            logger.warn("Đăng nhập admin thất bại vì tài khoản không có quyền admin username={} userId={} role={}",
                    username, user.getId(), user.getRole());
            throw new BusinessException("Tài khoản không có quyền admin");

        }

        if (!admin.verifyPassword(rawPassword)) {
            logger.warn("Đăng nhập admin thất bại vì mật khẩu không đúng username={} userId={}", username, admin.getId());
            throw new BusinessException("Mật khẩu không đúng");

        }

        if (!admin.verifyAdminCode(rawAdminCode)) {
            logger.warn("Đăng nhập admin thất bại vì mã admin không đúng username={} userId={}", username, admin.getId());
            throw new BusinessException("Mã admin không đúng");

        }

        logger.info("Đăng nhập admin thành công userId={} username={} accessLevel={}",
                admin.getId(), username, admin.getAccessLevel());
        return admin;
    }

    // Đỏi mật khẩu
    public void changePassword(String userId, String oldRawPassword, String newRawPassword) {
        logger.info("Đang đổi mật khẩu userId={}", userId);
        User user = userDAO.findById(userId);
        if (user == null) {
            logger.warn("Đổi mật khẩu thất bại vì người dùng không tồn tại userId={}", userId);
            throw new BusinessException("Người dùng không tồn tại");
        }
        if (!user.verifyPassword(oldRawPassword)) {
            logger.warn("Đổi mật khẩu thất bại vì mật khẩu cũ không đúng userId={}", userId);
            throw new BusinessException("Mật khẩu cũ không đúng");
        }
        user.changePasswordHash(PasswordHasher.hashPassword(newRawPassword));
        userDAO.update(user);
        logger.info("Đã đổi mật khẩu userId={} username={}", userId, user.getUsername());
    }
}
