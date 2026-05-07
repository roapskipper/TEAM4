package com.team4.handler;

import com.google.gson.JsonElement;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.team4.dao.impl.ItemDAOImpl;
import com.team4.model.Item;
import com.team4.server.ApiServer;
import com.team4.server.Server;

import java.io.IOException;
import java.util.List;

public class ItemsHandler implements HttpHandler {
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