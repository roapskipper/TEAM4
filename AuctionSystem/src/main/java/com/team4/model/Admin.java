package com.team4.model;

import java.io.Serializable;

public class Admin extends User implements Serializable {
    private int accessLevel; // Cấp độ truy cập (Ví dụ: 1 - Mod, 2 - Super Admin)
    private String adminCode; // Mã định danh riêng của quản trị viên

    public Admin(String id, String username, String password, int accessLevel, String adminCode, ) {
        super(id, username, password, "ADMIN");
        this.accessLevel = accessLevel;
        this.adminCode = adminCode;
    }

    public int getAccessLevel() { return accessLevel; }
    public void setAccessLevel(int accessLevel) { this.accessLevel = accessLevel; }

    public String getAdminCode() { return adminCode; }

    // Các phương thức nghiệp vụ của Admin (Chỉ mô phỏng bằng in ra màn hình)
    public void banUser(User user) {
        System.out.println("Admin " + username + " đã khóa tài khoản: " + user.getUsername());
    }

    public void removeInvalidItem(Item item) {
        System.out.println("Admin " + username + " đã xóa sản phẩm vi phạm: " + item.getName());
    }

    @Override
    public void displayRolePermissions() {
        System.out.println("--- [QUYỀN HẠN ADMIN] ---");
        System.out.println("Quản trị viên: " + username + " | Cấp độ: " + accessLevel);
        System.out.println("- Quyền hệ thống: Quản lý người dùng, Kiểm duyệt sản phẩm.");
        System.out.println("- Quyền dữ liệu: Xem toàn bộ lịch sử đấu giá và giao dịch.");
    }
}