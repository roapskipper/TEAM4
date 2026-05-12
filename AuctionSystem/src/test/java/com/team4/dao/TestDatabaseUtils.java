package com.team4.dao;

import com.team4.db.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Lớp tiện ích TestDatabaseUtils.
 * Mục đích: Hỗ trợ các công việc dọn dẹp và chuẩn bị dữ liệu cho môi trường Test.
 */
public class TestDatabaseUtils {
    private static final Logger logger = LoggerFactory.getLogger(TestDatabaseUtils.class);

    // Danh sách các bảng cần dọn dẹp theo thứ tự từ BẢNG CON đến BẢNG CHA.
    // Việc này cực kỳ quan trọng để không bị lỗi Ràng buộc khóa ngoại (Foreign Key Constraint).
    private static final List<String> TABLES_TO_CLEAN = List.of(
            "auto_biddings",    // Bảng con (tham chiếu đến auctions và users)
            "bid_transactions", // Bảng con (tham chiếu đến auctions và users)
            "auctions",         // Bảng con (tham chiếu đến items và users)
            "items",            // Bảng con (tham chiếu đến users)
            "users"             // Bảng cha cuối cùng
    );

    /**
     * Phương thức cleanDatabase: Xóa sạch toàn bộ dữ liệu trong Database Test.
     * Thường được gọi trước mỗi bài test để đảm bảo môi trường "sạch".
     */
    public static void cleanDatabase() {
        logger.info("Bắt đầu dọn dẹp Database Test...");
        
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {

            // Bước 1: Tạm thời tắt kiểm tra khóa ngoại để việc Truncate diễn ra suôn sẻ
            stmt.execute("SET FOREIGN_KEY_CHECKS = 0");

            // Bước 2: Duyệt qua danh sách các bảng và thực hiện lệnh TRUNCATE (Xóa sạch dữ liệu và reset ID tự tăng)
            for (String tableName : TABLES_TO_CLEAN) {
                stmt.execute("TRUNCATE TABLE " + tableName);
                logger.debug("Đã xóa sạch bảng: {}", tableName);
            }

            // Bước 3: Bật lại kiểm tra khóa ngoại để bảo vệ tính toàn vẹn dữ liệu trong lúc test
            stmt.execute("SET FOREIGN_KEY_CHECKS = 1");

            logger.info("Dọn dẹp Database Test hoàn tất!");

        } catch (SQLException e) {
            logger.error("Lỗi nghiêm trọng khi dọn dẹp Database Test: {}", e.getMessage());
            // Ném lỗi để dừng việc chạy test nếu không dọn dẹp được DB
            throw new RuntimeException("Không thể dọn dẹp Database Test", e);
        }
    }
}
