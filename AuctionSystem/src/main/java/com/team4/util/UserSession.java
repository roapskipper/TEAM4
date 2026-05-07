package com.team4.util;

public class UserSession {
    private static UserSession instance;
    private String username;
    private String role;

    private UserSession(String username, String role) {
        this.username = username;
        this.role = role;
    }

    public static void createSession(String username, String role) {
        instance = new UserSession(username, role);
    }

    public static UserSession getInstance() {
        return instance;
    }

    public static void clearSession() {
        instance = null;
    }

    public String getRole() { return role; }
    public String getUsername() { return username; }
}