package com.team4.dao;

import com.team4.dao.impl.AuctionDAOImpl;
import com.team4.dao.impl.ItemDAOImpl;
import com.team4.dao.impl.UserDAOImpl;
import com.team4.db.DatabaseManager;
import com.team4.model.Art;
import com.team4.model.Auction;
import com.team4.model.Seller;
import com.team4.model.Bidder;
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
 * Lớp AuctionDAOImplTest - Kiểm thử vòng đời phiên đấu giá trên DB thật.
 * Kế thừa từ BaseDAOTest để dọn dẹp dữ liệu tự động.
 */
@DisplayName("Kiểm thử chuyên sâu AuctionDAO (Database Thật)")
public class AuctionDAOImplTest extends BaseDAOTest {

    private final AuctionDAO auctionDAO = new AuctionDAOImpl();
    private final ItemDAO itemDAO = new ItemDAOImpl();
    private final UserDAO userDAO = new UserDAOImpl();

    private Seller testSeller;
    private Art testItem;

    /**
     * Chuẩn bị dữ liệu phụ thuộc (Seller và Item) trước mỗi bài test.
     */
    @BeforeEach
    void setupDependencies() {
        // 1. Tạo Người bán (Dữ liệu chuẩn: username >= 4 ký tự)
        testSeller = new Seller("seller001", "hash", "Chủ Cửa Hàng Xịn", "seller@test.com", "Shop Đấu Giá");
        userDAO.insert(testSeller);

        // 2. Tạo Mặt hàng tranh (Dữ liệu chuẩn: artist >= 2 ký tự, dimensions đúng format "RxC cm")
        testItem = new Art("Bức tranh quý", new BigDecimal("1000.00"), "Mô tả bức tranh cổ",
                         testSeller.getId(), "Nghệ nhân A", 1950, Art.Medium.SCULPTURE_CERAMIC, "20x50 cm");
        itemDAO.insert(testItem);
    }

    @Nested
    @DisplayName("Nghiệp vụ Lưu trữ và Truy vấn")
    class PersistenceTests {

        @Test
        @DisplayName("Lưu và tìm lại phiên đấu giá thành công")
        void testInsertAndFind() {
            // GIVEN: Tạo một phiên đấu giá kết thúc sau 7 ngày
            Auction auction = new Auction(testItem.getId(), testSeller.getId(),
                                        new BigDecimal("1000.00"), new BigDecimal("100.00"),
                                        LocalDateTime.now().plusDays(7));

            // WHEN: Lưu xuống DB
            boolean inserted = auctionDAO.insert(auction);
            Auction found = auctionDAO.findById(auction.getId());

            // THEN: Kiểm tra tính toàn vẹn của dữ liệu nạp lên
            assertTrue(inserted, "Phải lưu thành công vào Database");
            assertNotNull(found, "Phải tìm thấy phiên đấu giá theo ID");
            assertEquals(testItem.getId(), found.getItemId());
            assertEquals(Auction.AuctionStatus.PENDING, found.getStatus(), "Trạng thái mặc định phải là PENDING");
            assertEquals(0, new BigDecimal("1000.00").compareTo(found.getCurrentPrice()), "Giá hiện tại phải khớp với giá khởi điểm");
        }

        @Test
        @DisplayName("Lọc phiên đấu giá theo trạng thái (RUNNING)")
        void testFindByStatus() {
            // GIVEN: Tạo 2 phiên, 1 cái RUNNING, 1 cái PENDING
            Auction a1 = new Auction(testItem.getId(), testSeller.getId(), BigDecimal.TEN, BigDecimal.ONE, LocalDateTime.now().plusDays(1));
            auctionDAO.insert(a1);
            // Phê duyệt để chuyển sang RUNNING
            auctionDAO.updateStatus(a1.getId(), Auction.AuctionStatus.RUNNING);

            Auction a2 = new Auction(testItem.getId(), testSeller.getId(), BigDecimal.TEN, BigDecimal.ONE, LocalDateTime.now().plusDays(2));
            auctionDAO.insert(a2); // Giữ nguyên PENDING

            // WHEN: Lọc lấy các phiên đang hoạt động
            List<Auction> runningAuctions = auctionDAO.findByStatus(Auction.AuctionStatus.RUNNING);

            // THEN: Chỉ được thấy phiên a1
            assertEquals(1, runningAuctions.size(), "Danh sách RUNNING chỉ nên có 1 phần tử");
            assertEquals(a1.getId(), runningAuctions.get(0).getId());
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ Cập nhật (Updates)")
    class UpdateTests {
@Test
@DisplayName("Cập nhật lượt đặt giá mới (Update Current Bid)")
void testUpdateCurrentBid() throws SQLException {
    // GIVEN: Tạo phiên đang chạy và một người mua (Bidder) hợp lệ
    Auction auction = new Auction(testItem.getId(), testSeller.getId(), new BigDecimal("100.00"), new BigDecimal("10.00"), LocalDateTime.now().plusDays(1));
    auctionDAO.insert(auction);
    auctionDAO.updateStatus(auction.getId(), Auction.AuctionStatus.RUNNING);

    Bidder bidder = new Bidder("bidder001", "hash", "Người mua A", "bid@test.com", "Địa chỉ", "0912345678");
    userDAO.insert(bidder);

    // WHEN: Lấy Connection và cập nhật giá bid dẫn đầu mới (150.00)
    BigDecimal newPrice = new BigDecimal("150.00");
    boolean updated;
    try (Connection conn = DatabaseManager.getConnection()) {
        updated = auctionDAO.updateCurrentBid(conn, auction.getId(), newPrice, bidder.getId());
    }

    // THEN: DB phải ghi nhận giá mới và ID người dẫn đầu mới
    assertTrue(updated);
    Auction found = auctionDAO.findById(auction.getId());
    assertEquals(0, newPrice.compareTo(found.getCurrentPrice()), "Giá hiện tại trong DB chưa đúng");
    assertEquals(bidder.getId(), found.getCurrentHighestBidderId(), "ID người dẫn đầu trong DB chưa đúng");
}

        @Test
        @DisplayName("Cập nhật thời gian kết thúc sử dụng Connection (Anti-sniping)")
        void testUpdateEndTime() throws SQLException {
            // GIVEN: Một phiên đấu giá
            Auction auction = new Auction(testItem.getId(), testSeller.getId(), BigDecimal.TEN, BigDecimal.ONE, LocalDateTime.now().plusHours(1));
            auctionDAO.insert(auction);

            // Giả lập thời gian mới tăng thêm 5 phút
            LocalDateTime newEndTime = auction.getEndTime().plusMinutes(5);

            // WHEN: Cập nhật thời gian thông qua một Connection (kiểu Transaction)
            try (Connection conn = DatabaseManager.getConnection()) {
                boolean ok = auctionDAO.updateEndTime(conn, auction.getId(), newEndTime);
                assertTrue(ok, "Cập nhật thời gian phải thành công");
            }

            // THEN: Nạp lại từ DB và so sánh (lưu ý so sánh phút để tránh sai lệch nhỏ)
            Auction found = auctionDAO.findById(auction.getId());
            assertNotNull(found);
            assertEquals(newEndTime.getMinute(), found.getEndTime().getMinute(), "Thời gian kết thúc trong DB chưa khớp");
        }
    }
}
