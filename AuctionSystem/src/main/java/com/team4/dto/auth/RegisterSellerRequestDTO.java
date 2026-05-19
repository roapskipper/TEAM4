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

    // Validate
    private static final Pattern USERNAME_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._-]{4,30}$");
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    public void validate() {
        if (username == null || !USERNAME_PATTERN.matcher(username).matches()) {
            throw new IllegalArgumentException("Tên đăng nhập phải dài từ 4–30 ký tự và chỉ được chứa chữ cái, số, dấu chấm (.), gạch dưới (_) và gạch ngang (-).");
        }
        if (fullName == null || fullName.isEmpty()) {
            throw new IllegalArgumentException("Họ tên không được trống");
        }
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("email không hợp lệ");
        }
        if (storeName == null || storeName.isEmpty()) {
            throw new IllegalArgumentException("Tên cửa hàng khong được trống");
        }
    }

    public String getUsername() {
        return username;
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
        return "RegisterBidderRequestDTO: username=" + username + ", fullName=" + fullName + ", email=" + email + ", storeName=" + storeName;
    }
}
