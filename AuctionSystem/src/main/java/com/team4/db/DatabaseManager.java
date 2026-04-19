package com.team4.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Quản lý kết nối Database.
 * Áp dụng SINGLETON PATTERN (Double-Checked Locking) để đảm bảo:
 * 1. Chỉ có 1 instance quản lý việc tạo kết nối.
 * 2. An toàn trong môi trường đa luồng (Thread-safe).
 */
public class DatabaseManager {

    // Khai báo các thông tin cấu hình kết nối tới MySQL
    private static final String URL = "jdbc:mysql://localhost:3306/auction_system?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Ho_Chi_Minh";
    private static final String USER = "root";
    private static final String PASSWORD = "admin07";

    // 1. Biến static lưu trữ instance duy nhất (volatile để tránh lỗi bộ nhớ cache trong đa luồng)
    private static volatile DatabaseManager instance;

    /**
     * 2. Constructor private: Ngăn chặn việc khởi tạo bằng từ khóa 'new' từ bên ngoài.
     */
    private DatabaseManager() {
        try {
            // Đăng ký (Load) MySQL JDBC Driver
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("[DatabaseManager] LỖI: Không tìm thấy thư viện MySQL JDBC Driver.");
            e.printStackTrace();
        }
    }

    /**
     * 3. Phương thức public static để lấy instance duy nhất.
     * Sử dụng kỹ thuật Double-Checked Locking để tối ưu hiệu năng trong môi trường đa luồng (Concurrent).
     */
    public static DatabaseManager getInstance() {
        if (instance == null) {
            synchronized (DatabaseManager.class) {
                if (instance == null) {
                    instance = new DatabaseManager();
                }
            }
        }
        return instance;
    }

    /**
     * Phương thức cấp phát một Kết nối (Connection) tới MySQL.
     * Lưu ý: Mỗi lần gọi hàm này sẽ trả về một Connection mới.
     * Người gọi (các DAO) PHẢI chịu trách nhiệm đóng kết nối bằng try-with-resources.
     *
     * @return Đối tượng java.sql.Connection
     * @throws SQLException Nếu thông tin đăng nhập sai hoặc MySQL chưa bật.
     */
    public Connection createConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /**
     * Phương thức tiện ích tĩnh (Static utility method) để gọi nhanh.
     * Viết hàm này để code bên các DAO ngắn gọn hơn.
     * Thay vì gọi: DatabaseManager.getInstance().createConnection();
     * Chỉ cần gọi: DatabaseManager.getConnection();
     */
    public static Connection getConnection() throws SQLException {
        return getInstance().createConnection();
    }
    /**
     * Bắt đầu một giao dịch (Tắt tự động lưu)
     */
    public static void beginTransaction(Connection conn) throws SQLException {
        if (conn != null && !conn.isClosed()) {
            conn.setAutoCommit(false);
        }
    }

    /**
     * Xác nhận và lưu toàn bộ thay đổi xuống Database
     */
    public static void commitTransaction(Connection conn) throws SQLException {
        if (conn != null && !conn.isClosed()) {
            conn.commit();
            conn.setAutoCommit(true); // Trả lại trạng thái mặc định
        }
    }

    /**
     * Hủy bỏ thay đổi nếu có lỗi xảy ra (Rollback)
     */
    public static void rollbackTransaction(Connection conn) {
        if (conn != null) {
            try {
                if (!conn.isClosed()) {
                    conn.rollback();
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                System.err.println("[DB ERROR] Không thể rollback giao dịch!");
                e.printStackTrace();
            }
        }
    }
}