package com.team4.dao;

import com.team4.db.DatabaseManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lớp BaseDAOTest – lớp cha cho tất cả DAO integration tests.
 *
 * @Tag("integration") – các test này cần MySQL thật, sẽ bị bỏ qua trong CI pipeline.
 * Chạy thủ công bằng: mvn test -Dgroups=integration
 */
@Tag("integration")
public abstract class BaseDAOTest {
    private static final Logger logger = LoggerFactory.getLogger(BaseDAOTest.class);

    /**
     * Chạy duy nhất 1 lần trước khi bắt đầu bất kỳ bài test nào trong Class con.
     */
    @BeforeAll
    static void initAll() {
        logger.info("--- KHỞI TẠO HỆ THỐNG KIỂM THỬ DAO ---");
        DatabaseManager.initialize();
    }

    /**
     * Chạy trước mỗi phương thức @Test.
     * Đảm bảo mỗi bài test đều bắt đầu với một Database trống.
     */
    @BeforeEach
    void initEach() {
        logger.info("Chuẩn bị môi trường sạch cho bài test tiếp theo...");
        TestDatabaseUtils.cleanDatabase();
    }
}
