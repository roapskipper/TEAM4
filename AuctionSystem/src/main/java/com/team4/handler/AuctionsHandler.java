package com.team4.handler;

import com.google.gson.JsonElement;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.team4.dao.impl.AuctionDAOImpl;
import com.team4.dao.impl.ItemDAOImpl;
import com.team4.model.Auction;
import com.team4.server.ApiServer;
import com.team4.server.Server;
import com.team4.service.AuctionService;
import com.team4.util.BusinessException;

import java.io.IOException;
import java.util.List;

public class AuctionsHandler implements HttpHandler {
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