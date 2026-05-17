package com.team4.model;

import com.team4.util.PasswordHasher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests cho validation của model Admin.
 * Tập trung vào adminCodeHash regex và logic verifyAdminCode.
 */
@DisplayName("Kiểm thử validation Admin")
public class AdminValidationTest {

    private static final String VALID_PASSWORD_HASH = "$2a$12$dummyhashvaluefortest12345678901";

    // Helper tạo Admin hợp lệ
    private Admin validAdmin(String adminCode) {
        return new Admin(
                "admin01", VALID_PASSWORD_HASH,
                "Admin User", "admin@example.com",
                Admin.AccessLevel.SUPER_ADMIN, adminCode
        );
    }

    // =========================================================================
    // adminCode format validation (constructor 1 – tạo mới)
    // =========================================================================
    @Nested
    @DisplayName("adminCode format validation")
    class AdminCodeFormatTests {

        @Test
        @DisplayName("adminCode hợp lệ → OK")
        void adminCode_valid() {
            // Có hoa, thường, số, ký tự đặc biệt, 8-128 ký tự
            assertDoesNotThrow(() -> validAdmin("Admin@123"));
        }

        @ParameterizedTest(name = "adminCode=\"{0}\"")
        @ValueSource(strings = {
                "short1!",          // < 8 ký tự
                "alllowercase1!",   // thiếu chữ hoa
                "ALLUPPERCASE1!",   // thiếu chữ thường
                "NoNumbers!Abc",    // thiếu số
                "NoSpecial1Abc",    // thiếu ký tự đặc biệt
                "Has Space@1Abc",   // có khoảng trắng
        })
        @DisplayName("adminCode không hợp lệ → IllegalArgumentException")
        void adminCode_invalid(String code) {
            assertThrows(IllegalArgumentException.class, () -> validAdmin(code));
        }

        @Test
        @DisplayName("adminCode null → IllegalArgumentException")
        void adminCode_null() {
            assertThrows(IllegalArgumentException.class, () -> validAdmin(null));
        }

        @Test
        @DisplayName("adminCode 128 ký tự (giới hạn trên) → OK")
        void adminCode_maxLength() {
            // Tạo chuỗi 128 ký tự đáp ứng tất cả yêu cầu
            String maxCode = "Admin@1" + "a".repeat(121);  // 7 + 121 = 128
            assertDoesNotThrow(() -> validAdmin(maxCode));
        }
    }

    // =========================================================================
    // AccessLevel
    // =========================================================================
    @Nested
    @DisplayName("AccessLevel validation")
    class AccessLevelTests {

        @Test
        @DisplayName("AccessLevel null → IllegalArgumentException")
        void accessLevel_null() {
            assertThrows(IllegalArgumentException.class, () ->
                    new Admin("admin01", VALID_PASSWORD_HASH,
                            "Admin", "admin@example.com", null, "Admin@123"));
        }

        @Test
        @DisplayName("SUPER_ADMIN.getLevel() = 2")
        void accessLevel_superAdmin_levelIs2() {
            assertEquals(2, Admin.AccessLevel.SUPER_ADMIN.getLevel());
        }

        @Test
        @DisplayName("MODERATOR.getLevel() = 1")
        void accessLevel_moderator_levelIs1() {
            assertEquals(1, Admin.AccessLevel.MODERATOR.getLevel());
        }
    }

    // =========================================================================
    // verifyAdminCode
    // =========================================================================
    @Nested
    @DisplayName("verifyAdminCode logic")
    class VerifyAdminCodeTests {

        @Test
        @DisplayName("Đúng mã → true")
        void verifyAdminCode_correct_returnsTrue() {
            String rawCode = "Admin@12345";
            // Tạo Admin với hash của rawCode
            String hashedCode = PasswordHasher.hashPassword(rawCode);
            Admin admin = new Admin(
                    "id1", LocalDateTime.now(),
                    "admin01", VALID_PASSWORD_HASH,
                    "Admin", "admin@example.com",
                    BigDecimal.ZERO, Admin.AccessLevel.SUPER_ADMIN, hashedCode
            );

            assertTrue(admin.verifyAdminCode(rawCode));
        }

        @Test
        @DisplayName("Sai mã → false")
        void verifyAdminCode_wrong_returnsFalse() {
            String hashedCode = PasswordHasher.hashPassword("Admin@12345");
            Admin admin = new Admin(
                    "id1", LocalDateTime.now(),
                    "admin01", VALID_PASSWORD_HASH,
                    "Admin", "admin@example.com",
                    BigDecimal.ZERO, Admin.AccessLevel.SUPER_ADMIN, hashedCode
            );

            assertFalse(admin.verifyAdminCode("WrongCode@999"));
        }
    }

    // =========================================================================
    // fromInt helper
    // =========================================================================
    @Nested
    @DisplayName("Admin.fromInt() helper")
    class FromIntTests {

        @Test
        @DisplayName("fromInt(1) → MODERATOR")
        void fromInt_1_returnsModerator() {
            assertEquals(Admin.AccessLevel.MODERATOR, Admin.fromInt(1));
        }

        @Test
        @DisplayName("fromInt(2) → SUPER_ADMIN")
        void fromInt_2_returnsSuperAdmin() {
            assertEquals(Admin.AccessLevel.SUPER_ADMIN, Admin.fromInt(2));
        }
    }
}
