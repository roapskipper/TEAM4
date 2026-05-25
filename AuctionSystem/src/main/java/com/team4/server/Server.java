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
import com.team4.service.BiddingService;
import com.team4.dao.impl.AutoBiddingDAOImpl;
import com.team4.dao.impl.BidTransactionDAOImpl;


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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Server {
    private static final int PORT = 18368;
    private static Set<ClientHandler> clientHandlers = ConcurrentHashMap.newKeySet();
    private static ConcurrentHashMap<String, ClientHandler> activeUsers = new ConcurrentHashMap<>();
    private static ExecutorService threadPool;
    private static ScheduledExecutorService auctionScheduler;

    private static final AuctionService auctionService = new AuctionService(
            new AuctionDAOImpl(),
            new ItemDAOImpl()
    );
    private static final UserService userService = new UserService(new UserDAOImpl());
    private static final com.team4.service.AdminService adminService = new com.team4.service.AdminService(
            userService,
            auctionService,
            new AuctionDAOImpl(),
            new UserDAOImpl(),
            new ItemDAOImpl()
    );
    private static final com.team4.service.JwtService jwtService = new com.team4.service.JwtService();
    private static final com.team4.service.AuthenticationService authenticationService = new com.team4.service.AuthenticationService(
            new UserDAOImpl(),
            jwtService
    );
    private static BiddingService biddingService = new BiddingService(
            new AuctionDAOImpl(),
            new BidTransactionDAOImpl(),
            new UserDAOImpl(),
            new AutoBiddingDAOImpl()
    );


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
    public static com.team4.service.AdminService getAdminService() { return adminService; }
    public static com.team4.service.JwtService getJwtService() { return jwtService; }
    public static com.team4.service.AuthenticationService getAuthenticationService() { return authenticationService; }
    public static BiddingService getBiddingService() { return biddingService; }
    public static void setBiddingService(BiddingService service) { biddingService = service; }
    public static Gson getGson()                     { return gson; }

    public static void main(String[] args) {
        com.team4.util.DatabaseSetup.initDatabase();
        com.team4.db.DatabaseManager.initialize();
        new ApiServer().start();
        startAuctionScheduler();

        threadPool = Executors.newFixedThreadPool(50);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down server. Releasing thread pool...");
            if (auctionScheduler != null && !auctionScheduler.isShutdown()) {
                auctionScheduler.shutdown();
            }
            if (threadPool != null && !threadPool.isShutdown()) {
                threadPool.shutdown();
            }
        }));

        System.out.println("Server is starting on port " + PORT + "...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Co ket noi moi: " + socket.getInetAddress());

                ClientHandler handler = new ClientHandler(socket);
                clientHandlers.add(handler);
                threadPool.execute(handler);

                if (threadPool instanceof java.util.concurrent.ThreadPoolExecutor) {
                    java.util.concurrent.ThreadPoolExecutor tpe = (java.util.concurrent.ThreadPoolExecutor) threadPool;
                    System.out.println("Trang thai ThreadPool - Active: " + tpe.getActiveCount() + ", Pool Size: " + tpe.getPoolSize());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void startAuctionScheduler() {
        auctionScheduler = Executors.newSingleThreadScheduledExecutor();
        auctionScheduler.scheduleAtFixedRate(() -> {
            try {
                auctionService.closeExpiredAuctions();
            } catch (Exception e) {
                System.err.println("Unable to close expired auctions: " + e.getMessage());
            }
        }, 5, 10, TimeUnit.SECONDS);
    }

    public static void removeClient(ClientHandler handler) {
        clientHandlers.remove(handler);
        if (handler.getUserId() != null) {
            // Chỉ xóa nếu ClientHandler hiện tại trong map đúng là handler này (tránh xóa nhầm của session mới)
            activeUsers.remove(handler.getUserId(), handler);
        }
        System.out.println("Mot client da ngat ket noi. So luong hien tai: " + clientHandlers.size());
    }

    public static void registerUser(String userId, ClientHandler newHandler) {
        ClientHandler oldHandler = activeUsers.get(userId);
        if (oldHandler != null && oldHandler != newHandler) {
        System.out.println("Detected login from another device for user: " + userId);
            oldHandler.forceLogout();
            removeClient(oldHandler);
        }
        activeUsers.put(userId, newHandler);
        System.out.println("Registered socket session for user: " + userId);
    }

    public static void broadcast(String message, ClientHandler excludeUser) {
        for (ClientHandler handler : clientHandlers) {
            if (handler != excludeUser) {
                handler.sendMessage(message);
            }
        }
    }
}
