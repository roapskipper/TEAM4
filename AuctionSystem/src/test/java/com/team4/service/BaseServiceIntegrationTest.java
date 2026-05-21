package com.team4.service;

import com.team4.db.DatabaseManager;
import com.team4.dao.TestDatabaseUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lớp cha cho tất cả các bài kiểm thử tích hợp (Integration Test) tầng Service.
 * Đảm bảo Database được khởi tạo và dọn dẹp sạch sẽ trước mỗi ca kiểm thử.
 * 
 * @Tag("integration") Đánh dấu đây là test tích hợp, cần môi trường DB thật.
 */
@Tag("integration")
public abstract class BaseServiceIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(BaseServiceIntegrationTest.class);

    @BeforeAll
    static void setupDatabase() {
        logger.info("Initializing Test Database System...");
        DatabaseManager.initialize();
    }

    @BeforeEach
    void resetState() {
        logger.info("Cleaning database for fresh test state...");
        TestDatabaseUtils.cleanDatabase();
    }
}
