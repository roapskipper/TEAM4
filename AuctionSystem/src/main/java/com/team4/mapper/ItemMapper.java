package com.team4.mapper;

import com.team4.dto.item.ItemResponseDTO;
import com.team4.model.Item;
import java.time.format.DateTimeFormatter;

public class ItemMapper {
    /**
     * Chuyển Item từ Model về DTO
     */
    public static ItemResponseDTO toItemResponseDTO(Item item) {
        if (item == null) {
            return null;
        }
        return new ItemResponseDTO(
                item.getId(),
                item.getName(),
                item.getStartingPrice(),
                item.getCategory(),
                item.getOwnerId(),
                item.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );
    }
}
