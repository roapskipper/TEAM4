package com.team4.util;

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

/** DatabaseSetup chỉ lo 4 việc:
 * 1. đọc cấu hình DB
 * 2. đảm bảo database tồn tại
 * 3. đọc file schema.sql
 * 4. chạy schema vào MySQL
 */
public final class DatabaseSetup {

    // method public để bên ngoài có thể gọi vào
    public static void initDatabase() {
        DbConfig config = loadConfig();
        ensureDatabaseExists(config);

        String schema = readSchemaSql();
        List<String> statements = splitStatements(schema);

        applySchema(config, statements);
    }

    /** Nhiệm vụ của loadConfig là:
     đọc file database.properties
     lấy ra các giá trị như:
     db.url
     db.user
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
                throw new IOException("Không tồn tại database.properties trong classpath"); // Kiểm tra file có tồn tại không
            }
            properties.load(is);
        } catch (IOException e) {
            // Sử dụng RuntimeException là cách tốt nhất để Ngắt ứng dụng ngay khi lỗi nền tảng xảy ra và Giữ cho mã nguồn các tầng bên trên được sạch đẹp, không vướng bận các ngoại lệ của I/O.
            throw new RuntimeException("Không thể tải database.properties", e);
        }
        String url = properties.getProperty("db.url");
        String user = properties.getProperty("db.user");
        String password = properties.getProperty("db.password");
        /** Validate 3 thành phần đều không được null/trống
         * Do đã cài mât khẩu trong Workbench nên password không được null
         * Dùng trim() ở đây thay vè bên trên để tránh NPE :((
         */
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("db.url không được trống");
        }
        if (user == null || user.trim().isEmpty()) {
            throw new IllegalArgumentException("db.user is không đươ trống");
        }
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("password không được trống");
        }
        return new DbConfig(url.trim(), user.trim(), password);
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
            throw new RuntimeException("Không thể kết nối hoặc tạo Database: auction_system", e);
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
                throw new IOException("Không thấy schema.sql trong classpath");
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
            throw new RuntimeException("Không thể đọc schema.sql", e);
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
                stmt.execute(sql);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Không thể chạy schema.sql", e);
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
            throw new IllegalArgumentException("db.url không hợp lệ: " + dbUrl);
        }

        String adminBase = baseUrl.substring(0, slashIdx + 1);;
        // Nếu query rỗng,vẫn thêm để đảm bảo connection server-level vẫn có các tham số cần thiết.
        if (query.isEmpty()) {
            query = "?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Ho_Chi_Minh";
        }

        return adminBase + query;
    }

    private record DbConfig(String url, String user, String password) {}
}
