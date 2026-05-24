package com.team4.dto.auth;

import java.util.regex.Pattern;

public class RegisterBidderRequestDTO {
    private String username;
    private String password;
    private String fullName;
    private String email;
    private String shippingAddress;
    private String phoneNumber;

    public RegisterBidderRequestDTO() {}

    public RegisterBidderRequestDTO(String username, String fullName, String password, String email, String shippingAddress, String phoneNumber) {
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.password = password;
        this.shippingAddress = shippingAddress;
        this.phoneNumber = phoneNumber;
        validate();
    }

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^\\+?[0-9]\\d{6,14}$"
    );
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
        if (phoneNumber == null || !PHONE_PATTERN.matcher(phoneNumber).matches()) {
            throw new IllegalArgumentException("Phone number is invalid.");
        }
        if (shippingAddress == null || shippingAddress.isEmpty()) {
            throw new IllegalArgumentException("Address is required.");
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
    public String getShippingAddress() {
        return shippingAddress;
    }
    public String getPhoneNumber() {
        return phoneNumber;
    }

    @Override
    public String toString() {
        return "RegisterBidderRequestDTO: username=" + username + ", fullName=" + fullName + ", email=" + email + ", shippingAddress=" + shippingAddress + ", phoneNumber=" + phoneNumber;
    }
}
