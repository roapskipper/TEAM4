package com.team4.mapper;

import com.team4.dto.bidding.AutoBidResponseDTO;
import com.team4.model.AutoBidding;

public class AutoBidMapper {
    /**
     * Chuyển đổi từ AutoBidding model sang AutoBidResponseDTO.
     */
    public static AutoBidResponseDTO toAutoBidResponseDTO(AutoBidding config) {
        if (config == null) {
            return null;
        }
        return new AutoBidResponseDTO(
                config.getId(),
                config.getAuctionId(),
                config.getBidderId(),
                config.getMaxLimit(),
                config.isActive()
        );
    }
}
