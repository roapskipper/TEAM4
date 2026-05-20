package com.team4.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.team4.server.Server; // Nếu dùng chung Gson cấu hình sẵn
import io.github.cdimascio.dotenv.Dotenv;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client {
    private static final Dotenv dotenv = loadDotenv();
    private static final String SERVER_ADDRESS;
    private static final int SERVER_PORT;

    static {
        String host = dotenv.get("SERVER_HOST", null);
        String port = dotenv.get("SERVER_PORT", null);
        SERVER_ADDRESS = (host != null && !host.isBlank()) ? host : "127.0.0.1";
        SERVER_PORT    = (port != null && !port.isBlank()) ? Integer.parseInt(port) : 18368;
    }

    private static Dotenv loadDotenv() {
        try {
            Dotenv d = Dotenv.configure().ignoreIfMissing().load();
            if (d.get("SERVER_HOST") != null) return d;
        } catch (Exception ignored) {}
        try {
            Dotenv d = Dotenv.configure().directory("../").ignoreIfMissing().load();
            if (d.get("SERVER_HOST") != null) return d;
        } catch (Exception ignored) {}
        return Dotenv.configure().ignoreIfMissing().load();
    }

    private static Client instance;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    public static Client getInstance() {
        if (instance == null) {
            instance = new Client();
        }
        return instance;
    }

    private Runnable onForceLogout;

    public void setOnForceLogout(Runnable onForceLogout) {
        this.onForceLogout = onForceLogout;
    }

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

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed() && out != null && in != null;
    }

    private MessageListener currentListener;
    private Thread readThread;

    public void startListening(MessageListener listener) {
        this.currentListener = listener;
        if (!isConnected()) return;
        if (readThread != null && readThread.isAlive()) return; // Chỉ start 1 thread duy nhất

        readThread = new Thread(() -> {
            try {
                String serverMessage;
                while ((serverMessage = in.readLine()) != null) {
                    if (serverMessage.contains("\"action\":\"FORCE_LOGOUT\"")) {
                        if (onForceLogout != null) {
                            javafx.application.Platform.runLater(onForceLogout);
                        }
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
        request.addProperty("command", "BID");

        JsonObject data = new JsonObject();
        data.addProperty("auctionId", auctionId);
        data.addProperty("bidderId", bidderId);
        data.addProperty("amount", amount);

        request.addProperty("data", data.toString());
        sendMessage(request.toString());
    }

    public void sendLogin(String userId) {
        JsonObject request = new JsonObject();
        request.addProperty("command", "LOGIN");
        JsonObject data = new JsonObject();
        data.addProperty("userId", userId);
        request.addProperty("data", data.toString());
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
