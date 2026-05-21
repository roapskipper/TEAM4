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
 * Kiểm thử nghiệp vụ UserService.
 * Đảm bảo thông tin người dùng được bảo mật qua DTO và các nghiệp vụ cập nhật hồ sơ hoạt động đúng.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests for UserService")
public class UserServiceTest {

    @Mock
    private UserDAO userDAO;

    @InjectMocks
    private UserService userService;

    // Helper tạo Bidder thật
    private Bidder createRealBidder(String id, String email) {
        return new Bidder(id, LocalDateTime.now(), "user_" + id, "hashed_pass", "Test User " + id, email, BigDecimal.ZERO, "Hanoi", "0912345678");
    }

    @Nested
    @DisplayName("Nghiệp vụ Tìm kiếm người dùng (getUserById)")
    class GetUserTests {

        @Test
        @DisplayName("Lấy thông tin người dùng thành công (Trả về DTO)")
        void testGetUserById_Success() {
            // GIVEN: Người dùng tồn tại trong hệ thống
            String userId = "user-123";
            User realUser = createRealBidder(userId, "test@gmail.com");
            when(userDAO.findById(userId)).thenReturn(realUser);

            // WHEN: Thực hiện lấy thông tin qua Service
            UserResponseDTO result = userService.getUserById(userId);

            // THEN: Phải trả về DTO hợp lệ, không lộ mật khẩu
            assertNotNull(result);
            assertEquals(userId, result.getId());
            assertEquals("test@gmail.com", result.getEmail());
            verify(userDAO).findById(userId);
        }

        @Test
        @DisplayName("Thất bại khi tìm kiếm ID không tồn tại")
        void testGetUserById_NotFound() {
            // GIVEN
            when(userDAO.findById("none")).thenReturn(null);

            // WHEN & THEN: Phải ném BusinessException theo logic mới của UserService
            assertThrows(BusinessException.class, () -> userService.getUserById("none"));
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ Cập nhật hồ sơ (updateProfile)")
    class UpdateProfileTests {

        @Test
        @DisplayName("Cập nhật hồ sơ thành công")
        void testUpdateProfile_Success() {
            // GIVEN: Người dùng hiện tại
            String userId = "user-123";
            User realUser = createRealBidder(userId, "old@gmail.com");
            when(userDAO.findById(userId)).thenReturn(realUser);
            when(userDAO.update(any(User.class))).thenReturn(true);

            // WHEN: Cập nhật thông tin mới
            UserResponseDTO updated = userService.updateProfile(userId, "New Name", "NEW@gmail.com", "0987654321");

            // THEN: DTO trả về phản ánh thông tin đã chuẩn hóa (email viết thường)
            assertEquals("New Name", updated.getFullName());
            assertEquals("new@gmail.com", updated.getEmail());
            verify(userDAO).update(realUser);
        }

        @Test
        @DisplayName("Thất bại khi cập nhật người dùng không tồn tại")
        void testUpdateProfile_UserNotFound() {
            when(userDAO.findById("unknown")).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class, () -> 
                userService.updateProfile("unknown", "Name", "e@t.com", "0987654321")
            );
            assertEquals("User does not exist", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ Quản trị (getAllUsers)")
    class AdminUserTests {

        @Test
        @DisplayName("Lấy danh sách tất cả người dùng (Admin)")
        void testGetAllUsers_Success() {
            // GIVEN
            List<User> list = List.of(createRealBidder("1", "u1@t.com"), createRealBidder("2", "u2@t.com"));
            when(userDAO.findAll()).thenReturn(list);

            // WHEN
            List<UserResponseDTO> results = userService.getAllUsers();

            // THEN
            assertEquals(2, results.size());
            verify(userDAO).findAll();
        }

        @Test
        @DisplayName("Trả về danh sách rỗng khi không có người dùng")
        void testGetAllUsers_Empty() {
            when(userDAO.findAll()).thenReturn(Collections.emptyList());

            List<UserResponseDTO> results = userService.getAllUsers();

            assertNotNull(results);
            assertTrue(results.isEmpty());
        }
    }
}
