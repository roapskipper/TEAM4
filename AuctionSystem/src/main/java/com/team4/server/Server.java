package com.team4.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
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
                new Thread(handler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void broadcast(String message, ClientHandler excludeUser) {
        for (ClientHandler handler : clientHandlers) {
            if (handler != excludeUser) {
                handler.sendMessage(message);
            }
        }
    }

    public static void removeClient(ClientHandler handler) {
        clientHandlers.remove(handler);
        System.out.println("Mot client da ngat ket noi. So luong hien tai: " + clientHandlers.size());
    }
}

class ClientHandler implements Runnable {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private DatabaseHelper dbHelper;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.dbHelper = new DatabaseHelper();
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
                    try {
                        if (command.equals("BID") && parts.length == 4) {
                            int itemId = Integer.parseInt(parts[1]);
                            int bidderId = Integer.parseInt(parts[2]);
                            double amount = Double.parseDouble(parts[3]);

                            dbHelper.insertBid(itemId, bidderId, amount);
                            Server.broadcast("BID_UPDATE," + itemId + "," + bidderId + "," + amount, this);
                        }
                    } catch (Exception e) {
                        System.out.println("Loi xu ly database: " + e.getMessage());
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