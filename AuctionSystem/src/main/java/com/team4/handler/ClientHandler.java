package com.team4.handler;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.team4.dao.impl.AuctionDAOImpl;
import com.team4.dao.impl.AutoBiddingDAOImpl;
import com.team4.dao.impl.BidTransactionDAOImpl;
import com.team4.dao.impl.UserDAOImpl;
import com.team4.model.Auction;
import com.team4.model.BidTransaction;
import com.team4.network.NetworkMessage;
import com.team4.observer.BidObserver;
import com.team4.server.Server;
import com.team4.service.BiddingService;
import com.team4.util.BusinessException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.Socket;
import java.util.List;

public class ClientHandler implements Runnable, BidObserver {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String userId;

    public String getUserId() {
        return userId;
    }

    private static final BiddingService biddingService = new BiddingService(
            new AuctionDAOImpl(),
            new BidTransactionDAOImpl(),
            new UserDAOImpl(),
            new AutoBiddingDAOImpl()
    );

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
        if (auction.getEndTime() != null) {
            data.addProperty("endTime", auction.getEndTime().toString());
        }

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
                try {
                    NetworkMessage netMsg = Server.getGson().fromJson(message, NetworkMessage.class);
                    if (netMsg != null && netMsg.getCommand() != null) {
                        handleRequest(netMsg);
                    }
                } catch (JsonSyntaxException e) {
                    System.out.println("Loi parse JSON tu client: " + e.getMessage());
                }
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

    private void handleRequest(NetworkMessage netMsg) {
        String command = netMsg.getCommand();
        switch (command) {
            case "LOGIN":
                handleLogin(netMsg.getData());
                break;
            case "BID":
                handleBid(netMsg.getData());
                break;
            case "UPDATE":
                // TODO: Xử lý cập nhật
                break;
            case "GET_AUCTIONS":
                handleGetAuctions();
                break;
            default:
                System.out.println("Unknown command: " + command);
                break;
        }
    }

    private void handleLogin(String dataJson) {
        if (dataJson == null || dataJson.isEmpty()) return;
        try {
            JsonObject data = JsonParser.parseString(dataJson).getAsJsonObject();
            this.userId = data.get("userId").getAsString();
            Server.registerUser(this.userId, this);
        } catch (JsonSyntaxException | IllegalStateException e) {
            System.out.println("Loi parse JSON trong handleLogin: " + e.getMessage());
        }
    }

    private void handleBid(String dataJson) {
        if (dataJson == null || dataJson.isEmpty()) return;
        try {
            JsonObject data = JsonParser.parseString(dataJson).getAsJsonObject();
            String auctionId = data.get("auctionId").getAsString();
            String bidderId  = data.get("bidderId").getAsString();
            double amount    = data.get("amount").getAsDouble();

            try {
                Auction auction = Server.getAuctionService().getAuctionById(auctionId);

                if (auction.getStatus() != Auction.AuctionStatus.RUNNING) {
                    sendMessage(buildResponse("ERROR", "Phien dau gia khong con hoat dong", "BID_FAILED", null));
                    return;
                }

                // Gọi BiddingService để xử lý proxy bidding
                biddingService.placeBid(auctionId, bidderId, BigDecimal.valueOf(amount));

                // Lấy lại auction sau khi bid để có giá và endTime mới nhất
                Auction updatedAuction = Server.getAuctionService().getAuctionById(auctionId);

                // Broadcast kết quả mới cho tất cả client
                JsonObject broadcastData = new JsonObject();
                broadcastData.addProperty("auctionId", auctionId);
                broadcastData.addProperty("currentPrice", updatedAuction.getCurrentPrice().doubleValue());
                broadcastData.addProperty("currentHighestBidderId", updatedAuction.getCurrentHighestBidderId());
                if (updatedAuction.getEndTime() != null) {
                    broadcastData.addProperty("endTime", updatedAuction.getEndTime().toString());
                }
                Server.broadcast(buildResponse("SUCCESS", "Co gia moi!", "BID_UPDATE", broadcastData), this);

                sendMessage(buildResponse("SUCCESS", "Dat gia thanh cong!", "BID_SUCCESS", broadcastData));

            } catch (BusinessException e) {
                sendMessage(buildResponse("ERROR", e.getMessage(), "BID_FAILED", null));
            }
        } catch (JsonSyntaxException | IllegalStateException e) {
            sendMessage(buildResponse("ERROR", "Sai dinh dang JSON", null, null));
        }
    }

    private void handleGetAuctions() {
        try {
            List<Auction> auctions = Server.getAuctionService().getAuctionsByStatus(Auction.AuctionStatus.RUNNING);
            sendMessage(buildResponse("SUCCESS", "Lay danh sach phien dau gia thanh cong", "AUCTIONS_LIST",
                    Server.getGson().toJsonTree(auctions)));
        } catch (BusinessException e) {
            sendMessage(buildResponse("ERROR", e.getMessage(), null, null));
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

    public void sendResponse(NetworkMessage msg) {
        if (msg != null) {
            sendMessage(Server.getGson().toJson(msg));
        }
    }

    public void forceLogout() {
        sendMessage(buildResponse("ERROR", "Your account has been signed in on another device.", "FORCE_LOGOUT", null));
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}