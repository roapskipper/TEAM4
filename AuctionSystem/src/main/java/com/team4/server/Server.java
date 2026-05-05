package com.team4.server;

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
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static final int PORT = 18367;
    private static Set<ClientHandler> clientHandlers = ConcurrentHashMap.newKeySet();

    private static final AuctionService auctionService = new AuctionService(
            new AuctionDAOImpl(),
            new ItemDAOImpl()
    );

    private static final UserService userService = new UserService(new UserDAOImpl());

    public static AuctionService getAuctionService() {
        return auctionService;
    }

    public static UserService getUserService() {
        return userService;
    }

    public static void main(String[] args) {
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

    public static void addObserver(ClientHandler handler) {
        clientHandlers.add(handler);
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
        String data = "{"
                + "\"auctionId\":\"" + auction.getId() + "\","
                + "\"itemId\":\"" + auction.getItemId() + "\","
                + "\"bidderId\":\"" + transaction.getBidderId() + "\","
                + "\"amount\":" + transaction.getBidAmount()
                + "}";
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

    private void handleRequest(String json) {
        String action = extractValue(json, "action");

        if (action.equals("BID")) {
            String data = extractBlock(json, "data");
            String auctionId = extractValue(data, "auctionId");
            String bidderId = extractValue(data, "bidderId");
            double amount = Double.parseDouble(extractValue(data, "amount"));

            try {
                Auction auction = Server.getAuctionService().getAuctionById(auctionId);

                // TODO: Cắm BiddingService vào đây khi hoàn thiện
                // BiddingService.placeBid(bidderId, auctionId, amount);

                // Tạm thời kiểm tra cơ bản
                if (auction.getStatus() != Auction.AuctionStatus.RUNNING) {
                    sendMessage(buildResponse("ERROR", "Phien dau gia khong con hoat dong", null, "null"));
                    return;
                }

                sendMessage(buildResponse("ERROR", "BiddingService chua hoan thien", null, "null"));

            } catch (BusinessException e) {
                sendMessage(buildResponse("ERROR", e.getMessage(), null, "null"));
            }

        } else if (action.equals("GET_AUCTIONS")) {
            try {
                List<Auction> auctions = Server.getAuctionService().getAuctionsByStatus(Auction.AuctionStatus.RUNNING);
                String dataArr = "[";
                boolean first = true;
                for (int i = 0; i < auctions.size(); i++) {
                    if (!first) dataArr = dataArr + ",";
                    Auction a = auctions.get(i);
                    dataArr = dataArr + "{"
                            + "\"id\":\"" + a.getId() + "\","
                            + "\"itemId\":\"" + a.getItemId() + "\","
                            + "\"currentPrice\":" + a.getCurrentPrice() + ","
                            + "\"endTime\":\"" + a.getEndTime() + "\","
                            + "\"status\":\"" + a.getStatus() + "\""
                            + "}";
                    first = false;
                }
                dataArr = dataArr + "]";
                sendMessage(buildResponse("SUCCESS", "Lay danh sach phien dau gia thanh cong", null, dataArr));
            } catch (BusinessException e) {
                sendMessage(buildResponse("ERROR", e.getMessage(), null, "null"));
            }
        }
    }

    private String buildResponse(String status, String message, String action, String data) {
        String actionField = action != null ? "\"action\":\"" + action + "\"," : "";
        return "{" + actionField
                + "\"status\":\"" + status + "\","
                + "\"message\":\"" + message + "\","
                + "\"data\":" + data + "}";
    }

    private String extractValue(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start == -1) {
            search = "\"" + key + "\":";
            start = json.indexOf(search);
            if (start == -1) return "";
            start = start + search.length();
            int end = json.indexOf(",", start);
            if (end == -1) end = json.indexOf("}", start);
            return json.substring(start, end).trim();
        }
        start = start + search.length();
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }

    private String extractBlock(String json, String key) {
        String search = "\"" + key + "\":{";
        int start = json.indexOf(search);
        if (start == -1) return "{}";
        start = start + search.length() - 1;
        int depth = 0;
        int end = start;
        while (end < json.length()) {
            if (json.charAt(end) == '{') depth++;
            else if (json.charAt(end) == '}') {
                depth--;
                if (depth == 0) break;
            }
            end++;
        }
        return json.substring(start, end + 1);
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