package com.team4.handler;

import com.google.gson.JsonElement;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.team4.dao.impl.ItemDAOImpl;
import com.team4.dto.item.ItemResponseDTO;
import com.team4.mapper.ItemMapper;
import com.team4.model.Item;
import com.team4.server.ApiServer;
import com.team4.server.Server;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

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
            // Dùng ItemMapper → ItemResponseDTO thay vì toJsonTree trực tiếp trên model
            List<ItemResponseDTO> dtos = items.stream()
                    .map(ItemMapper::toItemResponseDTO)
                    .collect(Collectors.toList());
            JsonElement dataArr = Server.getGson().toJsonTree(dtos);
            ApiServer.sendResponse(exchange, 200, ApiServer.buildResponse("SUCCESS", "Lay danh sach items thanh cong!", dataArr));
        } catch (Exception e) {
            ApiServer.sendResponse(exchange, 500, ApiServer.buildResponse("ERROR", e.getMessage(), null));
        }
    }
}