package com.team4.db;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * DatabaseManager - Quản lý kết nối MySQL tập trung.
 * Áp dụng Singleton Pattern (Thread-safe) & Tự động làm mới kết nối (Auto-refresh).
 * Tuân thủ yêu cầu Java 25 (JDK 25).
 */
public class DatabaseManager {
    // volatile đảm bảo mọi Thread đều thấy trạng thái mới nhất của instance
    private static volatile DatabaseManager instance;
    private Connection connection;

    // Hằng số quản lý tuổi thọ kết nối (30 phút) để tránh lỗi "Wait Timeout" của MySQL
    private static final long MAX_CONNECTION_AGE = TimeUnit.MINUTES.toMillis(30);
    private long connectionCreatedTime;

    /**
     * Private Constructor: Chặn khởi tạo từ bên ngoài.
     */
    private DatabaseManager() {
        initConnection();
    }

    /**
     * Singleton với Double-Checked Locking.
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
     * Đọc cấu hình từ file database.properties và thiết lập kết nối ban đầu.
     */
    private void initConnection() {
        Properties props = new Properties();
        // Load file từ thư mục resources
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("database.properties")) {
            if (input == null) {
                throw new IOException("Không tìm thấy file 'database.properties' trong thư mục resources!");
            }
            props.load(input);

            // Nạp Driver (Không bắt đầu từ JDBC 4.0 nhưng viết vào để đảm bảo tương thích mọi môi trường)
            Class.forName("com.mysql.cj.jdbc.Driver");

            String url = props.getProperty("db.url");
            String user = props.getProperty("db.user"); // Lưu ý: Đồng bộ tên field với file .properties
            String password = props.getProperty("db.password");

            this.connection = DriverManager.getConnection(url, user, password);
            this.connectionCreatedTime = System.currentTimeMillis();

            System.out.println("[DB-INFO] Kết nối MySQL đã được thiết lập thành công.");

        } catch (ClassNotFoundException | SQLException | IOException e) {
            System.err.println("[DB-ERROR] Khởi tạo kết nối thất bại: " + e.getMessage());
            throw new RuntimeException("Lỗi nghiêm trọng khi nạp Database: " + e.getMessage(), e);
        }
    }

    /**
     * Phương thức quan trọng nhất: Trả về kết nối đang hoạt động.
     * Tự động tạo lại kết nối nếu bị ngắt (Timeout) hoặc bị Admin MySQL kick.
     */
    public synchronized Connection getConnection() {
        try {
            boolean isExpired = (System.currentTimeMillis() - connectionCreatedTime > MAX_CONNECTION_AGE);

            // Nếu kết nối chưa tồn tại, đã đóng, không còn hiệu lực (2 giây test), hoặc quá cũ
            if (connection == null || connection.isClosed() || !connection.isValid(2) || isExpired) {
                System.out.println("[DB-INFO] Kết nối cũ/hết hạn. Đang thực hiện kết nối lại...");
                reconnect();
            }
        } catch (SQLException e) {
            System.err.println("[DB-WARN] Phát hiện lỗi kết nối, đang khởi động lại...");
            reconnect();
        }
        return connection;
    }

    /**
     * Ngắt kết nối cũ và nạp lại từ đầu.
     */
    private synchronized void reconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ignored) {
            // Bỏ qua lỗi khi đóng kết nối đã hỏng
        }
        initConnection();
    }

    /**
     * Đóng kết nối hoàn toàn khi tắt ứng dụng.
     */
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("[DB-INFO] Đã đóng toàn bộ kết nối Database.");
            }
        } catch (SQLException e) {
            System.err.println("[DB-ERROR] Lỗi khi đóng Database: " + e.getMessage());
        }
    }

    /**
     * Kiểm tra trạng thái "sức khỏe" của Database.
     */
    public boolean isReady() {
        try {
            return connection != null && !connection.isClosed() && connection.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }
}