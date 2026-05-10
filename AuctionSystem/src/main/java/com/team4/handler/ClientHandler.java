package com.team4.handler;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.team4.model.Auction;
import com.team4.model.BidTransaction;
import com.team4.observer.BidObserver;
import com.team4.server.Server;
import com.team4.util.BusinessException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

public class ClientHandler implements Runnable, BidObserver {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void updateNewBid(Auction auction, BidTransaction transaction) {
        JsonObject data = new JsonObject();
        data.addProperty("auctionId", auction.getId());
        data.addProperty("itemId", auction.getItemId());
        data.addProperty("bidderId", transaction.getBidderId());
        data.addProperty("amount", transaction.getBidAmount());

        sendMessage(buildResponse("SUCCESS", "Co gia moi!", "BID_UPDATE", data));
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("Nhan duoc tu Socket: " + message);
                handleRequest(message);
            }
        } catch (IOException e) {
            System.out.println("Loi ket noi tu client: " + e.getMessage());
        } finally {
            Server.removeClient(this);
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleRequest(String jsonStr) {
        try {
            JsonObject request = JsonParser.parseString(jsonStr).getAsJsonObject();
            String action = request.has("action") ? request.get("action").getAsString() : "";

            if ("BID".equals(action)) {
                JsonObject data = request.getAsJsonObject("data");
                String auctionId = data.get("auctionId").getAsString();
                String bidderId  = data.get("bidderId").getAsString();
                double amount    = data.get("amount").getAsDouble();

                try {
                    Auction auction = Server.getAuctionService().getAuctionById(auctionId);

                    if (auction.getStatus() != Auction.AuctionStatus.RUNNING) {
                        sendMessage(buildResponse("ERROR", "Phien dau gia khong con hoat dong", "BID_FAILED", null));
                        return;
                    }

                    sendMessage(buildResponse("ERROR", "BiddingService chua hoan thien", "BID_FAILED", null));
                } catch (BusinessException e) {
                    sendMessage(buildResponse("ERROR", e.getMessage(), "BID_FAILED", null));
                }

            } else if ("GET_AUCTIONS".equals(action)) {
                try {
                    List<Auction> auctions = Server.getAuctionService().getAuctionsByStatus(Auction.AuctionStatus.RUNNING);
                    sendMessage(buildResponse("SUCCESS", "Lay danh sach phien dau gia thanh cong", "AUCTIONS_LIST",
                            Server.getGson().toJsonTree(auctions)));
                } catch (BusinessException e) {
                    sendMessage(buildResponse("ERROR", e.getMessage(), null, null));
                }
            }
        } catch (JsonSyntaxException | IllegalStateException e) {
            sendMessage(buildResponse("ERROR", "Sai dinh dang JSON", null, null));
        }
    }

    private String buildResponse(String status, String message, String action, com.google.gson.JsonElement data) {
        JsonObject response = new JsonObject();
        if (action != null) response.addProperty("action", action);
        response.addProperty("status", status);
        response.addProperty("message", message);
        if (data != null) response.add("data", data);
        return Server.getGson().toJson(response);
    }

    public void sendMessage(String message) {
        if (out != null && !socket.isClosed()) {
            out.println(message);
            if (out.checkError()) {
                Server.removeClient(this);
            }
        }
    }
}