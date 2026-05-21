package com.team4.service;

import com.team4.dao.ItemDAO;
import com.team4.dao.UserDAO;
import com.team4.dao.impl.ItemDAOImpl;
import com.team4.dao.impl.UserDAOImpl;
import com.team4.dto.item.CreateArtRequestDTO;
import com.team4.dto.item.ItemResponseDTO;
import com.team4.model.Art;
import com.team4.model.Item;
import com.team4.model.Seller;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử tích hợp nghiệp vụ Quản lý mặt hàng (ItemService).
 * Kiểm tra việc lưu trữ đa hình các loại Item vào Database.
 */
@DisplayName("Integration Tests for ItemService")
public class ItemIntegrationTest extends BaseServiceIntegrationTest {

    private final UserDAO userDAO = new UserDAOImpl();
    private final ItemDAO itemDAO = new ItemDAOImpl();
    private final ItemService itemService = new ItemService(itemDAO, userDAO);

    @Test
    @DisplayName("Tạo vật phẩm ART: Lưu thông tin chi tiết vào database và truy vấn lại")
    void testCreateArtItem_Integration() {
        // 1. CHUẨN BỊ (GIVEN)
        String sellerId = "seller_item_test";
        Seller seller = new Seller(sellerId, "pass", "Seller Test", "s@test.com", "Art Gallery");
        userDAO.insert(seller);

        CreateArtRequestDTO request = new CreateArtRequestDTO(
                "Starry Night Replica",
                new BigDecimal("500000.00"),
                "High quality replica of Van Gogh",
                Item.ItemCategory.ART,
                "Van Gogh",
                1889,
                Art.Medium.OIL_PAINT,
                "73cm x 92cm"
        );

        // 2. THỰC THI (WHEN)
        ItemResponseDTO response = itemService.createItem(sellerId, request);

        // 3. KIỂM CHỨNG (THEN)
        assertNotNull(response.getId());
        
        // Truy vấn model thực tế từ DB để kiểm tra các trường đặc thù của ART
        Item dbItem = itemDAO.findById(response.getId());
        assertTrue(dbItem instanceof Art, "Saved item should be an instance of Art");
        
        Art savedArt = (Art) dbItem;
        assertEquals("Van Gogh", savedArt.getArtist());
        assertEquals(1889, savedArt.getCreationYear());
        assertEquals(Art.Medium.OIL_PAINT, savedArt.getMedium());
        assertEquals(0, new BigDecimal("500000.00").compareTo(savedArt.getStartingPrice()));
    }
}
