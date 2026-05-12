package com.team4.service;

import com.team4.dao.ItemDAO;
import com.team4.dao.UserDAO;
import com.team4.factory.ItemRequest;
import com.team4.model.*;
import com.team4.util.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Lớp kiểm thử ItemServiceTest.
 * Môi trường: JDK 21, JUnit 5, Mockito.
 * 
 * TUÂN THỦ QUY TẮC:
 * 1. KHÔNG MOCK các class chứa dữ liệu (DTO, Entity, Request) -> Sử dụng 'new'.
 * 2. CHỈ MOCK các Interface phụ thuộc logic (DAO).
 * 3. Sử dụng @ExtendWith(MockitoExtension.class).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Kiểm thử nghiệp vụ Quản lý mặt hàng (ItemService)")
public class ItemServiceTest {

    @Mock
    private ItemDAO itemDAO;

    @Mock
    private UserDAO userDAO;

    @InjectMocks
    private ItemService itemService;

    // Helper tạo Seller thật
    private Seller createRealSeller(String id) {
        return new Seller(id, LocalDateTime.now(), "seller", "hash", "Nguyễn Người Bán", "seller@test.com", BigDecimal.ZERO, "Shop Name", 4.88);
    }

    // Helper tạo Request thật
    private ItemRequest createArtRequest(String ownerId, String name) {
        ItemRequest req = new ItemRequest();
        req.setCategory(Item.ItemCategory.ART);
        req.setName(name);
        req.setDescription("Mô tả " + name);
        req.setStartingPrice(new BigDecimal("100.00"));
        req.setOwnerId(ownerId);
        req.setArtist("Họa sĩ");
        req.setCreationYear(2024);
        req.setMedium(Art.Medium.OIL_PAINT);
        req.setDimensions("50x50");
        return req;
    }

    @Nested
    @DisplayName("Nghiệp vụ Tạo mặt hàng (Create Item)")
    class CreateItemTests {

        @Test
        @DisplayName("Tạo mặt hàng ART thành công")
        void testCreateItem_Art_Success() {
            // GIVEN: Một Seller hợp lệ và một yêu cầu tạo tranh (ART)
            String sellerId = "seller-123";
            Seller realSeller = createRealSeller(sellerId);
            ItemRequest req = createArtRequest(sellerId, "Bức tranh quý");

            when(userDAO.findById(sellerId)).thenReturn(realSeller);
            when(itemDAO.insert(any(Item.class))).thenReturn(true);

            // WHEN: Gọi nghiệp vụ tạo mặt hàng
            Item result = itemService.createItem(sellerId, req);

            // THEN: 
            // 1. Kết quả trả về không null và đúng loại Art
            assertNotNull(result);
            assertTrue(result instanceof Art);
            assertEquals("Bức tranh quý", result.getName());
            // 2. Phải gọi DAO để lưu vào DB
            verify(itemDAO).insert(any(Item.class));
        }

        @Test
        @DisplayName("Thất bại - Người bán không tồn tại")
        void testCreateItem_SellerNotFound() {
            // GIVEN: Không tìm thấy user trong DB
            String sellerId = "unknown";
            ItemRequest req = createArtRequest(sellerId, "Vô danh");
            when(userDAO.findById(sellerId)).thenReturn(null);

            // WHEN & THEN: Kỳ vọng ném BusinessException
            BusinessException ex = assertThrows(BusinessException.class, () -> 
                itemService.createItem(sellerId, req)
            );
            assertEquals("Người bán không tồn tại.", ex.getMessage());
            verify(itemDAO, never()).insert(any());
        }

        @Test
        @DisplayName("Thất bại - Người dùng không phải Seller (ví dụ là Bidder)")
        void testCreateItem_InvalidRole() {
            // GIVEN: User tồn tại nhưng là Bidder (không có quyền bán)
            String bidderId = "bidder-456";
            Bidder realBidder = new Bidder(bidderId, LocalDateTime.now(), "bidder1", "hash", "Người mua", "b@test.com", BigDecimal.ZERO, "HN", "0912383838");
            ItemRequest req = createArtRequest(bidderId, "Tranh lậu");

            when(userDAO.findById(bidderId)).thenReturn(realBidder);

            // WHEN & THEN: Kỳ vọng ném lỗi vì không phải instance của Seller
            assertThrows(BusinessException.class, () -> itemService.createItem(bidderId, req));
        }

        @Test
        @DisplayName("Thất bại - Lỗi lưu Database")
        void testCreateItem_DatabaseError() {
            String sellerId = "seller-1";
            when(userDAO.findById(sellerId)).thenReturn(createRealSeller(sellerId));
            // Giả lập DB trả về lỗi khi insert
            when(itemDAO.insert(any())).thenReturn(false);

            assertThrows(BusinessException.class, () -> 
                itemService.createItem(sellerId, createArtRequest(sellerId, "Hỏng"))
            );
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ Cập nhật mặt hàng (Update Item)")
    class UpdateItemTests {

        @Test
        @DisplayName("Cập nhật thành công - Đúng chủ sở hữu")
        void testUpdateItem_Success() {
            // GIVEN: Mặt hàng hiện có thuộc về seller-1
            String sellerId = "seller-1";
            String itemId = "item-99";
            Art existingArt = new Art(itemId, LocalDateTime.now(), "Cũ", new BigDecimal("10"), "Mô tả cũ", sellerId, "Họa sĩ", 2000, Art.Medium.INK, "10x10");
            
            when(itemDAO.findById(itemId)).thenReturn(existingArt);
            when(itemDAO.update(any())).thenReturn(true);

            // WHEN: Cập nhật thông tin mới
            Item result = itemService.updateItem(sellerId, itemId, "Mới", "Mô tả mới");

            // THEN: Thông tin trong đối tượng trả về phải thay đổi
            assertEquals("Mới", result.getName());
            assertEquals("Mô tả mới", result.getDescription());
            verify(itemDAO).update(existingArt);
        }

        @Test
        @DisplayName("Thất bại - Cố ý cập nhật hàng của người khác (Security)")
        void testUpdateItem_WrongOwner() {
            // GIVEN: Hàng của seller-real, nhưng hacker cố cập nhật
            String realOwner = "seller-real";
            String hackerId = "hacker-123";
            String itemId = "item-secret";
            Art secretArt = new Art(itemId, LocalDateTime.now(), "Bí mật", new BigDecimal("10"), "...", realOwner, "Hitler", 2000, Art.Medium.INK, "1x1");

            when(itemDAO.findById(itemId)).thenReturn(secretArt);

            // WHEN & THEN: Kỳ vọng ném lỗi Quyền sở hữu
            BusinessException ex = assertThrows(BusinessException.class, () -> 
                itemService.updateItem(hackerId, itemId, "Hack", "Hack")
            );
            assertEquals("Lỗi về quyền sở hữu.", ex.getMessage());
            verify(itemDAO, never()).update(any());
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ Xóa mặt hàng (Delete Item)")
    class DeleteItemTests {

        @Test
        @DisplayName("Xóa thành công")
        void testDeleteItem_Success() {
            String sellerId = "seller-1";
            String itemId = "item-1";
            Art art = new Art(itemId, LocalDateTime.now(), "Xóa", new BigDecimal("10"), "...", sellerId, "AB", 2000, Art.Medium.INK, "1x1");

            when(itemDAO.findById(itemId)).thenReturn(art);
            when(itemDAO.delete(itemId)).thenReturn(true);

            // WHEN
            itemService.deleteItem(itemId, sellerId);

            // THEN
            verify(itemDAO).delete(itemId);
        }

        @Test
        @DisplayName("Thất bại - Xóa hàng không tồn tại")
        void testDeleteItem_NotFound() {
            when(itemDAO.findById("none")).thenReturn(null);

            assertThrows(BusinessException.class, () -> itemService.deleteItem("none", "any"));
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ Truy vấn (Queries)")
    class QueryTests {

        @Test
        @DisplayName("Tìm kiếm theo danh mục - Trả về danh sách")
        void testGetByCategory() {
            // GIVEN
            when(itemDAO.findByCategory("ART")).thenReturn(List.of(
                new Art("1", new BigDecimal("10"), "T1", "S1", "A1", 2020, Art.Medium.OIL_PAINT, "10x10")
            ));

            // WHEN
            List<Item> results = itemService.getItemsByCategory("ART");

            // THEN
            assertEquals(1, results.size());
            verify(itemDAO).findByCategory("ART");
        }

        @Test
        @DisplayName("Tìm kiếm theo danh mục - Không có kết quả")
        void testGetByCategory_Empty() {
            when(itemDAO.findByCategory("VEHICLE")).thenReturn(Collections.emptyList());

            List<Item> results = itemService.getItemsByCategory("VEHICLE");

            assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("Tìm kiếm theo chủ sở hữu")
        void testFindByOwner() {
            String ownerId = "owner-1";
            when(itemDAO.findByOwnerId(ownerId)).thenReturn(List.of(
                new Art("1", new BigDecimal("10"), "T1", ownerId, "A1", 2020, Art.Medium.OIL_PAINT, "10x10")
            ));

            List<Item> results = itemService.findByOwnerId(ownerId);

            assertEquals(1, results.size());
            assertEquals(ownerId, results.get(0).getOwnerId());
        }
    }
}
