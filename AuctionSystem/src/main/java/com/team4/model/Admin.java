package com.team4.model;

import java.io.Serializable;

/**
 * Lớp Admin - Quản trị viên hệ thống.
 * Kế thừa từ User (Thể hiện tính Inheritance và Abstraction)
 */
public class Admin extends User implements Serializable {
    private int accessLevel;  // 1: Moderator, 2: Super Admin
    private String adminCode; // Mã định danh bảo mật riêng

    /**
     * CONSTRUCTOR 1: Dùng khi tạo một Admin mới
     * Tự động gán role "ADMIN" và sinh UUID.
     */
    public Admin(String username, String password, int accessLevel, String adminCode) {
        super(username, password, "ADMIN");
        this.accessLevel = accessLevel;
        this.adminCode = adminCode;
    }

    /**
     * CONSTRUCTOR 2: Dùng khi DAO nạp dữ liệu từ MySQL lên
     */
    public Admin(String id, String username, String password, String fullName,
                 String email, double balance, int accessLevel, String adminCode) {
        super(id, username, password, fullName, email, "ADMIN", balance);
        this.accessLevel = accessLevel;
        this.adminCode = adminCode;
    }

    // --- CÁC PHƯƠNG THỨC NGHIỆP VỤ CỦA ADMIN ---

    public void banUser(User user) {
        if (user != null) {
            System.out.println("[ADMIN ACTION] Quản trị viên " + username +
                    " đã khóa tài khoản: " + user.getUsername() + " (ID: " + user.getId() + ")");
        }
    }

    // Ghi chú: Đảm bảo lớp Item đã có phương thức getName()
    public void removeInvalidItem(Item item) {
        if (item != null) {
            System.out.println("[ADMIN ACTION] Quản trị viên " + username +
                    " đã xóa sản phẩm vi phạm: " + item.getName());
        }
    }

    // --- TRIỂN KHAI ĐA HÌNH (POLYMORPHISM) ---

    @Override
    public void displayRolePermissions() {
        System.out.println("\n========== [ QUYỀN HẠN QUẢN TRỊ ] ==========");
        System.out.println("Admin      : " + username);
        System.out.println("ID Hệ thống: " + getId());
        System.out.println("Cấp độ     : " + (accessLevel == 2 ? "Super Admin" : "Moderator"));
        System.out.println("Mã bảo mật : " + adminCode);
        System.out.println("Quyền hạn  : Khóa tài khoản, Xóa sản phẩm, Xem báo cáo.");
        System.out.println("==============================================\n");
    }

    // --- GETTERS & SETTERS ---

    public int getAccessLevel() { return accessLevel; }
    public void setAccessLevel(int accessLevel) { this.accessLevel = accessLevel; }
    public String getAdminCode() { return adminCode; }
    public void setAdminCode(String adminCode) { this.adminCode = adminCode; }
}