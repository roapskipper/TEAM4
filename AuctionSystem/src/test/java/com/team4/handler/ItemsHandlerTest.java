package com.team4.handler;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.team4.dao.impl.ItemDAOImpl;
import com.team4.model.Art;
import com.team4.model.Item;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests cho ItemsHandler (GET /api/items).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Kiểm thử ItemsHandler")
public class ItemsHandlerTest {

    @Mock
    private ItemDAOImpl itemDAO;

    @Mock
    private HttpExchange exchange;

    private ItemsHandler handler;
    private ByteArrayOutputStream responseBody;

    @BeforeEach
    void setUp() throws Exception {
        handler = new ItemsHandler();

        Field field = ItemsHandler.class.getDeclaredField("itemDAO");
        field.setAccessible(true);
        field.set(handler, itemDAO);

        responseBody = new ByteArrayOutputStream();
        Headers headers = new Headers();
        lenient().when(exchange.getResponseHeaders()).thenReturn(headers);
        lenient().when(exchange.getResponseBody()).thenReturn(responseBody);
        lenient().when(exchange.getRequestURI()).thenReturn(URI.create("/api/items"));
        lenient().doNothing().when(exchange).sendResponseHeaders(anyInt(), anyLong());
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
        verifyNoInteractions(itemDAO);
    }

    // =========================================================================
    // Phương thức không hợp lệ
    // =========================================================================
    @Test
    @DisplayName("PATCH -> 405")
    void unsupportedMethod_returns405() throws IOException {
        when(exchange.getRequestMethod()).thenReturn("PATCH");

        handler.handle(exchange);

        verify(exchange).sendResponseHeaders(405, -1);
        verifyNoInteractions(itemDAO);
    }

    // =========================================================================
    // GET – thành công
    // =========================================================================
    @Nested
    @DisplayName("GET – lấy danh sách items")
    class GetItemsTests {

        @Test
        @DisplayName("Có items → 200 + SUCCESS")
        void getItems_hasItems_returns200() throws IOException {
            // Art là concrete subclass của abstract Item
            Art art = new Art(
                    "item-1",
                    LocalDateTime.now(),
                    "Starry Night",
                    new BigDecimal("500.00"),
                    "A beautiful painting",
                    "seller-1",
                    "Van Gogh",
                    1889,
                    Art.Medium.OIL_PAINT,
                    "73x92 cm"
            );
            when(itemDAO.findAll()).thenReturn(List.of(art));
            when(exchange.getRequestMethod()).thenReturn("GET");

            handler.handle(exchange);

            verify(exchange).sendResponseHeaders(eq(200), anyLong());
            String resp = responseBody.toString(StandardCharsets.UTF_8);
            assertTrue(resp.contains("SUCCESS"), "Phải chứa SUCCESS");
            assertTrue(resp.contains("Starry Night"), "Phải có tên item");
        }

        @Test
        @DisplayName("Không có items → 200 + danh sách rỗng")
        void getItems_empty_returns200() throws IOException {
            when(itemDAO.findAll()).thenReturn(Collections.emptyList());
            when(exchange.getRequestMethod()).thenReturn("GET");

            handler.handle(exchange);

            verify(exchange).sendResponseHeaders(eq(200), anyLong());
            String resp = responseBody.toString(StandardCharsets.UTF_8);
            assertTrue(resp.contains("SUCCESS"));
        }
    }

    // =========================================================================
    // GET – lỗi
    // =========================================================================
    @Test
    @DisplayName("RuntimeException từ DAO → 500 + ERROR")
    void getItems_exception_returns500() throws IOException {
        when(itemDAO.findAll()).thenThrow(new RuntimeException("DB error"));
        when(exchange.getRequestMethod()).thenReturn("GET");

        handler.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(500), anyLong());
        String resp = responseBody.toString(StandardCharsets.UTF_8);
        assertTrue(resp.contains("ERROR"));
    }
}
