package com.team4.dto.auth;

import java.util.regex.Pattern;

public class LoginRequestDTO {
    private String username;
    private String password;
    private String adminCode;

    public LoginRequestDTO() {}

    public LoginRequestDTO(String username, String password, String adminCode) {
        this.username = username;
        this.password = password;
        this.adminCode = adminCode;
        validate();
    }

    private static final Pattern USERNAME_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._-]{4,30}$");

    public void validate() {
        if (username == null || !USERNAME_PATTERN.matcher(username).matches()) {
            throw new IllegalArgumentException(
                    "Username must be 4-30 characters and may contain only letters, numbers, dots (.), underscores (_), and hyphens (-)."
            );
        }
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password is required.");
        }
    }

    public String getUsername() {
        return username;
    }
    public String getPassword() {
        return password;
    }
    public String getAdminCode() {
        return adminCode;
    }

    @Override
    public String toString() {
        return "LoginRequestDTO: username=" + username;
    }
}
