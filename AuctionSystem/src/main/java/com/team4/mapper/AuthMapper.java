package com.team4.mapper;

import com.team4.dto.auth.LoginResponseDTO;
import com.team4.model.Admin;
import com.team4.model.User;

public class AuthMapper {
    /**
     * Chuyển đổi từ User model sang LoginResponseDTO.
     * - balance: trả về ngay sau đăng nhập để client hiển thị số dư ví.
     * - accessLevel: chỉ điền khi user là Admin, ngược lại để null.
     */
    public static LoginResponseDTO toLoginResponseDTO(User user, String token) {
        if (user == null) {
            return null;
        }
        Integer accessLevel = null;
        if (user instanceof Admin admin) {
            accessLevel = admin.getAccessLevel().getLevel();
        }
        return new LoginResponseDTO(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getFullName(),
                token,
                user.getBalance(),
                accessLevel
        );
    }
}
