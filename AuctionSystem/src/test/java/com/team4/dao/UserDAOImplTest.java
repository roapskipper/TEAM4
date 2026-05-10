package com.team4.dao;

import com.team4.dao.impl.UserDAOImpl;
import com.team4.db.DatabaseManager;
import com.team4.model.Bidder;
import com.team4.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

public class UserDAOImplTest {

    private Connection testConnection;
    private UserDAOImpl userDAO;
    private MockedStatic<DatabaseManager> mockedDatabaseManager;

    @BeforeEach
    public void setUp() throws Exception {
        // Khởi tạo connection với H2 DB trong bộ nhớ
        testConnection = DriverManager.getConnection("jdbc:h2:mem:auction_test_db;DB_CLOSE_DELAY=-1", "sa", "");
        
        // Tạo bảng giả lập cho test
        try (Statement stmt = testConnection.createStatement()) {
            stmt.execute("CREATE TABLE users (" +
                    "id VARCHAR(50) PRIMARY KEY, " +
                    "created_at TIMESTAMP, " +
                    "username VARCHAR(100), " +
                    "password_hash VARCHAR(255), " +
                    "full_name VARCHAR(255), " +
                    "email VARCHAR(255), " +
                    "role VARCHAR(20), " +
                    "balance DECIMAL(15,2), " +
                    "access_level INT, " +
                    "admin_code_hash VARCHAR(255), " +
                    "shipping_address VARCHAR(255), " +
                    "phone_number VARCHAR(20), " +
                    "store_name VARCHAR(255), " +
                    "rating DOUBLE)");
        }
        
        // Mock static method DatabaseManager.getConnection() để luôn trả về connection mới tới H2 DB
        mockedDatabaseManager = Mockito.mockStatic(DatabaseManager.class);
        mockedDatabaseManager.when(DatabaseManager::getConnection).thenAnswer(invocation -> 
            DriverManager.getConnection("jdbc:h2:mem:auction_test_db;DB_CLOSE_DELAY=-1", "sa", "")
        );

        userDAO = new UserDAOImpl();
    }

    @AfterEach
    public void tearDown() throws Exception {
        // Đóng mock static và connection sau khi test xong
        if (mockedDatabaseManager != null) {
            mockedDatabaseManager.close();
        }
        if (testConnection != null && !testConnection.isClosed()) {
            testConnection.close();
        }
    }

    @Test
    public void testInsertAndFindById() {
        // Tạo User
        User newBidder = new Bidder("id-99", java.time.LocalDateTime.now(), "bidder99", "hashed_pass", "John Doe", "bidder99@gmail.com", java.math.BigDecimal.ZERO, "Address", "0123456789");
        
        // Test Insert
        boolean isInserted = userDAO.insert(newBidder);
        assertTrue(isInserted, "Phải insert thành công vào DB");

        // Test Find
        User foundUser = userDAO.findById("id-99");
        assertNotNull(foundUser);
        assertEquals("bidder99", foundUser.getUsername());
        assertEquals("bidder99@gmail.com", foundUser.getEmail());
        assertEquals("John Doe", foundUser.getFullName());
    }
}
