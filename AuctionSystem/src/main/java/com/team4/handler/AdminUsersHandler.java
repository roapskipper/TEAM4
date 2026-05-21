package com.team4.handler;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.team4.mapper.UserMapper;
import com.team4.dto.auth.UserResponseDTO;
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
        // getAllUsers() giờ trả về List<UserResponseDTO>
        List<UserResponseDTO> users = Server.getUserService().getAllUsers();
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);

        JsonArray data = new JsonArray();
        for (UserResponseDTO dto : users) {
            if (!matchesQuery(dto, normalizedQuery)) {
                continue;
            }
            data.add(toJson(dto));
        }

        ApiServer.sendResponse(exchange, 200,
                ApiServer.buildResponse("SUCCESS", "Users loaded successfully", data));
    }

    private boolean matchesQuery(UserResponseDTO dto, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        return contains(dto.getUsername(), query)
                || contains(dto.getFullName(), query)
                || contains(dto.getEmail(), query)
                || dto.getRole().name().toLowerCase(Locale.ROOT).contains(query);
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    /**
     * Serialize UserResponseDTO sang JsonObject, thêm status (hardcode ACTIVE).
     * Lưu ý: không có accessLevel vì UserResponseDTO không chứa trường này.
     */
    private JsonObject toJson(UserResponseDTO dto) {
        JsonObject obj = Server.getGson().toJsonTree(dto).getAsJsonObject();
        obj.addProperty("status", "ACTIVE");
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
