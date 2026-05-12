package com.team4.dao;

import com.team4.db.DatabaseManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lớp BaseDAOTest.
 * Đây là lớp cha (Base Class) cho tất cả các lớp kiểm thử DAO khác.
 * Mục đích: 
 * 1. Đảm bảo DatabaseManager được khởi tạo đúng một lần duy nhất.
 * 2. Đảm bảo dữ liệu được xóa sạch trước mỗi phương thức test (@Test).
 */
public abstract class BaseDAOTest {
    private static final Logger logger = LoggerFactory.getLogger(BaseDAOTest.class);

    /**
     * Chạy duy nhất 1 lần trước khi bắt đầu bất kỳ bài test nào trong Class con.
     */
    @BeforeAll
    static void initAll() {
        logger.info("--- KHỞI TẠO HỆ THỐNG KIỂM THỬ DAO ---");
        // Khởi tạo DatabaseManager. 
        // Nhờ file database.properties trong src/test/resources, nó sẽ kết nối vào DB auction_system_test.
        DatabaseManager.initialize();
    }

    /**
     * Chạy trước mỗi phương thức @Test.
     * Đảm bảo mỗi bài test đều bắt đầu với một Database trống, không bị ảnh hưởng bởi dữ liệu cũ.
     */
    @BeforeEach
    void initEach() {
        logger.info("Chuẩn bị môi trường sạch cho bài test tiếp theo...");
        TestDatabaseUtils.cleanDatabase();
    }
}
