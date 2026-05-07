package com.team4.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Class này để băm và kiểm tra mật khẩu
 */
public class PasswordHasher {
    // Dùng khi đăng kí
    public static String hashPassword(String plainTextPassword) {
        // Hệ thống tự động sinh salt và băm
        return BCrypt.hashpw(plainTextPassword, BCrypt.gensalt());
    }

    // Dùng khi đăng nhập
    public static boolean checkPassword(String plainTextPassword, String hashedPasswordFromDB) {
        // So sánh mật khẩu nhập vào với hash đã lưu
        return BCrypt.checkpw(plainTextPassword ,hashedPasswordFromDB);
    }
}
