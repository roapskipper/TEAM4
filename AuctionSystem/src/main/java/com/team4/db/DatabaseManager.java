package com.team4.db;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseManager {
    private static volatile DatabaseManager instance;

    private final String url;
    private final String user;
    private final String password;

    private DatabaseManager() {
        Properties props = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("database.properties")) {
            if (input == null) {
                throw new IOException("Missing database.properties in classpath");
            }
            props.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Cannot load database configuration", e);
        }

        String rawUrl = trim(props.getProperty("db.url"));
        String rawUser = trim(props.getProperty("db.user"));
        String rawPassword = props.getProperty("db.password");
        if (rawPassword == null) {
            rawPassword = "";
        } else {
            rawPassword = rawPassword.trim();
        }

        if (rawUrl == null || rawUrl.isEmpty()) {
            throw new IllegalArgumentException("db.url is empty");
        }
        if (rawUser == null || rawUser.isEmpty()) {
            throw new IllegalArgumentException("db.user is empty");
        }

        this.url = appendMysqlParams(rawUrl);
        this.user = rawUser;
        this.password = rawPassword;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException("MySQL JDBC driver not found in runtime classpath", ex);
        }
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

    public Connection getConnection() {
        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            throw new RuntimeException("Cannot connect to MySQL using configured URL/user", e);
        }
    }

    public void closeConnection() {
        // No-op. DAO methods open/close short-lived connections via try-with-resources.
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String appendMysqlParams(String rawUrl) {
        if (rawUrl.contains("?")) {
            return rawUrl;
        }
        return rawUrl + "?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh&allowPublicKeyRetrieval=true";
    }
}
