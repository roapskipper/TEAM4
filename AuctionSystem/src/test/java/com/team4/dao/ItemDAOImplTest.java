package com.team4.dao;

import com.team4.dao.impl.ItemDAOImpl;
import com.team4.dao.impl.UserDAOImpl;
import com.team4.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lớp ItemDAOImplTest - Kiểm thử kho hàng đa dạng.
 * Kế thừa từ BaseDAOTest để dọn dẹp DB tự động.
 */
@DisplayName("Kiểm thử chuyên sâu ItemDAO (Database Thật)")
public class ItemDAOImplTest extends BaseDAOTest {

    private final ItemDAO itemDAO = new ItemDAOImpl();
    private final UserDAO userDAO = new UserDAOImpl();
    private Seller testOwner;

    @BeforeEach
    void setupDependencies() {
        // Mọi Item đều cần 1 Owner hợp lệ (username >= 4 ký tự)
        testOwner = new Seller("owner001", "hash", "Chủ Hàng Xịn", "owner@test.com", "My Shop");
        userDAO.insert(testOwner);
    }

    @Nested
    @DisplayName("Kiểm thử lưu trữ đa hình (Polymorphic Storage)")
    class PolymorphismTests {

        @Test
        @DisplayName("Lưu và nạp mặt hàng ART (Tranh)")
        void testInsertArt() {
            // artist >= 2 ký tự, dimensions đúng format
            Art art = new Art("Đêm đầy sao", new BigDecimal("5000.00"), "Tranh Van Gogh nổi tiếng", 
                            testOwner.getId(), "Van Gogh", 1889, Art.Medium.OIL_PAINT, "73x92 cm");

            boolean inserted = itemDAO.insert(art);
            Item found = itemDAO.findById(art.getId());

            assertTrue(inserted);
            assertTrue(found instanceof Art);
            assertEquals("Van Gogh", ((Art) found).getArtist());
        }

        @Test
        @DisplayName("Lưu và nạp mặt hàng VEHICLE (Xe cộ)")
        void testInsertVehicle() {
            Vehicle v = new Vehicle("Honda SH 150i", new BigDecimal("8000.00"), "Xe tay ga hạng sang mới 99%", 
                                    testOwner.getId(), "Honda", "SH 150i", 2023, 1000, 
                                    Vehicle.EngineType.GASOLINE, "Trắng", true, Vehicle.Transmission.AUTOMATIC);

            itemDAO.insert(v);
            Item found = itemDAO.findById(v.getId());

            assertTrue(found instanceof Vehicle);
            assertEquals(2023, ((Vehicle) found).getManufacturingYear());
        }
    }

    @Nested
    @DisplayName("Kiểm thử các hàm lọc dữ liệu (Filters)")
    class FilterTests {

        @Test
        @DisplayName("Lọc theo danh mục (findByCategory)")
        void testFindByCategory() {
            // artist >= 2 ký tự, tên sản phẩm dài hơn 4 ký tự
            itemDAO.insert(new Art("Tranh Phong Cảnh", BigDecimal.TEN, "Mô tả tranh", testOwner.getId(), "Họa sĩ A", 2000, Art.Medium.INK, "10x10 cm"));
            itemDAO.insert(new Vehicle("Xe máy Honda", BigDecimal.TEN, "Mô tả xe", testOwner.getId(), "Honda", "Wave", 2020, 0, Vehicle.EngineType.ELECTRIC, "Đen", true, Vehicle.Transmission.CVT));

            List<Item> arts = itemDAO.findByCategory("ART");
            List<Item> vehicles = itemDAO.findByCategory("VEHICLE");

            assertEquals(1, arts.size());
            assertTrue(arts.get(0) instanceof Art);
            assertEquals(1, vehicles.size());
        }

        @Test
        @DisplayName("Lọc theo chủ sở hữu (findByOwnerId)")
        void testFindByOwnerId() {
            itemDAO.insert(new Art("Sản phẩm 01", BigDecimal.TEN, "Mô tả 01", testOwner.getId(), "Tác giả X", 2020, Art.Medium.INK, "10x10 cm"));
            itemDAO.insert(new Art("Sản phẩm 02", BigDecimal.TEN, "Mô tả 02", testOwner.getId(), "Tác giả Y", 2021, Art.Medium.INK, "20x20 cm"));

            List<Item> myItems = itemDAO.findByOwnerId(testOwner.getId());

            assertEquals(2, myItems.size());
        }
    }

    @Nested
    @DisplayName("Kiểm thử cập nhật và xóa (CRUD)")
    class CRUDTests {

        @Test
        @DisplayName("Cập nhật thông tin chung")
        void testUpdateItem() {
            Art art = new Art("Tên Cũ", BigDecimal.TEN, "Mô tả cũ", testOwner.getId(), "Họa sĩ", 2000, Art.Medium.INK, "10x10 cm");
            itemDAO.insert(art);

            art.setName("Tên Mới");
            art.setDescription("Mô tả mới dài hơn");
            boolean updated = itemDAO.update(art);

            assertTrue(updated);
            Item found = itemDAO.findById(art.getId());
            assertEquals("Tên Mới", found.getName());
        }
    }
}
