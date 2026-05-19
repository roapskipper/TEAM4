package com.team4.handler;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.team4.model.Bidder;
import com.team4.model.User;
import com.team4.server.ApiServer;
import com.team4.server.Server;
import com.team4.util.BusinessException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class UserHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        if ("OPTIONS".equals(method)) {
            ApiServer.sendResponse(exchange, 204, "");
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String[] parts = path.split("/");
        // /api/user/{userId}/profile or /api/user/{userId}/password
        if (parts.length < 5) {
            ApiServer.sendResponse(exchange, 400, ApiServer.buildResponse("ERROR", "Invalid path", null));
            return;
        }

        String userId = parts[3];
        String action = parts[4];

        try {
            if ("GET".equals(method) && "profile".equals(action)) {
                handleGetProfile(exchange, userId);
            } else if ("PUT".equals(method) && "profile".equals(action)) {
                handleUpdateProfile(exchange, userId);
            } else if ("PUT".equals(method) && "password".equals(action)) {
                handleChangePassword(exchange, userId);
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        } catch (BusinessException e) {
            ApiServer.sendResponse(exchange, 400, ApiServer.buildResponse("ERROR", e.getMessage(), null));
        } catch (Exception e) {
            e.printStackTrace();
            ApiServer.sendResponse(exchange, 500, ApiServer.buildResponse("ERROR", "Lỗi server nội bộ", null));
        }
    }

    private void handleGetProfile(HttpExchange exchange, String userId) throws IOException {
        User user = Server.getUserService().getUserById(userId);
        if (user == null) {
            ApiServer.sendResponse(exchange, 404, ApiServer.buildResponse("ERROR", "Người dùng không tồn tại", null));
            return;
        }

        JsonObject data = new JsonObject();
        data.addProperty("id", user.getId());
        data.addProperty("username", user.getUsername());
        data.addProperty("fullName", user.getFullName());
        data.addProperty("email", user.getEmail());
        data.addProperty("role", user.getRole().name());
        data.addProperty("balance", user.getBalance());
        
        if (user instanceof Bidder) {
            data.addProperty("phoneNumber", ((Bidder) user).getPhoneNumber());
        } else {
            data.addProperty("phoneNumber", ""); // For Sellers/Admins if they don't have phone
        }

        ApiServer.sendResponse(exchange, 200, ApiServer.buildResponse("SUCCESS", "Lấy thông tin thành công", data));
    }

    private void handleUpdateProfile(HttpExchange exchange, String userId) throws IOException {
        InputStream is = exchange.getRequestBody();
        String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        is.close();

        String fullName = ApiServer.parseParam(body, "fullName");
        String email = ApiServer.parseParam(body, "email");
        String phone = ApiServer.parseParam(body, "phone");

        if (fullName == null || email == null) {
            ApiServer.sendResponse(exchange, 400, ApiServer.buildResponse("ERROR", "Thiếu fullName hoặc email", null));
            return;
        }

        User updated = Server.getUserService().updateProfile(userId, fullName, email, phone);
        JsonObject data = new JsonObject();
        data.addProperty("fullName", updated.getFullName());
        ApiServer.sendResponse(exchange, 200, ApiServer.buildResponse("SUCCESS", "Cập nhật thành công", data));
    }

    private void handleChangePassword(HttpExchange exchange, String userId) throws IOException {
        InputStream is = exchange.getRequestBody();
        String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        is.close();

        String oldPassword = ApiServer.parseParam(body, "oldPassword");
        String newPassword = ApiServer.parseParam(body, "newPassword");

        if (oldPassword == null || newPassword == null) {
            ApiServer.sendResponse(exchange, 400, ApiServer.buildResponse("ERROR", "Thiếu mật khẩu", null));
            return;
        }

        Server.getUserService().changePassword(userId, oldPassword, newPassword);
        ApiServer.sendResponse(exchange, 200, ApiServer.buildResponse("SUCCESS", "Đổi mật khẩu thành công", null));
    }
}
