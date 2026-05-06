package com.team4.client;

import com.google.gson.*;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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

    public String login(String username, String password) {
        try {
            String body = "username=" + username + "&password=" + password;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + "login"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() == 200) return response.body();
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public String registerBidder(String username, String password, String fullName, String email, String shippingAddress, String phoneNumber) {
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
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() == 200) return response.body();
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
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() == 200) return response.body();
            System.out.println("Dang ky that bai. Status: " + response.statusCode());
            return null;
        } catch (Exception e) {
            System.out.println("Loi ket noi API: " + e.getMessage());
            return null;
        }
    }

    public String getItems() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + "items"))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() == 200) return response.body();
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public Gson getGson() {
        return this.gson;
    }
}