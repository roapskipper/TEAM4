package com.team4.handler;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.team4.dto.auction.AdminAuctionResponseDTO;
import com.team4.server.ApiServer;
import com.team4.server.Server;
import com.team4.util.BusinessException;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class AdminAuctionsHandler implements HttpHandler {
    private static final String BASE_PATH = "/api/admin/auctions";

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

            if ("GET".equals(method)) {
                String filter = queryParam(queryParams, "filter");
                if (filter.isEmpty()) {
                    filter = "all";
                }
                
                List<AdminAuctionResponseDTO> auctions = Server.getAdminService().viewAuctions(requesterId, filter);
                JsonArray dataArr = new JsonArray();
                if (auctions != null) {
                    for (AdminAuctionResponseDTO auction : auctions) {
                        JsonObject obj = new JsonObject();
                        obj.addProperty("id", auction.getId());
                        obj.addProperty("itemName", auction.getItemName());
                        obj.addProperty("sellerName", auction.getSellerName());
                        obj.addProperty("startPrice", auction.getStartPrice());
                        obj.addProperty("status", auction.getStatus());
                        obj.addProperty("reportCount", auction.getReportCount());
                        dataArr.add(obj);
                    }
                }
                ApiServer.sendResponse(exchange, 200, ApiServer.buildResponse("SUCCESS", "Admin auction list loaded successfully!", dataArr));
                return;
            }

            if ("PUT".equals(method)) {
                String[] parts = path.split("/");
                if (parts.length >= 6 && "auctions".equals(parts[3])) {
                    String auctionId = parts[4];
                    String action = parts[5];

                    // Read body
                    java.io.InputStream is = exchange.getRequestBody();
                    String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    is.close();

                    String reqId = ApiServer.getRequesterId(exchange, ApiServer.parseParam(body, "requesterId"));

                    if ("approve".equals(action)) {
                        Server.getAdminService().approveAuction(reqId, auctionId);
                        ApiServer.sendResponse(exchange, 200, ApiServer.buildResponse("SUCCESS", "Auction approved successfully", null));
                        return;
                    } else if ("reject".equals(action)) {
                        Server.getAdminService().rejectAuction(reqId, auctionId);
                        ApiServer.sendResponse(exchange, 200, ApiServer.buildResponse("SUCCESS", "Auction rejected successfully", null));
                        return;
                    }
                }
            }

            exchange.sendResponseHeaders(405, -1);
        } catch (BusinessException e) {
            ApiServer.sendResponse(exchange, 400, ApiServer.buildResponse("ERROR", e.getMessage(), null));
        } catch (Exception e) {
            e.printStackTrace();
            ApiServer.sendResponse(exchange, 500, ApiServer.buildResponse("ERROR", "Operation failed: " + e.getMessage(), null));
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
