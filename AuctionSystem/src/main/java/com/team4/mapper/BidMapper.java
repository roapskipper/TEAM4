package com.team4.mapper;

import com.team4.dto.auction.BidTransactionResponseDTO;
import com.team4.model.BidTransaction;
import java.time.format.DateTimeFormatter;

public class BidMapper {
    /**
     * Chuyển đổi từ BidTransaction model sang BidTransactionResponseDTO.
     */
    public static BidTransactionResponseDTO toBidTransactionResponseDTO(BidTransaction bid) {
        if (bid == null) {
            return null;
        }
        return new BidTransactionResponseDTO(
                bid.getId(),
                bid.getAuctionId(),
                bid.getBidderId(),
                bid.getBidAmount(),
                bid.getBidTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );
    }
}
