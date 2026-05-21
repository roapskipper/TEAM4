package com.team4.mapper;

import com.team4.dto.auction.BidTransactionResponseDTO;
import com.team4.dto.bidding.AutoBidResponseDTO;
import com.team4.dto.socket.AuctionEndResponseDTO;
import com.team4.dto.socket.BidUpdateResponseDTO;
import com.team4.dto.socket.SocketMessageDTO;
import com.team4.model.Auction;
import com.team4.model.AutoBidding;
import com.team4.model.BidTransaction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử các lớp Mapper liên quan đến Đặt giá và Socket.
 */
@DisplayName("Unit Tests for Bidding and Socket Mappers")
public class BiddingMapperTest {

    @Test
    @DisplayName("BidMapper - Chuyển đổi BidTransaction sang DTO")
    void testBidMapper() {
        // GIVEN
        BidTransaction bid = new BidTransaction("auc1", "bidder1", new BigDecimal("500"));

        // WHEN
        BidTransactionResponseDTO result = BidMapper.toBidTransactionResponseDTO(bid);

        // THEN
        assertNotNull(result);
        assertEquals("bidder1", result.getBidderId());
        assertEquals(new BigDecimal("500"), result.getBidAmount());
    }

    @Test
    @DisplayName("AutoBidMapper - Chuyển đổi AutoBidding sang DTO")
    void testAutoBidMapper() {
        // GIVEN
        AutoBidding config = new AutoBidding("auc1", "bidder1", new BigDecimal("1000"));

        // WHEN
        AutoBidResponseDTO result = AutoBidMapper.toAutoBidResponseDTO(config);

        // THEN
        assertNotNull(result);
        assertEquals(new BigDecimal("1000"), result.getMaxLimit());
        assertTrue(result.isActive());
    }

    @Test
    @DisplayName("SocketMapper - Tạo gói tin Bid Update")
    void testSocketMapper_BidUpdate() {
        // GIVEN
        Auction auction = new Auction("i1", "s1", new BigDecimal("100"), new BigDecimal("10"), LocalDateTime.now().plusHours(1));
        auction.approve();
        auction.applyBid("winner1", new BigDecimal("150"));

        // WHEN
        SocketMessageDTO<BidUpdateResponseDTO> message = SocketMapper.toBidUpdateMessage(auction);

        // THEN
        assertNotNull(message);
        assertEquals(SocketMapper.CMD_BID_UPDATE, message.getCommand());
        assertEquals("winner1", message.getPayload().getCurrentHighestBidderId());
        assertEquals(new BigDecimal("150"), message.getPayload().getCurrentPrice());
    }

    @Test
    @DisplayName("SocketMapper - Tạo gói tin Auction End")
    void testSocketMapper_AuctionEnd() {
        // GIVEN
        Auction auction = new Auction("i1", "s1", new BigDecimal("100"), new BigDecimal("10"), LocalDateTime.now());
        auction.approve();
        auction.applyBid("winner1", new BigDecimal("1000"));

        // WHEN
        SocketMessageDTO<AuctionEndResponseDTO> message = SocketMapper.toAuctionEndMessage(auction);

        // THEN
        assertNotNull(message);
        assertEquals(SocketMapper.CMD_AUCTION_END, message.getCommand());
        assertEquals("winner1", message.getPayload().getWinnerId());
        assertEquals(new BigDecimal("1000"), message.getPayload().getFinalPrice());
    }
}
