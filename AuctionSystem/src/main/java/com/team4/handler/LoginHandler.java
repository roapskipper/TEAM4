package com.team4.handler;

import com.google.gson.JsonElement;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.team4.dao.impl.UserDAOImpl;
import com.team4.dto.auth.LoginRequestDTO;
import com.team4.dto.auth.LoginResponseDTO;
import com.team4.server.ApiServer;
import com.team4.server.Server;
import com.team4.service.AuthenticationService;
import com.team4.service.JwtService;
import com.team4.util.BusinessException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class LoginHandler implements HttpHandler {
    private AuthenticationService authService = new AuthenticationService(new UserDAOImpl(), new JwtService());

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

        // Parse form-urlencoded (giữ tương thích với client và tests hiện tại)
        String username  = ApiServer.parseParam(body, "username");
        String password  = ApiServer.parseParam(body, "password");
        String adminCode = ApiServer.parseParam(body, "adminCode");

        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            ApiServer.sendResponse(exchange, 400,
                    ApiServer.buildResponse("ERROR", "Missing username or password.", null));
            return;
        }

        try {
            // Tạo LoginRequestDTO từ params đã parse (no-arg + set fields via Gson)
            com.google.gson.JsonObject p = new com.google.gson.JsonObject();
            p.addProperty("username", username);
            p.addProperty("password", password);
            if (adminCode != null) p.addProperty("adminCode", adminCode);
            LoginRequestDTO req = Server.getGson().fromJson(p, LoginRequestDTO.class);

            LoginResponseDTO loginResponse;
            if (adminCode != null && !adminCode.isEmpty()) {
                // Đăng nhập Admin
                loginResponse = authService.loginAdmin(req);
            } else {
                // Thử Bidder trước; nếu tài khoản không phải Bidder, thử Seller
                try {
                    loginResponse = authService.loginBidder(req);
                } catch (BusinessException e) {
                    // "not registered as a Bidder" → thử login Seller
                    if (e.getMessage() != null && e.getMessage().contains("Bidder")) {
                        loginResponse = authService.loginSeller(req);
                    } else {
                        throw e; // sai mật khẩu / tài khoản không tồn tại
                    }
                }
            }

            JsonElement data = Server.getGson().toJsonTree(loginResponse);
            ApiServer.sendResponse(exchange, 200, ApiServer.buildResponse("SUCCESS", "Login successful.", data));
        } catch (BusinessException e) {
            ApiServer.sendResponse(exchange, 401, ApiServer.buildResponse("ERROR", e.getMessage(), null));
        }
    }
}
