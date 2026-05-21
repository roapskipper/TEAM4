package com.team4.mapper;

import com.team4.dto.auction.*;
import com.team4.model.Auction;
import com.team4.model.Item;

import java.time.format.DateTimeFormatter;

public class AuctionMapper {
    /**
     * Chuyển Auction từ Model về DTO
     */
    public static AuctionResponseDTO toAuctionResponseDTO(Auction auction) {
        if (auction == null) {
            return null;
        }
        return new AuctionResponseDTO(auction.getId(), auction.getItemId(), auction.getSellerId(),
                auction.getBidIncrement(), auction.getCurrentPrice(),
                auction.getEndTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), auction.getStatus(),
                auction.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }
}
