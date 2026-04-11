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

public class DatabaseSetup {
    public static void initDatabase() {
        DbConfig config = loadConfig();
        String adminUrl = buildAdminUrl(config.url);

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL JDBC driver not found", e);
        }

        try (Connection adminConn = DriverManager.getConnection(adminUrl, config.user, config.password);
             Statement adminStmt = adminConn.createStatement()) {
            adminStmt.execute("CREATE DATABASE IF NOT EXISTS auction_system");
        } catch (Exception e) {
            throw new RuntimeException("Cannot create/check database auction_system", e);
        }

        String schema = readSchemaSQL();
        List<String> statements = splitStatements(schema);

        try (Connection conn = DriverManager.getConnection(config.urlWithParams(), config.user, config.password);
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
        } catch (Exception e) {
            throw new RuntimeException("Cannot apply schema.sql", e);
        }
    }

    private static DbConfig loadConfig() {
        Properties props = new Properties();
        try (InputStream is = DatabaseSetup.class.getClassLoader().getResourceAsStream("database.properties")) {
            if (is == null) {
                throw new IOException("Missing database.properties in classpath");
            }
            props.load(is);
        } catch (IOException e) {
            throw new RuntimeException("Cannot load database.properties", e);
        }

        String url = trim(props.getProperty("db.url"));
        String user = trim(props.getProperty("db.user"));
        String password = props.getProperty("db.password");
        if (password == null) {
            password = "";
        } else {
            password = password.trim();
        }

        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("db.url is empty");
        }
        if (user == null || user.isEmpty()) {
            throw new IllegalArgumentException("db.user is empty");
        }

        return new DbConfig(url, user, password);
    }

    private static String readSchemaSQL() {
        StringBuilder sb = new StringBuilder();
        try (InputStream is = DatabaseSetup.class.getClassLoader().getResourceAsStream("schema.sql")) {
            if (is == null) {
                throw new IOException("Missing schema.sql in classpath");
            }
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String trimmed = line.trim();
                    if (!trimmed.startsWith("--") && !trimmed.isEmpty()) {
                        sb.append(line).append('\n');
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Cannot read schema.sql", e);
        }
        return sb.toString();
    }

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

    private static String buildAdminUrl(String dbUrl) {
        String baseUrl = dbUrl;
        String query = "";

        int queryIdx = dbUrl.indexOf('?');
        if (queryIdx >= 0) {
            baseUrl = dbUrl.substring(0, queryIdx);
            query = dbUrl.substring(queryIdx);
        }

        int slashIdx = baseUrl.lastIndexOf('/');
        if (slashIdx < 0) {
            throw new IllegalArgumentException("Invalid db.url: " + dbUrl);
        }

        String adminBase = baseUrl.substring(0, slashIdx + 1);
        if (query.isEmpty()) {
            query = "?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh&allowPublicKeyRetrieval=true";
        }
        return adminBase + query;
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static boolean isIgnorableSchemaError(SQLException e) {
        String msg = e.getMessage();
        if (msg == null) {
            return false;
        }
        return msg.contains("Duplicate column name")
                || msg.contains("Duplicate key name")
                || msg.contains("already exists");
    }

    private record DbConfig(String url, String user, String password) {
        String urlWithParams() {
            if (url.contains("?")) {
                return url;
            }
            return url + "?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh&allowPublicKeyRetrieval=true";
        }
    }
}
