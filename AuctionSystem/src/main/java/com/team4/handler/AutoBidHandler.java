package com.team4.handler;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.team4.dao.AutoBiddingDAO;
import com.team4.dao.AuctionDAO;
import com.team4.dao.UserDAO;
import com.team4.dao.impl.AutoBiddingDAOImpl;
import com.team4.dao.impl.AuctionDAOImpl;
import com.team4.dao.impl.UserDAOImpl;
import com.team4.dto.bidding.AutoBidRequestDTO;
import com.team4.dto.bidding.AutoBidResponseDTO;
import com.team4.model.AutoBidding;
import com.team4.server.ApiServer;
import com.team4.service.AutoBiddingService;
import com.team4.util.BusinessException;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Xử lý các yêu cầu bật/tắt/xem trạng thái Auto Bidding qua REST API.
 *
 * Các endpoint được hỗ trợ:
 *   POST   /api/auctions/{auctionId}/autobid          -> Bật auto-bid
 *   DELETE /api/auctions/{auctionId}/autobid          -> Tắt auto-bid
 *   GET    /api/auctions/{auctionId}/autobid?bidderId -> Lấy trạng thái hiện tại
 */
public class AutoBidHandler implements HttpHandler {

    private final AutoBiddingService autoBiddingService;

    public AutoBidHandler() {
        AutoBiddingDAO autoBiddingDAO = new AutoBiddingDAOImpl();
        AuctionDAO auctionDAO = new AuctionDAOImpl();
        UserDAO userDAO = new UserDAOImpl();
        this.autoBiddingService = new AutoBiddingService(autoBiddingDAO, auctionDAO, userDAO);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            ApiServer.sendResponse(exchange, 204, "");
            return;
        }

        try {
            String path = exchange.getRequestURI().getPath();
            // Expected path: /api/auctions/{auctionId}/autobid
            String auctionId = extractAuctionId(path);
            if (auctionId == null || auctionId.isBlank()) {
                ApiServer.sendResponse(exchange, 400,
                        ApiServer.buildResponse("ERROR", "Missing auction ID in path.", null));
                return;
            }

            String method = exchange.getRequestMethod();

            switch (method) {
                case "POST":
                    handleEnable(exchange, auctionId);
                    break;
                case "DELETE":
                    handleDisable(exchange, auctionId);
                    break;
                case "GET":
                    handleGetStatus(exchange, auctionId);
                    break;
                default:
                    exchange.sendResponseHeaders(405, -1);
            }
        } catch (BusinessException e) {
            ApiServer.sendResponse(exchange, 400, ApiServer.buildResponse("ERROR", e.getMessage(), null));
        } catch (Exception e) {
            ApiServer.sendResponse(exchange, 500, ApiServer.buildResponse("ERROR", e.getMessage(), null));
        }
    }

    /**
     * POST /api/auctions/{auctionId}/autobid
     * Body (form-urlencoded): bidderId=...&maxAmount=...
     */
    private void handleEnable(HttpExchange exchange, String auctionId) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String bidderId = ApiServer.parseParam(body, "bidderId");
        String maxAmountStr = ApiServer.parseParam(body, "maxAmount");

        if (bidderId == null || bidderId.isBlank()) {
            ApiServer.sendResponse(exchange, 400, ApiServer.buildResponse("ERROR", "Missing bidderId.", null));
            return;
        }
        if (maxAmountStr == null || maxAmountStr.isBlank()) {
            ApiServer.sendResponse(exchange, 400, ApiServer.buildResponse("ERROR", "Missing maxAmount.", null));
            return;
        }

        BigDecimal maxAmount;
        try {
            maxAmount = new BigDecimal(maxAmountStr);
        } catch (NumberFormatException e) {
            ApiServer.sendResponse(exchange, 400, ApiServer.buildResponse("ERROR", "Invalid maxAmount value.", null));
            return;
        }

        AutoBidRequestDTO dto = new AutoBidRequestDTO(auctionId, bidderId, maxAmount);
        AutoBidResponseDTO result = autoBiddingService.enableAutoBidding(dto);
        ApiServer.sendResponse(exchange, 200,
                ApiServer.buildResponse("SUCCESS", "Auto-bid enabled successfully.", toJson(result)));
    }

    /**
     * DELETE /api/auctions/{auctionId}/autobid
     * Body (form-urlencoded): bidderId=...
     */
    private void handleDisable(HttpExchange exchange, String auctionId) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String bidderId = ApiServer.parseParam(body, "bidderId");

        if (bidderId == null || bidderId.isBlank()) {
            ApiServer.sendResponse(exchange, 400, ApiServer.buildResponse("ERROR", "Missing bidderId.", null));
            return;
        }

        // Tìm configId dựa vào auctionId + bidderId
        AutoBidResponseDTO existing = autoBiddingService.findConfig(bidderId, auctionId);
        autoBiddingService.disableAutoBidding(existing.getId());
        ApiServer.sendResponse(exchange, 200,
                ApiServer.buildResponse("SUCCESS", "Auto-bid disabled successfully.", null));
    }

    /**
     * GET /api/auctions/{auctionId}/autobid?bidderId=...
     */
    private void handleGetStatus(HttpExchange exchange, String auctionId) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        String bidderId = null;
        if (query != null) {
            for (String param : query.split("&")) {
                String[] kv = param.split("=", 2);
                if (kv.length == 2 && "bidderId".equals(kv[0])) {
                    bidderId = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                }
            }
        }

        if (bidderId == null || bidderId.isBlank()) {
            ApiServer.sendResponse(exchange, 400, ApiServer.buildResponse("ERROR", "Missing bidderId.", null));
            return;
        }

        try {
            AutoBidResponseDTO config = autoBiddingService.findConfig(bidderId, auctionId);
            ApiServer.sendResponse(exchange, 200,
                    ApiServer.buildResponse("SUCCESS", "Auto-bid config found.", toJson(config)));
        } catch (BusinessException e) {
            // Không có config nào -> trả về trạng thái inactive
            JsonObject inactive = new JsonObject();
            inactive.addProperty("active", false);
            ApiServer.sendResponse(exchange, 200,
                    ApiServer.buildResponse("SUCCESS", "No auto-bid configured.", inactive));
        }
    }

    /** Trích xuất auctionId từ path dạng /api/auctions/{id}/autobid */
    private String extractAuctionId(String path) {
        // path: /api/auctions/XXXX/autobid
        String prefix = "/api/auctions/";
        String suffix = "/autobid";
        if (path == null || !path.startsWith(prefix) || !path.endsWith(suffix)) {
            return null;
        }
        String middle = path.substring(prefix.length(), path.length() - suffix.length());
        return URLDecoder.decode(middle, StandardCharsets.UTF_8);
    }

    private JsonObject toJson(AutoBidResponseDTO dto) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", dto.getId());
        obj.addProperty("auctionId", dto.getAuctionId());
        obj.addProperty("bidderId", dto.getBidderId());
        obj.addProperty("maxAmount", dto.getMaxLimit());
        obj.addProperty("active", dto.isActive());
        return obj;
    }
}
