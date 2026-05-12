package com.team4.dao;

import com.team4.dao.impl.UserDAOImpl;
import com.team4.model.Admin;
import com.team4.model.Bidder;
import com.team4.model.Seller;
import com.team4.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lớp UserDAOImplTest - Kiểm thử chi tiết toàn bộ tính năng của UserDAO.
 * Đã sửa lại dữ liệu mẫu để tuân thủ các ràng buộc logic của Model.
 */
@DisplayName("Kiểm thử chuyên sâu UserDAO (Database Thật)")
public class UserDAOImplTest extends BaseDAOTest {

    private final UserDAO userDAO = new UserDAOImpl();

    @Nested
    @DisplayName("Kiểm thử lưu trữ đa hình (Polymorphism Persistence)")
    class PolymorphismTests {

        @Test
        @DisplayName("Lưu và nạp đúng đối tượng Bidder (Người mua)")
        void testInsertAndFindBidder() {
            // GIVEN: username >= 4 ký tự, phone_number đúng định dạng
            Bidder bidder = new Bidder("bidder001", "hash", "Nguyễn Bidder", "bidder@test.com", "Hà Nội", "0912345678");
            
            userDAO.insert(bidder);
            User found = userDAO.findById(bidder.getId());

            assertNotNull(found);
            assertTrue(found instanceof Bidder);
            assertEquals("0912345678", ((Bidder) found).getPhoneNumber());
        }

        @Test
        @DisplayName("Lưu và nạp đúng đối tượng Seller (Người bán)")
        void testInsertAndFindSeller() {
            Seller seller = new Seller("seller001", "hash", "Trần Seller", "seller@test.com", "Mixi Shop");
            
            userDAO.insert(seller);
            User found = userDAO.findById(seller.getId());

            assertNotNull(found);
            assertEquals("Mixi Shop", ((Seller) found).getStoreName());
        }

        @Test
        @DisplayName("Lưu và nạp đúng đối tượng Admin (Quản trị viên)")
        void testInsertAndFindAdmin() {
            // adminCodeHash phải thỏa mãn regex raw (8-128 kí tự, 1 hoa, 1 thường, 1 số, 1 đặc biệt)
            String validHash = "Admin@12345"; 
            Admin admin = new Admin("admin001", "hash", "Lê Admin", "admin@test.com", Admin.AccessLevel.SUPER_ADMIN, validHash);
            
            userDAO.insert(admin);
            User found = userDAO.findById(admin.getId());

            assertNotNull(found);
            assertEquals(Admin.AccessLevel.SUPER_ADMIN, ((Admin) found).getAccessLevel());
        }
    }

    @Nested
    @DisplayName("Kiểm thử các hàm tìm kiếm (Query Methods)")
    class QueryTests {

        @Test
        @DisplayName("Tìm kiếm theo Username thành công")
        void testFindByUsername() {
            Bidder b = new Bidder("find_user", "hash", "Name", "find@test.com", "Adr", "0912345678");
            userDAO.insert(b);

            User found = userDAO.findByUsername("find_user");
            assertNotNull(found);
            assertEquals(b.getId(), found.getId());
        }

        @Test
        @DisplayName("Lấy danh sách tất cả người dùng")
        void testFindAll() {
            userDAO.insert(new Bidder("user01", "h", "N1", "e1@t.com", "A1", "0911111111"));
            userDAO.insert(new Bidder("user02", "h", "N2", "e2@t.com", "A2", "0922222222"));

            List<User> list = userDAO.findAll();
            assertEquals(2, list.size());
        }
    }
}
