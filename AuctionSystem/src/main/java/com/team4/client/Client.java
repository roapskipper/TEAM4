package com.team4.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private static final String SERVER_ADDRESS = "127.0.0.1";
    private static final int SERVER_PORT = 18368;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private final Gson gson;

    public Client() {
        this.gson = new Gson();
    }

    public boolean connect() {
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println("Da ket noi toi server " + SERVER_ADDRESS + ":" + SERVER_PORT);
            return true;
        } catch (IOException e) {
            System.out.println("Khong the ket noi toi server: " + e.getMessage());
            return false;
        }
    }

    public void startListening(MessageListener listener) {
        Thread readThread = new Thread(() -> {
            try {
                String serverMessage;
                while ((serverMessage = in.readLine()) != null) {
                    try {
                        JsonObject jsonResponse = JsonParser.parseString(serverMessage).getAsJsonObject();
                        String action = jsonResponse.has("action") ? jsonResponse.get("action").getAsString() : "";

                        if ("BID_UPDATE".equals(action)) {
                            JsonObject data = jsonResponse.getAsJsonObject("data");
                            String itemId = data.get("itemId").getAsString();
                            String bidderId = data.get("bidderId").getAsString();
                            String amount = data.get("amount").getAsString();
                            System.out.println("[Cap nhat gia] Item: " + itemId + " | Nguoi dat: " + bidderId + " | Gia moi: " + amount);
                        } else if ("BID_FAILED".equals(action)) {
                            JsonObject data = jsonResponse.getAsJsonObject("data");
                            String itemId = data.has("itemId") ? data.get("itemId").getAsString() : "Unknown";
                            String reason = jsonResponse.has("message") ? jsonResponse.get("message").getAsString() : "Loi khong xac dinh";
                            System.out.println("[Dat gia that bai] Item: " + itemId + " | Ly do: " + reason);
                        } else {
                            System.out.println("[Server JSON]: " + serverMessage);
                        }
                    } catch (Exception ex) {
                        System.out.println("[Server Raw]: " + serverMessage);
                    }

                    if (listener != null) {
                        listener.onMessage(serverMessage);
                    }
                }
            } catch (IOException e) {
                System.out.println("Da ngat ket noi khoi server.");
            }
        });
        readThread.setDaemon(true);
        readThread.start();
    }

    public void sendBid(int itemId, int bidderId, double amount) {
        JsonObject request = new JsonObject();
        request.addProperty("action", "BID");

        JsonObject data = new JsonObject();
        data.addProperty("itemId", itemId);
        data.addProperty("bidderId", bidderId);
        data.addProperty("amount", amount);

        request.add("data", data);

        sendMessage(request.toString());
    }

    public void sendMessage(String message) {
        if (out != null && !socket.isClosed()) {
            out.println(message);
            if (out.checkError()) {
                System.out.println("Loi khi gui tin nhan.");
            }
        }
    }

    public void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isConnected() {
        return socket != null && !socket.isClosed() && socket.isConnected();
    }

    public interface MessageListener {
        void onMessage(String message);
    }

    public static void main(String[] args) {
        Client client = new Client();

        if (!client.connect()) return;

        client.startListening(message -> {});

        Scanner scanner = new Scanner(System.in);
        System.out.println("Nhap lenh (BID <itemId> <bidderId> <amount> hoac exit):");

        while (true) {
            String input = scanner.nextLine();
            if (input.equalsIgnoreCase("exit")) {
                break;
            }

            String[] parts = input.split(" ");
            if (parts.length == 4 && parts[0].equalsIgnoreCase("BID")) {
                try {
                    int itemId = Integer.parseInt(parts[1]);
                    int bidderId = Integer.parseInt(parts[2]);
                    double amount = Double.parseDouble(parts[3]);
                    client.sendBid(itemId, bidderId, amount);
                } catch (NumberFormatException e) {
                    System.out.println("Sai dinh dang. Vi du: BID 1 2 500000");
                }
            } else {
                System.out.println("Lenh khong hop le. Vi du: BID 1 2 500000");
            }
        }

        client.disconnect();
        scanner.close();
    }
}