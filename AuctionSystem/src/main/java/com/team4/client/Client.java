package com.team4.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.team4.server.Server; // Nếu dùng chung Gson cấu hình sẵn
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client {
    private static final String SERVER_ADDRESS = "127.0.0.1";
    private static final int SERVER_PORT = 18368;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    public boolean connect() {
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private MessageListener currentListener;
    private Thread readThread;

    public void startListening(MessageListener listener) {
        this.currentListener = listener;
        if (readThread != null && readThread.isAlive()) return; // Chỉ start 1 thread duy nhất

        readThread = new Thread(() -> {
            try {
                String serverMessage;
                while ((serverMessage = in.readLine()) != null) {
                    if (serverMessage.contains("\"action\":\"FORCE_LOGOUT\"")) {
                        javafx.application.Platform.runLater(() -> {
                            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                            alert.setTitle("Cảnh báo");
                            alert.setHeaderText("Đăng xuất bắt buộc");
                            alert.setContentText("Tài khoản của bạn vừa đăng nhập từ thiết bị khác. Ứng dụng sẽ thoát.");
                            alert.showAndWait();
                            javafx.application.Platform.exit();
                            System.exit(0);
                        });
                        break;
                    }
                    if (currentListener != null) {
                        currentListener.onMessage(serverMessage);
                    }
                }
            } catch (IOException e) {
                System.out.println("Disconnected.");
            }
        });
        readThread.setDaemon(true);
        readThread.start();
    }

    public void sendBid(String auctionId, String bidderId, double amount) {
        JsonObject request = new JsonObject();
        request.addProperty("action", "BID");

        JsonObject data = new JsonObject();
        data.addProperty("auctionId", auctionId);
        data.addProperty("bidderId", bidderId);
        data.addProperty("amount", amount);

        request.add("data", data);
        sendMessage(request.toString());
    }

    public void sendLogin(String userId) {
        JsonObject request = new JsonObject();
        request.addProperty("action", "LOGIN");
        JsonObject data = new JsonObject();
        data.addProperty("userId", userId);
        request.add("data", data);
        sendMessage(request.toString());
    }

    private void sendMessage(String message) {
        if (out != null && !socket.isClosed()) {
            out.println(message);
        }
    }

    public void disconnect() {
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public interface MessageListener {
        void onMessage(String message);
    }
}