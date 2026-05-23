package com.team4.handler;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.team4.dto.bidding.AutoBidRequestDTO;
import com.team4.dto.bidding.AutoBidResponseDTO;
import com.team4.service.AutoBiddingService;
import com.team4.util.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Kiểm thử AutoBidHandler")
public class AutoBidHandlerTest {

    @Mock
    private HttpExchange exchange;

    @Mock
    private AutoBiddingService autoBiddingService;

    private AutoBidHandler handler;
    private ByteArrayOutputStream responseBody;

    @BeforeEach
    void setUp() throws Exception {
        handler = new AutoBidHandler();

        Field serviceField = AutoBidHandler.class.getDeclaredField("autoBiddingService");
        serviceField.setAccessible(true);
        serviceField.set(handler, autoBiddingService);

        responseBody = new ByteArrayOutputStream();
        Headers headers = new Headers();
        lenient().when(exchange.getResponseHeaders()).thenReturn(headers);
        lenient().when(exchange.getResponseBody()).thenReturn(responseBody);
        lenient().doNothing().when(exchange).sendResponseHeaders(anyInt(), anyLong());
    }

    @Test
    @DisplayName("OPTIONS → 204")
    void options_returns204() throws IOException {
        when(exchange.getRequestMethod()).thenReturn("OPTIONS");

        handler.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(204), eq(0L));
    }

    @Test
    @DisplayName("POST → Bật Auto-Bid thành công (200)")
    void post_enableAutoBid_success() throws IOException {
        String auctionId = "auction-123";
        String bidderId = "bidder-456";
        BigDecimal maxAmount = new BigDecimal("500.00");
        String requestBody = "bidderId=" + bidderId + "&maxAmount=" + maxAmount;

        when(exchange.getRequestMethod()).thenReturn("POST");
        when(exchange.getRequestURI()).thenReturn(URI.create("/api/auctions/" + auctionId + "/autobid"));
        when(exchange.getRequestBody()).thenReturn(new ByteArrayInputStream(requestBody.getBytes(StandardCharsets.UTF_8)));

        AutoBidResponseDTO mockResponse = new AutoBidResponseDTO("config-789", auctionId, bidderId, maxAmount, true);
        when(autoBiddingService.enableAutoBidding(any(AutoBidRequestDTO.class))).thenReturn(mockResponse);

        handler.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(200), anyLong());
        String response = responseBody.toString(StandardCharsets.UTF_8);
        assertTrue(response.contains("SUCCESS"));
        assertTrue(response.contains("config-789"));
        assertTrue(response.contains("active"));
    }

    @Test
    @DisplayName("POST → Thiếu bidderId → 400")
    void post_missingBidderId_returns400() throws IOException {
        String auctionId = "auction-123";
        String requestBody = "maxAmount=500.00";

        when(exchange.getRequestMethod()).thenReturn("POST");
        when(exchange.getRequestURI()).thenReturn(URI.create("/api/auctions/" + auctionId + "/autobid"));
        when(exchange.getRequestBody()).thenReturn(new ByteArrayInputStream(requestBody.getBytes(StandardCharsets.UTF_8)));

        handler.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(400), anyLong());
        String response = responseBody.toString(StandardCharsets.UTF_8);
        assertTrue(response.contains("ERROR"));
        assertTrue(response.contains("Missing bidderId"));
    }

    @Test
    @DisplayName("DELETE → Tắt Auto-Bid thành công (200)")
    void delete_disableAutoBid_success() throws IOException {
        String auctionId = "auction-123";
        String bidderId = "bidder-456";
        String requestBody = "bidderId=" + bidderId;

        when(exchange.getRequestMethod()).thenReturn("DELETE");
        when(exchange.getRequestURI()).thenReturn(URI.create("/api/auctions/" + auctionId + "/autobid"));
        when(exchange.getRequestBody()).thenReturn(new ByteArrayInputStream(requestBody.getBytes(StandardCharsets.UTF_8)));

        AutoBidResponseDTO existingConfig = new AutoBidResponseDTO("config-789", auctionId, bidderId, new BigDecimal("500.00"), true);
        when(autoBiddingService.findConfig(bidderId, auctionId)).thenReturn(existingConfig);
        doNothing().when(autoBiddingService).disableAutoBidding("config-789");

        handler.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(200), anyLong());
        String response = responseBody.toString(StandardCharsets.UTF_8);
        assertTrue(response.contains("SUCCESS"));
        assertTrue(response.contains("Auto-bid disabled successfully"));
    }

    @Test
    @DisplayName("GET → Lấy trạng thái hoạt động (200)")
    void get_statusActive() throws IOException {
        String auctionId = "auction-123";
        String bidderId = "bidder-456";

        when(exchange.getRequestMethod()).thenReturn("GET");
        when(exchange.getRequestURI()).thenReturn(URI.create("/api/auctions/" + auctionId + "/autobid?bidderId=" + bidderId));

        AutoBidResponseDTO config = new AutoBidResponseDTO("config-789", auctionId, bidderId, new BigDecimal("500.00"), true);
        when(autoBiddingService.findConfig(bidderId, auctionId)).thenReturn(config);

        handler.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(200), anyLong());
        String response = responseBody.toString(StandardCharsets.UTF_8);
        assertTrue(response.contains("SUCCESS"));
        assertTrue(response.contains("active"));
        assertTrue(response.contains("true"));
    }

    @Test
    @DisplayName("GET → Không cấu hình auto-bid → active=false (200)")
    void get_statusInactive() throws IOException {
        String auctionId = "auction-123";
        String bidderId = "bidder-456";

        when(exchange.getRequestMethod()).thenReturn("GET");
        when(exchange.getRequestURI()).thenReturn(URI.create("/api/auctions/" + auctionId + "/autobid?bidderId=" + bidderId));
        when(autoBiddingService.findConfig(bidderId, auctionId)).thenThrow(new BusinessException("Config not found"));

        handler.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(200), anyLong());
        String response = responseBody.toString(StandardCharsets.UTF_8);
        assertTrue(response.contains("SUCCESS"));
        assertTrue(response.contains("active"));
        assertTrue(response.contains("false"));
    }
}
