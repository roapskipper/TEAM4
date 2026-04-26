package com.team4.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * Lớp Admin - Quản trị viên hệ thống.
 * Kế thừa từ User (Thể hiện tính Inheritance và Abstraction)
 */
public class Admin extends User {
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
    private static final Pattern ADMIN_CODE_PATTERN = Pattern.compile("^(?=.{8,128}$)(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9])\\S+$");
    private AccessLevel accessLevel;
    private String adminCode; // Mã định danh bảo mật riêng

    // Constructor 1: Dùng khi tạo Admin mới trong hệ thống
    public Admin(String username, String passwordHash, String fullName, String email, AccessLevel accessLevel, String adminCode) {
        super(username, passwordHash, fullName, email, Role.ADMIN );
        this.accessLevel = accessLevel;
        this.adminCode = adminCode;
        validateAdminInfo();
        // Tự động dùng constructor mặc định của Entity để sinh UUID và creatAt
    }

    // Constructor 2: Dùng khi nạp Admin từ database (đã có ID và balance)
    public Admin(String id, LocalDateTime creatAt, String username, String passwordHash, String fullName,
                 String email, BigDecimal balance, AccessLevel accessLevel, String adminCode) {
        super(id, creatAt, username, passwordHash, fullName, email, Role.ADMIN, balance);
        this.accessLevel = accessLevel;
        this.adminCode = adminCode;
        validateAdminInfo();
    }

    // Kiểm tra định dạng của accessLevel và adminCode
    private void validateAdminInfo() {
        if (accessLevel == null) {
            throw new IllegalArgumentException("accessLevel không được null.");
        }
        // Admincode phải có ít nhất 1 chữ thường, 1 chữ hoa, 1 chữ số, 1 ký tự đặc biệt; không chứa khoảng trắng
        if (adminCode == null || !ADMIN_CODE_PATTERN.matcher(adminCode.trim()).matches()) {
            throw new IllegalArgumentException("adminCode phải gồm 8-128 kí tự, chứa ít nhất 1 chữ hoa, 1 chữ thường, 1 chữ số và 1 ký tự đặc biệt; không chứa khoảng trắng.");
        }
    }
    // Những quyền của Admin sẽ do Service quản lý
    // Hiển thị thông tin của Admin và quyền hạn của họ (Dùng Polymorphism để hiển thị khác nhau giữa các loại User)
    @Override
    public String toString() {
        return super.toString() + ", accessLevel: " + accessLevel;
    }

    // Setter/Getter. Việc điều chỉnh Access Level và Admin Code sẽ để cho service
    public AccessLevel getAccessLevel() { return accessLevel; }
}