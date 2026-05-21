package com.team4.service;

import com.team4.dao.*;
import com.team4.dao.impl.*;
import com.team4.dto.bidding.BidRequestDTO;
import com.team4.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử tích hợp nghiệp vụ Đặt giá (BiddingService).
 * Đảm bảo luồng đặt giá (Proxy Bidding) cập nhật đúng nhiều bảng trong Database cùng lúc.
 */
@DisplayName("Integration Tests for BiddingService (Complex Flows)")
public class BiddingIntegrationTest extends BaseServiceIntegrationTest {

    private final UserDAO userDAO = new UserDAOImpl();
    private final ItemDAO itemDAO = new ItemDAOImpl();
    private final AuctionDAO auctionDAO = new AuctionDAOImpl();
    private final AutoBiddingDAO autoDAO = new AutoBiddingDAOImpl();
    private final BidTransactionDAO bidDAO = new BidTransactionDAOImpl();

    private final BiddingService biddingService = new BiddingService(auctionDAO, bidDAO, userDAO, autoDAO);

    @Test
    @DisplayName("Luồng đặt giá phức tạp: Cạnh tranh giữa hai người dùng sử dụng Proxy Bidding")
    void testProxyBidding_IntegrationFlow() {
        // 1. CHUẨN BỊ DỮ LIỆU GỐC (SEED DATA)
        // Tạo Seller và Item
        Seller seller = new Seller("s1", "pass", "Seller", "s@test.com", "Store");
        userDAO.insert(seller);
        Item item = new Art("Item 1", new BigDecimal("100"), "Desc", "s1", "Artist", 2024, Art.Medium.OIL_PAINT, "10x10");
        itemDAO.insert(item);

        // Tạo Auction đang chạy (Starting price: 100, Step: 10)
        Auction auction = new Auction(item.getId(), "s1", new BigDecimal("100.00"), new BigDecimal("10.00"), LocalDateTime.now().plusHours(1));
        auction.approve(); // Chuyển sang RUNNING
        auctionDAO.insert(auction);
        String auctionId = auction.getId();

        // Tạo hai Bidder (A có 1000 đồng, B có 2000 đồng)
        Bidder bidderA = new Bidder("userA", "pass", "Bidder A", "a@test.com", "Addr", "091");
        userDAO.insert(bidderA);
        userDAO.updateBalance(bidderA.getId(), new BigDecimal("1000.00"));

        Bidder bidderB = new Bidder("userB", "pass", "Bidder B", "b@test.com", "Addr", "092");
        userDAO.insert(bidderB);
        userDAO.updateBalance(bidderB.getId(), new BigDecimal("2000.00"));

        // 2. THỰC THI (WHEN)
        // Bước A: Người A đặt tối đa 200 đồng
        biddingService.placeBid(new BidRequestDTO(auctionId, bidderA.getId(), new BigDecimal("200.00")));
        
        // Bước B: Người B đặt tối đa 500 đồng
        biddingService.placeBid(new BidRequestDTO(auctionId, bidderB.getId(), new BigDecimal("500.00")));

        // 3. KIỂM CHỨNG (THEN)
        // Kiểm tra bảng Auctions: Người B phải dẫn đầu, giá hiển thị phải là 210 (Max A + Step)
        Auction updatedAuction = auctionDAO.findById(auctionId);
        assertEquals(bidderB.getId(), updatedAuction.getCurrentHighestBidderId());
        assertEquals(0, new BigDecimal("210.00").compareTo(updatedAuction.getCurrentPrice()));

        // Kiểm tra bảng Bid Transactions: Phải có ít nhất 2 bản ghi lịch sử
        List<BidTransaction> history = bidDAO.findByAuctionId(auctionId);
        assertTrue(history.size() >= 2, "Should have at least 2 bid records");

        // Kiểm tra bảng Auto Bidding: Cấu hình của A phải bị tắt (isActive = false) vì bị vượt mặt
        AutoBidding configA = autoDAO.findByAuctionAndBidder(auctionId, bidderA.getId());
        assertFalse(configA.isActive(), "Bidder A's auto-bid should be deactivated after being outbid");
        
        // Cấu hình của B vẫn phải đang hoạt động
        AutoBidding configB = autoDAO.findByAuctionAndBidder(auctionId, bidderB.getId());
        assertTrue(configB.isActive(), "Bidder B's auto-bid should remain active");
    }
}
