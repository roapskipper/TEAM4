package com.team4.mapper;

import com.team4.dto.auth.LoginResponseDTO;
import com.team4.model.User;

public class AuthMapper {
    /**
     * Chuyển đổi từ User model sang LoginResponseDTO
     */
    public static LoginResponseDTO toLoginResponseDTO(User user, String token) {
        if (user == null) {
            return null;
        }
        return new LoginResponseDTO(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getFullName(),
                token
        );
    }

}
