package com.team4.handler;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.team4.model.Auction;
import com.team4.service.AuctionService;
import com.team4.util.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.net.URI;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests cho AuctionsHandler (GET /api/auctions).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Kiểm thử AuctionsHandler")
public class AuctionsHandlerTest {

    @Mock
    private com.team4.dao.AuctionDAO auctionDAO;

    @Mock
    private HttpExchange exchange;

    @Mock private com.team4.dao.ItemDAO itemDAO;
    @Mock private com.team4.dao.UserDAO userDAO;
    @Mock private com.team4.dao.BidTransactionDAO bidTransactionDAO;

    private AuctionsHandler handler;
    private ByteArrayOutputStream responseBody;

    @BeforeEach
    void setUp() throws Exception {
        handler = new AuctionsHandler();

        // Inject mock AuctionService qua reflection
        Field field = AuctionsHandler.class.getDeclaredField("auctionDAO");
        field.setAccessible(true);
        field.set(handler, auctionDAO);

        Field itemDaoField = AuctionsHandler.class.getDeclaredField("itemDAO");
        itemDaoField.setAccessible(true);
        itemDaoField.set(handler, itemDAO);

        Field userDaoField = AuctionsHandler.class.getDeclaredField("userDAO");
        userDaoField.setAccessible(true);
        userDaoField.set(handler, userDAO);

        Field bidDaoField = AuctionsHandler.class.getDeclaredField("bidTransactionDAO");
        bidDaoField.setAccessible(true);
        bidDaoField.set(handler, bidTransactionDAO);

        responseBody = new ByteArrayOutputStream();
        Headers headers = new Headers();
        lenient().when(exchange.getResponseHeaders()).thenReturn(headers);
        lenient().when(exchange.getResponseBody()).thenReturn(responseBody);
        lenient().doNothing().when(exchange).sendResponseHeaders(anyInt(), anyLong());
    }

    // Helper tạo Auction thật
    private Auction realAuction(String id, Auction.AuctionStatus status) {
        return new Auction(
                id,
                LocalDateTime.now(),
                "item-1",
                "seller-1",
                null,
                new BigDecimal("100.00"),
                new BigDecimal("100.00"),
                new BigDecimal("10.00"),
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(2),
                status
        );
    }

    // =========================================================================
    // OPTIONS
    // =========================================================================
    @Test
    @DisplayName("OPTIONS → 204")
    void options_returns204() throws IOException {
        when(exchange.getRequestMethod()).thenReturn("OPTIONS");
        Headers h = new Headers();
        when(exchange.getResponseHeaders()).thenReturn(h);

        handler.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(204), anyLong());
        verifyNoInteractions(auctionDAO);
    }

    // =========================================================================
    // Phương thức không hợp lệ
    // =========================================================================
    @Test
    @DisplayName("POST → 405")
    void post_returns405() throws IOException {
        when(exchange.getRequestMethod()).thenReturn("POST");

        handler.handle(exchange);

        verify(exchange).sendResponseHeaders(405, -1);
        verifyNoInteractions(auctionDAO);
    }

    // =========================================================================
    // GET – nghiệp vụ thành công
    // =========================================================================
    @Nested
    @DisplayName("GET – lấy danh sách phiên đấu giá RUNNING")
    class GetAuctionsTests {

        @Test
        @DisplayName("Có phiên → 200 + SUCCESS + danh sách không rỗng")
        void getAuctions_hasResults_returns200() throws IOException {
            List<Auction> auctions = List.of(realAuction("auction-1", Auction.AuctionStatus.RUNNING));
            when(auctionDAO.findAll()).thenReturn(auctions);
            when(exchange.getRequestMethod()).thenReturn("GET");
            when(exchange.getRequestURI()).thenReturn(URI.create("/api/auctions"));
            when(bidTransactionDAO.findByAuctionId(anyString())).thenReturn(Collections.emptyList());

            handler.handle(exchange);

            verify(exchange).sendResponseHeaders(eq(200), anyLong());
            String resp = responseBody.toString(StandardCharsets.UTF_8);
            assertTrue(resp.contains("SUCCESS"));
            assertTrue(resp.contains("auction-1"));
        }

        @Test
        @DisplayName("Không có phiên nào → 200 + mảng rỗng")
        void getAuctions_empty_returns200() throws IOException {
            when(auctionDAO.findAll())
                    .thenReturn(Collections.emptyList());
            when(exchange.getRequestMethod()).thenReturn("GET");
            when(exchange.getRequestURI()).thenReturn(URI.create("/api/auctions"));

            handler.handle(exchange);

            verify(exchange).sendResponseHeaders(eq(200), anyLong());
            String resp = responseBody.toString(StandardCharsets.UTF_8);
            assertTrue(resp.contains("SUCCESS"));
        }
    }

    // =========================================================================
    // GET – nghiệp vụ thất bại
    // =========================================================================
    @Nested
    @DisplayName("GET – lỗi service")
    class ErrorTests {

        @Test
        @DisplayName("BusinessException → 400 + ERROR")
        void getAuctions_businessException_returns400() throws IOException {
            when(auctionDAO.findAll())
                    .thenThrow(new BusinessException("Lỗi nghiệp vụ"));
            when(exchange.getRequestMethod()).thenReturn("GET");
            when(exchange.getRequestURI()).thenReturn(URI.create("/api/auctions"));

            handler.handle(exchange);

            verify(exchange).sendResponseHeaders(eq(400), anyLong());
            String resp = responseBody.toString(StandardCharsets.UTF_8);
            assertTrue(resp.contains("ERROR"));
        }

        @Test
        @DisplayName("RuntimeException → 500 + ERROR")
        void getAuctions_runtimeException_returns500() throws IOException {
            when(auctionDAO.findAll())
                    .thenThrow(new RuntimeException("DB crash"));
            when(exchange.getRequestMethod()).thenReturn("GET");
            when(exchange.getRequestURI()).thenReturn(URI.create("/api/auctions"));

            handler.handle(exchange);

            verify(exchange).sendResponseHeaders(eq(500), anyLong());
            String resp = responseBody.toString(StandardCharsets.UTF_8);
            assertTrue(resp.contains("ERROR"));
        }
    }
}
