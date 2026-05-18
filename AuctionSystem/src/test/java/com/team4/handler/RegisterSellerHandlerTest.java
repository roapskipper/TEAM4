package com.team4.handler;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.team4.service.AuthenticationService;
import com.team4.util.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests cho RegisterSellerHandler.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Kiểm thử RegisterSellerHandler")
public class RegisterSellerHandlerTest {

    @Mock
    private AuthenticationService authService;

    @Mock
    private HttpExchange exchange;

    private RegisterSellerHandler handler;
    private ByteArrayOutputStream responseBody;

    @BeforeEach
    void setUp() throws Exception {
        handler = new RegisterSellerHandler();

        Field field = RegisterSellerHandler.class.getDeclaredField("authService");
        field.setAccessible(true);
        field.set(handler, authService);

        responseBody = new ByteArrayOutputStream();
        Headers headers = new Headers();
        lenient().when(exchange.getResponseHeaders()).thenReturn(headers);
        lenient().when(exchange.getResponseBody()).thenReturn(responseBody);
        lenient().doNothing().when(exchange).sendResponseHeaders(anyInt(), anyLong());
    }

    private InputStream bodyOf(String s) {
        return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
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
        verifyNoInteractions(authService);
    }

    // =========================================================================
    // Method không hợp lệ
    // =========================================================================
    @Test
    @DisplayName("GET → 405")
    void get_returns405() throws IOException {
        when(exchange.getRequestMethod()).thenReturn("GET");

        handler.handle(exchange);

        verify(exchange).sendResponseHeaders(405, -1);
        verifyNoInteractions(authService);
    }

    // =========================================================================
    // POST – thiếu thông tin
    // =========================================================================
    @Nested
    @DisplayName("POST – thiếu username / password")
    class MissingParamTests {

        @Test
        @DisplayName("Body rỗng → 400")
        void emptyBody_returns400() throws IOException {
            when(exchange.getRequestMethod()).thenReturn("POST");
            when(exchange.getRequestBody()).thenReturn(bodyOf(""));

            handler.handle(exchange);

            verify(exchange).sendResponseHeaders(eq(400), anyLong());
            verifyNoInteractions(authService);
        }

        @Test
        @DisplayName("Username trống → 400")
        void emptyUsername_returns400() throws IOException {
            when(exchange.getRequestMethod()).thenReturn("POST");
            when(exchange.getRequestBody()).thenReturn(bodyOf("username=&password=Pass1234"));

            handler.handle(exchange);

            verify(exchange).sendResponseHeaders(eq(400), anyLong());
            verifyNoInteractions(authService);
        }
    }

    // =========================================================================
    // POST – đăng ký thành công
    // =========================================================================
    @Nested
    @DisplayName("POST – đăng ký Seller thành công")
    class SuccessTests {

        @Test
        @DisplayName("Đủ thông tin → 200 + SUCCESS")
        void register_success_returns200() throws IOException {
            doNothing().when(authService).registerSeller(
                    anyString(), anyString(), anyString(), anyString(), anyString());

            when(exchange.getRequestMethod()).thenReturn("POST");
            when(exchange.getRequestBody()).thenReturn(bodyOf(
                    "username=seller1&password=Pass1234&fullName=Bob&email=bob@example.com&storeName=BobShop"));

            handler.handle(exchange);

            verify(exchange).sendResponseHeaders(eq(200), anyLong());
            String resp = responseBody.toString(StandardCharsets.UTF_8);
            assertTrue(resp.contains("SUCCESS"));
            assertTrue(resp.contains("Dang ky Seller thanh cong"));
        }

        @Test
        @DisplayName("Không có storeName → gọi service với chuỗi rỗng")
        void register_withoutStoreName_usesEmptyString() throws IOException {
            doNothing().when(authService).registerSeller(
                    anyString(), anyString(), anyString(), anyString(), eq(""));

            when(exchange.getRequestMethod()).thenReturn("POST");
            when(exchange.getRequestBody()).thenReturn(bodyOf(
                    "username=seller1&password=Pass1234"));

            handler.handle(exchange);

            verify(authService).registerSeller(anyString(), anyString(), eq(""), eq(""), eq(""));
            verify(exchange).sendResponseHeaders(eq(200), anyLong());
        }
    }

    // =========================================================================
    // POST – đăng ký thất bại
    // =========================================================================
    @Nested
    @DisplayName("POST – đăng ký thất bại")
    class FailureTests {

        @Test
        @DisplayName("Username đã tồn tại → 400 + ERROR")
        void register_duplicateUsername_returns400() throws IOException {
            doThrow(new BusinessException("Tên đăng nhập đã tồn tại"))
                    .when(authService).registerSeller(
                            anyString(), anyString(), anyString(), anyString(), anyString());

            when(exchange.getRequestMethod()).thenReturn("POST");
            when(exchange.getRequestBody()).thenReturn(bodyOf(
                    "username=seller1&password=Pass1234&fullName=Bob&email=bob@example.com&storeName=BobShop"));

            handler.handle(exchange);

            verify(exchange).sendResponseHeaders(eq(400), anyLong());
            String resp = responseBody.toString(StandardCharsets.UTF_8);
            assertTrue(resp.contains("ERROR"));
        }

        @Test
        @DisplayName("Lỗi không mong muốn → 500")
        void register_runtimeException_returns500() throws IOException {
            doThrow(new RuntimeException("DB down"))
                    .when(authService).registerSeller(
                            anyString(), anyString(), anyString(), anyString(), anyString());

            when(exchange.getRequestMethod()).thenReturn("POST");
            when(exchange.getRequestBody()).thenReturn(bodyOf(
                    "username=seller1&password=Pass1234&fullName=Bob&email=bob@example.com&storeName=BobShop"));

            handler.handle(exchange);

            verify(exchange).sendResponseHeaders(eq(500), anyLong());
        }
    }
}
