package com.team4.service;

import com.team4.dao.UserDAO;
import com.team4.dto.auth.LoginRequestDTO;
import com.team4.dto.auth.LoginResponseDTO;
import com.team4.dto.auth.RegisterBidderRequestDTO;
import com.team4.dto.auth.RegisterSellerRequestDTO;
import com.team4.model.Admin;
import com.team4.model.Bidder;
import com.team4.model.Seller;
import com.team4.model.User;
import com.team4.util.BusinessException;
import com.team4.util.PasswordHasher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Lớp kiểm thử AuthenticationServiceTest.
 * Môi trường: JDK 21, JUnit 5, Mockito.
 * 
 * TUÂN THỦ QUY TẮC:
 * 1. KHÔNG MOCK các class chứa dữ liệu (Bidder, Seller, Admin) -> Sử dụng 'new'.
 * 2. CHỈ MOCK các Interface phụ thuộc logic (UserDAO).
 * 3. Sử dụng @ExtendWith(MockitoExtension.class).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Kiểm thử nghiệp vụ Xác thực (AuthenticationService)")
public class AuthenticationServiceTest {

    @Mock
    private UserDAO userDAO;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthenticationService authService;

    // Một chuỗi thỏa mãn regex AdminCode: 8-128 ký tự, 1 hoa, 1 thường, 1 số, 1 đặc biệt
    private final String VALID_ADMIN_CODE_HASH = "Admin@123456"; 

    @Nested
    @DisplayName("Nghiệp vụ Đăng ký (Registration)")
    class RegistrationTests {

        @Test
        @DisplayName("Đăng ký Bidder thành công")
        void testRegisterBidder_Success() {
            // GIVEN: Username chưa tồn tại
            String username = "new_bidder";
            when(userDAO.findByUsername(username)).thenReturn(null);
            when(userDAO.insert(any(Bidder.class))).thenReturn(true);

            // WHEN: Thực hiện đăng ký
            authService.registerBidder(new RegisterBidderRequestDTO(username, "Password123", "Người Mua Mới", "bidder@test.com", "Hà Nội", "0912345678"));

            // THEN: Phải gọi DAO để lưu thông tin
            verify(userDAO).insert(any(Bidder.class));
        }

        @Test
        @DisplayName("Đăng ký Seller thành công")
        void testRegisterSeller_Success() {
            // GIVEN
            String username = "new_seller";
            when(userDAO.findByUsername(username)).thenReturn(null);
            when(userDAO.insert(any(Seller.class))).thenReturn(true);

            // WHEN
            authService.registerSeller(new RegisterSellerRequestDTO(username, "Password123", "Người Bán Mới", "seller@test.com", "Cửa hàng ABC"));

            // THEN
            verify(userDAO).insert(any(Seller.class));
        }

        @Test
        @DisplayName("Đăng ký thất bại - Tên đăng nhập đã tồn tại")
        void testRegister_DuplicateUsername() {
            // GIVEN: Giả lập username đã có người dùng
            String username = "existing_user";
            User existingUser = new Bidder(username, "hash", "Tên", "email@test.com", "HN", "09128846584");
            when(userDAO.findByUsername(username)).thenReturn(existingUser);

            // WHEN & THEN: Kỳ vọng ném BusinessException
            BusinessException ex = assertThrows(BusinessException.class, () -> 
                authService.registerBidder(new RegisterBidderRequestDTO(username, "Name", "Password123", "e@test.com", "Adr", "01235748768"))
            );
            assertEquals("Username already exists.", ex.getMessage());
            verify(userDAO, never()).insert(any());
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ Đăng nhập (Login)")
    class LoginTests {

        @Test
        @DisplayName("Đăng nhập người dùng thành công")
        void testLogin_Success() {
            // GIVEN: Người dùng tồn tại với mật khẩu "Secret123"
            String username = "user_test";
            String rawPass = "Secret123";
            String hashedPass = PasswordHasher.hashPassword(rawPass);
            User realUser = new Bidder(username, hashedPass, "Nguyễn Văn A", "a@test.com", "HN", "0996664646");

            when(userDAO.findByUsername(username)).thenReturn(realUser);
            when(jwtService.generateToken(any())).thenReturn("mock-token");

            // WHEN
            LoginResponseDTO result = authService.loginBidder(new LoginRequestDTO(username, rawPass, null));

            // THEN
            assertNotNull(result);
            assertEquals(username, result.getUsername());
        }

        @Test
        @DisplayName("Đăng nhập thất bại - Sai mật khẩu")
        void testLogin_WrongPassword() {
            // GIVEN
            String username = "user_test";
            String hashedPass = PasswordHasher.hashPassword("CorrectPass");
            User realUser = new Bidder(username, hashedPass, "Tên", "e@test.com", "HN", "0912375456");

            when(userDAO.findByUsername(username)).thenReturn(realUser);

            // WHEN & THEN: Đăng nhập với mật khẩu "WrongPass"
            BusinessException ex = assertThrows(BusinessException.class, () -> 
                authService.loginBidder(new LoginRequestDTO(username, "WrongPass", null))
            );
            assertEquals("Invalid username or password.", ex.getMessage());
        }

        @Test
        @DisplayName("Đăng nhập thất bại - Tên đăng nhập không tồn tại")
        void testLogin_UsernameNotFound() {
            when(userDAO.findByUsername("unknown")).thenReturn(null);

            assertThrows(BusinessException.class, () -> 
                authService.loginBidder(new LoginRequestDTO("unknown", "any_pass", null))
            );
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ Đăng nhập Admin")
    class AdminLoginTests {

        @Test
        @DisplayName("Admin đăng nhập thành công")
        void testLoginAdmin_Success() {
            // GIVEN: Tài khoản Admin với đầy đủ thông tin xác thực
            String username = "admin_user";
            String rawPass = "AdminPass123";
            String rawCode = "Code@Secret2024";
            
            String hashedPass = PasswordHasher.hashPassword(rawPass);
            String hashedCode = PasswordHasher.hashPassword(rawCode);

            // Admin model yêu cầu hashedCode phải thỏa mãn regex đặc biệt (do code hiện tại đang validate hash bằng regex raw)
            // Lưu ý: Tôi dùng hashedCode thỏa mãn regex để tránh lỗi constructor Admin
            Admin realAdmin = new Admin(username, hashedPass, "Hệ Thống", "admin@test.com", Admin.AccessLevel.SUPER_ADMIN, hashedCode);

            when(userDAO.findByUsername(username)).thenReturn(realAdmin);
            when(jwtService.generateToken(any())).thenReturn("mock-admin-token");

            // WHEN
            LoginResponseDTO result = authService.loginAdmin(new LoginRequestDTO(username, rawPass, rawCode));

            // THEN
            assertNotNull(result);
            assertEquals(username, result.getUsername());
        }

        @Test
        @DisplayName("Admin đăng nhập thất bại - Sai mã Admin Code")
        void testLoginAdmin_WrongCode() {
            String username = "admin_user";
            String rawPass = "Pass";
            String correctCode = "Code@12345";
            
            Admin realAdmin = new Admin(username, PasswordHasher.hashPassword(rawPass), "A", "a@t.com", Admin.AccessLevel.MODERATOR, PasswordHasher.hashPassword(correctCode));
            when(userDAO.findByUsername(username)).thenReturn(realAdmin);

            // WHEN & THEN: Nhập đúng pass nhưng sai admin code
            BusinessException ex = assertThrows(BusinessException.class, () -> 
                authService.loginAdmin(new LoginRequestDTO(username, rawPass, "WrongCode@123"))
            );
            assertEquals("Invalid admin security code.", ex.getMessage());
        }

        @Test
        @DisplayName("Admin đăng nhập thất bại - Tài khoản không có quyền Admin")
        void testLoginAdmin_NotAnAdmin() {
            String username = "regular_bidder";
            // Đối tượng thật là Bidder, không phải Admin
            User bidder = new Bidder(username, PasswordHasher.hashPassword("pass"), "Tên", "e@t.com", "HN", "0912587548");
            when(userDAO.findByUsername(username)).thenReturn(bidder);

            BusinessException ex = assertThrows(BusinessException.class, () -> 
                authService.loginAdmin(new LoginRequestDTO(username, "pass", "code"))
            );
            assertEquals("Access denied. Admin privileges required.", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ Đổi mật khẩu (Change Password)")
    class ChangePasswordTests {

        @Test
        @DisplayName("Đổi mật khẩu thành công")
        void testChangePassword_Success() {
            // GIVEN: User hiện tại mật khẩu là "OldPass"
            String userId = "u-123";
            String oldPass = "OldPass@123";
            String newPass = "NewPass@456";
            User realUser = new Bidder(userId, LocalDateTime.now(), "user1", PasswordHasher.hashPassword(oldPass), "Tên", "e@t.com", BigDecimal.ZERO, "HN", "091274757");

            when(userDAO.findById(userId)).thenReturn(realUser);

            // WHEN
            authService.changePassword(userId, oldPass, newPass);

            // THEN: Mật khẩu mới phải được verify thành công
            assertTrue(realUser.verifyPassword(newPass), "Mật khẩu mới không khớp sau khi đổi");
            verify(userDAO).update(realUser);
        }

        @Test
        @DisplayName("Đổi mật khẩu thất bại - Sai mật khẩu cũ")
        void testChangePassword_WrongOldPassword() {
            String userId = "u-123";
            User realUser = new Bidder(userId, LocalDateTime.now(), "uftrhgf", PasswordHasher.hashPassword("CorrectOld"), "Ththnjf", "ehdg@gmail.com", BigDecimal.ZERO, "H", "096364327");
            when(userDAO.findById(userId)).thenReturn(realUser);

            // WHEN & THEN: Nhập sai mật khẩu cũ
            BusinessException ex = assertThrows(BusinessException.class, () -> 
                authService.changePassword(userId, "WrongOld", "NewPass")
            );
            assertEquals("Authentication failed. Current password incorrect.", ex.getMessage());
            verify(userDAO, never()).update(any());
        }
    }
}
