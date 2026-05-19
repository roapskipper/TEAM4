package com.team4.util;

public class UserSession {
    private static UserSession instance;
    private String userId;
    private String username;
    private String role;

    private UserSession(String userId, String username, String role) {
        this.userId = userId;
        this.username = username;
        this.role = role;
    }

    public static void createSession(String userId, String username, String role) {
        instance = new UserSession(userId, username, role);
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
}