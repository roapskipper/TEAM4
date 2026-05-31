package com.team4.handler;

import com.google.gson.JsonElement;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.team4.dao.impl.AuctionDAOImpl;
import com.team4.dao.impl.ItemDAOImpl;
import com.team4.dao.impl.UserDAOImpl;
import com.team4.dto.item.ItemResponseDTO;
import com.team4.factory.ItemRequest;
import com.team4.mapper.ItemMapper;
import com.team4.model.Art;
import com.team4.model.Collectible;
import com.team4.model.Electronics;
import com.team4.model.Fashion;
import com.team4.model.Item;
import com.team4.model.Vehicle;
import com.team4.server.ApiServer;
import com.team4.server.Server;
import com.team4.service.ItemService;
import com.team4.util.BusinessException;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class ItemsHandler implements HttpHandler {
    private static final String BASE_PATH = "/api/items";

    private final ItemDAOImpl itemDAO = new ItemDAOImpl();
    private final ItemService itemService =
            new ItemService(itemDAO, new UserDAOImpl(), new AuctionDAOImpl());

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        if ("OPTIONS".equals(method)) {
            ApiServer.sendResponse(exchange, 204, "");
            return;
        }

        try {
            String itemId = readItemId(exchange.getRequestURI().getPath());
            if ("GET".equals(method) && itemId == null) {
                handleList(exchange);
                return;
            }
            if ("POST".equals(method) && itemId == null) {
                handleCreate(exchange);
                return;
            }
            if ("PUT".equals(method) && itemId != null) {
                handleUpdate(exchange, itemId);
                return;
            }
            if ("DELETE".equals(method) && itemId != null) {
                handleDelete(exchange, itemId);
                return;
            }

            exchange.sendResponseHeaders(405, -1);
        } catch (BusinessException | IllegalArgumentException e) {
            ApiServer.sendResponse(exchange, 400,
                    ApiServer.buildResponse("ERROR", e.getMessage(), null));
        } catch (Exception e) {
            ApiServer.sendResponse(exchange, 500,
                    ApiServer.buildResponse("ERROR", e.getMessage(), null));
        }
    }

    private void handleList(HttpExchange exchange) throws IOException {
        String category = queryParam(exchange.getRequestURI().getRawQuery(), "category");
        JsonElement dataArr;
        if (category == null || category.isBlank()) {
            List<ItemResponseDTO> dtos = itemDAO.findAll().stream()
                    .map(ItemMapper::toItemResponseDTO)
                    .collect(Collectors.toList());
            dataArr = Server.getGson().toJsonTree(dtos);
        } else {
            List<ItemResponseDTO> dtos =
                    itemService.getItemsByCategory(category.trim().toUpperCase(Locale.ROOT));
            dataArr = Server.getGson().toJsonTree(dtos);
        }
        ApiServer.sendResponse(exchange, 200,
                ApiServer.buildResponse("SUCCESS", "Items loaded successfully", dataArr));
    }

    private void handleCreate(HttpExchange exchange) throws IOException {
        String body = readBody(exchange);
        String sellerId = param(body, "sellerId");
        ItemRequest request = buildItemRequest(sellerId, body);
        Item item = itemService.createItem(sellerId, request);
        ApiServer.sendResponse(exchange, 201,
                ApiServer.buildResponse("SUCCESS", "Item created successfully",
                        Server.getGson().toJsonTree(ItemMapper.toItemResponseDTO(item))));
    }

    private void handleUpdate(HttpExchange exchange, String itemId) throws IOException {
        String body = readBody(exchange);
        ItemResponseDTO dto = itemService.updateItem(
                param(body, "sellerId"),
                itemId,
                param(body, "name"),
                param(body, "description"),
                parseBigDecimal(param(body, "startingPrice")),
                parseEnum(Item.ItemCategory.class, param(body, "category")));
        ApiServer.sendResponse(exchange, 200,
                ApiServer.buildResponse("SUCCESS", "Item updated successfully",
                        Server.getGson().toJsonTree(dto)));
    }

    private void handleDelete(HttpExchange exchange, String itemId) throws IOException {
        String rawQuery = exchange.getRequestURI().getRawQuery();
        String sellerId = queryParam(rawQuery, "sellerId");
        if (sellerId == null || sellerId.isBlank()) {
            sellerId = param(readBody(exchange), "sellerId");
        }
        itemService.deleteItem(itemId, sellerId);
        ApiServer.sendResponse(exchange, 200,
                ApiServer.buildResponse("SUCCESS", "Item deleted successfully", null));
    }

    private ItemRequest buildItemRequest(String sellerId, String body) {
        ItemRequest request = new ItemRequest();
        request.setOwnerId(sellerId);
        request.setName(param(body, "name"));
        request.setDescription(param(body, "description"));
        request.setStartingPrice(parseBigDecimal(param(body, "startingPrice")));
        request.setCategory(parseEnum(Item.ItemCategory.class, param(body, "category")));

        String startTimeStr = param(body, "startTime");
        if (startTimeStr != null && !startTimeStr.isBlank()) {
            request.setStartTime(java.time.LocalDateTime.parse(startTimeStr.trim()));
        }
        String endTimeStr = param(body, "endTime");
        if (endTimeStr != null && !endTimeStr.isBlank()) {
            request.setEndTime(java.time.LocalDateTime.parse(endTimeStr.trim()));
        }

        Item.ItemCategory category = request.getCategory();
        if (category == null) {
            return request;
        }

        switch (category) {
            case ART -> {
                request.setArtist(param(body, "artist"));
                request.setCreationYear(parseInt(param(body, "creationYear")));
                request.setMedium(parseEnum(Art.Medium.class, param(body, "medium")));
                request.setDimensions(param(body, "dimensions"));
            }
            case COLLECTIBLE -> {
                request.setYearOfOrigin(parseInt(param(body, "yearOfOrigin")));
                request.setRarityLevel(parseEnum(Collectible.RarityLevel.class, param(body, "rarityLevel")));
                request.setConditionGrade(parseEnum(Collectible.ConditionGrade.class, param(body, "conditionGrade")));
                request.setHasCertificate(parseBoolean(param(body, "hasCertificate")));
                request.setOrigin(param(body, "origin"));
            }
            case ELECTRONICS -> {
                request.setBrand(param(body, "brand"));
                request.setModel(param(body, "model"));
                request.setItemCondition(parseEnum(Electronics.ConditionGrade.class, param(body, "itemCondition")));
                request.setWarrantyMonths(parseInt(param(body, "warrantyMonths")));
                request.setFullyFunctional(parseBoolean(param(body, "fullyFunctional")));
            }
            case FASHION -> {
                request.setBrand(param(body, "brand"));
                request.setSize(parseEnum(Fashion.Size.class, param(body, "size")));
                request.setMaterial(param(body, "material"));
                request.setColor(param(body, "color"));
                request.setGender(parseEnum(Fashion.Gender.class, param(body, "gender")));
                request.setCondition(parseEnum(Fashion.ConditionGrade.class, param(body, "condition")));
                request.setAuthentic(parseBoolean(param(body, "authentic")));
            }
            case VEHICLE -> {
                request.setBrand(param(body, "brand"));
                request.setModel(param(body, "model"));
                request.setManufacturingYear(parseInt(param(body, "manufacturingYear")));
                request.setOdo(parseInt(param(body, "odo")));
                request.setEngineType(parseEnum(Vehicle.EngineType.class, param(body, "engineType")));
                request.setColor(param(body, "color"));
                request.setHasLegalPapers(parseBoolean(param(body, "hasLegalPapers")));
                request.setTransmission(parseEnum(Vehicle.Transmission.class, param(body, "transmission")));
            }
            default -> { }
        }
        return request;
    }

    private String readItemId(String path) {
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

    private String readBody(HttpExchange exchange) throws IOException {
        try (InputStream input = exchange.getRequestBody()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String param(String body, String key) {
        if (body == null || body.isBlank()) {
            return null;
        }
        return ApiServer.parseParam(body, key);
    }

    private String queryParam(String rawQuery, String key) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return null;
        }
        String[] pairs = rawQuery.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && key.equals(kv[0])) {
                return URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private BigDecimal parseBigDecimal(String value) {
        return value == null || value.isBlank() ? null : new BigDecimal(value.trim());
    }

    private int parseInt(String value) {
        return value == null || value.isBlank() ? 0 : Integer.parseInt(value.trim());
    }

    private boolean parseBoolean(String value) {
        return value != null && Boolean.parseBoolean(value.trim());
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumType, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);
        return Enum.valueOf(enumType, normalized);
    }
}
