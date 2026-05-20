package com.team4.dto.auth;

import com.team4.model.User;
public class LoginResponseDTO {
    private String userId;
    private String username;
    private User.Role role;
    private String fullName;
    private String token;

    public LoginResponseDTO() {}

    public LoginResponseDTO(String userId, String username, User.Role role, String fullName, String token) {
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.fullName = fullName;
        this.token = token;
    }


    public String getUserId() {
        return userId;
    }
    public String getUsername() {
        return username;
    }
    public User.Role getRole() {
        return role;
    }
    public String getFullName() {
        return fullName;
    }
    public String getToken() {
        return token;
    }

    @Override
    public String toString() {
        return "LoginResponseDTO: UserId=" + userId + ", username=" + username + ", role=" + role + ", fullName=" + fullName  + ", token=" + token;
    }
}
