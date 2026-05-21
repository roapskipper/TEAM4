package com.team4.mapper;

import com.team4.dto.socket.AuctionEndResponseDTO;
import com.team4.dto.socket.BidUpdateResponseDTO;
import com.team4.dto.socket.SocketMessageDTO;
import com.team4.model.Auction;
import java.time.format.DateTimeFormatter;

public class SocketMapper {

    // Các hằng số Command
    public static final String CMD_BID_UPDATE = "BID_UPDATE";
    public static final String CMD_AUCTION_END = "AUCTION_END";

    /**
     * Tạo gói tin cập nhật giá đặt
     */
    public static SocketMessageDTO<BidUpdateResponseDTO> toBidUpdateMessage(Auction auction) {
        if (auction == null) return null;
        
        BidUpdateResponseDTO payload = new BidUpdateResponseDTO(
                auction.getId(),
                auction.getCurrentPrice(),
                auction.getCurrentHighestBidderId(),
                auction.getEndTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );
        
        return new SocketMessageDTO<>(CMD_BID_UPDATE, payload);
    }

    /**
     * Tạo gói tin thông báo kết thúc phiên đấu giá
     */
    public static SocketMessageDTO<AuctionEndResponseDTO> toAuctionEndMessage(Auction auction) {
        if (auction == null) return null;
        
        AuctionEndResponseDTO payload = new AuctionEndResponseDTO(
                auction.getId(),
                auction.getCurrentPrice(),
                auction.getCurrentHighestBidderId()
        );
        
        return new SocketMessageDTO<>(CMD_AUCTION_END, payload);
    }
}
