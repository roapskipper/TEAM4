package com.team4.handler;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.team4.dao.impl.UserDAOImpl;
import com.team4.model.User;
import com.team4.server.ApiServer;
import com.team4.service.AuthenticationService;
import com.team4.util.BusinessException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class LoginHandler implements HttpHandler {
    private AuthenticationService authService = new AuthenticationService(new UserDAOImpl());

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

        String username = ApiServer.parseParam(body, "username");
        String password = ApiServer.parseParam(body, "password");
        String adminCode = ApiServer.parseParam(body, "adminCode");

        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            ApiServer.sendResponse(exchange, 400,
                    ApiServer.buildResponse("ERROR", "Thieu username hoac password", null));
            return;
        }

        try {
            User user;
            if (adminCode != null && !adminCode.isEmpty()) {
                user = authService.loginAdmin(username, password, adminCode);
            } else {
                user = authService.login(username, password);
            }
            JsonObject data = new JsonObject();
            data.addProperty("userId", user.getId());
            data.addProperty("username", user.getUsername());
            data.addProperty("role", user.getRole().name());
            data.addProperty("fullName", user.getFullName());
            ApiServer.sendResponse(exchange, 200, ApiServer.buildResponse("SUCCESS", "Dang nhap thanh cong!", data));
        } catch (BusinessException e) {
            ApiServer.sendResponse(exchange, 401, ApiServer.buildResponse("ERROR", e.getMessage(), null));
        }
    }
}