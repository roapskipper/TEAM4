package com.team4.db;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class DatabaseManager {
    // Thread-safe Singleton với double-checked locking
    private static volatile DatabaseManager instance;
    private Connection connection;
    private final long MAX_CONNECTION_AGE = TimeUnit.MINUTES.toMillis(30);
    private long connectionCreatedTime;

    // Private constructor
    private DatabaseManager() {
        loadConfigAndConnect();
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

    private void loadConfigAndConnect() {
        Properties props = new Properties();
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("database.properties")) {

            if (input == null) {
                throw new RuntimeException("Không tìm thấy file database.properties");
            }

            props.load(input);
            String url = props.getProperty("db.url");
            String user = props.getProperty("db.username");
            String password = props.getProperty("db.password");

            if (url == null || user == null || password == null) {
                throw new RuntimeException("Thiếu thông tin kết nối database trong file config");
            }

            connection = DriverManager.getConnection(url, user, password);
            connectionCreatedTime = System.currentTimeMillis();

        } catch (SQLException | IOException e) {
            throw new RuntimeException("Không thể kết nối database: " + e.getMessage(), e);
        }
    }

    public Connection getConnection() {
        try {
            // Kiểm tra connection còn sống và không quá hạn
            if (connection == null || connection.isClosed() ||
                    !connection.isValid(2) ||
                    (System.currentTimeMillis() - connectionCreatedTime > MAX_CONNECTION_AGE)) {
                recreateConnection();
            }
            return connection;
        } catch (SQLException e) {
            // Fallback: tạo lại connection nếu có lỗi
            recreateConnection();
            return connection;
        }
    }

    private synchronized void recreateConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ignored) {
        } finally {
            loadConfigAndConnect();
        }
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi đóng connection: " + e.getMessage());
        }
    }

    public boolean testConnection() {
        try {
            return connection != null && !connection.isClosed() && connection.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }
}