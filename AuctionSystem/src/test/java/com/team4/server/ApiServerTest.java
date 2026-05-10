package com.team4.server;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ApiServerTest {

    private static ApiServer apiServer;
    private static final int TEST_PORT = 8080; // Dùng port mặc định 8080 của ApiServer

    @BeforeAll
    public static void startServer() throws Exception {
        // Khởi động API Server thật
        apiServer = new ApiServer();
        apiServer.start();
        Thread.sleep(1000); // Đợi server khởi động hoàn toàn
    }

    @Test
    @Disabled("Yêu cầu MySQL Database thật đang chạy để không bị lỗi 500")
    public void testItemsEndpoint() throws Exception {
        // Gửi HTTP GET request thật tới endpoint /api/auctions
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/auctions"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Kiểm tra xem HTTP Server có phản hồi mã 200 OK không
        assertEquals(200, response.statusCode());
        assertNotNull(response.body());
    }
}
