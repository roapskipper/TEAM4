package com.team4.util;

import com.team4.db.DatabaseManager;
import io.github.cdimascio.dotenv.Dotenv;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** DatabaseSetup chỉ lo 4 việc:
 * 1. đọc cấu hình DB
 * 2. đảm bảo database tồn tại
 * 3. đọc file schema.sql
 * 4. chạy schema vào MySQL
 */
public final class DatabaseSetup {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseSetup.class);
    private static final String DEFAULT_DATABASE = "auction_system";

    // method public để bên ngoài có thể gọi vào
    public static void initDatabase() {
        DbConfig config = loadConfig();
        ensureDatabaseExists(config);

        String schema = readSchemaSql();
        List<String> statements = splitStatements(schema);

        applySchema(config, statements);
        applyMigrations(config);
    }

    /** Nhiệm vụ của loadConfig là:
     đọc file database.properties
     lấy ra các giá trị như:
     db.url
     db.username
     db.password
     kiểm tra dữ liệu có ổn không
     gói lại thành một object cấu hình DbConfig
     */
    private static DbConfig loadConfig() {
        Properties properties = new Properties();
        // Lấy file từ resource
        try (InputStream is = DatabaseSetup.class
                .getClassLoader()
                .getResourceAsStream("database.properties")
        ) {
            if (is == null) {
                throw new IOException("database.properties does not exist on the classpath"); // Kiểm tra file có tồn tại không
            }
            properties.load(is);
        } catch (IOException e) {
            // Sử dụng RuntimeException là cách tốt nhất để Ngắt ứng dụng ngay khi lỗi nền tảng xảy ra và Giữ cho mã nguồn các tầng bên trên được sạch đẹp, không vướng bận các ngoại lệ của I/O.
            throw new RuntimeException("Unable to load database.properties", e);
        }
        Dotenv dotenv = loadDotenv();
        String rawUrl = firstNonBlank(dotenv.get("DB_URL"), properties.getProperty("db.url"));
        String user = firstNonBlank(dotenv.get("DB_USERNAME"), properties.getProperty("db.username"));
        String password = firstNonBlank(dotenv.get("DB_PASSWORD"), properties.getProperty("db.password"));
        /** Validate 3 thành phần đều không được null/trống
         * Do đã cài mât khẩu trong Workbench nên password không được null
         * Dùng trim() ở đây thay vè bên trên để tránh NPE :((
         */
        if (rawUrl == null) {
            throw new IllegalArgumentException("DB_URL must not be blank");
        }
        if (user == null) {
            throw new IllegalArgumentException("DB_USERNAME must not be blank");
        }
        if (password == null) {
            throw new IllegalArgumentException("DB_PASSWORD must not be blank");
        }
        return new DbConfig(DatabaseManager.normalizeJdbcUrl(rawUrl, DEFAULT_DATABASE), user, password);
    }

    private static Dotenv loadDotenv() {
        try {
            Dotenv d = Dotenv.configure().ignoreIfMissing().load();
            if (d.get("DB_URL") != null) return d;
        } catch (Exception e) {
            logger.debug("Failed to load .env from current directory", e);
        }
        try {
            Dotenv d = Dotenv.configure().directory("../").ignoreIfMissing().load();
            if (d.get("DB_URL") != null) return d;
        } catch (Exception e) {
            logger.debug("Failed to load .env from parent directory", e);
        }
        return Dotenv.configure().ignoreIfMissing().load();
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.trim().isEmpty()) {
            return first.trim();
        }
        if (second != null && !second.trim().isEmpty()) {
            return second.trim();
        }
        return null;
    }

    /**
     * Mục đích của ensureDatabaseExists(DbConfig config) là đảm bảo database đã tồn tại trước khi hệ thống chạy schema.
     * nếu DB đã có rồi -> không cần tạo lại
     * nếu DB chưa có -> tạo nó ra
     */
    private static void ensureDatabaseExists(DbConfig config) {
        // không dùng thẳng config.url vì cần sever level url
        String adminUrl = buildAdminUrl(config.url()) ;
        // Mở kết nối JDBC
        try (Connection conn = DriverManager.getConnection(adminUrl, config.user(), config.password());
        Statement stmt = conn.createStatement()) {
            // Chạy lệnh tạo DB
            // hardcode tên DB vì đây là dự án nhỏ
            String sql = "CREATE DATABASE IF NOT EXISTS auction_system " +
                    "CHARACTER SET utf8mb4 " +
                    "COLLATE utf8mb4_unicode_ci";
            stmt.execute(sql);

        } catch (Exception e) {
            throw new RuntimeException("Unable to connect to or create database: auction_system", e);
        }
    }

    /**
     * readSchemaSQL() là bước chuyển file SQL từ tài nguyên bên ngoài thành dữ liệu Java có thể xử lý.
     * đọc toàn bộ file thành một khối text
     */
    private static String readSchemaSql() {
        StringBuilder sb = new StringBuilder();
        /**
         * không đọc bằng FileReader("src/main/resources/schema.sql") do dễ hỏng khi build/jar
         * dùng try-with-resources do InputStream là tài nguyên cần đóng
         */
        try (InputStream is = DatabaseSetup.class.getClassLoader().getResourceAsStream("schema.sql")) {
            if (is == null) {
                throw new IOException("schema.sql not found on the classpath");
            }

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))) {

                String line;
                while ((line = br.readLine()) != null) {
                    String trimmed = line.trim();
                    // Bỏ dòng trống và comment
                    if (!trimmed.startsWith("--") && !trimmed.isEmpty()) {
                        sb.append(line).append('\n');
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to read schema.sql", e);
        }
        return sb.toString();
    }

    /**
     *Mục tiêu của splitStatements là:
     * nhận vào một chuỗi SQL lớn
     * tách nó thành nhiều câu lệnh SQL nhỏ
     * trả về danh sách các câu lệnh đó
     */
    private static List<String> splitStatements(String schema) {
        String[] pieces = schema.split(";");
        List<String> statements = new ArrayList<>();

        for (String piece : pieces) {
            String sql = piece.trim();
            if (!sql.isEmpty()) {
                statements.add(sql);
            }
        }
        return statements;
    }

    /** applySchema() chạy các câu đã được tách từ splitStatements()
     * không dùng adminUrl vì DDL phải chạy bên trong database cụ thể.
     */
    private static void applySchema(DbConfig config, List<String> statements) {
        try (Connection conn = DriverManager.getConnection(config.url(), config.user(), config.password());
        Statement stmt = conn.createStatement()) {

            for (String sql : statements) {
                try {
                    stmt.execute(sql);
                } catch (SQLException e) {
                    if (!isIgnorableSchemaError(e)) {
                        throw e;
                    }
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Unable to apply database schema", e);
        }
    }

    private static void applyMigrations(DbConfig config) {
        try (Connection conn = DriverManager.getConnection(config.url(), config.user(), config.password());
             Statement stmt = conn.createStatement()) {

            try {
                stmt.execute("ALTER TABLE users ADD COLUMN previous_role_before_admin ENUM('SELLER', 'BIDDER') NULL AFTER balance");
            } catch (SQLException e) {
                if (!isIgnorableSchemaError(e)) {
                    throw e;
                }
            }

            stmt.executeUpdate("UPDATE users SET access_level = 2 WHERE id = 'root-admin' AND role = 'ADMIN'");
            stmt.executeUpdate("UPDATE users SET access_level = 1 WHERE id LIKE 'mod-%' AND role = 'ADMIN'");
        } catch (SQLException e) {
            throw new RuntimeException("Unable to apply database migrations", e);
        }
    }

    /**
     *buildAdminUrl(String dbUrl) có nhiệm vụ:
     * nhận vào URL JDBC đang trỏ tới một database cụ thể, rồi biến nó thành URL cấp server để có thể tạo database.
     */
    private static String buildAdminUrl(String dbUrl) {
        String baseUrl = dbUrl;
        String query = "";
        // Kiểm tra URL có query string không
        int queryIdx = dbUrl.indexOf('?');
        if (queryIdx >= 0) {
            baseUrl = dbUrl.substring(0, queryIdx);;
            query = dbUrl.substring(queryIdx);
        }
        // Tìm dấu / cuối cùng trong baseUrl
        int slashIdx = baseUrl.lastIndexOf('/');
        if (slashIdx < 0) {
            throw new IllegalArgumentException("Invalid db.url: " + dbUrl);
        }

        String adminBase = baseUrl.substring(0, slashIdx + 1);;
        // Nếu query rỗng,vẫn thêm để đảm bảo connection server-level vẫn có các tham số cần thiết.
        if (query.isEmpty()) {
            query = "?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Ho_Chi_Minh";
        }

        return adminBase + query;
    }

    private static boolean isIgnorableSchemaError(SQLException e) {
        int errorCode = e.getErrorCode();
        // 1061 là mã lỗi của MySQL khi báo "Duplicate key name" (Trùng tên Index)
        // 1050 là mã lỗi khi báo "Table already exists"
        return errorCode == 1061 || errorCode == 1050 || errorCode == 1060;
    }

    private record DbConfig(String url, String user, String password) {}
}
