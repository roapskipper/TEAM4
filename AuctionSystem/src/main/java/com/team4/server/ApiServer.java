package com.team4.server;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

public class ApiServer {
    private static final int API_PORT = 8080;

    public void start() {
        new Thread(() -> {
            try {
                HttpServer server = HttpServer.create(new InetSocketAddress(API_PORT), 0);
                server.createContext("/api/login", new LoginHandler());
                server.createContext("/api/items", new ItemsHandler());
                server.setExecutor(null);
                server.start();
                System.out.println("API Server dang chay tren port " + API_PORT + "...");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
}

class LoginHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("POST".equals(exchange.getRequestMethod())) {
            String response = "{\"status\": \"success\", \"message\": \"Dang nhap thanh cong\"}";
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        } else {
            exchange.sendResponseHeaders(405, -1);
        }
    }
}

class ItemsHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("GET".equals(exchange.getRequestMethod())) {
            StringBuilder jsonBuilder = new StringBuilder();
            jsonBuilder.append("[");

            try (Connection conn = DatabaseHelper.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM items");
                 ResultSet rs = pstmt.executeQuery()) {

                boolean first = true;
                while (rs.next()) {
                    if (!first) {
                        jsonBuilder.append(",");
                    }
                    jsonBuilder.append("{")
                            .append("\"id\": ").append(rs.getInt("id")).append(", ")
                            .append("\"name\": \"").append(rs.getString("name")).append("\", ")
                            .append("\"category_id\": ").append(rs.getInt("category_id")).append(", ")
                            .append("\"start_price\": ").append(rs.getDouble("start_price")).append(", ")
                            .append("\"status\": \"").append(rs.getString("status")).append("\"")
                            .append("}");
                    first = false;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            jsonBuilder.append("]");
            String response = jsonBuilder.toString();

            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        } else {
            exchange.sendResponseHeaders(405, -1);
        }
    }
}