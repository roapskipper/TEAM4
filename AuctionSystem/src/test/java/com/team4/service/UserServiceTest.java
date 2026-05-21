package com.team4.service;

import com.team4.dao.UserDAO;
import com.team4.dto.auth.UserResponseDTO;
import com.team4.model.Bidder;
import com.team4.model.User;
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
 * Lớp kiểm thử UserServiceTest.
 * Môi trường: JDK 21, JUnit 5, Mockito.
 * 
 * TUÂN THỦ QUY TẮC:
 * 1. KHÔNG MOCK các class chứa dữ liệu (User, Bidder, Seller) -> Sử dụng 'new'.
 * 2. CHỈ MOCK các Interface phụ thuộc logic (UserDAO).
 * 3. Sử dụng @ExtendWith(MockitoExtension.class).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Kiểm thử nghiệp vụ Quản lý người dùng (UserService)")
public class UserServiceTest {

    @Mock
    private UserDAO userDAO;

    @InjectMocks
    private UserService userService;

    /**
     * Helper tạo đối tượng Bidder thật.
     */
    private Bidder createRealBidder(String id, String email) {
        return new Bidder(
                id,
                LocalDateTime.now(),
                "user_" + id,
                "hashed_pass",
                "Tên Người Dùng " + id,
                email,
                BigDecimal.ZERO,
                "Hà Nội",
                "0912345678"
        );
    }

    @Nested
    @DisplayName("Nghiệp vụ Tìm kiếm người dùng")
    class GetUserTests {

        @Test
        @DisplayName("Lấy User theo ID thành công")
        void testGetUserById_Success() {
            // GIVEN: Giả lập một người dùng tồn tại trong hệ thống
            String userId = "user-123";
            User realUser = createRealBidder(userId, "test@gmail.com");
            when(userDAO.findById(userId)).thenReturn(realUser);

            // WHEN: Thực hiện tìm kiếm theo ID
            UserResponseDTO result = userService.getUserById(userId);

            // THEN: 
            // 1. Phải trả về đúng người dùng đó
            assertNotNull(result);
            assertEquals(userId, result.getId());
            assertEquals("test@gmail.com", result.getEmail());
            // 2. Phải gọi xuống DAO đúng 1 lần
            verify(userDAO).findById(userId);
        }

        @Test
        @DisplayName("Lấy User theo ID thất bại - ID không tồn tại")
        void testGetUserById_NotFound() {
            // GIVEN: Không tìm thấy người dùng
            when(userDAO.findById("none")).thenReturn(null);

            // WHEN & THEN
            BusinessException ex = assertThrows(BusinessException.class, () -> 
                userService.getUserById("none")
            );
            assertEquals("User does not exist", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ Cập nhật hồ sơ (Update Profile)")
    class UpdateProfileTests {

        @Test
        @DisplayName("Cập nhật thành công - Email phải chuyển về chữ thường")
        void testUpdateProfile_Success() {
            // GIVEN: Người dùng cũ tên "Cũ", email "OLD@gmail.com"
            String userId = "user-123";
            User realUser = createRealBidder(userId, "old@gmail.com");
            when(userDAO.findById(userId)).thenReturn(realUser);
            when(userDAO.update(any(User.class))).thenReturn(true);

            // WHEN: Cập nhật tên mới, email có chữ hoa "NEW@gmail.com" và sđt mới
            UserResponseDTO updated = userService.updateProfile(userId, "Tên Mới", "NEW@gmail.com", "0987654321");

            // THEN: 
            // 1. Dữ liệu trong đối tượng phải thay đổi (email tự động về chữ thường do logic Model)
            assertEquals("Tên Mới", updated.getFullName());
            assertEquals("new@gmail.com", updated.getEmail());
            // 2. Phải gọi lệnh lưu vào DB
            verify(userDAO).update(realUser);
        }

        @Test
        @DisplayName("Cập nhật thất bại - Người dùng không tồn tại")
        void testUpdateProfile_UserNotFound() {
            // GIVEN
            when(userDAO.findById("unknown")).thenReturn(null);

            // WHEN & THEN: Kỳ vọng ném BusinessException
            BusinessException ex = assertThrows(BusinessException.class, () -> 
                userService.updateProfile("unknown", "Name", "email@test.com", "0987654321")
            );
            assertEquals("User does not exist", ex.getMessage());
            verify(userDAO, never()).update(any());
        }

        @Test
        @DisplayName("Cập nhật thất bại - Email sai định dạng")
        void testUpdateProfile_InvalidEmail() {
            // GIVEN: User tồn tại
            String userId = "user-123";
            User realUser = createRealBidder(userId, "old@gmail.com");
            when(userDAO.findById(userId)).thenReturn(realUser);

            // WHEN & THEN: Model User sẽ ném IllegalArgumentException nếu email sai định dạng
            // Service gọi user.updateProfile() nên lỗi này sẽ bắn ra ngoài
            assertThrows(IllegalArgumentException.class, () -> 
                userService.updateProfile(userId, "Name", "email-sai-dinh-dang", "0987654321")
            );
            
            // Đảm bảo không gọi update DB khi dữ liệu sai
            verify(userDAO, never()).update(any());
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ Danh sách người dùng (Admin)")
    class ListUserTests {

        @Test
        @DisplayName("Lấy tất cả người dùng thành công")
        void testGetAllUsers_Success() {
            // GIVEN: Giả lập danh sách có 2 người
            List<User> list = List.of(
                createRealBidder("1", "u1@test.com"),
                createRealBidder("2", "u2@test.com")
            );
            when(userDAO.findAll()).thenReturn(list);

            // WHEN
            List<UserResponseDTO> results = userService.getAllUsers();

            // THEN
            assertEquals(2, results.size());
            verify(userDAO).findAll();
        }

        @Test
        @DisplayName("Lấy danh sách rỗng")
        void testGetAllUsers_Empty() {
            // GIVEN: Hệ thống chưa có ai
            when(userDAO.findAll()).thenReturn(Collections.emptyList());

            // WHEN
            List<UserResponseDTO> results = userService.getAllUsers();

            // THEN: Phải trả về list rỗng, không được null
            assertNotNull(results);
            assertTrue(results.isEmpty());
        }
    }
}
