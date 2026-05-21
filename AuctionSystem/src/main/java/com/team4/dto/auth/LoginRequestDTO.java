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

    // Validate
    private static final Pattern USERNAME_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._-]{4,30}$");
    public void validate() {
        if (username == null || !USERNAME_PATTERN.matcher(username).matches()) {
            throw new IllegalArgumentException(
                    "Tên đăng nhập phải dài từ 4–30 ký tự và chỉ được chứa chữ cái, số, dấu chấm (.), gạch dưới (_) và gạch ngang (-)."
            );
        }
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Mật khẩu không được trống");
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
