package com.team4.client;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import io.github.cdimascio.dotenv.Dotenv;
import com.team4.model.Item;
import com.team4.model.Art;
import com.team4.model.Collectible;
import com.team4.model.Electronics;
import com.team4.model.Fashion;
import com.team4.model.Vehicle;

public class ApiClient {
    private static final String API_URL;

    static {
        Dotenv dotenv = loadDotenv();
        String url = dotenv.get("API_BASE_URL", null);
        API_URL = (url != null && !url.isBlank()) ? url : "http://localhost:8080/api/";
    }

    private static Dotenv loadDotenv() {
        try {
            Dotenv d = Dotenv.configure().ignoreIfMissing().load();
            if (d.get("API_BASE_URL") != null) return d;
        } catch (Exception ignored) {}
        try {
            Dotenv d = Dotenv.configure().directory("../").ignoreIfMissing().load();
            if (d.get("API_BASE_URL") != null) return d;
        } catch (Exception ignored) {}
        return Dotenv.configure().ignoreIfMissing().load();
    }
    private final HttpClient client;
    private final Gson gson;

    public ApiClient() {
        this(HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build());
    }

    public ApiClient(HttpClient client) {
        this.client = client;
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .registerTypeAdapter(Item.class, new ItemDeserializer())
                .create();
    }

    private static class ItemDeserializer implements JsonDeserializer<Item> {
        @Override
        public Item deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();
            String category = jsonObject.get("category").getAsString();
            switch (category) {
                case "ART":
                    return context.deserialize(jsonObject, Art.class);
                case "COLLECTIBLE":
                    return context.deserialize(jsonObject, Collectible.class);
                case "ELECTRONICS":
                    return context.deserialize(jsonObject, Electronics.class);
                case "FASHION":
                    return context.deserialize(jsonObject, Fashion.class);
                case "VEHICLE":
                    return context.deserialize(jsonObject, Vehicle.class);
                default:
                    throw new JsonParseException("Unknown category: " + category);
            }
        }
    }

    private static String enc(String value) {
        return java.net.URLEncoder.encode(value != null ? value : "", StandardCharsets.UTF_8);
    }

    private static String plainNumber(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    public static String extractErrorMessage(String rawMessage) {
        if (rawMessage == null || rawMessage.trim().isEmpty()) {
            return "Something went wrong. Please try again.";
        }

        String message = rawMessage.trim();
        try {
            JsonElement parsed = JsonParser.parseString(message);
            if (parsed.isJsonObject()) {
                JsonObject obj = parsed.getAsJsonObject();
                if (obj.has("message") && !obj.get("message").isJsonNull()) {
                    return obj.get("message").getAsString();
                }
                if (obj.has("error") && !obj.get("error").isJsonNull()) {
                    return obj.get("error").getAsString();
                }
            }
        } catch (Exception ignored) {
        }

        return message;
    }

    public static String toDisplayMessage(Throwable error) {
        String message = extractErrorMessage(error != null ? error.getMessage() : null);
        String lower = message.toLowerCase();
        if (lower.contains("connection refused") || lower.contains("connect timed out")
                || lower.contains("no route to host")) {
            return "Cannot connect to server. Please start the server and try again.";
        }
        return message;
    }

    private static Exception apiException(HttpResponse<String> response) {
        String body = response.body();
        if (body != null && !body.isEmpty()) {
            return new Exception(extractErrorMessage(body));
        }
        return new Exception("HTTP " + response.statusCode());
    }

    private static class LocalDateTimeAdapter
            implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
        private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        @Override
        public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(formatter.format(src));
        }

        @Override
        public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            return LocalDateTime.parse(json.getAsString(), formatter);
        }
    }

    public String login(String username, String password, String adminCode) throws Exception {
        String body = "username=" + enc(username) + "&password=" + enc(password);
        if (adminCode != null && !adminCode.trim().isEmpty()) {
            body += "&adminCode=" + enc(adminCode);
        }
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "login"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() == 200)
            return response.body();
        throw apiException(response);
    }

    public String registerBidder(String username, String password, String fullName, String email,
            String shippingAddress, String phoneNumber) throws Exception {
        String body = "username=" + enc(username)
                + "&password=" + enc(password)
                + "&fullName=" + enc(fullName)
                + "&email=" + enc(email)
                + "&shippingAddress=" + enc(shippingAddress)
                + "&phoneNumber=" + enc(phoneNumber);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "register/bidder"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() == 200)
            return response.body();
        throw apiException(response);
    }

    public String registerSeller(String username, String password, String fullName, String email, String storeName) throws Exception {
        String body = "username=" + enc(username)
                + "&password=" + enc(password)
                + "&fullName=" + enc(fullName)
                + "&email=" + enc(email)
                + "&storeName=" + enc(storeName);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "register/seller"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() == 200)
            return response.body();
        throw apiException(response);
    }

    public List<Item> getItems() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + "items"))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() == 200) {
                JsonObject responseObj = JsonParser.parseString(response.body()).getAsJsonObject();
                if ("SUCCESS".equals(responseObj.get("status").getAsString())) {
                    JsonArray dataArray = responseObj.getAsJsonArray("data");
                    Type listType = new TypeToken<ArrayList<Item>>() {
                    }.getType();
                    return gson.fromJson(dataArray, listType);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public List<Item> getSellerItems(String sellerId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "seller/" + java.net.URLEncoder.encode(sellerId, StandardCharsets.UTF_8) + "/items"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        
        if (response.statusCode() == 200) {
            JsonObject responseObj = JsonParser.parseString(response.body()).getAsJsonObject();
            if (responseObj.has("status") && "SUCCESS".equals(responseObj.get("status").getAsString())) {
                JsonArray dataArray = responseObj.getAsJsonArray("data");
                Type listType = new TypeToken<ArrayList<Item>>() {}.getType();
                return gson.fromJson(dataArray, listType);
            } else if (responseObj.has("message")) {
                throw new Exception(responseObj.get("message").getAsString());
            } else {
                Type listType = new TypeToken<ArrayList<Item>>() {}.getType();
                return gson.fromJson(response.body(), listType);
            }
        } else {
            throw apiException(response);
        }
    }

    public Gson getGson() {
        return this.gson;
    }

    public String changePassword(String userId, String oldPassword, String newPassword) throws Exception {
        String body = "oldPassword=" + java.net.URLEncoder.encode(oldPassword, StandardCharsets.UTF_8)
                + "&newPassword=" + java.net.URLEncoder.encode(newPassword, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "user/" + userId + "/password"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .PUT(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() == 200) {
            return response.body();
        } else {
            throw apiException(response);
        }
    }

    public String updateProfile(String userId, String fullName, String email, String phone) throws Exception {
        String body = "fullName=" + java.net.URLEncoder.encode(fullName, StandardCharsets.UTF_8)
                + "&email=" + java.net.URLEncoder.encode(email, StandardCharsets.UTF_8)
                + "&phone=" + java.net.URLEncoder.encode(phone, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "user/" + userId + "/profile"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .PUT(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() == 200) {
            return response.body();
        } else {
            throw apiException(response);
        }
    }

    public JsonObject getUserProfile(String userId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "user/" + java.net.URLEncoder.encode(userId, StandardCharsets.UTF_8) + "/profile"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() == 200) {
            JsonElement parsed = JsonParser.parseString(response.body());
            if (parsed.isJsonObject()) {
                JsonObject responseObj = parsed.getAsJsonObject();
                if (responseObj.has("data") && responseObj.get("data").isJsonObject()) {
                    return responseObj.getAsJsonObject("data");
                }
            }
            return new JsonObject();
        } else {
            throw apiException(response);
        }
    }

    public JsonArray getOwnedItems(String userId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "user/" + java.net.URLEncoder.encode(userId, StandardCharsets.UTF_8) + "/owned-items"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() == 200) {
            JsonElement parsed = JsonParser.parseString(response.body());
            if (parsed.isJsonObject()) {
                JsonObject responseObj = parsed.getAsJsonObject();
                if (responseObj.has("data") && responseObj.get("data").isJsonArray()) {
                    return responseObj.getAsJsonArray("data");
                }
            }
            return parsed.isJsonArray() ? parsed.getAsJsonArray() : new JsonArray();
        }
        throw apiException(response);
    }

    public JsonObject getSellerStats(String sellerId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "seller/" + sellerId + "/stats"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() == 200) {
            return JsonParser.parseString(response.body()).getAsJsonObject();
        } else {
            throw apiException(response);
        }
    }

    public String createItem(String sellerId, String name, String category, double startingPrice, String description) throws Exception {
        String body = "sellerId=" + enc(sellerId)
                + "&name=" + enc(name)
                + "&category=" + enc(category)
                + "&startingPrice=" + enc(plainNumber(startingPrice))
                + "&description=" + enc(description);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "items"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() == 200 || response.statusCode() == 201) {
            return response.body();
        } else {
            throw apiException(response);
        }
    }

    public String updateItem(String itemId, String name, String category, double startingPrice, String description) throws Exception {
        String body = "name=" + enc(name)
                + "&category=" + enc(category)
                + "&startingPrice=" + enc(plainNumber(startingPrice))
                + "&description=" + enc(description);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "items/" + itemId))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .PUT(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() == 200) {
            return response.body();
        } else {
            throw apiException(response);
        }
    }

    public JsonArray getAuctions(String filter) throws Exception {
        return getAuctions(filter, "");
    }

    public JsonArray getAuctions(String filter, String requesterId) throws Exception {
        String query = "filter=" + java.net.URLEncoder.encode(filter, StandardCharsets.UTF_8);
        if (requesterId != null && !requesterId.isBlank()) {
            query += "&requesterId=" + java.net.URLEncoder.encode(requesterId, StandardCharsets.UTF_8);
        }
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "admin/auctions?" + query))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() == 200) {
            JsonElement parsed = JsonParser.parseString(response.body());
            if (parsed.isJsonObject()) {
                JsonObject responseObj = parsed.getAsJsonObject();
                if (responseObj.has("data") && responseObj.get("data").isJsonArray()) {
                    return responseObj.getAsJsonArray("data");
                }
            }
            if (parsed.isJsonArray()) {
                return parsed.getAsJsonArray();
            }
            return new JsonArray();
        } else {
            throw apiException(response);
        }
    }

    public JsonArray getPublicAuctions() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "auctions"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() == 200) {
            JsonElement parsed = JsonParser.parseString(response.body());
            if (parsed.isJsonObject()) {
                JsonObject responseObj = parsed.getAsJsonObject();
                if (responseObj.has("data") && responseObj.get("data").isJsonArray()) {
                    return responseObj.getAsJsonArray("data");
                }
            }
            if (parsed.isJsonArray()) {
                return parsed.getAsJsonArray();
            }
            return new JsonArray();
        }
        throw apiException(response);
    }

    public JsonObject getAuctionDetail(String auctionId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "auctions/" + java.net.URLEncoder.encode(auctionId, StandardCharsets.UTF_8)))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() == 200) {
            JsonElement parsed = JsonParser.parseString(response.body());
            if (parsed.isJsonObject()) {
                JsonObject responseObj = parsed.getAsJsonObject();
                if (responseObj.has("data") && responseObj.get("data").isJsonObject()) {
                    return responseObj.getAsJsonObject("data");
                }
                return responseObj;
            }
            return new JsonObject();
        }
        throw apiException(response);
    }

    public String approveAuction(String auctionId) throws Exception {
        return approveAuction(auctionId, "");
    }

    public String approveAuction(String auctionId, String requesterId) throws Exception {
        String body = "requesterId=" + java.net.URLEncoder.encode(requesterId != null ? requesterId : "", StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "admin/auctions/" + auctionId + "/approve"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .PUT(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() == 200) {
            return response.body();
        } else {
            throw apiException(response);
        }
    }

    public String rejectAuction(String auctionId, String reason) throws Exception {
        return rejectAuction(auctionId, "", reason);
    }

    public String rejectAuction(String auctionId, String requesterId, String reason) throws Exception {
        String body = "requesterId=" + java.net.URLEncoder.encode(requesterId != null ? requesterId : "", StandardCharsets.UTF_8);
        if (reason != null && !reason.isBlank()) {
            body += "&reason=" + java.net.URLEncoder.encode(reason, StandardCharsets.UTF_8);
        }
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "admin/auctions/" + auctionId + "/reject"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .PUT(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() == 200) {
            return response.body();
        } else {
            throw apiException(response);
        }
    }

    public JsonArray getAllUsers() throws Exception {
        return getAllUsers("");
    }

    public JsonArray getAllUsers(String requesterId) throws Exception {
        String query = requesterId == null || requesterId.isBlank()
                ? ""
                : "?requesterId=" + java.net.URLEncoder.encode(requesterId, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "admin/users" + query))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() == 200) {
            JsonElement parsed = JsonParser.parseString(response.body());
            if (parsed.isJsonObject()) {
                JsonObject responseObj = parsed.getAsJsonObject();
                if (responseObj.has("data") && responseObj.get("data").isJsonArray()) {
                    return responseObj.getAsJsonArray("data");
                }
            }
            if (parsed.isJsonArray()) {
                return parsed.getAsJsonArray();
            }
            return new JsonArray();
        } else {
            throw apiException(response);
        }
    }

    public JsonArray searchUsers(String query) throws Exception {
        return searchUsers(query, "");
    }

    public JsonArray searchUsers(String query, String requesterId) throws Exception {
        String encodedQuery = java.net.URLEncoder.encode(query, StandardCharsets.UTF_8);
        String encodedRequester = requesterId == null || requesterId.isBlank()
                ? ""
                : "&requesterId=" + java.net.URLEncoder.encode(requesterId, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "admin/users/search?q=" + encodedQuery + encodedRequester))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() == 200) {
            JsonElement parsed = JsonParser.parseString(response.body());
            if (parsed.isJsonObject()) {
                JsonObject responseObj = parsed.getAsJsonObject();
                if (responseObj.has("data") && responseObj.get("data").isJsonArray()) {
                    return responseObj.getAsJsonArray("data");
                }
            }
            if (parsed.isJsonArray()) {
                return parsed.getAsJsonArray();
            }
            return new JsonArray();
        } else {
            throw apiException(response);
        }
    }


    public String grantAdmin(String userId, String requesterId, String adminCode) throws Exception {
        String body = "requesterId=" + java.net.URLEncoder.encode(requesterId, StandardCharsets.UTF_8)
                + "&adminCode=" + java.net.URLEncoder.encode(adminCode, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "admin/users/" + userId + "/grant-admin"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .PUT(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() == 200) {
            return response.body();
        } else {
            throw apiException(response);
        }
    }

    public String revokeAdmin(String userId, String requesterId) throws Exception {
        String body = "requesterId=" + java.net.URLEncoder.encode(requesterId == null ? "" : requesterId, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "admin/users/" + userId + "/revoke-admin"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .PUT(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() == 200) {
            return response.body();
        } else {
            throw apiException(response);
        }
    }

    public JsonObject getDashboardStats() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "admin/dashboard/stats"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() == 200) {
            JsonElement parsed = JsonParser.parseString(response.body());
            if (parsed.isJsonObject()) {
                JsonObject responseObj = parsed.getAsJsonObject();
                if (responseObj.has("data") && responseObj.get("data").isJsonObject()) {
                    return responseObj.getAsJsonObject("data");
                }
                return responseObj;
            }
            return new JsonObject();
        } else {
            throw apiException(response);
        }
    }

    public JsonArray getBidHistoryByBidder(String bidderId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "user/" + java.net.URLEncoder.encode(bidderId, StandardCharsets.UTF_8) + "/bid-history"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() == 200) {
            JsonElement parsed = JsonParser.parseString(response.body());
            if (parsed.isJsonObject()) {
                JsonObject responseObj = parsed.getAsJsonObject();
                if (responseObj.has("data") && responseObj.get("data").isJsonArray()) {
                    return responseObj.getAsJsonArray("data");
                }
            }
            return parsed.isJsonArray() ? parsed.getAsJsonArray() : new JsonArray();
        }
        throw apiException(response);
    }

    public JsonObject getHighestBid(String auctionId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "auctions/" + java.net.URLEncoder.encode(auctionId, StandardCharsets.UTF_8) + "/highest-bid"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() == 200) {
            JsonElement parsed = JsonParser.parseString(response.body());
            if (parsed.isJsonObject()) {
                JsonObject responseObj = parsed.getAsJsonObject();
                if (responseObj.has("data") && responseObj.get("data").isJsonObject()) {
                    return responseObj.getAsJsonObject("data");
                }
                return responseObj;
            }
            return new JsonObject();
        }
        throw apiException(response);
    }

    public JsonObject enableAutoBid(String auctionId, String bidderId, double maxAmount) throws Exception {
        String body = "bidderId=" + enc(bidderId)
                + "&maxAmount=" + enc(plainNumber(maxAmount));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "auctions/" + enc(auctionId) + "/autobid"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() == 200) {
            JsonElement parsed = JsonParser.parseString(response.body());
            if (parsed.isJsonObject()) {
                JsonObject responseObj = parsed.getAsJsonObject();
                if (responseObj.has("data") && responseObj.get("data").isJsonObject()) {
                    return responseObj.getAsJsonObject("data");
                }
                return responseObj;
            }
            return new JsonObject();
        }
        throw apiException(response);
    }

    /**
     * Tắt auto-bid cho một phiên đấu giá.
     * DELETE /api/auctions/{auctionId}/autobid
     */
    public void disableAutoBid(String auctionId, String bidderId) throws Exception {
        String body = "bidderId=" + enc(bidderId);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "auctions/" + enc(auctionId) + "/autobid"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .method("DELETE", HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw apiException(response);
        }
    }

    /**
     * Lấy trạng thái auto-bid hiện tại.
     * GET /api/auctions/{auctionId}/autobid?bidderId=...
     */
    public JsonObject getAutoBidStatus(String auctionId, String bidderId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "auctions/" + enc(auctionId) + "/autobid?bidderId=" + enc(bidderId)))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() == 200) {
            JsonElement parsed = JsonParser.parseString(response.body());
            if (parsed.isJsonObject()) {
                JsonObject responseObj = parsed.getAsJsonObject();
                if (responseObj.has("data") && responseObj.get("data").isJsonObject()) {
                    return responseObj.getAsJsonObject("data");
                }
                return responseObj;
            }
            return new JsonObject();
        }
        throw apiException(response);
    }
}
