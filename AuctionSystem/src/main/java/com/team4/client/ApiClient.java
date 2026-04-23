package com.team4.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class ApiClient {
    private static final String API_URL = "http://localhost:8080/api/";
    private final HttpClient client;

    public ApiClient() {
        this.client = HttpClient.newHttpClient();
    }

    public String login(String username, String password) {
        try {
            String body = "username=" + username + "&password=" + password;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + "login"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Accept", "application/json; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() == 200) {
                return response.body();
            } else {
                System.out.println("Dang nhap that bai. Status: " + response.statusCode());
                return null;
            }
        } catch (Exception e) {
            System.out.println("Loi ket noi API: " + e.getMessage());
            return null;
        }
    }

    public String register(String username, String password, String role) {
        try {
            String body = "username=" + username + "&password=" + password + "&role=" + role;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + "register"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Accept", "application/json; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() == 200) {
                return response.body();
            } else {
                System.out.println("Dang ky that bai. Status: " + response.statusCode());
                return null;
            }
        } catch (Exception e) {
            System.out.println("Loi ket noi API: " + e.getMessage());
            return null;
        }
    }

    public String getItems() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + "items"))
                    .header("Accept", "application/json; charset=UTF-8")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() == 200) {
                return response.body();
            } else {
                System.out.println("Loi lay danh sach items. Status: " + response.statusCode());
                return null;
            }
        } catch (Exception e) {
            System.out.println("Loi ket noi API: " + e.getMessage());
            return null;
        }
    }
}