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
import com.team4.util.BusinessException;

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
            String queryParams = exchange.getRequestURI().getRawQuery();
            String requesterId = ApiServer.getRequesterId(exchange, queryParam(queryParams, "requesterId"));

            if ("GET".equals(method) && (BASE_PATH.equals(path) || (BASE_PATH + "/").equals(path))) {
                handleListUsers(exchange, requesterId, null);
                return;
            }
            if ("GET".equals(method) && (BASE_PATH + "/search").equals(path)) {
                handleListUsers(exchange, requesterId, queryParam(queryParams, "q"));
                return;
            }
            if ("PUT".equals(method)) {
                String[] parts = path.split("/");
                if (parts.length >= 6 && "users".equals(parts[3])) {
                    String targetUserId = parts[4];
                    String action = parts[5];
                    
                    // Read body
                    java.io.InputStream is = exchange.getRequestBody();
                    String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    is.close();
                    
                    String reqId = ApiServer.getRequesterId(exchange, ApiServer.parseParam(body, "requesterId"));
                    
                    if ("grant-admin".equals(action)) {
                        String adminCode = ApiServer.parseParam(body, "adminCode");
                        Server.getAdminService().grantAdmin(reqId, targetUserId, adminCode);
                        ApiServer.sendResponse(exchange, 200, ApiServer.buildResponse("SUCCESS", "Admin role granted successfully", null));
                        return;
                    } else if ("revoke-admin".equals(action)) {
                        Server.getAdminService().revokeAdmin(reqId, targetUserId);
                        ApiServer.sendResponse(exchange, 200, ApiServer.buildResponse("SUCCESS", "Admin role revoked successfully", null));
                        return;
                    }
                }
            }

            exchange.sendResponseHeaders(405, -1);
        } catch (BusinessException e) {
            ApiServer.sendResponse(exchange, 400,
                    ApiServer.buildResponse("ERROR", e.getMessage(), null));
        } catch (Exception e) {
            e.printStackTrace();
            ApiServer.sendResponse(exchange, 500,
                    ApiServer.buildResponse("ERROR", "Operation failed: " + e.getMessage(), null));
        }
    }

    private void handleListUsers(HttpExchange exchange, String requesterId, String query) throws IOException {
        List<UserResponseDTO> users = Server.getAdminService().viewSystemUsers(requesterId);
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
     * Serialize UserResponseDTO sang JsonObject.
     * Lưu ý: không có accessLevel vì UserResponseDTO không chứa trường này.
     */
    private JsonObject toJson(UserResponseDTO dto) {
        return Server.getGson().toJsonTree(dto).getAsJsonObject();
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
