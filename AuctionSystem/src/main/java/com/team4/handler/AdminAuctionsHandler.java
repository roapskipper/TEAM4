package com.team4.handler;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.team4.dao.AuctionDAO;
import com.team4.dao.ItemDAO;
import com.team4.dao.UserDAO;
import com.team4.dao.impl.AuctionDAOImpl;
import com.team4.dao.impl.ItemDAOImpl;
import com.team4.dao.impl.UserDAOImpl;
import com.team4.model.Auction;
import com.team4.model.Item;
import com.team4.model.User;
import com.team4.server.ApiServer;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class AdminAuctionsHandler implements HttpHandler {
    private final AuctionDAO auctionDAO = new AuctionDAOImpl();
    private final ItemDAO itemDAO = new ItemDAOImpl();
    private final UserDAO userDAO = new UserDAOImpl();

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
            // Parse filter
            String query = exchange.getRequestURI().getQuery();
            String filter = "all";
            if (query != null && query.contains("filter=")) {
                String[] params = query.split("&");
                for (String param : params) {
                    if (param.startsWith("filter=")) {
                        filter = param.substring(7);
                        break;
                    }
                }
            }

            List<Auction> auctions;
            switch (filter.toLowerCase()) {
                case "pending":
                    auctions = auctionDAO.findByStatus(Auction.AuctionStatus.PENDING);
                    break;
                case "live":
                    auctions = auctionDAO.findByStatus(Auction.AuctionStatus.RUNNING);
                    break;
                case "rejected":
                    auctions = auctionDAO.findByStatus(Auction.AuctionStatus.CANCELLED);
                    break;
                case "all":
                default:
                    auctions = auctionDAO.findAll();
                    break;
            }

            JsonArray dataArr = new JsonArray();
            if (auctions != null) {
                for (Auction auction : auctions) {
                    JsonObject obj = new JsonObject();
                    obj.addProperty("id", auction.getId());
                    
                    Item item = itemDAO.findById(auction.getItemId());
                    if (item != null) {
                        obj.addProperty("itemName", item.getName());
                    } else {
                        obj.addProperty("itemName", "Unknown Item");
                    }
                    
                    User seller = userDAO.findById(auction.getSellerId());
                    if (seller != null) {
                        String sName = seller.getFullName();
                        if (sName == null || sName.trim().isEmpty()) {
                            sName = seller.getUsername();
                        }
                        obj.addProperty("sellerName", sName);
                    } else {
                        obj.addProperty("sellerName", "Unknown Seller");
                    }
                    
                    obj.addProperty("startPrice", auction.getStartingPrice());
                    obj.addProperty("status", auction.getStatus().name());
                    obj.addProperty("reportCount", 0);
                    
                    dataArr.add(obj);
                }
            }
            
            ApiServer.sendResponse(exchange, 200, ApiServer.buildResponse("SUCCESS", "Lấy danh sách phiên đấu giá (admin) thành công!", dataArr));
        } catch (Exception e) {
            e.printStackTrace();
            ApiServer.sendResponse(exchange, 500, ApiServer.buildResponse("ERROR", e.getMessage(), null));
        }
    }
}
