package com.team4.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
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
                server.createContext("/api/register", new RegisterHandler());
                server.setExecutor(null);
                server.start();
                System.out.println("API Server dang chay tren port " + API_PORT + "...");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }
}

class LoginHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            ApiServer.sendResponse(exchange, 204, "");
            return;
        }

        if (!"POST".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        InputStream is = exchange.getRequestBody();
        String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        is.close();

        String username = null;
        String password = null;

        String[] params = body.split("&");
        for (int i = 0; i < params.length; i++) {
            String[] kv = params[i].split("=", 2);
            if (kv.length == 2) {
                if (kv[0].equals("username")) {
                    username = kv[1];
                }
                if (kv[0].equals("password")) {
                    password = kv[1];
                }
            }
        }

        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            ApiServer.sendResponse(exchange, 400, "{\"status\": \"error\", \"message\": \"Thieu username hoac password\"}");
            return;
        }

        DatabaseHelper dbHelper = new DatabaseHelper();
        String[] userInfo = dbHelper.verifyLogin(username, password);

        if (userInfo != null) {
            String response = "{\"status\": \"success\", \"message\": \"Dang nhap thanh cong\", \"userId\": " + userInfo[0] + ", \"role\": \"" + userInfo[1] + "\"}";
            ApiServer.sendResponse(exchange, 200, response);
        } else {
            ApiServer.sendResponse(exchange, 401, "{\"status\": \"error\", \"message\": \"Sai username hoac password\"}");
        }
    }
}

class RegisterHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            ApiServer.sendResponse(exchange, 204, "");
            return;
        }

        if (!"POST".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        InputStream is = exchange.getRequestBody();
        String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        is.close();

        String username = null;
        String password = null;
        String role = null;

        String[] params = body.split("&");
        for (int i = 0; i < params.length; i++) {
            String[] kv = params[i].split("=", 2);
            if (kv.length == 2) {
                if (kv[0].equals("username")) {
                    username = kv[1];
                }
                if (kv[0].equals("password")) {
                    password = kv[1];
                }
                if (kv[0].equals("role")) {
                    role = kv[1];
                }
            }
        }

        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            ApiServer.sendResponse(exchange, 400, "{\"status\": \"error\", \"message\": \"Thieu username hoac password\"}");
            return;
        }

        if (role == null || role.isEmpty()) {
            role = "bidder";
        }

        DatabaseHelper dbHelper = new DatabaseHelper();
        dbHelper.insertUser(username, password, role);
        ApiServer.sendResponse(exchange, 200, "{\"status\": \"success\", \"message\": \"Dang ky thanh cong\"}");
    }
}

class ItemsHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            ApiServer.sendResponse(exchange, 204, "");
            return;
        }

        if (!"GET".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        String response = "[";
        boolean first = true;

        try {
            PreparedStatement pstmt = DatabaseHelper.getConnection().prepareStatement("SELECT * FROM items");
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                if (!first) {
                    response = response + ",";
                }
                String name = rs.getString("name").replace("\\", "\\\\").replace("\"", "\\\"");
                String status = rs.getString("status").replace("\\", "\\\\").replace("\"", "\\\"");
                response = response + "{";
                response = response + "\"id\":" + rs.getInt("id") + ",";
                response = response + "\"name\":\"" + name + "\",";
                response = response + "\"category_id\":" + rs.getInt("category_id") + ",";
                response = response + "\"start_price\":" + rs.getDouble("start_price") + ",";
                response = response + "\"status\":\"" + status + "\"";
                response = response + "}";
                first = false;
            }

            rs.close();
            pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            ApiServer.sendResponse(exchange, 500, "{\"status\": \"error\", \"message\": \"Loi truy van database\"}");
            return;
        }

        response = response + "]";
        ApiServer.sendResponse(exchange, 200, response);
    }
}