package com.team4.service;

import com.team4.dao.UserDAO;
import com.team4.dao.impl.UserDAOImpl;
import com.team4.dto.auth.RegisterBidderRequestDTO;
import com.team4.model.User;
import com.team4.util.PasswordHasher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử tích hợp nghiệp vụ Xác thực (AuthenticationService).
 * Kiểm tra luồng dữ liệu từ Service xuống Database thật.
 */
@DisplayName("Integration Tests for AuthenticationService")
public class AuthenticationIntegrationTest extends BaseServiceIntegrationTest {

    private final UserDAO userDAO = new UserDAOImpl();
    private final AuthenticationService authService = new AuthenticationService(userDAO, new JwtService());

    @Test
    @DisplayName("Đăng ký Bidder thành công và kiểm tra dữ liệu trong Database")
    void testRegisterBidder_FullFlow() {
        // GIVEN: Một yêu cầu đăng ký hợp lệ
        String username = "integration_tester";
        RegisterBidderRequestDTO request = new RegisterBidderRequestDTO(
                username, "Integration User", "Password123@", "integration@test.com", "Hanoi, Vietnam", "0987654321"
        );

        // WHEN: Gọi nghiệp vụ đăng ký (Không dùng Mock)
        authService.registerBidder(request);

        // THEN: Truy vấn trực tiếp vào DB qua DAO để kiểm chứng "sự thật"
        User savedUser = userDAO.findByUsername(username);
        
        assertNotNull(savedUser, "User should be saved in the database");
        assertEquals("Integration User", savedUser.getFullName());
        assertEquals("integration@test.com", savedUser.getEmail());
        assertEquals(User.Role.BIDDER, savedUser.getRole());
        
        // Kiểm tra bảo mật: Mật khẩu lưu xuống phải được băm, không phải chuỗi thô
        assertNotEquals("Password123@", savedUser.readPasswordHashForPersistence());
        assertTrue(savedUser.verifyPassword("Password123@"), "Password should be verifiable with the original raw password");
    }
}
