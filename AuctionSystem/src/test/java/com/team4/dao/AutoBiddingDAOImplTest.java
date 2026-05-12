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
 * Lớp AutoBiddingDAOImplTest - Kiểm thử tính năng đấu giá tự động trên DB.
 * Kế thừa từ BaseDAOTest để đảm bảo dọn dẹp dữ liệu tự động.
 */
@DisplayName("Kiểm thử chuyên sâu AutoBiddingDAO (Database Thật)")
public class AutoBiddingDAOImplTest extends BaseDAOTest {

    private final AutoBiddingDAO autoBiddingDAO = new AutoBiddingDAOImpl();
    private final AuctionDAO auctionDAO = new AuctionDAOImpl();
    private final ItemDAO itemDAO = new ItemDAOImpl();
    private final UserDAO userDAO = new UserDAOImpl();

    private Bidder testBidder;
    private Auction testAuction;

    /**
     * Chuẩn bị dữ liệu mẫu: User -> Item -> Auction.
     */
    @BeforeEach
    void setupDependencies() {
        // 1. Tạo Người mua (Dữ liệu chuẩn)
        testBidder = new Bidder("bidder001", "hash", "Người Mua A", "bid@test.com", "Hà Nội", "0912345678");
        userDAO.insert(testBidder);

        // 2. Tạo Người bán và Mặt hàng
        Seller seller = new Seller("seller001", "hash", "Người Bán", "sell@test.com", "Mixi Shop");
        userDAO.insert(seller);
        Art art = new Art("Tranh Quý", new BigDecimal("500.00"), "Mô tả", seller.getId(), "Họa sĩ X", 2000, Art.Medium.INK, "30x40 cm");
        itemDAO.insert(art);

        // 3. Tạo Phiên đấu giá
        testAuction = new Auction(art.getId(), seller.getId(), new BigDecimal("500.00"), new BigDecimal("50.00"), LocalDateTime.now().plusDays(1));
        auctionDAO.insert(testAuction);
        auctionDAO.updateStatus(testAuction.getId(), Auction.AuctionStatus.RUNNING);
    }

    @Nested
    @DisplayName("Nghiệp vụ Lưu trữ và Cập nhật")
    class CRUDTests {

        @Test
        @DisplayName("Lưu và tìm kiếm cấu hình Auto-bid thành công")
        void testInsertAndFind() {
            // GIVEN: Một cấu hình auto-bid với giới hạn 2000.00
            AutoBidding config = new AutoBidding(testAuction.getId(), testBidder.getId(), new BigDecimal("2000.00"));

            // WHEN
            boolean inserted = autoBiddingDAO.insert(config);
            AutoBidding found = autoBiddingDAO.findByAuctionAndBidder(testAuction.getId(), testBidder.getId());

            // THEN
            assertTrue(inserted);
            assertNotNull(found);
            assertEquals(0, new BigDecimal("2000.00").compareTo(found.getMaxLimit()));
            assertTrue(found.isActive(), "Mặc định auto-bid phải ở trạng thái active");
        }

        @Test
        @DisplayName("Cập nhật giới hạn giá tối đa (Max Limit)")
        void testUpdateMaxLimit() {
            // GIVEN
            AutoBidding config = new AutoBidding(testAuction.getId(), testBidder.getId(), new BigDecimal("1000.00"));
            autoBiddingDAO.insert(config);

            // WHEN: Đổi giới hạn lên 1500.00
            config.setMaxLimit(new BigDecimal("1500.00"));
            boolean updated = autoBiddingDAO.update(config);

            // THEN
            assertTrue(updated);
            AutoBidding found = autoBiddingDAO.findById(config.getId());
            assertEquals(0, new BigDecimal("1500.00").compareTo(found.getMaxLimit()));
        }

        @Test
        @DisplayName("Bật/Tắt trạng thái Auto-bid (updateActive)")
        void testUpdateActiveStatus() {
            // GIVEN
            AutoBidding config = new AutoBidding(testAuction.getId(), testBidder.getId(), new BigDecimal("1000.00"));
            autoBiddingDAO.insert(config);

            // WHEN: Tắt auto-bid
            boolean ok = autoBiddingDAO.updateActive(config.getId(), false);

            // THEN
            assertTrue(ok);
            AutoBidding found = autoBiddingDAO.findById(config.getId());
            assertFalse(found.isActive());
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ Truy vấn danh sách")
    class QueryTests {

        @Test
        @DisplayName("Lấy danh sách các cấu hình đang hoạt động trong phiên")
        void testFindActiveByAuctionId() {
            // GIVEN: Tạo thêm một người mua khác cũng bật Auto-bid
            Bidder bidder2 = new Bidder("bidder002", "hashgdsadgsg", "Người Mua B", "b2@test.com", "HCM", "0987654321");
            userDAO.insert(bidder2);
            
            autoBiddingDAO.insert(new AutoBidding(testAuction.getId(), testBidder.getId(), new BigDecimal("1000")));
            autoBiddingDAO.insert(new AutoBidding(testAuction.getId(), bidder2.getId(), new BigDecimal("1200")));

            // WHEN
            List<AutoBidding> activeConfigs = autoBiddingDAO.findActiveByAuctionId(testAuction.getId());

            // THEN: Phải lấy được cả 2
            assertEquals(2, activeConfigs.size());
        }

        @Test
        @DisplayName("Không lấy các cấu hình đã bị tắt (Inactive)")
        void testFindActiveOnly() {
            // GIVEN: Người mua 1 đang bật, Người mua 2 đã tắt
            Bidder bidder2 = new Bidder("bidder002", "hashdhssd", "Bdhshd", "b2@t.com", "H", "0169436899");
            userDAO.insert(bidder2);
            
            autoBiddingDAO.insert(new AutoBidding(testAuction.getId(), testBidder.getId(), new BigDecimal("1000")));
            AutoBidding config2 = new AutoBidding(testAuction.getId(), bidder2.getId(), new BigDecimal("1200"));
            config2.deactivate(); // Tắt
            autoBiddingDAO.insert(config2);

            // WHEN
            List<AutoBidding> activeConfigs = autoBiddingDAO.findActiveByAuctionId(testAuction.getId());

            // THEN: Chỉ lấy được 1 người
            assertEquals(1, activeConfigs.size());
            assertEquals(testBidder.getId(), activeConfigs.get(0).getBidderId());
        }
    }

    @Nested
    @DisplayName("Kiểm thử Transaction")
    class TransactionTests {

        @Test
        @DisplayName("Lưu cấu hình trong một Transaction (Connection)")
        void testInsertWithConnection() throws SQLException {
            AutoBidding config = new AutoBidding(testAuction.getId(), testBidder.getId(), new BigDecimal("3000.00"));
            
            try (Connection conn = DatabaseManager.getConnection()) {
                boolean ok = autoBiddingDAO.insert(conn, config);
                assertTrue(ok);
            }

            assertNotNull(autoBiddingDAO.findById(config.getId()));
        }
    }
}
