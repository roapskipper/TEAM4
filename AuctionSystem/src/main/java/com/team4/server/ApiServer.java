package com.team4.server;

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

    public static String buildSuccess(String message, String data) {
        return "{\"status\":\"SUCCESS\","
                + "\"message\":\"" + message + "\","
                + "\"data\":" + data + "}";
    }

    public static String buildError(String message) {
        return "{\"status\":\"ERROR\","
                + "\"message\":\"" + message + "\","
                + "\"data\":null}";
    }

    public static String parseParam(String body, String key) {
        String[] params = body.split("&");
        for (int i = 0; i < params.length; i++) {
            String[] kv = params[i].split("=", 2);
            if (kv.length == 2 && kv[0].equals(key)) {
                return kv[1];
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
            ApiServer.sendResponse(exchange, 400, ApiServer.buildError("Thieu username hoac password"));
            return;
        }

        try {
            User user = authService.login(username, password);
            String data = "{"
                    + "\"userId\":\"" + user.getId() + "\","
                    + "\"role\":\"" + user.getRole() + "\","
                    + "\"fullName\":\"" + user.getFullName() + "\""
                    + "}";
            ApiServer.sendResponse(exchange, 200, ApiServer.buildSuccess("Dang nhap thanh cong!", data));
        } catch (BusinessException e) {
            ApiServer.sendResponse(exchange, 401, ApiServer.buildError(e.getMessage()));
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
            ApiServer.sendResponse(exchange, 400, ApiServer.buildError("Thieu username hoac password"));
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
            ApiServer.sendResponse(exchange, 200, ApiServer.buildSuccess("Dang ky Bidder thanh cong!", "null"));
        } catch (BusinessException e) {
            ApiServer.sendResponse(exchange, 400, ApiServer.buildError(e.getMessage()));
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
            ApiServer.sendResponse(exchange, 400, ApiServer.buildError("Thieu username hoac password"));
            return;
        }

        try {
            authService.registerSeller(
                    username, password,
                    fullName != null ? fullName : "",
                    email != null ? email : "",
                    storeName != null ? storeName : ""
            );
            ApiServer.sendResponse(exchange, 200, ApiServer.buildSuccess("Dang ky Seller thanh cong!", "null"));
        } catch (BusinessException e) {
            ApiServer.sendResponse(exchange, 400, ApiServer.buildError(e.getMessage()));
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

        List<Item> items = itemDAO.findAll();
        StringBuilder dataBuilder = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i);
            String name = item.getName().replace("\\", "\\\\").replace("\"", "\\\"");
            String category = item.getCategory().name();

            dataBuilder.append("{")
                    .append("\"id\":\"").append(item.getId()).append("\",")
                    .append("\"name\":\"").append(name).append("\",")
                    .append("\"category\":\"").append(category).append("\",")
                    .append("\"startingPrice\":").append(item.getStartingPrice())
                    .append("}");

            if (i < items.size() - 1) {
                dataBuilder.append(",");
            }
        }
        dataBuilder.append("]");

        ApiServer.sendResponse(exchange, 200, ApiServer.buildSuccess("Lay danh sach items thanh cong!", dataBuilder.toString()));
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

        List<Auction> auctions = auctionService.getAuctionsByStatus(Auction.AuctionStatus.RUNNING);
        StringBuilder dataBuilder = new StringBuilder("[");
        for (int i = 0; i < auctions.size(); i++) {
            Auction a = auctions.get(i);
            dataBuilder.append("{")
                    .append("\"id\":\"").append(a.getId()).append("\",")
                    .append("\"itemId\":\"").append(a.getItemId()).append("\",")
                    .append("\"currentPrice\":").append(a.getCurrentPrice()).append(",")
                    .append("\"endTime\":\"").append(a.getEndTime()).append("\",")
                    .append("\"status\":\"").append(a.getStatus()).append("\"")
                    .append("}");

            if (i < auctions.size() - 1) {
                dataBuilder.append(",");
            }
        }
        dataBuilder.append("]");

        ApiServer.sendResponse(exchange, 200, ApiServer.buildSuccess("Lay danh sach phien dau gia thanh cong!", dataBuilder.toString()));
    }
}