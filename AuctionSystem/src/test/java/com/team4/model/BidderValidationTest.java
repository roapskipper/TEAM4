package com.team4.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests cho validation của model Bidder.
 * Kiểm tra tất cả ràng buộc trong constructor và setter.
 */
@DisplayName("Kiểm thử validation Bidder")
public class BidderValidationTest {

    // Hash hợp lệ (BCrypt-like, non-blank)
    private static final String VALID_HASH = "$2a$12$dummyhashvaluefortest12345678901";

    // Helper tạo Bidder hợp lệ từ DB (constructor 2)
    private Bidder validBidder() {
        return new Bidder("bidder-id", LocalDateTime.now(),
                "validuser", VALID_HASH,
                "Nguyen Van A", "valid@example.com",
                BigDecimal.ZERO, "123 ABC St", "0912345678");
    }

    // =========================================================================
    // Username
    // =========================================================================
    @Nested
    @DisplayName("Username validation")
    class UsernameTests {

        @Test
        @DisplayName("Username hợp lệ (4-30 ký tự, chữ/số/.-_) → OK")
        void username_valid() {
            assertDoesNotThrow(() ->
                    new Bidder("user01", VALID_HASH, "Name", "a@b.com", null, null));
        }

        @ParameterizedTest(name = "username=\"{0}\"")
        @ValueSource(strings = {"abc", "ab", "x"})
        @DisplayName("Username < 4 ký tự → IllegalArgumentException")
        void username_tooShort(String username) {
            assertThrows(IllegalArgumentException.class, () ->
                    new Bidder(username, VALID_HASH, "Name", "a@b.com", null, null));
        }

        @Test
        @DisplayName("Username > 30 ký tự → IllegalArgumentException")
        void username_tooLong() {
            String longName = "a".repeat(31);
            assertThrows(IllegalArgumentException.class, () ->
                    new Bidder(longName, VALID_HASH, "Name", "a@b.com", null, null));
        }

        @ParameterizedTest(name = "username=\"{0}\"")
        @ValueSource(strings = {"user name", "user@name", "user#1", "nguyễn"})
        @DisplayName("Username có ký tự không hợp lệ → IllegalArgumentException")
        void username_invalidChars(String username) {
            assertThrows(IllegalArgumentException.class, () ->
                    new Bidder(username, VALID_HASH, "Name", "a@b.com", null, null));
        }
    }

    // =========================================================================
    // Email
    // =========================================================================
    @Nested
    @DisplayName("Email validation")
    class EmailTests {

        @ParameterizedTest(name = "email=\"{0}\"")
        @ValueSource(strings = {"notanemail", "missing@tld", "@nodomain.com", "no-at-sign"})
        @DisplayName("Email không hợp lệ → IllegalArgumentException")
        void email_invalid(String email) {
            assertThrows(IllegalArgumentException.class, () ->
                    new Bidder("user01", VALID_HASH, "Name", email, null, null));
        }

        @Test
        @DisplayName("Email hợp lệ → OK")
        void email_valid() {
            assertDoesNotThrow(() ->
                    new Bidder("user01", VALID_HASH, "Name", "user@example.com", null, null));
        }
    }

    // =========================================================================
    // PhoneNumber
    // =========================================================================
    @Nested
    @DisplayName("PhoneNumber validation")
    class PhoneTests {

        @ParameterizedTest(name = "phone=\"{0}\"")
        @ValueSource(strings = {"12345", "abcdefgh", "0912 345 678", "090-123-456"})
        @DisplayName("Số điện thoại không hợp lệ → IllegalArgumentException")
        void phone_invalid(String phone) {
            assertThrows(IllegalArgumentException.class, () ->
                    new Bidder("user01", VALID_HASH, "Name", "a@b.com", null, phone));
        }

        @ParameterizedTest(name = "phone=\"{0}\"")
        @ValueSource(strings = {"0912345678", "+84912345678", "0123456789"})
        @DisplayName("Số điện thoại hợp lệ → OK")
        void phone_valid(String phone) {
            assertDoesNotThrow(() ->
                    new Bidder("user01", VALID_HASH, "Name", "a@b.com", null, phone));
        }

        @Test
        @DisplayName("PhoneNumber null → OK (optional)")
        void phone_null_isOptional() {
            assertDoesNotThrow(() ->
                    new Bidder("user01", VALID_HASH, "Name", "a@b.com", null, null));
        }
    }

    // =========================================================================
    // ShippingAddress
    // =========================================================================
    @Nested
    @DisplayName("ShippingAddress validation")
    class AddressTests {

        @Test
        @DisplayName("Address null → OK (optional)")
        void address_null_isOptional() {
            assertDoesNotThrow(() ->
                    new Bidder("user01", VALID_HASH, "Name", "a@b.com", null, null));
        }

        @Test
        @DisplayName("Address > 255 ký tự → IllegalArgumentException")
        void address_tooLong() {
            String longAddr = "A".repeat(256);
            assertThrows(IllegalArgumentException.class, () ->
                    new Bidder("user01", VALID_HASH, "Name", "a@b.com", longAddr, null));
        }

        @Test
        @DisplayName("Address hợp lệ → OK")
        void address_valid() {
            assertDoesNotThrow(() ->
                    new Bidder("user01", VALID_HASH, "Name", "a@b.com", "123 Hanoi", null));
        }
    }

    // =========================================================================
    // Balance
    // =========================================================================
    @Test
    @DisplayName("Balance âm → IllegalArgumentException")
    void balance_negative_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                new Bidder("bidder-id", LocalDateTime.now(),
                        "user01", VALID_HASH, "Name", "a@b.com",
                        new BigDecimal("-1.00"), null, null));
    }

    @Test
    @DisplayName("Balance = 0 → OK")
    void balance_zero_isValid() {
        assertDoesNotThrow(() ->
                new Bidder("bidder-id", LocalDateTime.now(),
                        "user01", VALID_HASH, "Name", "a@b.com",
                        BigDecimal.ZERO, null, null));
    }
}
