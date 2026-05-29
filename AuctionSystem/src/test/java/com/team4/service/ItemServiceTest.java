package com.team4.service;

import com.team4.dao.AuctionDAO;
import com.team4.dao.ItemDAO;
import com.team4.dao.UserDAO;
import com.team4.db.DatabaseManager;
import com.team4.dto.item.*;
import com.team4.factory.ItemRequest;
import com.team4.model.*;
import com.team4.util.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.Connection;
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

    private ItemRequest createArtRequest(String ownerId, String name) {
        ItemRequest req = new ItemRequest();
        req.setCategory(Item.ItemCategory.ART);
        req.setName(name);
        req.setDescription("Description for " + name);
        req.setStartingPrice(new BigDecimal("100000"));
        req.setOwnerId(ownerId);
        req.setMedium(Art.Medium.OIL_PAINT);
        req.setArtist("Artist Name");
        req.setCreationYear(2024);
        req.setDimensions("100x100");
        return req;
    }

    private ItemRequest createCollectibleRequest(String ownerId, String name) {
        ItemRequest req = new ItemRequest();
        req.setCategory(Item.ItemCategory.COLLECTIBLE);
        req.setName(name);
        req.setDescription("Mô tả " + name);
        req.setStartingPrice(new BigDecimal("50000.00"));
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
        req.setStartingPrice(new BigDecimal("100000.00"));
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
        req.setStartingPrice(new BigDecimal("200000.00"));
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

        @Test
        @DisplayName("Tạo mặt hàng thành công và tự động tạo phiên đấu giá tương ứng")
        void testCreateItem_AutoCreatesAuction() {
            String sellerId = "seller-123";
            Seller seller = createRealSeller(sellerId);
            CreateArtRequestDTO request = createArtRequest("Masterpiece");

            ItemDAO mockItemDAO = mock(ItemDAO.class);
            UserDAO mockUserDAO = mock(UserDAO.class);
            AuctionDAO mockAuctionDAO = mock(AuctionDAO.class);
            ItemService customItemService = new ItemService(mockItemDAO, mockUserDAO, mockAuctionDAO);

            when(mockUserDAO.findById(sellerId)).thenReturn(seller);
            when(mockItemDAO.insert(any(Item.class))).thenReturn(true);
            when(mockAuctionDAO.insert(any(Auction.class))).thenReturn(true);

            ItemResponseDTO result = customItemService.createItem(sellerId, request);

            assertNotNull(result);
            ArgumentCaptor<Auction> auctionCaptor = ArgumentCaptor.forClass(Auction.class);
            verify(mockAuctionDAO).insert(auctionCaptor.capture());
            assertEquals(Auction.AuctionStatus.PENDING, auctionCaptor.getValue().getStatus());
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
        @DisplayName("Thất bại khi giá khởi điểm dưới 50k VND")
        void testCreateItem_StartingPriceBelowMinimumThrows() {
            String sellerId = "seller-123";
            stubValidSeller(sellerId);
            ItemRequest req = createArtRequest(sellerId, "Rẻ quá");
            req.setStartingPrice(new BigDecimal("49000"));

            assertThrows(BusinessException.class, () -> itemService.createItem(sellerId, req));
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
            Art item = new Art(itemId, LocalDateTime.now(), "Vault", new BigDecimal("100"), "...", realOwner, "Artist", 2000, Art.Medium.INK, "10x10 cm");

            when(itemDAO.findById(itemId)).thenReturn(item);

            // WHEN & THEN: Kiểm tra lỗi quyền sở hữu (Ownership error)
            BusinessException ex = assertThrows(BusinessException.class, () ->
                    itemService.updateItem(hackerId, itemId, "Hack", "Hack")
            );
            assertEquals("Seller does not own this item.", ex.getMessage());
        }

        @Test
        @DisplayName("Khong cho sua gia neu auction da chay")
        void testUpdateItem_RunningAuctionPriceChangeBlocked() {
            String sellerId = "seller-1";
            String itemId = "item-running";
            Art item = new Art(itemId, LocalDateTime.now(), "Live Item", new BigDecimal("100000"), "Desc", sellerId, "Artist", 2000, Art.Medium.INK, "10x10 cm");
            Auction auction = new Auction(
                    "auction-1",
                    LocalDateTime.now(),
                    itemId,
                    sellerId,
                    null,
                    new BigDecimal("100000"),
                    new BigDecimal("100000"),
                    new BigDecimal("10000"),
                    LocalDateTime.now(),
                    LocalDateTime.now().plusHours(1),
                    Auction.AuctionStatus.RUNNING
            );

            ItemDAO mockItemDAO = mock(ItemDAO.class);
            UserDAO mockUserDAO = mock(UserDAO.class);
            AuctionDAO mockAuctionDAO = mock(AuctionDAO.class);
            ItemService customItemService = new ItemService(mockItemDAO, mockUserDAO, mockAuctionDAO);

            when(mockItemDAO.findById(itemId)).thenReturn(item);
            when(mockAuctionDAO.findByItemId(itemId)).thenReturn(auction);

            BusinessException ex = assertThrows(BusinessException.class, () ->
                    customItemService.updateItem(sellerId, itemId, "Live Item", "Desc", new BigDecimal("120000"), Item.ItemCategory.ART)
            );

            assertEquals("Starting price can only be changed while the auction is pending.", ex.getMessage());
            verify(mockItemDAO, never()).update(any(Item.class));
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
            Art item = new Art(itemId, LocalDateTime.now(), "DeleteMe", new BigDecimal("100"), "...", sellerId, "Artist", 2000, Art.Medium.INK, "10x10 cm");

            when(itemDAO.findById(itemId)).thenReturn(item);
            when(itemDAO.delete(itemId)).thenReturn(true);

            // WHEN: Thực hiện xóa qua service
            itemService.deleteItem(itemId, sellerId);

            // THEN: Xác nhận DAO xóa đã được gọi
            verify(itemDAO).delete(itemId);
        }

        @Test
        @DisplayName("Xoa mat hang kem auction PENDING thanh cong")
        void testDeleteItem_PendingAuctionDeletesAuctionAndItem() throws Exception {
            String sellerId = "seller-1";
            String itemId = "item-pending-delete";
            Art item = new Art(itemId, LocalDateTime.now(), "DeleteMe", new BigDecimal("100000"), "...", sellerId, "Artist", 2000, Art.Medium.INK, "10x10 cm");
            Auction auction = new Auction(
                    "auction-pending",
                    LocalDateTime.now(),
                    itemId,
                    sellerId,
                    null,
                    new BigDecimal("100000"),
                    new BigDecimal("100000"),
                    new BigDecimal("10000"),
                    LocalDateTime.now(),
                    LocalDateTime.now().plusHours(1),
                    Auction.AuctionStatus.PENDING
            );

            ItemDAO mockItemDAO = mock(ItemDAO.class);
            UserDAO mockUserDAO = mock(UserDAO.class);
            AuctionDAO mockAuctionDAO = mock(AuctionDAO.class);
            ItemService customItemService = new ItemService(mockItemDAO, mockUserDAO, mockAuctionDAO);
            Connection connection = mock(Connection.class);

            when(mockItemDAO.findById(itemId)).thenReturn(item);
            when(mockAuctionDAO.findByItemId(itemId)).thenReturn(auction);
            when(mockAuctionDAO.deletePendingByItemId(connection, itemId)).thenReturn(true);
            when(mockItemDAO.delete(connection, itemId)).thenReturn(true);

            try (MockedStatic<DatabaseManager> databaseManager = mockStatic(DatabaseManager.class)) {
                databaseManager.when(DatabaseManager::getConnection).thenReturn(connection);

                customItemService.deleteItem(itemId, sellerId);

                databaseManager.verify(() -> DatabaseManager.beginTransaction(connection));
                databaseManager.verify(() -> DatabaseManager.commitTransaction(connection));
            }

            verify(mockAuctionDAO).deletePendingByItemId(connection, itemId);
            verify(mockItemDAO).delete(connection, itemId);
            verify(connection).close();
            verify(mockItemDAO, never()).delete(itemId);
        }

        @Test
        @DisplayName("Khong xoa mat hang neu auction da chay")
        void testDeleteItem_RunningAuctionBlocked() {
            String sellerId = "seller-1";
            String itemId = "item-running-delete";
            Art item = new Art(itemId, LocalDateTime.now(), "Running", new BigDecimal("100000"), "...", sellerId, "Artist", 2000, Art.Medium.INK, "10x10 cm");
            Auction auction = new Auction(
                    "auction-running",
                    LocalDateTime.now(),
                    itemId,
                    sellerId,
                    null,
                    new BigDecimal("100000"),
                    new BigDecimal("100000"),
                    new BigDecimal("10000"),
                    LocalDateTime.now(),
                    LocalDateTime.now().plusHours(1),
                    Auction.AuctionStatus.RUNNING
            );

            ItemDAO mockItemDAO = mock(ItemDAO.class);
            UserDAO mockUserDAO = mock(UserDAO.class);
            AuctionDAO mockAuctionDAO = mock(AuctionDAO.class);
            ItemService customItemService = new ItemService(mockItemDAO, mockUserDAO, mockAuctionDAO);

            when(mockItemDAO.findById(itemId)).thenReturn(item);
            when(mockAuctionDAO.findByItemId(itemId)).thenReturn(auction);

            BusinessException ex = assertThrows(BusinessException.class, () ->
                    customItemService.deleteItem(itemId, sellerId)
            );

            assertEquals("Only products with pending auctions can be deleted.", ex.getMessage());
            verify(mockItemDAO, never()).delete(anyString());
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
                    new Art("1", LocalDateTime.now(), "Painting", new BigDecimal("100000"), "Description", "seller-1", "Artist", 2020, Art.Medium.OIL_PAINT, "10x10 cm")
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
