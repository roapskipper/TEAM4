package com.team4.handler;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.team4.dto.auth.UserResponseDTO;
import com.team4.server.ApiServer;
import com.team4.server.Server;
import com.team4.util.BusinessException;

import java.io.IOException;
import java.io.InputStream;
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
            if ("PUT".equals(method)) {
                String targetUserId = readUserId(path);
                if (targetUserId != null && path.endsWith("/grant-admin")) {
                    handleGrantAdmin(exchange, targetUserId);
                    return;
                }
                if (targetUserId != null && path.endsWith("/revoke-admin")) {
                    handleRevokeAdmin(exchange, targetUserId);
                    return;
                }
            }

            exchange.sendResponseHeaders(405, -1);
        } catch (BusinessException e) {
            ApiServer.sendResponse(exchange, 400,
                    ApiServer.buildResponse("ERROR", e.getMessage(), null));
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

    private void handleGrantAdmin(HttpExchange exchange, String targetUserId) throws IOException {
        String body = readBody(exchange);
        String requesterId = ApiServer.parseParam(body, "requesterId");
        String adminCode = ApiServer.parseParam(body, "adminCode");
        UserResponseDTO dto = Server.getUserService().grantAdminRole(requesterId, targetUserId, adminCode);
        ApiServer.sendResponse(exchange, 200,
                ApiServer.buildResponse("SUCCESS", "Admin privileges granted successfully", toJson(dto)));
    }

    private void handleRevokeAdmin(HttpExchange exchange, String targetUserId) throws IOException {
        String body = readBody(exchange);
        String requesterId = ApiServer.parseParam(body, "requesterId");
        UserResponseDTO dto = Server.getUserService().revokeAdminRole(requesterId, targetUserId);
        ApiServer.sendResponse(exchange, 200,
                ApiServer.buildResponse("SUCCESS", "Admin privileges removed successfully", toJson(dto)));
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

    private JsonObject toJson(UserResponseDTO dto) {
        return Server.getGson().toJsonTree(dto).getAsJsonObject();
    }

    private String readUserId(String path) {
        if (path == null || !path.startsWith(BASE_PATH + "/")) {
            return null;
        }
        String suffix = path.substring((BASE_PATH + "/").length());
        int slash = suffix.indexOf('/');
        if (slash < 0) {
            return null;
        }
        return URLDecoder.decode(suffix.substring(0, slash), StandardCharsets.UTF_8);
    }

    private String readBody(HttpExchange exchange) throws IOException {
        try (InputStream input = exchange.getRequestBody()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
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
