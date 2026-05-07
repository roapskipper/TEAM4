package com.team4.server;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.team4.dao.impl.ItemDAOImpl;
import com.team4.dao.impl.UserDAOImpl;
import com.team4.dao.impl.AuctionDAOImpl;
import com.team4.model.Item;
import com.team4.model.User;
import com.team4.model.Auction;
import com.team4.service.AuthenticationService;
import com.team4.service.AuctionService;
import com.team4.util.BusinessException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

public class ApiServer {
    private static final int API_PORT = 8080;

    public void start() {
        new Thread(() -> {
            try {
                HttpServer server = HttpServer.create(new InetSocketAddress(API_PORT), 0);
                server.createContext("/api/login", new LoginHandler());
                server.createContext("/api/register/bidder", new RegisterBidderHandler());
                server.createContext("/api/register/seller", new RegisterSellerHandler());
                server.createContext("/api/items", new ItemsHandler());
                server.createContext("/api/auctions", new AuctionsHandler());
                server.setExecutor(null);
                server.start();
                System.out.println("API Server dang chay tren port " + API_PORT + "...");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static void sendResponse(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    public static String buildResponse(String status, String message, JsonElement data) {
        JsonObject response = new JsonObject();
        response.addProperty("status", status);
        response.addProperty("message", message);
        if (data != null) {
            response.add("data", data);
        }
        return Server.getGson().toJson(response);
    }

    public static String parseParam(String body, String key) {
        String[] params = body.split("&");
        for (int i = 0; i < params.length; i++) {
            String[] kv = params[i].split("=", 2);
            if (kv.length == 2 && kv[0].equals(key)) {
                return java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }
}

class LoginHandler implements HttpHandler {
    private AuthenticationService authService = new AuthenticationService(new UserDAOImpl());

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            ApiServer.sendResponse(exchange, 204, "");
            return;
        }
        if (!"POST".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        InputStream is = exchange.getRequestBody();
        String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        is.close();

        String username = ApiServer.parseParam(body, "username");
        String password = ApiServer.parseParam(body, "password");

        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            ApiServer.sendResponse(exchange, 400, ApiServer.buildResponse("ERROR", "Thieu username hoac password", null));
            return;
        }

        try {
            User user = authService.login(username, password);
            JsonObject data = new JsonObject();
            data.addProperty("userId", user.getId());
            data.addProperty("role", user.getRole().name());
            data.addProperty("fullName", user.getFullName());

            ApiServer.sendResponse(exchange, 200, ApiServer.buildResponse("SUCCESS", "Dang nhap thanh cong!", data));
        } catch (BusinessException e) {
            ApiServer.sendResponse(exchange, 401, ApiServer.buildResponse("ERROR", e.getMessage(), null));
        }
    }
}

class RegisterBidderHandler implements HttpHandler {
    private AuthenticationService authService = new AuthenticationService(new UserDAOImpl());

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            ApiServer.sendResponse(exchange, 204, "");
            return;
        }
        if (!"POST".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        InputStream is = exchange.getRequestBody();
        String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        is.close();

        String username = ApiServer.parseParam(body, "username");
        String password = ApiServer.parseParam(body, "password");
        String fullName = ApiServer.parseParam(body, "fullName");
        String email = ApiServer.parseParam(body, "email");
        String shippingAddress = ApiServer.parseParam(body, "shippingAddress");
        String phoneNumber = ApiServer.parseParam(body, "phoneNumber");

        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            ApiServer.sendResponse(exchange, 400, ApiServer.buildResponse("ERROR", "Thieu username hoac password", null));
            return;
        }

        try {
            authService.registerBidder(
                    username, password,
                    fullName != null ? fullName : "",
                    email != null ? email : "",
                    shippingAddress != null ? shippingAddress : "",
                    phoneNumber != null ? phoneNumber : ""
            );
            ApiServer.sendResponse(exchange, 200, ApiServer.buildResponse("SUCCESS", "Dang ky Bidder thanh cong!", null));
        } catch (BusinessException e) {
            ApiServer.sendResponse(exchange, 400, ApiServer.buildResponse("ERROR", e.getMessage(), null));
        } catch (Exception e) {
            e.printStackTrace();
            ApiServer.sendResponse(exchange, 500, ApiServer.buildResponse("ERROR", "Internal Server Error: " + e.getMessage(), null));
        }
    }
}

class RegisterSellerHandler implements HttpHandler {
    private AuthenticationService authService = new AuthenticationService(new UserDAOImpl());

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            ApiServer.sendResponse(exchange, 204, "");
            return;
        }
        if (!"POST".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        InputStream is = exchange.getRequestBody();
        String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        is.close();

        String username = ApiServer.parseParam(body, "username");
        String password = ApiServer.parseParam(body, "password");
        String fullName = ApiServer.parseParam(body, "fullName");
        String email = ApiServer.parseParam(body, "email");
        String storeName = ApiServer.parseParam(body, "storeName");

        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            ApiServer.sendResponse(exchange, 400, ApiServer.buildResponse("ERROR", "Thieu username hoac password", null));
            return;
        }

        try {
            authService.registerSeller(
                    username, password,
                    fullName != null ? fullName : "",
                    email != null ? email : "",
                    storeName != null ? storeName : ""
            );
            ApiServer.sendResponse(exchange, 200, ApiServer.buildResponse("SUCCESS", "Dang ky Seller thanh cong!", null));
        } catch (BusinessException e) {
            ApiServer.sendResponse(exchange, 400, ApiServer.buildResponse("ERROR", e.getMessage(), null));
        } catch (Exception e) {
            e.printStackTrace();
            ApiServer.sendResponse(exchange, 500, ApiServer.buildResponse("ERROR", "Internal Server Error: " + e.getMessage(), null));
        }
    }
}

class ItemsHandler implements HttpHandler {
    private ItemDAOImpl itemDAO = new ItemDAOImpl();

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
            List<Item> items = itemDAO.findAll();
            JsonElement dataArr = Server.getGson().toJsonTree(items);
            ApiServer.sendResponse(exchange, 200, ApiServer.buildResponse("SUCCESS", "Lay danh sach items thanh cong!", dataArr));
        } catch (Exception e) {
            ApiServer.sendResponse(exchange, 500, ApiServer.buildResponse("ERROR", e.getMessage(), null));
        }
    }
}

class AuctionsHandler implements HttpHandler {
    private AuctionService auctionService = new AuctionService(new AuctionDAOImpl(), new ItemDAOImpl());

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
            List<Auction> auctions = auctionService.getAuctionsByStatus(Auction.AuctionStatus.RUNNING);
            JsonElement dataArr = Server.getGson().toJsonTree(auctions);
            ApiServer.sendResponse(exchange, 200, ApiServer.buildResponse("SUCCESS", "Lay danh sach phien dau gia thanh cong!", dataArr));
        } catch (BusinessException e) {
            ApiServer.sendResponse(exchange, 400, ApiServer.buildResponse("ERROR", e.getMessage(), null));
        } catch (Exception e) {
            ApiServer.sendResponse(exchange, 500, ApiServer.buildResponse("ERROR", e.getMessage(), null));
        }
    }
}