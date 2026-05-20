package com.team4.handler;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.team4.dao.impl.AuctionDAOImpl;
import com.team4.dao.impl.ItemDAOImpl;
import com.team4.model.Auction;
import com.team4.model.Bidder;
import com.team4.model.Item;
import com.team4.model.User;
import com.team4.server.ApiServer;
import com.team4.server.Server;
import com.team4.util.BusinessException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserHandler implements HttpHandler {
    private final ItemDAOImpl itemDAO = new ItemDAOImpl();
    private final AuctionDAOImpl auctionDAO = new AuctionDAOImpl();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        if ("OPTIONS".equals(method)) {
            ApiServer.sendResponse(exchange, 204, "");
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String[] parts = path.split("/");
        // /api/user/{userId}/profile or /api/user/{userId}/password
        if (parts.length < 5) {
            ApiServer.sendResponse(exchange, 400, ApiServer.buildResponse("ERROR", "Invalid path", null));
            return;
        }

        String userId = parts[3];
        String action = parts[4];

        try {
            if ("GET".equals(method) && "profile".equals(action)) {
                handleGetProfile(exchange, userId);
            } else if ("GET".equals(method) && "owned-items".equals(action)) {
                handleGetOwnedItems(exchange, userId);
            } else if ("PUT".equals(method) && "profile".equals(action)) {
                handleUpdateProfile(exchange, userId);
            } else if ("PUT".equals(method) && "password".equals(action)) {
                handleChangePassword(exchange, userId);
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        } catch (BusinessException e) {
            ApiServer.sendResponse(exchange, 400, ApiServer.buildResponse("ERROR", e.getMessage(), null));
        } catch (Exception e) {
            e.printStackTrace();
            ApiServer.sendResponse(exchange, 500, ApiServer.buildResponse("ERROR", "Internal server error", null));
        }
    }

    private void handleGetProfile(HttpExchange exchange, String userId) throws IOException {
        User user = Server.getUserService().getUserById(userId);
        if (user == null) {
            ApiServer.sendResponse(exchange, 404, ApiServer.buildResponse("ERROR", "User does not exist", null));
            return;
        }

        JsonObject data = new JsonObject();
        data.addProperty("id", user.getId());
        data.addProperty("username", user.getUsername());
        data.addProperty("fullName", user.getFullName());
        data.addProperty("email", user.getEmail());
        data.addProperty("role", user.getRole().name());
        data.addProperty("balance", user.getBalance());
        
        if (user instanceof Bidder) {
            data.addProperty("phoneNumber", ((Bidder) user).getPhoneNumber());
        } else {
            data.addProperty("phoneNumber", ""); // For Sellers/Admins if they don't have phone
        }

        ApiServer.sendResponse(exchange, 200, ApiServer.buildResponse("SUCCESS", "Profile loaded successfully", data));
    }

    private void handleGetOwnedItems(HttpExchange exchange, String userId) throws IOException {
        User user = Server.getUserService().getUserById(userId);
        if (user == null) {
            ApiServer.sendResponse(exchange, 404, ApiServer.buildResponse("ERROR", "User does not exist", null));
            return;
        }
        if (user.getRole() != User.Role.BIDDER) {
            ApiServer.sendResponse(exchange, 400, ApiServer.buildResponse("ERROR", "Only bidders have won items", null));
            return;
        }

        List<Item> items = itemDAO.findOwnedByBidderId(userId);
        Map<String, Auction> wonAuctionsByItemId = new HashMap<>();
        for (Auction auction : auctionDAO.findAll()) {
            boolean closed = auction.getStatus() == Auction.AuctionStatus.FINISHED
                    || auction.getStatus() == Auction.AuctionStatus.PAID;
            if (closed && userId.equals(auction.getCurrentHighestBidderId())) {
                wonAuctionsByItemId.put(auction.getItemId(), auction);
            }
        }

        JsonArray data = new JsonArray();
        for (Item item : items) {
            JsonObject obj = new JsonObject();
            obj.addProperty("id", item.getId());
            obj.addProperty("name", item.getName());
            obj.addProperty("description", item.getDescription());
            obj.addProperty("category", item.getCategory().name());
            obj.addProperty("startingPrice", item.getStartingPrice());
            obj.addProperty("createdAt", item.getCreatedAt().toString());

            Auction wonAuction = wonAuctionsByItemId.get(item.getId());
            if (wonAuction != null) {
                obj.addProperty("auctionId", wonAuction.getId());
                obj.addProperty("wonPrice", wonAuction.getCurrentPrice());
                obj.addProperty("wonAt", wonAuction.getEndTime().toString());
            }
            data.add(obj);
        }

        ApiServer.sendResponse(exchange, 200, ApiServer.buildResponse("SUCCESS", "Owned items loaded successfully", data));
    }

    private void handleUpdateProfile(HttpExchange exchange, String userId) throws IOException {
        InputStream is = exchange.getRequestBody();
        String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        is.close();

        String fullName = ApiServer.parseParam(body, "fullName");
        String email = ApiServer.parseParam(body, "email");
        String phone = ApiServer.parseParam(body, "phone");

        if (fullName == null || email == null) {
            ApiServer.sendResponse(exchange, 400, ApiServer.buildResponse("ERROR", "Missing fullName or email", null));
            return;
        }

        User updated = Server.getUserService().updateProfile(userId, fullName, email, phone);
        JsonObject data = new JsonObject();
        data.addProperty("fullName", updated.getFullName());
        ApiServer.sendResponse(exchange, 200, ApiServer.buildResponse("SUCCESS", "Updated successfully", data));
    }

    private void handleChangePassword(HttpExchange exchange, String userId) throws IOException {
        InputStream is = exchange.getRequestBody();
        String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        is.close();

        String oldPassword = ApiServer.parseParam(body, "oldPassword");
        String newPassword = ApiServer.parseParam(body, "newPassword");

        if (oldPassword == null || newPassword == null) {
            ApiServer.sendResponse(exchange, 400, ApiServer.buildResponse("ERROR", "Missing password", null));
            return;
        }

        Server.getUserService().changePassword(userId, oldPassword, newPassword);
        ApiServer.sendResponse(exchange, 200, ApiServer.buildResponse("SUCCESS", "Password changed successfully", null));
    }
}
