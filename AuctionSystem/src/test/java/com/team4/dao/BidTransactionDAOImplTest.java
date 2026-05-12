package com.team4.dao;

import com.team4.dao.impl.*;
import com.team4.db.DatabaseManager;
import com.team4.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lớp BidTransactionDAOImplTest - Kiểm thử lịch sử đặt giá.
 * Kế thừa từ BaseDAOTest để đảm bảo môi trường sạch.
 */
@DisplayName("Kiểm thử chuyên sâu BidTransactionDAO (Database Thật)")
public class BidTransactionDAOImplTest extends BaseDAOTest {

    private final BidTransactionDAO bidTransactionDAO = new BidTransactionDAOImpl();
    private final AuctionDAO auctionDAO = new AuctionDAOImpl();
    private final ItemDAO itemDAO = new ItemDAOImpl();
    private final UserDAO userDAO = new UserDAOImpl();

    private Bidder testBidder;
    private Auction testAuction;

    /**
     * Chuẩn bị các thực thể phụ thuộc: User -> Item -> Auction.
     */
    @BeforeEach
    void setupDependencies() {
        // 1. Tạo Người bán và Người mua (Dữ liệu chuẩn: username >= 4 ký tự, phone 10 số)
        Seller seller = new Seller("seller001", "hashsvfsg", "Người Bán", "seller@test.com", "Mixi Shop");
        testBidder = new Bidder("bidder001", "hashhrd", "Người Mua", "bidder@test.com", "Hà Nội", "0912345678");
        userDAO.insert(seller);
        userDAO.insert(testBidder);

        // 2. Tạo Mặt hàng
        Art art = new Art("Tranh Đông Hồ", new BigDecimal("100.00"), "Mô tả", seller.getId(), "Nghệ nhân", 2000, Art.Medium.INK, "30x40 cm");
        itemDAO.insert(art);

        // 3. Tạo Phiên đấu giá
        testAuction = new Auction(art.getId(), seller.getId(), new BigDecimal("100.00"), new BigDecimal("10.00"), LocalDateTime.now().plusDays(1));
        auctionDAO.insert(testAuction);
        auctionDAO.updateStatus(testAuction.getId(), Auction.AuctionStatus.RUNNING);
    }

    @Nested
    @DisplayName("Nghiệp vụ Lưu trữ và Truy vấn")
    class HistoryTests {

        @Test
        @DisplayName("Lưu lượt đặt giá mới bằng Connection (Transaction)")
        void testInsertBid() throws SQLException {
            // GIVEN: Một giao dịch đặt giá 150.00
            BidTransaction tx = new BidTransaction(testAuction.getId(), testBidder.getId(), new BigDecimal("150.00"));

            // WHEN: Lưu vào DB thông qua Connection
            try (Connection conn = DatabaseManager.getConnection()) {
                boolean inserted = bidTransactionDAO.insert(conn, tx);
                assertTrue(inserted);
            }

            // THEN: Lấy lại danh sách lịch sử của phiên
            List<BidTransaction> history = bidTransactionDAO.findByAuctionId(testAuction.getId());
            assertEquals(1, history.size());
            assertEquals(0, new BigDecimal("150.00").compareTo(history.get(0).getBidAmount()));
        }

        @Test
        @DisplayName("Lấy lịch sử bid theo Auction ID (Đúng thứ tự thời gian)")
        void testFindByAuctionId_Ordering() throws SQLException, InterruptedException {
            // GIVEN: Bidder đặt giá 2 lần khác nhau
            try (Connection conn = DatabaseManager.getConnection()) {
                bidTransactionDAO.insert(conn, new BidTransaction(testAuction.getId(), testBidder.getId(), new BigDecimal("110.00")));
                
                // Nghỉ 1 giây để đảm bảo bid_time trong DB khác nhau hoàn toàn (MySQL mặc định độ chính xác là giây)
                Thread.sleep(1000); 
                
                bidTransactionDAO.insert(conn, new BidTransaction(testAuction.getId(), testBidder.getId(), new BigDecimal("120.00")));
            }

            // WHEN: Lấy lịch sử (DAO dùng ORDER BY bid_time ASC)
            List<BidTransaction> history = bidTransactionDAO.findByAuctionId(testAuction.getId());

            // THEN: Lượt 110 phải đứng trước lượt 120
            assertEquals(2, history.size());
            assertTrue(history.get(0).getBidAmount().compareTo(history.get(1).getBidAmount()) < 0, 
                "Lượt đặt giá sớm hơn (110) phải xuất hiện trước trong lịch sử");
        }

        @Test
        @DisplayName("Lấy lịch sử bid theo Bidder ID")
        void testFindByBidderId() throws SQLException {
            try (Connection conn = DatabaseManager.getConnection()) {
                bidTransactionDAO.insert(conn, new BidTransaction(testAuction.getId(), testBidder.getId(), new BigDecimal("200.00")));
            }

            List<BidTransaction> myHistory = bidTransactionDAO.findByBidderId(testBidder.getId());
            assertFalse(myHistory.isEmpty());
            assertEquals(testBidder.getId(), myHistory.get(0).getBidderId());
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ Tìm lượt đặt giá cao nhất")
    class HighestBidTests {

        @Test
        @DisplayName("Lấy lượt bid cao nhất chính xác")
        void testGetHighestBid() throws SQLException {
            // GIVEN: Có 3 lượt bid: 150, 300, 200
            try (Connection conn = DatabaseManager.getConnection()) {
                bidTransactionDAO.insert(conn, new BidTransaction(testAuction.getId(), testBidder.getId(), new BigDecimal("150.00")));
                bidTransactionDAO.insert(conn, new BidTransaction(testAuction.getId(), testBidder.getId(), new BigDecimal("300.00")));
                bidTransactionDAO.insert(conn, new BidTransaction(testAuction.getId(), testBidder.getId(), new BigDecimal("200.00")));
            }

            // WHEN
            BidTransaction highest = bidTransactionDAO.getHighestBid(testAuction.getId());

            // THEN: Phải là lượt 300.00
            assertNotNull(highest);
            assertEquals(0, new BigDecimal("300.00").compareTo(highest.getBidAmount()));
        }

        @Test
        @DisplayName("Xử lý Tie-break: Hai lượt bid cùng giá -> Lấy người sớm hơn")
        void testGetHighestBid_TieBreak() throws SQLException, InterruptedException {
            // GIVEN: Hai lượt bid cùng 500.00 nhưng cách nhau 1 giây
            BidTransaction first = new BidTransaction(testAuction.getId(), testBidder.getId(), new BigDecimal("500.00"));
            
            try (Connection conn = DatabaseManager.getConnection()) {
                bidTransactionDAO.insert(conn, first);
                
                Thread.sleep(1000); // Nghỉ 1s để đảm bảo bid_time khác nhau
                
                BidTransaction second = new BidTransaction(testAuction.getId(), testBidder.getId(), new BigDecimal("500.00"));
                bidTransactionDAO.insert(conn, second);
            }

            // WHEN
            BidTransaction winner = bidTransactionDAO.getHighestBid(testAuction.getId());

            // THEN: Phải là lượt bid đầu tiên (first)
            assertEquals(first.getId(), winner.getId(), "Nếu bằng giá, người đặt trước phải thắng");
        }
    }
}
