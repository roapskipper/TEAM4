package com.team4.handler;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.team4.server.ApiServer;
import com.team4.server.Server;
import com.team4.util.BusinessException;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class AdminDashboardHandler implements HttpHandler {
    private static final String BASE_PATH = "/api/admin/dashboard/stats";

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
                JsonObject stats = Server.getAdminService().getDashboardStats(requesterId);
                ApiServer.sendResponse(exchange, 200, 
                        ApiServer.buildResponse("SUCCESS", "Dashboard stats loaded successfully", stats));
                return;
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
