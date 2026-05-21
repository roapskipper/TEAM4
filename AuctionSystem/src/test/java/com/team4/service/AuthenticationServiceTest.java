package com.team4.service;

import com.team4.dao.UserDAO;
import com.team4.dto.auth.*;
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
 * Kiểm thử nghiệp vụ AuthenticationService.
 * Đảm bảo các luồng Đăng ký, Đăng nhập và Đổi mật khẩu hoạt động đúng với cấu trúc DTO/Mapper mới.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests for AuthenticationService")
public class AuthenticationServiceTest {

    @Mock
    private UserDAO userDAO;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthenticationService authService;

    @Nested
    @DisplayName("Nghiệp vụ Đăng ký (Registration)")
    class RegistrationTests {

        @Test
        @DisplayName("Đăng ký Bidder thành công")
        void testRegisterBidder_Success() {
            // GIVEN: Yêu cầu đăng ký hợp lệ và username chưa tồn tại
            RegisterBidderRequestDTO request = new RegisterBidderRequestDTO(
                    "new_bidder", "New User", "Password123", "bidder@test.com", "Address", "0912345678"
            );
            when(userDAO.findByUsername(request.getUsername())).thenReturn(null);
            when(userDAO.findByEmail(request.getEmail())).thenReturn(null);
            when(userDAO.insert(any(Bidder.class))).thenReturn(true);

            // WHEN: Gọi nghiệp vụ đăng ký
            authService.registerBidder(request);

            // THEN: Phải lưu người dùng vào database
            verify(userDAO).insert(any(Bidder.class));
        }

        @Test
        @DisplayName("Đăng ký Seller thành công")
        void testRegisterSeller_Success() {
            // GIVEN
            RegisterSellerRequestDTO request = new RegisterSellerRequestDTO(
                    "new_seller", "Password123", "New Seller", "seller@test.com", "My Shop"
            );
            when(userDAO.findByUsername(request.getUsername())).thenReturn(null);
            when(userDAO.findByEmail(request.getEmail())).thenReturn(null);
            when(userDAO.insert(any(Seller.class))).thenReturn(true);

            // WHEN
            authService.registerSeller(request);

            // THEN
            verify(userDAO).insert(any(Seller.class));
        }

        @Test
        @DisplayName("Đăng ký thất bại khi trùng tên đăng nhập")
        void testRegister_DuplicateUsername() {
            // GIVEN: Username đã tồn tại trong DB
            RegisterBidderRequestDTO request = new RegisterBidderRequestDTO("existing", "Name", "Pass123", "e@t.com", "A", "0912345678");
            when(userDAO.findByUsername("existing")).thenReturn(mock(User.class));

            // WHEN & THEN: Kiểm tra ném lỗi Duplicate
            BusinessException ex = assertThrows(BusinessException.class, () -> authService.registerBidder(request));
            assertEquals("Username already exists.", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ Đăng nhập (Login)")
    class LoginTests {

        @Test
        @DisplayName("Bidder đăng nhập thành công")
        void testLoginBidder_Success() {
            // GIVEN: Bidder tồn tại và mật khẩu chính xác
            String username = "bidder_test";
            String pass = "Pass123";
            String hashed = PasswordHasher.hashPassword(pass);
            Bidder bidder = new Bidder(username, hashed, "Bidder User", "b@t.com", "Addr", "0912345678");

            LoginRequestDTO request = new LoginRequestDTO(username, pass, null);
            when(userDAO.findByUsername(username)).thenReturn(bidder);
            when(jwtService.generateToken(bidder)).thenReturn("mock_token");

            // WHEN: Thực hiện đăng nhập
            LoginResponseDTO response = authService.loginBidder(request);

            // THEN: Trả về DTO kèm token
            assertNotNull(response);
            assertEquals("mock_token", response.getToken());
            assertEquals(User.Role.BIDDER, response.getRole());
        }

        @Test
        @DisplayName("Bidder đăng nhập thất bại khi sai mật khẩu")
        void testLoginBidder_WrongPassword() {
            // GIVEN: User tồn tại nhưng nhập sai pass
            String username = "user";
            Bidder bidder = new Bidder(username, PasswordHasher.hashPassword("correct"), "A", "a@t.com", "A", "0912345678");

            LoginRequestDTO request = new LoginRequestDTO(username, "wrong", null);
            when(userDAO.findByUsername(username)).thenReturn(bidder);

            // WHEN & THEN: Phải ném lỗi Invalid credentials
            BusinessException ex = assertThrows(BusinessException.class, () -> authService.loginBidder(request));
            assertEquals("Invalid username or password.", ex.getMessage());
        }

        @Test
        @DisplayName("Seller đăng nhập thất bại khi sử dụng tài khoản Bidder")
        void testLoginSeller_WithBidderAccount() {
            // GIVEN: Tài khoản là Bidder
            String username = "not_a_seller";
            Bidder bidder = new Bidder(username, PasswordHasher.hashPassword("pass"), "A", "a@t.com", "A", "0912345678");

            LoginRequestDTO request = new LoginRequestDTO(username, "pass", null);
            when(userDAO.findByUsername(username)).thenReturn(bidder);

            // WHEN & THEN: Phải ném lỗi sai vai trò (This account is not registered as a Seller.)
            BusinessException ex = assertThrows(BusinessException.class, () -> authService.loginSeller(request));
            assertTrue(ex.getMessage().contains("is not registered as a Seller"));
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ Đăng nhập Admin")
    class AdminLoginTests {

        @Test
        @DisplayName("Admin đăng nhập thành công với mã AdminCode")
        void testLoginAdmin_Success() {
            // GIVEN: Tài khoản Admin hợp lệ
            String username = "admin";
            String pass = "AdminPass123";
            String code = "Secret@Code2024";

            Admin admin = new Admin(username, PasswordHasher.hashPassword(pass), "Admin", "adm@t.com",
                    Admin.AccessLevel.SUPER_ADMIN, PasswordHasher.hashPassword(code));

            LoginRequestDTO request = new LoginRequestDTO(username, pass, code);
            when(userDAO.findByUsername(username)).thenReturn(admin);
            when(jwtService.generateToken(admin)).thenReturn("admin_token");

            // WHEN: Thực hiện đăng nhập Admin
            LoginResponseDTO response = authService.loginAdmin(request);

            // THEN: Thành công
            assertNotNull(response);
            assertEquals("admin_token", response.getToken());
        }

        @Test
        @DisplayName("Admin đăng nhập thất bại khi sai mã bảo mật")
        void testLoginAdmin_WrongCode() {
            String username = "admin";
            Admin admin = new Admin(username, PasswordHasher.hashPassword("pass"), "A", "a@t.com",
                    Admin.AccessLevel.MODERATOR, PasswordHasher.hashPassword("correct_code"));

            LoginRequestDTO request = new LoginRequestDTO(username, "pass", "wrong_code");
            when(userDAO.findByUsername(username)).thenReturn(admin);

            // WHEN & THEN: Lỗi sai admin code
            BusinessException ex = assertThrows(BusinessException.class, () -> authService.loginAdmin(request));
            assertEquals("Invalid admin security code.", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ Đổi mật khẩu (Change Password)")
    class ChangePasswordTests {

        @Test
        @DisplayName("Đổi mật khẩu thành công")
        void testChangePassword_Success() {
            // GIVEN: User tồn tại và nhập đúng pass cũ
            String userId = "u-1";
            String oldPass = "Old@123456";
            String newPass = "New@123456";
            User user = new Bidder(userId, LocalDateTime.now(), "user", PasswordHasher.hashPassword(oldPass), "N", "e@t.com", BigDecimal.ZERO, "A", "0912345678");

            when(userDAO.findById(userId)).thenReturn(user);
            when(userDAO.update(any())).thenReturn(true);

            // WHEN: Đổi mật khẩu
            authService.changePassword(userId, oldPass, newPass);

            // THEN: Xác nhận cập nhật DB
            verify(userDAO).update(user);
            assertTrue(user.verifyPassword(newPass));
        }

        @Test
        @DisplayName("Đổi mật khẩu thất bại khi mật khẩu mới quá ngắn")
        void testChangePassword_TooShort() {
            String userId = "u-1";
            String oldPass = "OldPass";
            User user = new Bidder(userId, LocalDateTime.now(), "user", PasswordHasher.hashPassword(oldPass), "N", "e@t.com", BigDecimal.ZERO, "A", "0912345678");
            when(userDAO.findById(userId)).thenReturn(user);

            // WHEN & THEN: Lỗi độ dài pass mới (phải >= 6)
            BusinessException ex = assertThrows(BusinessException.class, () -> authService.changePassword(userId, oldPass, "12345"));
            assertEquals("New password must be at least 6 characters long.", ex.getMessage());
        }
    }
}
