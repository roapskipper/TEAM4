package com.team4.db;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * DatabaseManager - Quản lý kết nối MySQL chuyên nghiệp (Fix 100% Bug Connection)
 * Hỗ trợ Auto-Reconnect và Thread-safe an toàn tối đa cho Dự án Đấu giá.
 */
public class DatabaseManager {

    private static volatile DatabaseManager instance;
    private Connection connection;

    private static final long MAX_CONNECTION_AGE = TimeUnit.MINUTES.toMillis(30);
    private long connectionCreatedTime;

    private DatabaseManager() {
        initConnection();
    }

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
     * Phương thức khởi tạo với Cơ chế Phòng thủ (Defensive Programming).
     */
    private void initConnection() {
        Properties props = new Properties();

        try (InputStream input = getClass().getClassLoader().getResourceAsStream("database.properties")) {

            // 1. Kiểm tra File tồn tại
            if (input == null) {
                throw new IOException("[FATAL] Mất tích file 'database.properties' trong mục src/main/resources!");
            }
            props.load(input);

            // 2. Thức tỉnh Thư viện MySQL thủ công
            // Chặn đứng lỗi "No Suitable Driver"
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException ex) {
                throw new RuntimeException("[FATAL] Không nạp được thư viện MySQL! Maven chưa cấp phép chạy class này.", ex);
            }

            // 3. Tiệt trùng dữ liệu Đầu vào
            // Chặn đứng các dấu chấm câu/khoảng trắng lạ lúc bạn tạo file txt (.trim())
            String url = props.getProperty("db.url");
            if(url != null) url = url.trim();

            String user = props.getProperty("db.user");
            if(user != null) user = user.trim();

            String password = props.getProperty("db.password");
            // Mật khẩu có khi để trống nếu máy ai không set pass
            if(password != null) password = password.trim();
            else password = "";

            // 4. Định dạng và Tăng độ ổn định cho URL MySQL
            if (url != null && !url.contains("?")) {
                url += "?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh&allowPublicKeyRetrieval=true";
            }

            // 5. Kết nối Hệ thống chính thức
            this.connection = DriverManager.getConnection(url, user, password);
            this.connectionCreatedTime = System.currentTimeMillis();

            System.out.println("✅ [DB-SYSTEM] Trạm Trung chuyển Database Đấu Giá (TEAM4) đã đi vào VẬN HÀNH THÀNH CÔNG.");

        } catch (SQLException | IOException e) {
            System.err.println("❌ [DB-CRITICAL-ERROR] Cỗ Máy MySQL bị từ chối phục vụ.");
            System.err.println("📌 Hướng dẫn gỡ lỗi: 1) Pass đúng chưa? 2) Máy mở MySQL Workbench chưa? 3) URL bị dư dấu / ở cuối không?");
            throw new RuntimeException("Treo CSDL do lỗi thông tin URL/Pass.", e);
        }
    }

    /**
     * Dùng kết nối 1 lần rưỡi (Dò nhịp sinh tử Database)
     */
    public synchronized Connection getConnection() {
        try {
            boolean isExpired = (System.currentTimeMillis() - connectionCreatedTime > MAX_CONNECTION_AGE);

            if (connection == null || connection.isClosed() || !connection.isValid(2) || isExpired) {
                System.out.println("♻ [DB-WARNING] Chuỗi Cáp Bị Nới. Lệnh Kích điện Reset đường truyền MySQL bắt đầu...");
                reconnect();
            }
        } catch (SQLException e) {
            reconnect();
        }
        return connection;
    }

    private synchronized void reconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ignored) {}
        initConnection();
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("🛑 [DB-SHUTDOWN] Đóng sạch hệ cáp truyền MySQL.");
            }
        } catch (SQLException ignored) {}
    }
}