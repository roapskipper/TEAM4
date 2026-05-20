package com.team4.handler;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.team4.model.Admin;
import com.team4.model.Bidder;
import com.team4.model.Seller;
import com.team4.model.User;
import com.team4.server.ApiServer;
import com.team4.server.Server;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

public class AdminUsersHandler implements HttpHandler {
    private static final String BASE_PATH = "/api/admin/users";

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        if ("OPTIONS".equals(method)) {
            ApiServer.sendResponse(exchange, 204, "");
            return;
        }

        try {
            String path = exchange.getRequestURI().getPath();
            if ("GET".equals(method) && (BASE_PATH.equals(path) || (BASE_PATH + "/").equals(path))) {
                handleListUsers(exchange, null);
                return;
            }
            if ("GET".equals(method) && (BASE_PATH + "/search").equals(path)) {
                handleListUsers(exchange, queryParam(exchange.getRequestURI().getRawQuery(), "q"));
                return;
            }

            exchange.sendResponseHeaders(405, -1);
        } catch (Exception e) {
            e.printStackTrace();
            ApiServer.sendResponse(exchange, 500,
                    ApiServer.buildResponse("ERROR", "Unable to load users", null));
        }
    }

    private void handleListUsers(HttpExchange exchange, String query) throws IOException {
        List<User> users = Server.getUserService().getAllUsers();
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);

        JsonArray data = new JsonArray();
        for (User user : users) {
            if (!matchesQuery(user, normalizedQuery)) {
                continue;
            }
            data.add(toJson(user));
        }

        ApiServer.sendResponse(exchange, 200,
                ApiServer.buildResponse("SUCCESS", "Users loaded successfully", data));
    }

    private boolean matchesQuery(User user, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        return contains(user.getUsername(), query)
                || contains(user.getFullName(), query)
                || contains(user.getEmail(), query)
                || user.getRole().name().toLowerCase(Locale.ROOT).contains(query);
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private JsonObject toJson(User user) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", user.getId());
        obj.addProperty("username", user.getUsername());
        obj.addProperty("fullName", user.getFullName());
        obj.addProperty("email", user.getEmail());
        obj.addProperty("role", user.getRole().name());
        obj.addProperty("createdAt", user.getCreatedAt().toString());
        obj.addProperty("status", "ACTIVE");

        if (user instanceof Admin admin) {
            obj.addProperty("accessLevel", admin.getAccessLevel().name());
        } else if (user instanceof Seller seller) {
            obj.addProperty("storeName", seller.getStoreName());
            obj.addProperty("rating", seller.getRating());
        } else if (user instanceof Bidder bidder) {
            obj.addProperty("phoneNumber", bidder.getPhoneNumber());
        }
        return obj;
    }

    private String queryParam(String rawQuery, String key) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return "";
        }
        String[] pairs = rawQuery.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && key.equals(kv[0])) {
                return URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
            }
        }
        return "";
    }
}
