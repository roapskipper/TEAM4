package com.team4.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests cho PasswordHasher.
 * Kiểm tra tính đúng đắn của BCrypt hash + verify.
 */
@DisplayName("Kiểm thử tiện ích mã hoá mật khẩu (PasswordHasher)")
public class PasswordHasherTest {

    // =========================================================================
    // hashPassword
    // =========================================================================

    @Test
    @DisplayName("Hash không được bằng plaintext gốc")
    void hash_notEqualToPlaintext() {
        String plain = "MySecret@123";
        String hashed = PasswordHasher.hashPassword(plain);

        assertNotNull(hashed);
        assertNotEquals(plain, hashed, "Hash phải khác plaintext");
    }

    @Test
    @DisplayName("Hash có định dạng BCrypt ($2a$...)")
    void hash_hasBcryptFormat() {
        String hashed = PasswordHasher.hashPassword("AnyPassword1!");
        assertTrue(hashed.startsWith("$2"), "BCrypt hash phải bắt đầu bằng $2");
    }

    @RepeatedTest(3)
    @DisplayName("Hash cùng một password hai lần phải ra kết quả khác nhau (random salt)")
    void hash_differentSaltEachTime() {
        String plain = "SamePassword@99";
        String hash1 = PasswordHasher.hashPassword(plain);
        String hash2 = PasswordHasher.hashPassword(plain);

        assertNotEquals(hash1, hash2,
                "Mỗi lần hash phải sinh salt ngẫu nhiên → hash khác nhau");
    }

    // =========================================================================
    // checkPassword
    // =========================================================================

    @Test
    @DisplayName("checkPassword – đúng mật khẩu → true")
    void checkPassword_correct_returnsTrue() {
        String plain = "CorrectPass@1";
        String hashed = PasswordHasher.hashPassword(plain);

        assertTrue(PasswordHasher.checkPassword(plain, hashed),
                "Mật khẩu đúng phải trả về true");
    }

    @Test
    @DisplayName("checkPassword – sai mật khẩu → false")
    void checkPassword_wrong_returnsFalse() {
        String hashed = PasswordHasher.hashPassword("OriginalPass@1");

        assertFalse(PasswordHasher.checkPassword("WrongPass@1", hashed),
                "Mật khẩu sai phải trả về false");
    }

    @Test
    @DisplayName("checkPassword – chuỗi rỗng sai hash → false")
    void checkPassword_emptyPlaintext_returnsFalse() {
        String hashed = PasswordHasher.hashPassword("SomePass@1");

        // BCrypt.checkpw sẽ trả false (không ném exception) với empty string
        assertFalse(PasswordHasher.checkPassword("", hashed));
    }

    @Test
    @DisplayName("checkPassword – trường hợp case-sensitive")
    void checkPassword_caseSensitive() {
        String plain = "Password@123";
        String hashed = PasswordHasher.hashPassword(plain);

        assertFalse(PasswordHasher.checkPassword("password@123", hashed),
                "BCrypt phân biệt chữ hoa/thường");
        assertFalse(PasswordHasher.checkPassword("PASSWORD@123", hashed));
    }

    @Test
    @DisplayName("checkPassword – password có ký tự đặc biệt")
    void checkPassword_specialCharacters() {
        String plain = "P@$$w0rd!#^&*()";
        String hashed = PasswordHasher.hashPassword(plain);

        assertTrue(PasswordHasher.checkPassword(plain, hashed));
        assertFalse(PasswordHasher.checkPassword("P@$$w0rd!#^&*()", PasswordHasher.hashPassword("other")));
    }
}
