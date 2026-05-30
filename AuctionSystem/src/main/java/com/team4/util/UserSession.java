package com.team4.util;

import java.math.BigDecimal;

public class UserSession {
    private static UserSession instance;
    private String userId;
    private String username;
    private String fullName;
    private String role;
    private BigDecimal balance;

    private UserSession(String userId, String username, String role) {
        this.userId = userId;
        this.username = username;
        this.fullName = username;
        this.role = role;
        this.balance = BigDecimal.ZERO;
    }

    public static void createSession(String userId, String username, String role) {
        instance = new UserSession(userId, username, role);
    }

    public static void createSession(String userId, String username, String role, BigDecimal balance) {
        instance = new UserSession(userId, username, role);
        instance.setBalance(balance);
    }

    public static void createSession(String userId, String username, String fullName, String role, BigDecimal balance) {
        instance = new UserSession(userId, username, role);
        instance.setFullName(fullName);
        instance.setBalance(balance);
    }
    
    // Fallback for older code
    public static void createSession(String username, String role) {
        instance = new UserSession(username, username, role); // Use username as ID if not provided
    }

    public static UserSession getInstance() {
        return instance;
    }

    public static void clearSession() {
        instance = null;
    }

    public String getUserId() { return userId; }
    public String getRole() { return role; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) {
        this.fullName = fullName != null && !fullName.isBlank() ? fullName : username;
    }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) {
        this.balance = balance != null ? balance : BigDecimal.ZERO;
    }
}
