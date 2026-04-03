package com.team4.server;

import com.team4.model.Auction;
import com.team4.observer.BidObserver;
import com.team4.model.BidTransaction;
import com.team4.service.AuctionManager;

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

    public static void main(String[] args) {
        new ApiServer().start();

        System.out.println("Server dang khoi dong tren port " + PORT + "...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Co ket noi moi: " + socket.getInetAddress());

                ClientHandler handler = new ClientHandler(socket);
                clientHandlers.add(handler);
                AuctionManager.getInstance().registerObserver(handler);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void removeClient(ClientHandler handler) {
        clientHandlers.remove(handler);
        AuctionManager.getInstance().removeObserver(handler);
        System.out.println("Mot client da ngat ket noi. So luong hien tai: " + clientHandlers.size());
    }
}

class ClientHandler implements Runnable, BidObserver {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private DatabaseHelper dbHelper;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.dbHelper = new DatabaseHelper();
    }

    @Override
    public void updateNewBid(Auction auction, BidTransaction transaction) {
        String message = "BID_UPDATE," + auction.getItemId() + "," + transaction.getBidderId() + "," + transaction.getBidAmount();
        sendMessage(message);
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("Nhan duoc tu Socket: " + message);

                String[] parts = message.split(",");
                if (parts.length > 0) {
                    String command = parts[0];
                    if (command.equals("BID") && parts.length == 4) {
                        int itemId = Integer.parseInt(parts[1]);
                        int bidderId = Integer.parseInt(parts[2]);
                        double amount = Double.parseDouble(parts[3]);

                        Auction auction = null;
                        List<Auction> auctions = AuctionManager.getInstance().getActiveAuctions();
                        for (int i = 0; i < auctions.size(); i++) {
                            if (auctions.get(i).getItemId().equals(String.valueOf(itemId))) {
                                auction = auctions.get(i);
                                break;
                            }
                        }

                        if (auction != null) {
                            boolean success = AuctionManager.getInstance().placeBid(auction, String.valueOf(bidderId), amount);
                            if (success) {
                                dbHelper.insertBid(itemId, bidderId, amount);
                            } else {
                                sendMessage("BID_FAILED," + itemId + ",Gia qua thap hoac phien da ket thuc");
                            }
                        } else {
                            sendMessage("BID_FAILED," + itemId + ",Khong tim thay phien dau gia");
                        }
                    }
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

    public void sendMessage(String message) {
        if (out != null && !socket.isClosed()) {
            out.println(message);
            if (out.checkError()) {
                Server.removeClient(this);
            }
        }
    }
}