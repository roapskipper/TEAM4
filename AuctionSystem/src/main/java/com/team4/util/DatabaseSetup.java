package com.team4.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class DatabaseSetup {
    public static void initDatabase() {
        try {
            // Load MySQL Driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            // Connect without database to create it
            String url = "jdbc:mysql://localhost:3306/?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh&allowPublicKeyRetrieval=true";
            try (Connection conn = DriverManager.getConnection(url, "root", "123456");
                 Statement stmt = conn.createStatement()) {
                
                // Read schema.sql
                String schema = readSchemaSQL();
                
                // Split by semicolon and execute each statement
                String[] statements = schema.split(";");
                for (String sql : statements) {
                    sql = sql.trim();
                    if (!sql.isEmpty()) {
                        stmt.execute(sql);
                    }
                }
                
                System.out.println("✅ [DATABASE SETUP] Cơ sở dữ liệu và bảng đã được tạo thành công!");
            }
        } catch (ClassNotFoundException e) {
            System.err.println("❌ Không tìm thấy MySQL Driver");
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("❌ Lỗi khi tạo database: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static String readSchemaSQL() throws IOException {
        StringBuilder sb = new StringBuilder();
        try (InputStream is = DatabaseSetup.class.getClassLoader().getResourceAsStream("schema.sql");
             BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = br.readLine()) != null) {
                // Skip comments and empty lines
                if (!line.trim().startsWith("--") && !line.trim().isEmpty()) {
                    sb.append(line).append("\n");
                }
            }
        }
        return sb.toString();
    }
}

