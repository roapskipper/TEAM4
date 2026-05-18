package com.team4.client;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.team4.model.Item;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ApiClient {
    private static final String API_URL = "http://localhost:8080/api/";
    private final HttpClient client;
    private final Gson gson;

    public ApiClient() {
        this.client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .create();
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

    public String login(String username, String password, String adminCode) {
        try {
            String body = "username=" + username + "&password=" + password;
            if (adminCode != null && !adminCode.trim().isEmpty()) {
                body += "&adminCode=" + java.net.URLEncoder.encode(adminCode, StandardCharsets.UTF_8);
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
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public String registerBidder(String username, String password, String fullName, String email,
            String shippingAddress, String phoneNumber) {
        try {
            String body = "username=" + username
                    + "&password=" + password
                    + "&fullName=" + fullName
                    + "&email=" + email
                    + "&shippingAddress=" + shippingAddress
                    + "&phoneNumber=" + phoneNumber;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + "register/bidder"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() == 200)
                return response.body();
            System.out.println("Dang ky that bai. Status: " + response.statusCode());
            return null;
        } catch (Exception e) {
            System.out.println("Loi ket noi API: " + e.getMessage());
            return null;
        }
    }

    public String registerSeller(String username, String password, String fullName, String email, String storeName) {
        try {
            String body = "username=" + username
                    + "&password=" + password
                    + "&fullName=" + fullName
                    + "&email=" + email
                    + "&storeName=" + storeName;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + "register/seller"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() == 200)
                return response.body();
            System.out.println("Dang ky that bai. Status: " + response.statusCode());
            return null;
        } catch (Exception e) {
            System.out.println("Loi ket noi API: " + e.getMessage());
            return null;
        }
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
            throw new Exception(response.body() != null && !response.body().isEmpty() ? response.body() : "HTTP " + response.statusCode());
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
            throw new Exception(response.body() != null && !response.body().isEmpty() ? response.body() : "HTTP " + response.statusCode());
        }
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
            throw new Exception("HTTP " + response.statusCode());
        }
    }

    public String createItem(String sellerId, String name, String category, double startingPrice, String description) throws Exception {
        String body = "sellerId=" + java.net.URLEncoder.encode(sellerId, StandardCharsets.UTF_8)
                + "&name=" + java.net.URLEncoder.encode(name, StandardCharsets.UTF_8)
                + "&category=" + java.net.URLEncoder.encode(category, StandardCharsets.UTF_8)
                + "&startingPrice=" + startingPrice
                + "&description=" + java.net.URLEncoder.encode(description != null ? description : "", StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "items"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() == 200 || response.statusCode() == 201) {
            return response.body();
        } else {
            throw new Exception(response.body() != null && !response.body().isEmpty() ? response.body() : "HTTP " + response.statusCode());
        }
    }

    public String updateItem(String itemId, String name, String category, double startingPrice, String description) throws Exception {
        String body = "name=" + java.net.URLEncoder.encode(name, StandardCharsets.UTF_8)
                + "&category=" + java.net.URLEncoder.encode(category, StandardCharsets.UTF_8)
                + "&startingPrice=" + startingPrice
                + "&description=" + java.net.URLEncoder.encode(description != null ? description : "", StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "items/" + itemId))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .PUT(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() == 200) {
            return response.body();
        } else {
            throw new Exception(response.body() != null && !response.body().isEmpty() ? response.body() : "HTTP " + response.statusCode());
        }
    }

    public JsonArray getAuctions(String filter) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "admin/auctions?filter=" + java.net.URLEncoder.encode(filter, StandardCharsets.UTF_8)))
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
            throw new Exception("HTTP " + response.statusCode());
        }
    }

    public String approveAuction(String auctionId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "admin/auctions/" + auctionId + "/approve"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .PUT(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() == 200) {
            return response.body();
        } else {
            throw new Exception(response.body() != null && !response.body().isEmpty() ? response.body() : "HTTP " + response.statusCode());
        }
    }

    public String rejectAuction(String auctionId, String reason) throws Exception {
        String body = "reason=" + java.net.URLEncoder.encode(reason, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "admin/auctions/" + auctionId + "/reject"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .PUT(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() == 200) {
            return response.body();
        } else {
            throw new Exception(response.body() != null && !response.body().isEmpty() ? response.body() : "HTTP " + response.statusCode());
        }
    }
}