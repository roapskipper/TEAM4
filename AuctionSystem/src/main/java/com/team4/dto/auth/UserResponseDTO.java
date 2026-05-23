package com.team4.dto.auth;

import com.team4.model.User;
import java.math.BigDecimal;

/**
 * DTO dành cho việc phản hồi thông tin chi tiết người dùng.
 * Loại bỏ các trường nhạy cảm như passwordHash.
 */
public class UserResponseDTO {
    private String id;
    private String username;
    private String fullName;
    private String email;
    private User.Role role;
    private BigDecimal balance;
    private String createdAt;
    private Integer accessLevelCode;

    // Các trường bổ sung cho từng vai trò cụ thể
    private String storeName;       // Dành cho Seller
    private Double rating;          // Dành cho Seller
    private String shippingAddress; // Dành cho Bidder
    private String phoneNumber;     // Dành cho Bidder

    public UserResponseDTO() {}

    public UserResponseDTO(String id, String username, String fullName, String email, User.Role role, 
                           BigDecimal balance, String createdAt) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.balance = balance;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public User.Role getRole() { return role; }
    public BigDecimal getBalance() { return balance; }
    public String getCreatedAt() { return createdAt; }
    public Integer getAccessLevelCode() { return accessLevelCode; }
    public void setAccessLevelCode(Integer accessLevelCode) { this.accessLevelCode = accessLevelCode; }

    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }

    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }

    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    @Override
    public String toString() {
        return "UserResponseDTO{" +
                "id='" + id + '\'' +
                ", username='" + username + '\'' +
                ", role=" + role +
                '}';
    }
}
