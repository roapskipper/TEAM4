package com.team4.service;

import com.team4.dao.UserDAO;
import com.team4.dto.auth.*;
import com.team4.mapper.AuthMapper;
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
    private final UserDAO userDAO;
    private final JwtService jwtService;
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);

    public AuthenticationService(UserDAO userDAO, JwtService jwtService) {
        this.userDAO = userDAO;
        this.jwtService = jwtService;
    }

    // Tạo tài khoản Bidder mới
    public void registerBidder(RegisterBidderRequestDTO requestDTO) {
        logger.info("Registering new bidder: username={}, email={}", requestDTO.getUsername(), requestDTO.getEmail());
        validateNewUser(requestDTO.getUsername(), requestDTO.getEmail(), requestDTO.getPassword());
        
        String hashedPassword = PasswordHasher.hashPassword(requestDTO.getPassword());
        Bidder bidder = new Bidder(
                requestDTO.getUsername(), 
                hashedPassword, 
                requestDTO.getFullName(), 
                requestDTO.getEmail(), 
                requestDTO.getShippingAddress(), 
                requestDTO.getPhoneNumber()
        );
        
        if (!userDAO.insert(bidder)) {
            logger.error("Failed to save bidder to database: {}", requestDTO.getUsername());
            throw new BusinessException("Could not complete registration. Please try again.");
        }
        logger.info("Bidder registered successfully: userId={}", bidder.getId());
    }

    // Tạo tài khoản Seller mới
    public void registerSeller(RegisterSellerRequestDTO requestDTO) {
        logger.info("Registering new seller: username={}, email={}", requestDTO.getUsername(), requestDTO.getEmail());
        validateNewUser(requestDTO.getUsername(), requestDTO.getEmail(), requestDTO.getPassword());
        
        String hashedPassword = PasswordHasher.hashPassword(requestDTO.getPassword());
        Seller seller = new Seller(
                requestDTO.getUsername(), 
                hashedPassword, 
                requestDTO.getFullName(), 
                requestDTO.getEmail(), 
                requestDTO.getStoreName()
        );
        
        if (!userDAO.insert(seller)) {
            logger.error("Failed to save seller to database: {}", requestDTO.getUsername());
            throw new BusinessException("Could not complete registration. Please try again.");
        }
        logger.info("Seller registered successfully: userId={}", seller.getId());
    }

    // Đăng nhập cho Bidder
    public LoginResponseDTO loginBidder(LoginRequestDTO requestDTO) {
        logger.info("Bidder login attempt: username={}", requestDTO.getUsername());
        User user = authenticateBase(requestDTO.getUsername(), requestDTO.getPassword());
        
        if (user.getRole() != User.Role.BIDDER) {
            logger.warn("Login rejected: User {} is not a BIDDER (Role: {})", user.getUsername(), user.getRole());
            throw new BusinessException("This account is not registered as a Bidder.");
        }
        
        String token = jwtService.generateToken(user);
        return AuthMapper.toLoginResponseDTO(user, token);
    }

    // Đăng nhập cho Seller
    public LoginResponseDTO loginSeller(LoginRequestDTO requestDTO) {
        logger.info("Seller login attempt: username={}", requestDTO.getUsername());
        User user = authenticateBase(requestDTO.getUsername(), requestDTO.getPassword());
        
        if (user.getRole() != User.Role.SELLER) {
            logger.warn("Login rejected: User {} is not a SELLER (Role: {})", user.getUsername(), user.getRole());
            throw new BusinessException("This account is not registered as a Seller.");
        }
        
        String token = jwtService.generateToken(user);
        return AuthMapper.toLoginResponseDTO(user, token);
    }

    // Đăng nhập cho Admin
    public LoginResponseDTO loginAdmin(LoginRequestDTO requestDTO) {
        logger.info("Admin login attempt: username={}", requestDTO.getUsername());
        User user = userDAO.findByUsername(requestDTO.getUsername());

        if (user == null || !user.verifyPassword(requestDTO.getPassword())) {
            logger.warn("Admin login failed: Invalid credentials for username={}", requestDTO.getUsername());
            throw new BusinessException("Invalid username or password.");
        }
        
        if (!(user instanceof Admin admin)) {
            logger.warn("Login rejected: User {} does not have ADMIN privileges", user.getUsername());
            throw new BusinessException("Access denied. Admin privileges required.");
        }

        // Kiểm tra mã bảo mật riêng của Admin
        if (!admin.verifyAdminCode(requestDTO.getAdminCode())) {
            logger.warn("Admin login failed: Incorrect admin code for username={}", requestDTO.getUsername());
            throw new BusinessException("Invalid admin security code.");
        }
        
        String token = jwtService.generateToken(user);
        return AuthMapper.toLoginResponseDTO(user, token);
    }

    public void changePassword(String userId, String oldRawPassword, String newRawPassword) {
        logger.info("Password change requested for userId={}", userId);
        User user = userDAO.findById(userId);
        if (user == null || !user.verifyPassword(oldRawPassword)) {
            throw new BusinessException("Authentication failed. Current password incorrect.");
        }
        if (newRawPassword == null || newRawPassword.length() < 6) {
            throw new BusinessException("New password must be at least 6 characters long.");
        }
        
        user.changePasswordHash(PasswordHasher.hashPassword(newRawPassword));
        userDAO.update(user);
        logger.info("Password changed successfully for userId={}", userId);
    }

    // --- Helpers ---

    private void validateNewUser(String username, String email, String password) {
        if (userDAO.findByUsername(username) != null) {
            throw new BusinessException("Username already exists.");
        }
        if (userDAO.findByEmail(email) != null) {
            throw new BusinessException("Email already exists.");
        }
        if (password == null || password.length() < 6) {
            throw new BusinessException("Password must be at least 6 characters long.");
        }
    }

    private User authenticateBase(String username, String password) {
        User user = userDAO.findByUsername(username);
        if (user == null || !user.verifyPassword(password)) {
            logger.warn("Authentication failed for username: {}", username);
            throw new BusinessException("Invalid username or password.");
        }
        return user;
    }
}
