package com.team4.handler;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.team4.dao.AuctionDAO;
import com.team4.dao.BidTransactionDAO;
import com.team4.dao.ItemDAO;
import com.team4.dao.UserDAO;
import com.team4.dao.impl.AuctionDAOImpl;
import com.team4.dao.impl.BidTransactionDAOImpl;
import com.team4.dao.impl.ItemDAOImpl;
import com.team4.dao.impl.UserDAOImpl;
import com.team4.dto.auction.BidTransactionResponseDTO;
import com.team4.mapper.BidMapper;
import com.team4.model.Auction;
import com.team4.model.BidTransaction;
import com.team4.model.Item;
import com.team4.model.Seller;
import com.team4.model.User;
import com.team4.server.ApiServer;
import com.team4.server.Server;
import com.team4.service.AuctionService;
import com.team4.util.BusinessException;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class AuctionsHandler implements HttpHandler {
    private static final String BASE_PATH = "/api/auctions";

    private final AuctionDAO auctionDAO = new AuctionDAOImpl();
    private final ItemDAO itemDAO = new ItemDAOImpl();
    private final UserDAO userDAO = new UserDAOImpl();
    private final BidTransactionDAO bidTransactionDAO = new BidTransactionDAOImpl();
    private final AuctionService auctionService = new AuctionService(auctionDAO, itemDAO);

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            ApiServer.sendResponse(exchange, 204, "");
            return;
        }
        if (!"GET".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        try {
            String auctionId = readAuctionId(exchange.getRequestURI().getPath());
            if (auctionId != null) {
                Auction auction = auctionService.getRawAuctionById(auctionId);
                ApiServer.sendResponse(exchange, 200, ApiServer.buildResponse(
                        "SUCCESS",
                        "Lay chi tiet phien dau gia thanh cong!",
                        buildAuctionJson(auction, true)));
                return;
            }

            List<Auction> auctions = auctionDAO.findAll();
            JsonArray dataArr = new JsonArray();
            for (Auction auction : auctions) {
                dataArr.add(buildAuctionJson(auction, false));
            }
            ApiServer.sendResponse(exchange, 200,
                    ApiServer.buildResponse("SUCCESS", "Lay danh sach phien dau gia thanh cong!", dataArr));
        } catch (BusinessException e) {
            ApiServer.sendResponse(exchange, 400, ApiServer.buildResponse("ERROR", e.getMessage(), null));
        } catch (Exception e) {
            ApiServer.sendResponse(exchange, 500, ApiServer.buildResponse("ERROR", e.getMessage(), null));
        }
    }

    private String readAuctionId(String path) {
        if (path == null || path.length() <= BASE_PATH.length()) {
            return null;
        }
        String suffix = path.substring(BASE_PATH.length());
        if (suffix.equals("/") || suffix.isBlank()) {
            return null;
        }
        if (suffix.startsWith("/")) {
            suffix = suffix.substring(1);
        }
        int slash = suffix.indexOf('/');
        if (slash >= 0) {
            suffix = suffix.substring(0, slash);
        }
        return URLDecoder.decode(suffix, StandardCharsets.UTF_8);
    }

    private JsonObject buildAuctionJson(Auction auction, boolean includeHistory) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", auction.getId());
        obj.addProperty("itemId", auction.getItemId());
        obj.addProperty("sellerId", auction.getSellerId());
        obj.addProperty("startingPrice", auction.getStartingPrice());
        obj.addProperty("currentPrice", auction.getCurrentPrice());
        obj.addProperty("bidIncrement", auction.getBidIncrement());
        obj.addProperty("currentHighestBidderId", auction.getCurrentHighestBidderId());
        obj.addProperty("startTime", auction.getStartTime().toString());
        obj.addProperty("endTime", auction.getEndTime().toString());
        obj.addProperty("status", auction.getStatus().name());

        Item item = itemDAO.findById(auction.getItemId());
        if (item != null) {
            obj.addProperty("itemName", item.getName());
            obj.addProperty("itemDescription", item.getDescription());
            obj.addProperty("category", item.getCategory().name());
        } else {
            obj.addProperty("itemName", "Unknown Item");
            obj.addProperty("itemDescription", "");
            obj.addProperty("category", "");
        }

        User seller = userDAO.findById(auction.getSellerId());
        if (seller != null) {
            obj.addProperty("sellerName", displayName(seller));
            if (seller instanceof Seller) {
                Seller sellerInfo = (Seller) seller;
                obj.addProperty("sellerStoreName", sellerInfo.getStoreName());
                obj.addProperty("sellerRating", sellerInfo.getRating());
            }
        } else {
            obj.addProperty("sellerName", "Unknown Seller");
        }

        User leader = auction.getCurrentHighestBidderId() == null
                ? null
                : userDAO.findById(auction.getCurrentHighestBidderId());
        obj.addProperty("currentHighestBidderName", leader != null ? displayName(leader) : "");

        List<BidTransaction> history = bidTransactionDAO.findByAuctionId(auction.getId());
        obj.addProperty("bidCount", history.size());

        if (includeHistory) {
            JsonArray historyArr = new JsonArray();
            for (BidTransaction bid : history) {
                // Dùng BidMapper → BidTransactionResponseDTO để serialize
                BidTransactionResponseDTO bidDTO = BidMapper.toBidTransactionResponseDTO(bid);
                JsonObject bidObj = Server.getGson().toJsonTree(bidDTO).getAsJsonObject();
                // Bổ sung bidderName – trường này không có trong DTO nhưng client cần hiển thị
                User bidder = userDAO.findById(bid.getBidderId());
                bidObj.addProperty("bidderName", bidder != null ? displayName(bidder) : "Unknown Bidder");
                historyArr.add(bidObj);
            }
            obj.add("bidHistory", historyArr);
        }

        return obj;
    }

    private String displayName(User user) {
        String fullName = user.getFullName();
        if (fullName != null && !fullName.trim().isEmpty()) {
            return fullName;
        }
        return user.getUsername();
    }
}
