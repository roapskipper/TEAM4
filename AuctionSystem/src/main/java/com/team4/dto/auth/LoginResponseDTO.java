package com.team4.dto.auth;

import com.team4.model.User;
import java.math.BigDecimal;

public class LoginResponseDTO {
    private String userId;
    private String username;
    private User.Role role;
    private String fullName;
    private String token;
    private BigDecimal balance;   // Số dư ví – client cần hiển thị ngay sau đăng nhập
    private Integer accessLevel;  // Chỉ có giá trị với tài khoản Admin, null với các vai trò khác

    public LoginResponseDTO() {}

    public LoginResponseDTO(String userId, String username, User.Role role,
                            String fullName, String token,
                            BigDecimal balance, Integer accessLevel) {
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.fullName = fullName;
        this.token = token;
        this.balance = balance;
        this.accessLevel = accessLevel;
    }

    public String getUserId()    { return userId; }
    public String getUsername()  { return username; }
    public User.Role getRole()   { return role; }
    public String getFullName()  { return fullName; }
    public String getToken()     { return token; }
    public BigDecimal getBalance()  { return balance; }
    public Integer getAccessLevel() { return accessLevel; }

    @Override
    public String toString() {
        return "LoginResponseDTO: UserId=" + userId + ", username=" + username
                + ", role=" + role + ", fullName=" + fullName + ", token=" + token;
    }
}
