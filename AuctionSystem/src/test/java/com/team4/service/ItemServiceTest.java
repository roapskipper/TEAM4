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

    private ItemRequest createCollectibleRequest(String ownerId, String name) {
        ItemRequest req = new ItemRequest();
        req.setCategory(Item.ItemCategory.COLLECTIBLE);
        req.setName(name);
        req.setDescription("Mô tả " + name);
        req.setStartingPrice(new BigDecimal("500.00"));
        req.setOwnerId(ownerId);
        req.setRarityLevel(Collectible.RarityLevel.RARE);
        req.setConditionGrade(Collectible.ConditionGrade.GOOD);
        req.setYearOfOrigin(0);
        req.setHasCertificate(false);
        return req;
    }

    private ItemRequest createElectronicsRequest(String ownerId, String name) {
        ItemRequest req = new ItemRequest();
        req.setCategory(Item.ItemCategory.ELECTRONICS);
        req.setName(name);
        req.setDescription("Mô tả " + name);
        req.setStartingPrice(new BigDecimal("1000.00"));
        req.setOwnerId(ownerId);
        req.setItemCondition(Electronics.ConditionGrade.GOOD);
        req.setWarrantyMonths(12);
        return req;
    }

    private ItemRequest createFashionRequest(String ownerId, String name) {
        ItemRequest req = new ItemRequest();
        req.setCategory(Item.ItemCategory.FASHION);
        req.setName(name);
        req.setDescription("Mô tả " + name);
        req.setStartingPrice(new BigDecimal("200.00"));
        req.setOwnerId(ownerId);
        req.setSize(Fashion.Size.M);
        req.setCondition(Fashion.ConditionGrade.GOOD);
        return req;
    }

    private ItemRequest createVehicleRequest(String ownerId, String name) {
        ItemRequest req = new ItemRequest();
        req.setCategory(Item.ItemCategory.VEHICLE);
        req.setName(name);
        req.setDescription("Mô tả " + name);
        req.setStartingPrice(new BigDecimal("50000.00"));
        req.setOwnerId(ownerId);
        req.setOdo(10000);
        req.setEngineType(Vehicle.EngineType.GASOLINE);
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
            assertEquals("Seller does not exist.", ex.getMessage());
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

        @Test
        @DisplayName("Kiểm tra flow tạo Item và validate error message consistency")
        void testItemCreationFlow_ValidationOrderAndErrorConsistency() {
            String sellerId = "seller-flow-1";
            Seller realSeller = createRealSeller(sellerId);
            when(userDAO.findById(sellerId)).thenReturn(realSeller);
            
            // 1. Common validation fails first
            ItemRequest req = createArtRequest(sellerId, "   "); // Blank name
            BusinessException ex1 = assertThrows(BusinessException.class, () -> itemService.createItem(sellerId, req));
            assertEquals(Item.ValidationMessages.NAME_REQUIRED, ex1.getMessage());
            
            // 2. Fix common, category validation fails next
            req.setName("Tranh hợp lệ");
            req.setMedium(null); // Missing required field for ART
            BusinessException ex2 = assertThrows(BusinessException.class, () -> itemService.createItem(sellerId, req));
            assertEquals("Art medium is required.", ex2.getMessage()); // Message from Art validation
            
            // 3. Fix category, creation succeeds
            req.setMedium(Art.Medium.OIL_PAINT);
            when(itemDAO.insert(any(Item.class))).thenReturn(true);
            Item result = itemService.createItem(sellerId, req);
            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("Validation thông tin chung (Common fields)")
    class CommonItemValidationTests {

        private void stubValidSeller(String sellerId) {
            when(userDAO.findById(sellerId)).thenReturn(createRealSeller(sellerId));
        }

        @Test
        @DisplayName("Thất bại - Tên sản phẩm trống")
        void testCreateItem_BlankName() {
            String sellerId = "seller-123";
            stubValidSeller(sellerId);
            ItemRequest req = createArtRequest(sellerId, "   ");

            BusinessException ex = assertThrows(BusinessException.class, () -> itemService.createItem(sellerId, req));
            assertEquals(Item.ValidationMessages.NAME_REQUIRED, ex.getMessage());
            verify(itemDAO, never()).insert(any());
        }

        @Test
        @DisplayName("Thất bại - Tên sản phẩm vượt quá 255 ký tự")
        void testCreateItem_NameTooLong() {
            String sellerId = "seller-123";
            stubValidSeller(sellerId);
            ItemRequest req = createArtRequest(sellerId, "a".repeat(256));

            BusinessException ex = assertThrows(BusinessException.class, () -> itemService.createItem(sellerId, req));
            assertEquals(Item.ValidationMessages.NAME_TOO_LONG, ex.getMessage());
            verify(itemDAO, never()).insert(any());
        }

        @Test
        @DisplayName("Thất bại - Giá khởi điểm null")
        void testCreateItem_NullStartingPrice() {
            String sellerId = "seller-123";
            stubValidSeller(sellerId);
            ItemRequest req = createArtRequest(sellerId, "Hợp lệ");
            req.setStartingPrice(null);

            BusinessException ex = assertThrows(BusinessException.class, () -> itemService.createItem(sellerId, req));
            assertEquals(Item.ValidationMessages.STARTING_PRICE_REQUIRED, ex.getMessage());
            verify(itemDAO, never()).insert(any());
        }

        @Test
        @DisplayName("Thất bại - Giá khởi điểm âm")
        void testCreateItem_NegativeStartingPrice() {
            String sellerId = "seller-123";
            stubValidSeller(sellerId);
            ItemRequest req = createArtRequest(sellerId, "Hợp lệ");
            req.setStartingPrice(new BigDecimal("-1"));

            BusinessException ex = assertThrows(BusinessException.class, () -> itemService.createItem(sellerId, req));
            assertEquals(Item.ValidationMessages.STARTING_PRICE_NON_NEGATIVE, ex.getMessage());
            verify(itemDAO, never()).insert(any());
        }

        @Test
        @DisplayName("Thất bại - Thiếu danh mục")
        void testCreateItem_NullCategory() {
            String sellerId = "seller-123";
            stubValidSeller(sellerId);
            ItemRequest req = createArtRequest(sellerId, "Hợp lệ");
            req.setCategory(null);

            BusinessException ex = assertThrows(BusinessException.class, () -> itemService.createItem(sellerId, req));
            assertEquals(Item.ValidationMessages.CATEGORY_REQUIRED, ex.getMessage());
            verify(itemDAO, never()).insert(any());
        }

        @Test
        @DisplayName("Thất bại - Mô tả trống")
        void testCreateItem_BlankDescription() {
            String sellerId = "seller-123";
            stubValidSeller(sellerId);
            ItemRequest req = createArtRequest(sellerId, "Hợp lệ");
            req.setDescription("   ");

            BusinessException ex = assertThrows(BusinessException.class, () -> itemService.createItem(sellerId, req));
            assertEquals(Item.ValidationMessages.DESCRIPTION_REQUIRED, ex.getMessage());
            verify(itemDAO, never()).insert(any());
        }

        @Test
        @DisplayName("Thất bại - Mô tả vượt quá 2000 ký tự")
        void testCreateItem_DescriptionTooLong() {
            String sellerId = "seller-123";
            stubValidSeller(sellerId);
            ItemRequest req = createArtRequest(sellerId, "Hợp lệ");
            req.setDescription("x".repeat(2001));

            BusinessException ex = assertThrows(BusinessException.class, () -> itemService.createItem(sellerId, req));
            assertEquals(Item.ValidationMessages.DESCRIPTION_TOO_LONG, ex.getMessage());
            verify(itemDAO, never()).insert(any());
        }

        @Test
        @DisplayName("Thất bại - Thiếu ownerId")
        void testCreateItem_BlankOwnerId() {
            String sellerId = "seller-123";
            stubValidSeller(sellerId);
            ItemRequest req = createArtRequest(sellerId, "Hợp lệ");
            req.setOwnerId("  ");

            BusinessException ex = assertThrows(BusinessException.class, () -> itemService.createItem(sellerId, req));
            assertEquals(Item.ValidationMessages.OWNER_ID_REQUIRED, ex.getMessage());
            verify(itemDAO, never()).insert(any());
        }

        @Test
        @DisplayName("Thành công - Giá khởi điểm bằng 0")
        void testCreateItem_ZeroStartingPriceAllowed() {
            String sellerId = "seller-123";
            stubValidSeller(sellerId);
            ItemRequest req = createArtRequest(sellerId, "Miễn phí");
            req.setStartingPrice(BigDecimal.ZERO);
            when(itemDAO.insert(any(Item.class))).thenReturn(true);

            Item result = itemService.createItem(sellerId, req);

            assertNotNull(result);
            assertEquals(0, result.getStartingPrice().compareTo(BigDecimal.ZERO));
            verify(itemDAO).insert(any(Item.class));
        }
    }

    @Nested
    @DisplayName("Default normalization khi tạo mặt hàng")
    class CategoryDefaultNormalizationTests {

        private void stubValidSeller(String sellerId) {
            when(userDAO.findById(sellerId)).thenReturn(createRealSeller(sellerId));
        }

        @Test
        @DisplayName("Art: artist trống -> Unknown")
        void testCreateItem_Art_BlankArtistDefaultsToUnknown() {
            String sellerId = "seller-def-1";
            stubValidSeller(sellerId);
            ItemRequest req = createArtRequest(sellerId, "Tranh");
            req.setArtist("  ");
            req.setCreationYear(0);
            when(itemDAO.insert(any(Item.class))).thenReturn(true);

            Art art = (Art) itemService.createItem(sellerId, req);

            assertEquals("Unknown", art.getArtist());
            assertEquals(0, art.getCreationYear());
        }

        @Test
        @DisplayName("Electronics: brand/model trống -> Unknown")
        void testCreateItem_Electronics_BlankBrandModel() {
            String sellerId = "seller-def-1";
            stubValidSeller(sellerId);
            ItemRequest req = createElectronicsRequest(sellerId, "Laptop");
            req.setBrand("");
            req.setModel(null);
            when(itemDAO.insert(any(Item.class))).thenReturn(true);

            Electronics elec = (Electronics) itemService.createItem(sellerId, req);

            assertEquals("Unknown", elec.getBrand());
            assertEquals("Unknown", elec.getModel());
        }

        @Test
        @DisplayName("Fashion: gender null -> UNISEX")
        void testCreateItem_Fashion_NullGenderDefaultsToUnisex() {
            String sellerId = "seller-def-1";
            stubValidSeller(sellerId);
            ItemRequest req = createFashionRequest(sellerId, "Áo");
            req.setGender(null);
            when(itemDAO.insert(any(Item.class))).thenReturn(true);

            Fashion fash = (Fashion) itemService.createItem(sellerId, req);

            assertEquals(Fashion.Gender.UNISEX, fash.getGender());
        }

        @Test
        @DisplayName("Vehicle: transmission null -> OTHER")
        void testCreateItem_Vehicle_NullTransmissionDefaultsToOther() {
            String sellerId = "seller-def-1";
            stubValidSeller(sellerId);
            ItemRequest req = createVehicleRequest(sellerId, "Xe");
            req.setTransmission(null);
            when(itemDAO.insert(any(Item.class))).thenReturn(true);

            Vehicle veh = (Vehicle) itemService.createItem(sellerId, req);

            assertEquals(Vehicle.Transmission.OTHER, veh.getTransmission());
        }

        @Test
        @DisplayName("Art: default artist không bỏ qua validation medium bắt buộc")
        void testCreateItem_Art_DefaultsDoNotBypassRequiredMedium() {
            String sellerId = "seller-def-1";
            stubValidSeller(sellerId);
            ItemRequest req = createArtRequest(sellerId, "Tranh lỗi");
            req.setArtist(null);
            req.setMedium(null);

            assertThrows(BusinessException.class, () -> itemService.createItem(sellerId, req));
            verify(itemDAO, never()).insert(any());
        }

        @Test
        @DisplayName("Electronics: default brand không bỏ qua validation condition bắt buộc")
        void testCreateItem_Electronics_DefaultsDoNotBypassRequiredCondition() {
            String sellerId = "seller-def-1";
            stubValidSeller(sellerId);
            ItemRequest req = createElectronicsRequest(sellerId, "Máy hỏng");
            req.setBrand("  ");
            req.setItemCondition(null);

            assertThrows(BusinessException.class, () -> itemService.createItem(sellerId, req));
            verify(itemDAO, never()).insert(any());
        }
    }

    @Nested
    @DisplayName("Validation Collectible (Đồ sưu tầm)")
    class CollectibleValidationTests {

        private void stubValidSeller(String sellerId) {
            when(userDAO.findById(sellerId)).thenReturn(createRealSeller(sellerId));
        }

        @Test
        @DisplayName("Thất bại - Thiếu độ hiếm (rarityLevel)")
        void testCreateItem_MissingRarityLevel() {
            String sellerId = "seller-col-1";
            stubValidSeller(sellerId);
            ItemRequest req = createCollectibleRequest(sellerId, "Tem cổ");
            req.setRarityLevel(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> itemService.createItem(sellerId, req));
            assertEquals(Collectible.ValidationMessages.RARITY_REQUIRED, ex.getMessage());
            verify(itemDAO, never()).insert(any());
        }

        @Test
        @DisplayName("Thất bại - Thiếu tình trạng (conditionGrade)")
        void testCreateItem_MissingConditionGrade() {
            String sellerId = "seller-col-1";
            stubValidSeller(sellerId);
            ItemRequest req = createCollectibleRequest(sellerId, "Đồng xu");
            req.setConditionGrade(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> itemService.createItem(sellerId, req));
            assertEquals(Collectible.ValidationMessages.CONDITION_REQUIRED, ex.getMessage());
            verify(itemDAO, never()).insert(any());
        }

        @Test
        @DisplayName("Thành công - Tạo Collectible hợp lệ")
        void testCreateItem_Collectible_Success() {
            String sellerId = "seller-col-1";
            stubValidSeller(sellerId);
            ItemRequest req = createCollectibleRequest(sellerId, "Tem hiếm");
            req.setYearOfOrigin(1945);
            req.setOrigin("Việt Nam");
            req.setHasCertificate(true);
            when(itemDAO.insert(any(Item.class))).thenReturn(true);

            Item result = itemService.createItem(sellerId, req);

            assertNotNull(result);
            assertTrue(result instanceof Collectible);
            Collectible c = (Collectible) result;
            assertEquals(Collectible.RarityLevel.RARE, c.getRarityLevel());
            assertEquals(Collectible.ConditionGrade.GOOD, c.getConditionGrade());
            assertEquals(1945, c.getYearOfOrigin());
            assertEquals("Việt Nam", c.getOrigin());
            assertTrue(c.isHasCertificate());
            verify(itemDAO).insert(any(Item.class));
        }

        @Test
        @DisplayName("Thành công - Xuất xứ trống mặc định Unknown")
        void testCreateItem_Collectible_BlankOriginDefaultsToUnknown() {
            String sellerId = "seller-col-1";
            stubValidSeller(sellerId);
            ItemRequest req = createCollectibleRequest(sellerId, "Huy hiệu");
            req.setOrigin("   ");
            when(itemDAO.insert(any(Item.class))).thenReturn(true);

            Item result = itemService.createItem(sellerId, req);

            assertEquals("Unknown", ((Collectible) result).getOrigin());
        }
    }

    @Nested
    @DisplayName("Validation Art (Tranh ảnh / Nghệ thuật)")
    class ArtValidationTests {

        private void stubValidSeller(String sellerId) {
            when(userDAO.findById(sellerId)).thenReturn(createRealSeller(sellerId));
        }

        @Test
        @DisplayName("Thất bại - Thiếu chất liệu (medium)")
        void testCreateItem_MissingMedium() {
            String sellerId = "seller-art-1";
            stubValidSeller(sellerId);
            ItemRequest req = createArtRequest(sellerId, "Tranh đẹp");
            req.setMedium(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> itemService.createItem(sellerId, req));
            assertEquals("Art medium is required.", ex.getMessage());
            verify(itemDAO, never()).insert(any());
        }

        @Test
        @DisplayName("Thất bại - Năm sáng tác không hợp lệ")
        void testCreateItem_InvalidCreationYear() {
            String sellerId = "seller-art-1";
            stubValidSeller(sellerId);
            ItemRequest req = createArtRequest(sellerId, "Tranh tương lai");
            req.setCreationYear(3000); // Tương lai

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> itemService.createItem(sellerId, req));
            assertTrue(ex.getMessage().contains("Invalid creation year"));
            verify(itemDAO, never()).insert(any());
        }
    }

    @Nested
    @DisplayName("Validation Electronics (Đồ điện tử)")
    class ElectronicsValidationTests {

        private void stubValidSeller(String sellerId) {
            when(userDAO.findById(sellerId)).thenReturn(createRealSeller(sellerId));
        }

        @Test
        @DisplayName("Thất bại - Số tháng bảo hành âm")
        void testCreateItem_InvalidWarrantyMonths() {
            String sellerId = "seller-elec-1";
            stubValidSeller(sellerId);
            ItemRequest req = createElectronicsRequest(sellerId, "Laptop cũ");
            req.setWarrantyMonths(-1);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> itemService.createItem(sellerId, req));
            assertEquals("Warranty months cannot be negative.", ex.getMessage());
            verify(itemDAO, never()).insert(any());
        }
    }

    @Nested
    @DisplayName("Validation Fashion (Thời trang)")
    class FashionValidationTests {

        private void stubValidSeller(String sellerId) {
            when(userDAO.findById(sellerId)).thenReturn(createRealSeller(sellerId));
        }

        @Test
        @DisplayName("Thất bại - Thiếu kích cỡ (size)")
        void testCreateItem_MissingSize() {
            String sellerId = "seller-fash-1";
            stubValidSeller(sellerId);
            ItemRequest req = createFashionRequest(sellerId, "Áo thun");
            req.setSize(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> itemService.createItem(sellerId, req));
            assertEquals("Fashion size is required.", ex.getMessage());
            verify(itemDAO, never()).insert(any());
        }
    }

    @Nested
    @DisplayName("Validation Vehicle (Phương tiện)")
    class VehicleValidationTests {

        private void stubValidSeller(String sellerId) {
            when(userDAO.findById(sellerId)).thenReturn(createRealSeller(sellerId));
        }

        @Test
        @DisplayName("Thất bại - Odo âm")
        void testCreateItem_InvalidOdo() {
            String sellerId = "seller-veh-1";
            stubValidSeller(sellerId);
            ItemRequest req = createVehicleRequest(sellerId, "Xe máy");
            req.setOdo(-100);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> itemService.createItem(sellerId, req));
            assertEquals("Odometer (odo) cannot be negative.", ex.getMessage());
            verify(itemDAO, never()).insert(any());
        }

        @Test
        @DisplayName("Thất bại - Năm sản xuất không hợp lệ")
        void testCreateItem_InvalidManufacturingYear() {
            String sellerId = "seller-veh-1";
            stubValidSeller(sellerId);
            ItemRequest req = createVehicleRequest(sellerId, "Xe ô tô");
            req.setManufacturingYear(1800); // Quá cũ

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> itemService.createItem(sellerId, req));
            assertTrue(ex.getMessage().contains("Invalid production year"));
            verify(itemDAO, never()).insert(any());
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
            assertEquals("Ownership error.", ex.getMessage());
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
