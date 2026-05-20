package com.team4.handler;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.team4.dao.UserDAO;
import com.team4.dao.impl.UserDAOImpl;
import com.team4.model.Admin;
import com.team4.model.Bidder;
import com.team4.model.Seller;
import com.team4.model.User;
import com.team4.server.ApiServer;
import com.team4.server.Server;
import com.team4.util.BusinessException;
import com.team4.util.PasswordHasher;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

public class AdminUsersHandler implements HttpHandler {
    private static final String BASE_PATH = "/api/admin/users";
    private final UserDAO userDAO = new UserDAOImpl();

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
                handleListUsers(exchange);
                return;
            }
            if ("GET".equals(method) && (BASE_PATH + "/search").equals(path)) {
                handleListUsers(exchange);
                return;
            }
            if ("PUT".equals(method)) {
                handleUserAction(exchange, path);
                return;
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

    private void handleListUsers(HttpExchange exchange) throws IOException {
        List<User> users = Server.getUserService().getAllUsers();
        String rawQuery = exchange.getRequestURI().getRawQuery();
        String query = queryParam(rawQuery, "q");
        String requesterId = queryParam(rawQuery, "requesterId");
        User requester = requesterId.isBlank() ? null : Server.getUserService().getUserById(requesterId);
        boolean superAdmin = isSuperAdmin(requester);
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);

        JsonArray data = new JsonArray();
        for (User user : users) {
            if (!canViewerSee(user, superAdmin)) {
                continue;
            }
            if (!matchesQuery(user, normalizedQuery)) {
                continue;
            }
            data.add(toJson(user));
        }

        ApiServer.sendResponse(exchange, 200,
                ApiServer.buildResponse("SUCCESS", "Users loaded successfully", data));
    }

    private void handleUserAction(HttpExchange exchange, String path) throws IOException {
        String suffix = path.substring(BASE_PATH.length());
        if (suffix.startsWith("/")) {
            suffix = suffix.substring(1);
        }
        String[] parts = suffix.split("/");
        if (parts.length != 2) {
            exchange.sendResponseHeaders(404, -1);
            return;
        }

        String userId = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
        String action = parts[1];
        String body = readBody(exchange);

        switch (action) {
            case "suspend":
                updateStatus(exchange, userId, body, "SUSPENDED", "User suspended successfully");
                break;
            case "ban":
                updateStatus(exchange, userId, body, "BANNED", "User banned successfully");
                break;
            case "unsuspend":
            case "unban":
                updateStatus(exchange, userId, body, "ACTIVE", "User restored successfully");
                break;
            case "grant-admin":
                grantAdmin(exchange, userId, body);
                break;
            case "revoke-admin":
                revokeAdmin(exchange, userId, body);
                break;
            default:
                exchange.sendResponseHeaders(404, -1);
        }
    }

    private void updateStatus(HttpExchange exchange, String userId, String body, String status, String message) throws IOException {
        String requesterId = ApiServer.parseParam(body, "requesterId");
        User requester = Server.getUserService().getUserById(requesterId);
        User user = Server.getUserService().getUserById(userId);
        if (user == null) {
            throw new BusinessException("User does not exist");
        }
        ensureAdminCanManage(requester, user);
        if (!userDAO.updateAccountStatus(userId, status)) {
            throw new BusinessException("Unable to update user status");
        }
        ApiServer.sendResponse(exchange, 200, ApiServer.buildResponse("SUCCESS", message, null));
    }

    private void grantAdmin(HttpExchange exchange, String userId, String body) throws IOException {
        String requesterId = ApiServer.parseParam(body, "requesterId");
        String adminCode = ApiServer.parseParam(body, "adminCode");
        User requester = Server.getUserService().getUserById(requesterId);
        if (!isSuperAdmin(requester)) {
            throw new BusinessException("Only Super Admin can grant admin privileges");
        }

        User target = Server.getUserService().getUserById(userId);
        if (target == null) {
            throw new BusinessException("User does not exist");
        }
        if (target.getRole() == User.Role.ADMIN) {
            throw new BusinessException("User is already an admin");
        }
        if (adminCode == null || adminCode.length() < 8) {
            throw new BusinessException("Admin code must be at least 8 characters long");
        }

        String hashedCode = PasswordHasher.hashPassword(adminCode);
        if (!userDAO.grantAdminRole(userId, hashedCode)) {
            throw new BusinessException("Unable to grant admin privileges");
        }
        ApiServer.sendResponse(exchange, 200,
                ApiServer.buildResponse("SUCCESS", "Admin privileges granted successfully", null));
    }

    private void revokeAdmin(HttpExchange exchange, String userId, String body) throws IOException {
        String requesterId = ApiServer.parseParam(body, "requesterId");
        User requester = Server.getUserService().getUserById(requesterId);
        if (!isSuperAdmin(requester)) {
            throw new BusinessException("Only Super Admin can revoke admin privileges");
        }

        User target = Server.getUserService().getUserById(userId);
        if (target == null) {
            throw new BusinessException("User does not exist");
        }
        if (requester.getId().equals(target.getId())) {
            throw new BusinessException("Super Admin cannot revoke the current account");
        }
        if (target.getRole() != User.Role.ADMIN) {
            throw new BusinessException("User is not an admin");
        }
        if (isSuperAdmin(target)) {
            throw new BusinessException("Super Admin role cannot be revoked here");
        }

        if (!userDAO.revokeAdminRole(userId)) {
            throw new BusinessException("Unable to revoke admin privileges");
        }
        ApiServer.sendResponse(exchange, 200,
                ApiServer.buildResponse("SUCCESS", "Admin privileges revoked successfully", null));
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

    private boolean canViewerSee(User user, boolean superAdmin) {
        if (superAdmin) {
            return true;
        }
        return user.getRole() == User.Role.BIDDER || user.getRole() == User.Role.SELLER;
    }

    private void ensureAdminCanManage(User requester, User target) {
        if (!(requester instanceof Admin)) {
            throw new BusinessException("Admin permission is required");
        }
        if (requester.getId().equals(target.getId())) {
            throw new BusinessException("Cannot update the current account here");
        }
        if (target.getRole() == User.Role.ADMIN && !isSuperAdmin(requester)) {
            throw new BusinessException("Only Super Admin can manage admin accounts");
        }
    }

    private JsonObject toJson(User user) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", user.getId());
        obj.addProperty("username", user.getUsername());
        obj.addProperty("fullName", user.getFullName());
        obj.addProperty("email", user.getEmail());
        obj.addProperty("role", user.getRole().name());
        obj.addProperty("createdAt", user.getCreatedAt().toString());
        obj.addProperty("status", userDAO.getAccountStatus(user.getId()));

        if (user instanceof Admin admin) {
            obj.addProperty("accessLevel", admin.getAccessLevel().name());
            obj.addProperty("accessLevelCode", admin.getAccessLevel().getLevel());
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

    private boolean isSuperAdmin(User user) {
        return user instanceof Admin admin && admin.getAccessLevel() == Admin.AccessLevel.SUPER_ADMIN;
    }

    private String readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
