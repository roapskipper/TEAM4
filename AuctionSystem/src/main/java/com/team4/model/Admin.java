package com.team4.model;

import com.team4.util.PasswordHasher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * Lớp Admin - Quản trị viên hệ thống.
 * Kế thừa từ User
 */
public class Admin extends User {
    private static final long serialVersionUID = 1L;
    // Dùng enum cho 2 kiểu Admin
    public enum AccessLevel {
        MODERATOR(1),
        SUPER_ADMIN(2);

        private final int level;

        AccessLevel(int level) {
            this.level = level;
        }
        // Getter để lấy giá trị số của cấp độ
        public int getLevel() {
            return level;
        }
    }

    public static AccessLevel fromInt(int code) {
        return AccessLevel.values()[code-1];
    }
    private static final Pattern ADMIN_CODE_PATTERN = Pattern.compile("^(?=.{8,128}$)(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9])\\S+$");
    private AccessLevel accessLevel;
    private String adminCodeHash; // Mã định danh bảo mật riêng

    // Constructor 1: Dùng khi tạo Admin mới trong hệ thống
    public Admin(String username, String passwordHash, String fullName, String email, AccessLevel accessLevel, String adminCodeHash) {
        super(username, passwordHash, fullName, email, Role.ADMIN );
        this.accessLevel = accessLevel;
        this.adminCodeHash = adminCodeHash;
        validateAdminInfo();
        // Tự động dùng constructor mặc định của Entity để sinh UUID và creatAt
    }

    // Constructor 2: Dùng khi nạp Admin từ database (đã có ID và balance)
    public Admin(String id, LocalDateTime creatAt, String username, String passwordHash, String fullName,
                 String email, BigDecimal balance, AccessLevel accessLevel, String adminCodeHash) {
        super(id, creatAt, username, passwordHash, fullName, email, Role.ADMIN, balance);
        this.accessLevel = accessLevel;
        this.adminCodeHash = adminCodeHash;
        validateAdminInfo();
    }

    // Kiểm tra định dạng của accessLevel và adminCode
    private void validateAdminInfo() {
        if (accessLevel == null) {
            throw new IllegalArgumentException("accessLevel không được null.");
        }
        // Admincode phải có ít nhất 1 chữ thường, 1 chữ hoa, 1 chữ số, 1 ký tự đặc biệt; không chứa khoảng trắng
        if (adminCodeHash == null || !ADMIN_CODE_PATTERN.matcher(adminCodeHash.trim()).matches()) {
            throw new IllegalArgumentException("adminCode phải gồm 8-128 kí tự, chứa ít nhất 1 chữ hoa, 1 chữ thường, 1 chữ số và 1 ký tự đặc biệt; không chứa khoảng trắng.");
        }
    }

    public boolean verifyAdminCode(String rawAdminCode) {
        // Gọi class tiện ích PasswordHasher để kiểm tra
        return PasswordHasher.checkPassword(rawAdminCode, this.adminCodeHash);
    }

    // Những quyền của Admin sẽ do Service quản lý
    // Hiển thị thông tin của Admin và quyền hạn của họ (Dùng Polymorphism để hiển thị khác nhau giữa các loại User)
    @Override
    public String toString() {
        return super.toString() + " | accessLevel: " + accessLevel ;
    }

    // Setter/Getter. Việc điều chỉnh Access Level và Admin Code sẽ để cho service
    public AccessLevel getAccessLevel() { return accessLevel; }
    /**
     * Chỉ dùng cho DAO - không expose ra ngoài
     */
    public String getAdminCodeHash() {
        return adminCodeHash;
    }
}
