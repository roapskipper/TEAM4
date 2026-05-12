package com.team4.server;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.team4.handler.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import com.sun.net.httpserver.HttpServer;

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
        for (String param : params) {
            String[] kv = param.split("=", 2);
            if (kv.length == 2 && kv[0].equals(key)) {
                return java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }
}