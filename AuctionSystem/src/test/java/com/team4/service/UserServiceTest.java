package com.team4.service;

import com.team4.dao.UserDAO;
import com.team4.model.Bidder;
import com.team4.model.User;
import com.team4.util.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserDAO userDAO;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userDAO);
    }

    @Test
    void testGetUserById_Success() {
        String userId = "user1";
        User mockUser = new Bidder(userId, "Test User", "test@email.com", "123", "Addr", "Pass");
        when(userDAO.findById(userId)).thenReturn(mockUser);

        User result = userService.getUserById(userId);

        assertNotNull(result);
        assertEquals("Test User", result.getFullName());
    }

    @Test
    void testUpdateProfile_Success() {
        String userId = "user1";
        User mockUser = new Bidder(userId, "Old Name", "old@email.com", "123", "Addr", "Pass");
        when(userDAO.findById(userId)).thenReturn(mockUser);
        when(userDAO.update(any(User.class))).thenReturn(true);

        User result = userService.updateProfile(userId, "New Name", "new@email.com");

        assertEquals("New Name", result.getFullName());
        assertEquals("new@email.com", result.getEmail());
        verify(userDAO).update(mockUser);
    }

    @Test
    void testUpdateProfile_UserNotFound() {
        String userId = "none";
        when(userDAO.findById(userId)).thenReturn(null);

        assertThrows(BusinessException.class, () -> 
            userService.updateProfile(userId, "Name", "email")
        );
    }
}
