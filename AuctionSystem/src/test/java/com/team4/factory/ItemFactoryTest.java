package com.team4.factory;

import com.team4.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lớp ItemFactoryTest - Kiểm thử logic khởi tạo mặt hàng đa hình.
 * Mục đích: Đảm bảo dữ liệu từ ItemRequest được chuyển đổi chính xác sang các Class con của Item.
 */
@DisplayName("Kiểm thử Tầng Factory (Item Creation)")
public class ItemFactoryTest {

    @Nested
    @DisplayName("Kiểm thử ArtFactory (Tranh nghệ thuật)")
    class ArtFactoryTests {
        @Test
        @DisplayName("Khởi tạo đúng đối tượng Art và các trường đặc thù")
        void testCreateArt() {
            // GIVEN: Một yêu cầu tạo tranh ART
            ItemRequest req = new ItemRequest();
            req.setName("Đêm đầy sao");
            req.setStartingPrice(new BigDecimal("5000.00"));
            req.setDescription("Tranh của Van Gogh");
            req.setOwnerId("owner-001");
            req.setArtist("Van Gogh");
            req.setCreationYear(1889);
            req.setMedium(Art.Medium.OIL_PAINT);
            req.setDimensions("73x92 cm");

            ItemFactory factory = new ArtFactory();

            // WHEN: Thực hiện tạo
            Item result = factory.createItem(req);

            // THEN: 
            // 1. Phải đúng loại Class Art
            assertTrue(result instanceof Art);
            Art art = (Art) result;
            // 2. Các trường thông thường khớp
            assertEquals("Đêm đầy sao", art.getName());
            assertEquals(Item.ItemCategory.ART, art.getCategory());
            // 3. Các trường đặc thù khớp
            assertEquals("Van Gogh", art.getArtist());
            assertEquals(1889, art.getCreationYear());
            assertEquals(Art.Medium.OIL_PAINT, art.getMedium());
        }
    }

    @Nested
    @DisplayName("Kiểm thử VehicleFactory (Xe cộ)")
    class VehicleFactoryTests {
        @Test
        @DisplayName("Khởi tạo đúng đối tượng Vehicle và các trường kỹ thuật")
        void testCreateVehicle() {
            // GIVEN: Một yêu cầu tạo xe máy
            ItemRequest req = new ItemRequest();
            req.setName("Honda SH");
            req.setStartingPrice(new BigDecimal("80.00"));
            req.setOwnerId("owner-001");
            req.setDescription("Xe mới");
            req.setBrand("Honda");
            req.setModel("SH 150i");
            req.setManufacturingYear(2023);
            req.setOdo(500);
            req.setEngineType(Vehicle.EngineType.GASOLINE);
            req.setTransmission(Vehicle.Transmission.AUTOMATIC);
            req.setHasLegalPapers(true);

            ItemFactory factory = new VehicleFactory();

            // WHEN
            Item result = factory.createItem(req);

            // THEN
            assertTrue(result instanceof Vehicle);
            Vehicle v = (Vehicle) result;
            assertEquals("Honda", v.getBrand());
            assertEquals(2023, v.getManufacturingYear());
            assertEquals(Vehicle.Transmission.AUTOMATIC, v.getTransmission());
            assertTrue(v.hasLegalPapers());
        }
    }

    @Nested
    @DisplayName("Kiểm thử ElectronicsFactory (Đồ điện tử)")
    class ElectronicsFactoryTests {
        @Test
        @DisplayName("Khởi tạo đúng đối tượng Electronics")
        void testCreateElectronics() {
            ItemRequest req = new ItemRequest();
            req.setName("iPhone 15");
            req.setStartingPrice(new BigDecimal("1000"));
            req.setOwnerId("owner-001");
            req.setDescription("Máy zin");
            req.setBrand("Apple");
            req.setModel("iPhone 15 Pro");
            req.setWarrantyMonths(12);
            req.setFullyFunctional(true);
            req.setItemCondition(Electronics.ConditionGrade.EXCELLENT);

            ItemFactory factory = new ElectronicsFactory();
            Item result = factory.createItem(req);

            assertTrue(result instanceof Electronics);
            Electronics e = (Electronics) result;
            assertEquals(12, e.getWarrantyMonths());
            assertTrue(e.isFullyFunctional());
            assertEquals(Electronics.ConditionGrade.EXCELLENT, e.getItemCondition());
        }
    }

    @Nested
    @DisplayName("Kiểm thử CollectibleFactory (Đồ sưu tầm)")
    class CollectibleFactoryTests {
        @Test
        @DisplayName("Khởi tạo đúng đối tượng Collectible")
        void testCreateCollectible() {
            ItemRequest req = new ItemRequest();
            req.setName("Tem cổ");
            req.setStartingPrice(new BigDecimal("500"));
            req.setOwnerId("o1");
            req.setDescription("Tem hiếm");
            req.setYearOfOrigin(1945);
            req.setRarityLevel(Collectible.RarityLevel.ULTRA_RARE);
            req.setConditionGrade(Collectible.ConditionGrade.MINT);
            req.setHasCertificate(true);

            ItemFactory factory = new CollectibleFactory();
            Item result = factory.createItem(req);

            assertTrue(result instanceof Collectible);
            Collectible c = (Collectible) result;
            assertEquals(1945, c.getYearOfOrigin());
            assertEquals(Collectible.RarityLevel.ULTRA_RARE, c.getRarityLevel());
            assertTrue(c.isHasCertificate());
        }

        @Test
        @DisplayName("Thất bại - Thiếu rarityLevel")
        void testCreateCollectible_MissingRarity() {
            ItemRequest req = new ItemRequest();
            req.setName("Tem");
            req.setStartingPrice(new BigDecimal("500"));
            req.setOwnerId("o1");
            req.setDescription("Mô tả");
            req.setConditionGrade(Collectible.ConditionGrade.GOOD);

            ItemFactory factory = new CollectibleFactory();
            assertThrows(IllegalArgumentException.class, () -> factory.createItem(req));
        }
    }

    @Nested
    @DisplayName("Kiểm thử FashionFactory (Thời trang)")
    class FashionFactoryTests {
        @Test
        @DisplayName("Khởi tạo đúng đối tượng Fashion")
        void testCreateFashion() {
            ItemRequest req = new ItemRequest();
            req.setName("Áo Gucci");
            req.setStartingPrice(new BigDecimal("200"));
            req.setOwnerId("o1");
            req.setDescription("Áo khoác");
            req.setBrand("Gucci");
            req.setSize(Fashion.Size.L);
            req.setGender(Fashion.Gender.UNISEX);
            req.setCondition(Fashion.ConditionGrade.VERY_GOOD);
            req.setAuthentic(true);

            ItemFactory factory = new FashionFactory();
            Item result = factory.createItem(req);

            assertTrue(result instanceof Fashion);
            Fashion f = (Fashion) result;
            assertEquals(Fashion.Size.L, f.getSize());
            assertEquals(Fashion.Gender.UNISEX, f.getGender());
            assertTrue(f.isAuthentic());
        }
    }
}
