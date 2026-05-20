package com.team4.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import io.github.cdimascio.dotenv.Dotenv;

/**Là cửa vào duy nhất để lấy connection và quản lý transaction.
 * Có các nhiệm vụ: Mở pool, Cấp connection, Transaction, Đóng pool
 * Sử dụng Singleton Pattern để đảm bảo chỉ có một instance duy nhất của DatabaseManager trong toàn bộ ứng dụng.
 * Dùng HikariCP.
 * Điều này giúp tiết kiệm tài nguyên và đảm bảo tính nhất quán khi quản lý kết nối tới MySQL.
 */
public final class DatabaseManager {

    private static final String DEFAULT_DATABASE = "auction_system";
    private static final String DEFAULT_JDBC_QUERY =
            "useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Ho_Chi_Minh";

    private static HikariDataSource dataSource;
    private static String url;
    private static String username;
    private static String password;
    private static int poolSize;
    private static int connectionTimeout;
    private static int idleTimeout;
    private static int maxLifetime;
    private static final Dotenv dotenv = loadDotenv();

    private static Dotenv loadDotenv() {
        // Thử load từ working directory (AuctionSystem/ khi chạy từ IntelliJ)
        try {
            Dotenv d = Dotenv.configure().ignoreIfMissing().load();
            if (d.get("DB_URL") != null) return d;
        } catch (Exception ignored) {}
        // Fallback: thử từ thư mục cha (TEAM4/ khi run từ root)
        try {
            Dotenv d = Dotenv.configure().directory("../").ignoreIfMissing().load();
            if (d.get("DB_URL") != null) return d;
        } catch (Exception ignored) {}
        return Dotenv.configure().ignoreIfMissing().load();
    }

    // Không cho tạo instance
    private DatabaseManager() {}

    // 1. Khởi tạo - gọi 1 lần khi app start
        // Tránh race khi khởi tạo
    public static synchronized void initialize() {
        if (dataSource == null) {
            loadConfig();
            initDataSource();
        }
    }

    // 2. Đọc config
    private static void loadConfig() {
        Properties props = new Properties(); // Tạo object để chứa các cặp key=value
        // Đọc từ file
        try (InputStream is = DatabaseManager.class
                .getClassLoader()
                .getResourceAsStream("database.properties")) {

            if (is == null)
                throw new RuntimeException("database.properties not found"); // getResourceAsStream trả null nếu không tìm thấy file

            props.load(is);
            // Lấy các giá trị
            url               = normalizeJdbcUrl(dotenv.get("DB_URL"), DEFAULT_DATABASE);
            username          = requireConfig("DB_USERNAME", dotenv.get("DB_USERNAME"));
            password          = requireConfig("DB_PASSWORD", dotenv.get("DB_PASSWORD"));
            poolSize          = Integer.parseInt(props.getProperty("db.poolSize", "10"));
            connectionTimeout = Integer.parseInt(props.getProperty("db.connectionTimeout", "30000"));
            idleTimeout       = Integer.parseInt(props.getProperty("db.idleTimeout", "600000"));
            maxLifetime       = Integer.parseInt(props.getProperty("db.maxLifetime", "1800000"));

        } catch (IOException e) {
            throw new RuntimeException("Unable to read database.properties", e);
        }
    }

    // 3. Khởi tạo pool
    private static void initDataSource() {
        HikariConfig config = new HikariConfig();
        // Truyền thông tin kết nối
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        // Truyền thông số pool
        config.setMaximumPoolSize(poolSize);
        config.setConnectionTimeout(connectionTimeout);
        config.setIdleTimeout(idleTimeout);
        config.setMaxLifetime(maxLifetime);

        dataSource = new HikariDataSource(config);
    }

    public static String normalizeJdbcUrl(String rawUrl, String defaultDatabase) {
        String value = requireConfig("DB_URL", rawUrl).trim();
        String base = value;
        String query = "";
        int queryIdx = value.indexOf('?');

        if (queryIdx >= 0) {
            base = value.substring(0, queryIdx);
            query = value.substring(queryIdx + 1);
        }

        int schemeIdx = base.indexOf("://");
        if (schemeIdx < 0 || !base.startsWith("jdbc:mysql://")) {
            throw new IllegalArgumentException("DB_URL must be a MySQL JDBC URL");
        }

        int pathStart = base.indexOf('/', schemeIdx + 3);
        boolean missingDatabase = pathStart < 0 || pathStart == base.length() - 1;
        if (missingDatabase) {
            if (pathStart < 0) {
                base = base + "/" + defaultDatabase;
            } else {
                base = base + defaultDatabase;
            }
        }

        if (query.isBlank()) {
            query = DEFAULT_JDBC_QUERY;
        }

        return base + "?" + query;
    }

    private static String requireConfig(String key, String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(key + " must not be blank");
        }
        return value;
    }

    // 4. Lấy connection
    public static Connection getConnection() throws SQLException {
        if (dataSource == null)
            throw new IllegalStateException("DatabaseManager has not been initialized; call initialize() first");
        return dataSource.getConnection();
    }


    // 5. Transaction
    public static void beginTransaction(Connection conn) throws SQLException {
        if (conn == null)
            throw new IllegalArgumentException("Connection must not be null");
        conn.setAutoCommit(false);
    }
    public static void commitTransaction(Connection conn) throws SQLException {
        if (conn == null)
            throw new IllegalArgumentException("Connection must not be null");
        conn.commit(); //lưu toàn bộ thay đổi xuống DB
        conn.setAutoCommit(true); //trả về trạng thái ban đầu trước khi trả connection về pool
    }
    public static void rollbackTransaction(Connection conn) {
        if (conn == null) return;
        try {
            conn.rollback(); //hủy toàn bộ thay đổi chưa commit
            conn.setAutoCommit(true);
        } catch (SQLException e) {
            throw new RuntimeException("Rollback failed", e);
        }
    }

    // 6. Kiểm tra kết nối
    public static boolean healthCheck() {
        if (dataSource == null) return false;
        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(5); // chờ tối đa 5 giây
        } catch (SQLException e) {
            return false;
        }
    }

    // 7. Đóng pool
    public static void shutdown() {
        // chưa đóng thì mới đóng, tránh đóng 2 lần
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close(); //đóng toàn bộ pool, giải phóng connection
            dataSource = null;
        }
    }
}
