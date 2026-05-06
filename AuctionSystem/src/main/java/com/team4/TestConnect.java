package com.team4;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class TestConnect {
    public static void main(String[] args) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            String body = "username=testuserpost&password=password123&fullName=Người Dùng&email=testuserpost@example.com&shippingAddress=Hà Nội&phoneNumber=0123456789";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/register/bidder"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            System.out.println("Sending POST request to /api/register/bidder...");
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Response Status: " + response.statusCode());
            System.out.println("Response Body: " + response.body());
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
        System.exit(0);
    }
}