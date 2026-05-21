package com.team4.service;

import com.team4.dao.ItemDAO;
import com.team4.dao.UserDAO;
import com.team4.dto.item.*;
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
 * Đảm bảo các nghiệp vụ về mặt hàng hoạt động đúng với cấu trúc DTO và Mapper mới.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests for ItemService")
public class ItemServiceTest {

    @Mock
    private ItemDAO itemDAO;

    @Mock
    private UserDAO userDAO;

    @InjectMocks
    private ItemService itemService;

    // Helper tạo Seller thật để dùng trong các ca kiểm thử
    private Seller createRealSeller(String id) {
        return new Seller(id, LocalDateTime.now(), "seller_test", "hashed_pwd", "Test Seller", "seller@test.com", BigDecimal.ZERO, "Test Shop", 5.0);
    }

    // Helper tạo yêu cầu tạo vật phẩm ART
    private CreateArtRequestDTO createArtRequest(String name) {
        return new CreateArtRequestDTO(
                name,
                new BigDecimal("100000"),
                "Description for " + name,
                Item.ItemCategory.ART,
                "Artist Name",
                2024,
                Art.Medium.OIL_PAINT,
                "100x100"
        );
    }

    @Nested
    @DisplayName("Nghiệp vụ Tạo mặt hàng (createItem)")
    class CreateItemTests {

        @Test
        @DisplayName("Tạo mặt hàng ART thành công")
        void testCreateItem_Art_Success() {
            // GIVEN: Một người bán hợp lệ và yêu cầu tạo vật phẩm
            String sellerId = "seller-123";
            Seller seller = createRealSeller(sellerId);
            CreateArtRequestDTO request = createArtRequest("Masterpiece");

            when(userDAO.findById(sellerId)).thenReturn(seller);
            when(itemDAO.insert(any(Item.class))).thenReturn(true);

            // WHEN: Thực hiện tạo vật phẩm qua service
            ItemResponseDTO result = itemService.createItem(sellerId, request);

            // THEN: Kết quả phải là DTO và đã được lưu vào database
            assertNotNull(result);
            assertEquals("Masterpiece", result.getName());
            assertEquals(sellerId, result.getOwnerId());
            verify(itemDAO).insert(any(Item.class));
        }

        @Test
        @DisplayName("Thất bại khi người bán không tồn tại")
        void testCreateItem_SellerNotFound() {
            // GIVEN: ID người bán không có trong hệ thống
            String sellerId = "unknown";
            CreateArtRequestDTO request = createArtRequest("Ghost Item");
            when(userDAO.findById(sellerId)).thenReturn(null);

            // WHEN & THEN: Phải ném BusinessException với thông báo tiếng Anh
            BusinessException ex = assertThrows(BusinessException.class, () -> 
                itemService.createItem(sellerId, request)
            );
            assertEquals("Seller does not exist.", ex.getMessage());
            verify(itemDAO, never()).insert(any());
        }

        @Test
        @DisplayName("Thất bại khi người dùng không có quyền bán (là Bidder)")
        void testCreateItem_InvalidRole() {
            // GIVEN: Người dùng là Bidder cố tình tạo mặt hàng
            String bidderId = "bidder-456";
            Bidder bidder = new Bidder(bidderId, LocalDateTime.now(), "bidder", "pwd", "Bidder User", "b@test.com", BigDecimal.ZERO, "Address", "0123456789");
            
            when(userDAO.findById(bidderId)).thenReturn(bidder);

            // WHEN & THEN: Kiểm tra ném lỗi
            assertThrows(BusinessException.class, () -> itemService.createItem(bidderId, createArtRequest("Fake")));
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ Cập nhật mặt hàng (updateItem)")
    class UpdateItemTests {

        @Test
        @DisplayName("Cập nhật thành công bởi chủ sở hữu")
        void testUpdateItem_Success() {
            // GIVEN: Mặt hàng tồn tại và thuộc về sellerId
            String sellerId = "seller-1";
            String itemId = "item-99";
            Art existingItem = new Art(itemId, LocalDateTime.now(), "Old Name", new BigDecimal("50000"), "Old Desc", sellerId, "Artist", 2000, Art.Medium.INK, "10x10");
            
            when(itemDAO.findById(itemId)).thenReturn(existingItem);
            when(itemDAO.update(any())).thenReturn(true);

            // WHEN: Thực hiện cập nhật
            ItemResponseDTO result = itemService.updateItem(sellerId, itemId, "New Name", "New Desc");

            // THEN: DTO trả về phản ánh thông tin mới
            assertEquals("New Name", result.getName());
            verify(itemDAO).update(existingItem);
        }

        @Test
        @DisplayName("Thất bại khi cố tình cập nhật hàng của người khác (Security)")
        void testUpdateItem_WrongOwner() {
            // GIVEN: Hacker cố cập nhật hàng của real-seller
            String realOwner = "real-seller";
            String hackerId = "hacker-007";
            String itemId = "item-vault";
            Art item = new Art(itemId, LocalDateTime.now(), "Vault", new BigDecimal("100"), "...", realOwner, "A", 2000, Art.Medium.INK, "1x1");

            when(itemDAO.findById(itemId)).thenReturn(item);

            // WHEN & THEN: Kiểm tra lỗi quyền sở hữu (Ownership error)
            BusinessException ex = assertThrows(BusinessException.class, () -> 
                itemService.updateItem(hackerId, itemId, "Hack", "Hack")
            );
            assertEquals("Ownership error.", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ Xóa mặt hàng (deleteItem)")
    class DeleteItemTests {

        @Test
        @DisplayName("Xóa mặt hàng thành công")
        void testDeleteItem_Success() {
            String sellerId = "seller-1";
            String itemId = "item-delete";
            Art item = new Art(itemId, LocalDateTime.now(), "DeleteMe", new BigDecimal("100"), "...", sellerId, "A", 2000, Art.Medium.INK, "1x1");

            when(itemDAO.findById(itemId)).thenReturn(item);
            when(itemDAO.delete(itemId)).thenReturn(true);

            // WHEN: Thực hiện xóa qua service
            itemService.deleteItem(itemId, sellerId);

            // THEN: Xác nhận DAO xóa đã được gọi
            verify(itemDAO).delete(itemId);
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ Truy vấn (Queries)")
    class QueryTests {

        @Test
        @DisplayName("Lấy danh sách theo danh mục (Trả về DTO)")
        void testGetByCategory() {
            // GIVEN
            when(itemDAO.findByCategory("ART")).thenReturn(List.of(
                new Art("1", new BigDecimal("100000"), "Painting", "seller-1", "Artist", 2020, Art.Medium.OIL_PAINT, "10x10")
            ));

            // WHEN: Lấy danh sách
            List<ItemResponseDTO> results = itemService.getItemsByCategory("ART");

            // THEN: Đảm bảo kết quả là DTO
            assertEquals(1, results.size());
            assertEquals("Painting", results.get(0).getName());
        }

        @Test
        @DisplayName("Lấy danh sách theo ID người sở hữu")
        void testFindByOwner() {
            String ownerId = "owner-100";
            when(itemDAO.findByOwnerId(ownerId)).thenReturn(List.of(
                new Art("id-1", new BigDecimal("1000"), "My Item", ownerId, "Me", 2024, Art.Medium.INK, "5x5")
            ));

            List<ItemResponseDTO> results = itemService.findByOwnerId(ownerId);

            assertFalse(results.isEmpty());
            assertEquals(ownerId, results.get(0).getOwnerId());
        }
    }
}
