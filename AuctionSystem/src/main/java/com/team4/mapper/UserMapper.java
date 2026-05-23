package com.team4.mapper;

import com.team4.dto.auth.UserResponseDTO;
import com.team4.model.Admin;
import com.team4.model.Bidder;
import com.team4.model.Seller;
import com.team4.model.User;

import java.time.format.DateTimeFormatter;

public class UserMapper {
    /**
     * Chuyển đổi từ User model sang UserResponseDTO.
     * Tự động ánh xạ các trường đặc thù của Seller hoặc Bidder.
     */
    public static UserResponseDTO toUserResponseDTO(User user) {
        if (user == null) {
            return null;
        }

        UserResponseDTO dto = new UserResponseDTO(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getBalance(),
                user.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );

        // Ánh xạ thêm thông tin dựa trên loại User thực tế
        if (user instanceof Admin admin) {
            dto.setAccessLevelCode(admin.getAccessLevel().getLevel());
        } else if (user instanceof Seller seller) {
            dto.setStoreName(seller.getStoreName());
            dto.setRating(seller.getRating());
        } else if (user instanceof Bidder bidder) {
            dto.setShippingAddress(bidder.getShippingAddress());
            dto.setPhoneNumber(bidder.getPhoneNumber());
        }

        return dto;
    }
}
