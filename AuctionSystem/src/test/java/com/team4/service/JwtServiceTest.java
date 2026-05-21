package com.team4.service;

import com.team4.model.Bidder;
import com.team4.model.User;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử nghiệp vụ tạo và giải mã JWT.
 */
@DisplayName("Unit Tests for JwtService")
public class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
    }

    @Test
    @DisplayName("Tạo và giải mã Token thành công")
    void testJwtFlow_Success() {
        // GIVEN: Một người dùng hợp lệ
        User user = new Bidder("u123", LocalDateTime.now(), "tester", "hash", "Tester", "test@t.com", BigDecimal.ZERO, "A", "0912345678");

        // WHEN: Tạo token
        String token = jwtService.generateToken(user);
        assertNotNull(token);

        // THEN: Giải mã token phải trích xuất đúng thông tin (Claims)
        Claims claims = jwtService.getClaimsFromToken(token);
        assertEquals("tester", claims.getSubject());
        assertEquals("u123", claims.get("userId"));
        assertEquals(User.Role.BIDDER.name(), claims.get("role"));
    }

    @Test
    @DisplayName("Thất bại khi giải mã Token không hợp lệ")
    void testGetClaimsFromToken_Invalid() {
        assertThrows(Exception.class, () -> jwtService.getClaimsFromToken("invalid_token_string"));
    }
}
