package com.team4.dto;

import com.team4.dto.auth.RegisterBidderRequestDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử logic ràng buộc (Validation) trong các DTO.
 */
@DisplayName("Unit Tests for DTO Validation")
public class DTOTest {

    @Test
    @DisplayName("RegisterBidderRequestDTO - Kiểm tra dữ liệu hợp lệ")
    void testRegisterBidderValidation_Success() {
        assertDoesNotThrow(() -> {
            new RegisterBidderRequestDTO("validUser", "Full Name", "Pass123", "valid@email.com", "Hanoi", "0912345678");
        });
    }

    @Test
    @DisplayName("RegisterBidderRequestDTO - Thất bại khi sai định dạng Email")
    void testRegisterBidderValidation_InvalidEmail() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new RegisterBidderRequestDTO("user1", "Name", "Pass123", "invalid-email", "Addr", "0912345678");
        });
        assertTrue(exception.getMessage().contains("email"));
    }

    @Test
    @DisplayName("RegisterBidderRequestDTO - Thất bại khi mật khẩu quá ngắn")
    void testRegisterBidderValidation_ShortPassword() {
        assertThrows(IllegalArgumentException.class, () -> {
            new RegisterBidderRequestDTO("user1", "Name", "123", "v@e.com", "Addr", "0912345678");
        });
    }

    @Test
    @DisplayName("RegisterBidderRequestDTO - Thất bại khi sai định dạng Username")
    void testRegisterBidderValidation_InvalidUsername() {
        assertThrows(IllegalArgumentException.class, () -> {
            new RegisterBidderRequestDTO("u", "Name", "Pass123", "v@e.com", "Addr", "0912345678");
        });
    }
}
