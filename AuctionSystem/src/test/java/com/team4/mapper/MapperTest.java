package com.team4.mapper;

import com.team4.dto.auction.AuctionResponseDTO;
import com.team4.dto.item.ItemResponseDTO;
import com.team4.dto.auth.LoginResponseDTO;
import com.team4.dto.auth.UserResponseDTO;
import com.team4.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử các lớp Mapper.
 * Đảm bảo dữ liệu từ Model được ánh xạ chính xác sang DTO và không làm lộ thông tin nhạy cảm.
 */
@DisplayName("Unit Tests for Mapper Classes")
public class MapperTest {

    @Test
    @DisplayName("AuthMapper - Chuyển đổi User sang LoginResponseDTO")
    void testAuthMapper() {
        // GIVEN: Một đối tượng Bidder thực tế
        Bidder bidder = new Bidder("u1", LocalDateTime.now(), "user1", "hash", "Full Name", "a@t.com", BigDecimal.ZERO, "Addr", "0912");
        String token = "jwt_token_sample";

        // WHEN: Ánh xạ qua AuthMapper
        LoginResponseDTO result = AuthMapper.toLoginResponseDTO(bidder, token);

        // THEN: Kiểm tra các trường cơ bản
        assertNotNull(result);
        assertEquals("u1", result.getUserId());
        assertEquals("user1", result.getUsername());
        assertEquals(token, result.getToken());
        assertEquals(User.Role.BIDDER, result.getRole());
    }

    @Test
    @DisplayName("UserMapper - Chuyển đổi User sang UserResponseDTO (Đa hình)")
    void testUserMapper() {
        // CASE 1: Ánh xạ Seller
        Seller seller = new Seller("s1", LocalDateTime.now(), "sel", "h", "Seller Name", "s@t.com", BigDecimal.TEN, "My Store", 4.5);
        UserResponseDTO sellerDto = UserMapper.toUserResponseDTO(seller);
        
        assertNotNull(sellerDto);
        assertEquals("My Store", sellerDto.getStoreName());
        assertEquals(4.5, sellerDto.getRating());
        assertEquals(BigDecimal.TEN, sellerDto.getBalance());

        // CASE 2: Ánh xạ Bidder
        Bidder bidder = new Bidder("b1", LocalDateTime.now(), "bid", "h", "Bidder Name", "b@t.com", BigDecimal.ZERO, "Hanoi", "0987");
        UserResponseDTO bidderDto = UserMapper.toUserResponseDTO(bidder);
        
        assertNotNull(bidderDto);
        assertEquals("Hanoi", bidderDto.getShippingAddress());
        assertEquals("0987", bidderDto.getPhoneNumber());
    }

    @Test
    @DisplayName("ItemMapper - Chuyển đổi Item sang ItemResponseDTO")
    void testItemMapper() {
        // GIVEN: Một vật phẩm Art
        Art art = new Art("i1", LocalDateTime.now(), "Mona Lisa", new BigDecimal("1000000"), "Painting", "owner1", "Da Vinci", 1503, Art.Medium.OIL_PAINT, "77x53");

        // WHEN
        ItemResponseDTO result = ItemMapper.toItemResponseDTO(art);

        // THEN
        assertNotNull(result);
        assertEquals("i1", result.getId());
        assertEquals("Mona Lisa", result.getName());
        assertEquals(Item.ItemCategory.ART, result.getCategory());
    }

    @Test
    @DisplayName("AuctionMapper - Chuyển đổi Auction sang AuctionResponseDTO")
    void testAuctionMapper() {
        // GIVEN
        Auction auction = new Auction("item1", "seller1", new BigDecimal("100"), new BigDecimal("10"), LocalDateTime.now().plusDays(1));

        // WHEN
        AuctionResponseDTO result = AuctionMapper.toAuctionResponseDTO(auction);

        // THEN
        assertNotNull(result);
        assertEquals("item1", result.getItemId());
        assertEquals(Auction.AuctionStatus.PENDING, result.getStatus());
    }

    @Test
    @DisplayName("Kiểm tra xử lý Null trong các Mapper")
    void testMapperNullHandling() {
        assertNull(AuthMapper.toLoginResponseDTO(null, "token"));
        assertNull(UserMapper.toUserResponseDTO(null));
        assertNull(ItemMapper.toItemResponseDTO(null));
        assertNull(AuctionMapper.toAuctionResponseDTO(null));
    }
}
