package com.team4.handler;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.team4.dao.impl.UserDAOImpl;
import com.team4.dto.auth.RegisterSellerRequestDTO;
import com.team4.server.ApiServer;
import com.team4.server.Server;
import com.team4.service.AuthenticationService;
import com.team4.service.JwtService;
import com.team4.util.BusinessException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class RegisterSellerHandler implements HttpHandler {
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

        String username  = ApiServer.parseParam(body, "username");
        String password  = ApiServer.parseParam(body, "password");
        String fullName  = ApiServer.parseParam(body, "fullName");
        String email     = ApiServer.parseParam(body, "email");
        String storeName = ApiServer.parseParam(body, "storeName");

        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            ApiServer.sendResponse(exchange, 400, ApiServer.buildResponse("ERROR", "Missing username or password.", null));
            return;
        }

        try {
            // Tạo RegisterSellerRequestDTO qua Gson (không gọi validate() trong constructor)
            JsonObject p = new JsonObject();
            p.addProperty("username",  username);
            p.addProperty("password",  password);
            p.addProperty("fullName",  fullName  != null ? fullName  : "");
            p.addProperty("email",     email     != null ? email     : "");
            p.addProperty("storeName", storeName != null ? storeName : "");
            RegisterSellerRequestDTO dto = Server.getGson().fromJson(p, RegisterSellerRequestDTO.class);

            authService.registerSeller(dto);
            ApiServer.sendResponse(exchange, 200, ApiServer.buildResponse("SUCCESS", "Seller registration successful.", null));
        } catch (BusinessException | IllegalArgumentException e) {
            ApiServer.sendResponse(exchange, 400, ApiServer.buildResponse("ERROR", e.getMessage(), null));
        } catch (Exception e) {
            e.printStackTrace();
            ApiServer.sendResponse(exchange, 500, ApiServer.buildResponse("ERROR", "Internal Server Error: " + e.getMessage(), null));
        }
    }
}
