package com.team4.server;

import com.google.gson.*;
import com.team4.dao.impl.AuctionDAOImpl;
import com.team4.dao.impl.ItemDAOImpl;
import com.team4.dao.impl.UserDAOImpl;
import com.team4.model.Auction;
import com.team4.model.BidTransaction;
import com.team4.observer.BidObserver;
import com.team4.service.AuctionService;
import com.team4.service.UserService;
import com.team4.util.BusinessException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static final int PORT = 18368;
    private static Set<ClientHandler> clientHandlers = ConcurrentHashMap.newKeySet();

    private static final AuctionService auctionService = new AuctionService(
            new AuctionDAOImpl(),
            new ItemDAOImpl()
    );

    private static final UserService userService = new UserService(new UserDAOImpl());

    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();

    private static class LocalDateTimeAdapter implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
        private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        @Override
        public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(formatter.format(src));
        }
        @Override
        public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return LocalDateTime.parse(json.getAsString(), formatter);
        }
    }

    public static AuctionService getAuctionService() {
        return auctionService;
    }

    public static UserService getUserService() {
        return userService;
    }

    public static Gson getGson() {
        return gson;
    }

    public static void main(String[] args) {
        com.team4.db.DatabaseManager.initialize();
        new ApiServer().start();

        System.out.println("Server dang khoi dong tren port " + PORT + "...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Co ket noi moi: " + socket.getInetAddress());

                ClientHandler handler = new ClientHandler(socket);
                clientHandlers.add(handler);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void removeClient(ClientHandler handler) {
        clientHandlers.remove(handler);
        System.out.println("Mot client da ngat ket noi. So luong hien tai: " + clientHandlers.size());
    }

    public static void broadcast(String message, ClientHandler excludeUser) {
        for (ClientHandler handler : clientHandlers) {
            if (handler != excludeUser) {
                handler.sendMessage(message);
            }
        }
    }
}

class ClientHandler implements Runnable, BidObserver {
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
                String bidderId = data.get("bidderId").getAsString();
                double amount = data.get("amount").getAsDouble();

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
                    JsonElement dataArr = Server.getGson().toJsonTree(auctions);
                    sendMessage(buildResponse("SUCCESS", "Lay danh sach phien dau gia thanh cong", "AUCTIONS_LIST", dataArr));
                } catch (BusinessException e) {
                    sendMessage(buildResponse("ERROR", e.getMessage(), null, null));
                }
            }
        } catch (JsonSyntaxException | IllegalStateException e) {
            sendMessage(buildResponse("ERROR", "Sai dinh dang JSON", null, null));
        }
    }

    private String buildResponse(String status, String message, String action, JsonElement data) {
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