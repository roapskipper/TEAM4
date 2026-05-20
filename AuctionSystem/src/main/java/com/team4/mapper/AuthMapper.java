package com.team4.mapper;

import com.team4.dto.auth.*;
import com.team4.model.*;

public class AuthMapper {
    /**
     * Chuyển User từ model về dto
     */
    public static LoginResponseDTO loginResponseDTO(User user) {
        if (user == null) {
            return null;
        }
        if (user instanceof Admin) {

        }
    }
}
