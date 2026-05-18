package com.team4.handler;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.team4.dao.impl.ItemDAOImpl;
import com.team4.model.Item;
import com.team4.server.ApiServer;
import com.team4.server.Server;

import java.io.IOException;
import java.util.List;

public class SellerHandler implements HttpHandler {
    private ItemDAOImpl itemDAO = new ItemDAOImpl();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        if ("OPTIONS".equals(method)) {
            ApiServer.sendResponse(exchange, 204, "");
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String[] parts = path.split("/");
        
        // Cấu trúc URL: /api/seller/{sellerId}/items hoặc /api/seller/{sellerId}/stats
        if (parts.length < 5) {
            ApiServer.sendResponse(exchange, 400, ApiServer.buildResponse("ERROR", "Invalid path", null));
            return;
        }

        String sellerId = parts[3];
        String action = parts[4];

        try {
            if ("GET".equals(method) && "items".equals(action)) {
                List<Item> items = itemDAO.findByOwnerId(sellerId);
                JsonElement dataArr = Server.getGson().toJsonTree(items);
                ApiServer.sendResponse(exchange, 200, ApiServer.buildResponse("SUCCESS", "Lấy danh sách sản phẩm thành công", dataArr));
            } else if ("GET".equals(method) && "stats".equals(action)) {
                List<Item> items = itemDAO.findByOwnerId(sellerId);
                int totalProducts = items.size();
                int activeAuctions = 0; 
                int pendingProducts = totalProducts; 
                int soldProducts = 0;

                JsonObject stats = new JsonObject();
                stats.addProperty("totalProducts", totalProducts);
                stats.addProperty("activeAuctions", activeAuctions);
                stats.addProperty("pendingProducts", pendingProducts);
                stats.addProperty("soldProducts", soldProducts);

                ApiServer.sendResponse(exchange, 200, ApiServer.buildResponse("SUCCESS", "Lấy thống kê thành công", stats));
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        } catch (Exception e) {
            e.printStackTrace();
            ApiServer.sendResponse(exchange, 500, ApiServer.buildResponse("ERROR", "Lỗi máy chủ: " + e.getMessage(), null));
        }
    }
}
