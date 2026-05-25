package com.team4.dto.auth;

import java.util.regex.Pattern;

public class RegisterSellerRequestDTO {
    private String username;
    private String password;
    private String fullName;
    private String email;
    private String storeName;

    public RegisterSellerRequestDTO() {}

    public RegisterSellerRequestDTO(String username, String password, String fullName, String email, String storeName) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.email = email;
        this.storeName = storeName;
        validate();
    }

    private static final Pattern USERNAME_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._-]{4,30}$");
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    public void validate() {
        if (username == null || !USERNAME_PATTERN.matcher(username).matches()) {
            throw new IllegalArgumentException("Username must be 4-30 characters and may contain only letters, numbers, dots (.), underscores (_), and hyphens (-).");
        }
        if (fullName == null || fullName.isEmpty()) {
            throw new IllegalArgumentException("Full name is required.");
        }
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Invalid email.");
        }
        if (storeName == null || storeName.isEmpty()) {
            throw new IllegalArgumentException("Store name is required.");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters.");
        }
    }

    public String getUsername() {
        return username;
    }
    public String getPassword() {
        return password;
    }
    public String getFullName() {
        return fullName;
    }
    public String getEmail() {
        return email;
    }
    public String getStoreName() {
        return storeName;
    }

    @Override
    public String toString() {
        return "RegisterSellerRequestDTO: username=" + username + ", fullName=" + fullName + ", email=" + email + ", storeName=" + storeName;
    }
}
