package com.team4.model;

import java.io.Serializable;
import java.util.UUID;

/**
 * Lớp trừu tượng User - Đại diện cho người dùng trong hệ thống.
 * Kế thừa từ Entity để có sẵn thuộc tính String id (UUID).
 */
public abstract class User extends Entity implements Serializable {
    private static final long serialVersionUID = 1L;

    // Các thuộc tính bảo vệ (Encapsulation)
    protected String username;
    protected String password;
    protected String fullName;
    protected String email;
    protected String role;      // "ADMIN", "SELLER", "BUYER"
    protected double balance;   // Số dư tài khoản để đấu giá

    /**
     * Constructor dùng khi tạo User mới hoàn toàn
     * Tự động tạo UUID và gán role mặc định
     */
    public User(String username, String password, String role) {
        // Gọi constructor của Entity, truyền vào một UUID mới
        super(UUID.randomUUID().toString());
        this.username = username;
        this.password = password;
        this.role = role;
        this.balance = 0.0;
    }

    /**
     * Constructor đầy đủ dùng khi lấy dữ liệu từ Database lên (Mapping DAO)
     */
    public User(String id, String username, String password, String fullName, String email, String role, double balance) {
        super(id); // Truyền ID cũ từ DB vào lớp Entity
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.balance = balance;
    }

    // --- PHƯƠNG THỨC TRỪU TƯỢNG (ABSTRACTION & POLYMORPHISM) ---
    /**
     * Mỗi loại User (Seller/Buyer/Admin) sẽ có quyền hạn hiển thị khác nhau.
     * Bắt buộc các lớp con phải triển khai (Override).
     */
    public abstract void displayRolePermissions();

    // --- GETTERS & SETTERS (ENCAPSULATION) ---

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    /**
     * Tăng số dư tài khoản
     */
    public void deposit(double amount) {
        if (amount > 0) this.balance += amount;
    }

    /**
     * Trừ tiền (Ví dụ khi thắng đấu giá)
     */
    public boolean withdraw(double amount) {
        if (this.balance >= amount) {
            this.balance -= amount;
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "User [" + role + "] - ID: " + getId() + ", Username: " + username + ", Balance: $" + balance;
    }
}