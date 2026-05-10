package com.team4.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSerializationContext;
import com.team4.dao.impl.AuctionDAOImpl;
import com.team4.dao.impl.ItemDAOImpl;
import com.team4.dao.impl.UserDAOImpl;
import com.team4.handler.ClientHandler;
import com.team4.service.AuctionService;
import com.team4.service.UserService;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final int PORT = 18368;
    private static Set<ClientHandler> clientHandlers = ConcurrentHashMap.newKeySet();
    private static ExecutorService threadPool;

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

    public static AuctionService getAuctionService() { return auctionService; }
    public static UserService getUserService()       { return userService; }
    public static Gson getGson()                     { return gson; }

    public static void main(String[] args) {
        com.team4.db.DatabaseManager.initialize();
        new ApiServer().start();

        threadPool = Executors.newFixedThreadPool(50);
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