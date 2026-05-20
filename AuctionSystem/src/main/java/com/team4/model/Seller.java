package com.team4.model;

import java.time.LocalDateTime;
import java.math.BigDecimal;
/**
 * Lớp Seller - Đại diện cho Người bán trong hệ thống.
 * Kế thừa từ User
 */
public class Seller extends User {
    private String storeName;
    private double rating;
    // Danh sách vật phẩm được bán sẽ được chuyển xuống cho DAO xử lý
    /**
     * CONSTRUCTOR 1: Dùng khi tạo một Seller mới
     * Tự động gán role "SELLER", rating 5.0.
     */
    public Seller(String username, String passwordHash, String fullName, String email, String storeName) {
        super(username, passwordHash, fullName, email, Role.SELLER);
        this.storeName = normalizeOptional(storeName);
        this.rating = 5.0;
        validateStoreName(this.storeName);
    }

    /**
     * CONSTRUCTOR 2: Dùng khi DAO lấy dữ liệu từ DB
     * Chứa đầy đủ thông tin.
     */
    public Seller(String id, LocalDateTime creatAt, String username, String passwordHash, String fullName,
                  String email, BigDecimal balance, String storeName, double rating) {
        super(id, creatAt, username, passwordHash, fullName, email, Role.SELLER, balance);
        this.storeName = normalizeOptional(storeName);
        this.rating = rating;
        validateStoreName(this.storeName);
    }

    // Kiểm tra định dạng storeName
    // Rating sẽ cho hệ thống quản lý nên không cần validate
    private void validateStoreName(String storeName) {
        if (storeName == null || storeName.trim().isEmpty()) {
            throw new IllegalArgumentException("Store name must not be blank.");
        }
        if (storeName.trim().length() > 100) {
            throw new IllegalArgumentException("Store name must not exceed 100 characters.");
        }
    }

    @Override
    public String toString() {
        return super.toString() + " | storeName: " + storeName + " | rating: " + rating;
    }

    // Setter/Getter
    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { validateStoreName(storeName);
    this.storeName = normalizeOptional(storeName);}
    public double getRating() { return rating; }
}
