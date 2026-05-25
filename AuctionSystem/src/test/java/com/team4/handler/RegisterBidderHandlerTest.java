package com.team4.handler;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.team4.dto.auth.RegisterBidderRequestDTO;
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
 * Unit tests cho RegisterBidderHandler.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Kiểm thử RegisterBidderHandler")
public class RegisterBidderHandlerTest {

    @Mock
    private AuthenticationService authService;

    @Mock
    private HttpExchange exchange;

    private RegisterBidderHandler handler;
    private ByteArrayOutputStream responseBody;

    @BeforeEach
    void setUp() throws Exception {
        handler = new RegisterBidderHandler();

        Field field = RegisterBidderHandler.class.getDeclaredField("authService");
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
    @Nested
    @DisplayName("OPTIONS request")
    class OptionsTests {

        @Test
        @DisplayName("OPTIONS → 204, không gọi service")
        void options_returns204() throws IOException {
            when(exchange.getRequestMethod()).thenReturn("OPTIONS");
            Headers h = new Headers();
            when(exchange.getResponseHeaders()).thenReturn(h);

            handler.handle(exchange);

            verify(exchange).sendResponseHeaders(eq(204), anyLong());
            verifyNoInteractions(authService);
        }
    }

    // =========================================================================
    // Method không hợp lệ
    // =========================================================================
    @Nested
    @DisplayName("Phương thức HTTP không hợp lệ")
    class InvalidMethodTests {

        @Test
        @DisplayName("GET → 405")
        void get_returns405() throws IOException {
            when(exchange.getRequestMethod()).thenReturn("GET");

            handler.handle(exchange);

            verify(exchange).sendResponseHeaders(405, -1);
            verifyNoInteractions(authService);
        }
    }

    // =========================================================================
    // POST – thiếu thông tin bắt buộc
    // =========================================================================
    @Nested
    @DisplayName("POST – thiếu username / password")
    class MissingParamTests {

        @Test
        @DisplayName("Không có body → 400")
        void emptyBody_returns400() throws IOException {
            when(exchange.getRequestMethod()).thenReturn("POST");
            when(exchange.getRequestBody()).thenReturn(bodyOf(""));

            handler.handle(exchange);

            verify(exchange).sendResponseHeaders(eq(400), anyLong());
            verifyNoInteractions(authService);
        }

        @Test
        @DisplayName("Thiếu password → 400")
        void missingPassword_returns400() throws IOException {
            when(exchange.getRequestMethod()).thenReturn("POST");
            when(exchange.getRequestBody()).thenReturn(bodyOf("username=alice"));

            handler.handle(exchange);

            verify(exchange).sendResponseHeaders(eq(400), anyLong());
            verifyNoInteractions(authService);
        }
    }

    // =========================================================================
    // POST – đăng ký thành công
    // =========================================================================
    @Nested
    @DisplayName("POST – đăng ký Bidder thành công")
    class SuccessTests {

        @Test
        @DisplayName("Đủ thông tin → 200 + SUCCESS")
        void register_success_returns200() throws IOException {
            // authService.registerBidder(RegisterBidderRequestDTO) → không ném exception → thành công
            doNothing().when(authService).registerBidder(any(RegisterBidderRequestDTO.class));

            when(exchange.getRequestMethod()).thenReturn("POST");
            when(exchange.getRequestBody()).thenReturn(bodyOf(
                    "username=alice&password=Pass1234&fullName=Alice&email=alice@example.com" +
                    "&shippingAddress=123+ABC&phoneNumber=0901234567"));

            handler.handle(exchange);

            verify(exchange).sendResponseHeaders(eq(200), anyLong());
            String resp = responseBody.toString(StandardCharsets.UTF_8);
            assertTrue(resp.contains("SUCCESS"));
            assertTrue(resp.contains("Bidder registration successful."));
        }

        @Test
        @DisplayName("Thiếu optional fields → vẫn đăng ký được với chuỗi rỗng")
        void register_withoutOptionalFields_stillCallsService() throws IOException {
            doNothing().when(authService).registerBidder(any(RegisterBidderRequestDTO.class));

            when(exchange.getRequestMethod()).thenReturn("POST");
            when(exchange.getRequestBody()).thenReturn(bodyOf("username=alice&password=Pass1234"));

            handler.handle(exchange);

            verify(exchange).sendResponseHeaders(eq(200), anyLong());
        }
    }

    // =========================================================================
    // POST – nghiệp vụ thất bại
    // =========================================================================
    @Nested
    @DisplayName("POST – đăng ký thất bại")
    class FailureTests {

        @Test
        @DisplayName("Username đã tồn tại → 400 + ERROR")
        void register_duplicateUsername_returns400() throws IOException {
            doThrow(new BusinessException("Tên đăng nhập đã tồn tại"))
                    .when(authService).registerBidder(any(RegisterBidderRequestDTO.class));

            when(exchange.getRequestMethod()).thenReturn("POST");
            when(exchange.getRequestBody()).thenReturn(bodyOf(
                    "username=alice&password=Pass1234&fullName=Alice&email=alice@example.com" +
                    "&shippingAddress=&phoneNumber="));

            handler.handle(exchange);

            verify(exchange).sendResponseHeaders(eq(400), anyLong());
            String resp = responseBody.toString(StandardCharsets.UTF_8);
            assertTrue(resp.contains("ERROR"));
            assertTrue(resp.contains("Tên đăng nhập đã tồn tại"));
        }

        @Test
        @DisplayName("Lỗi nội bộ bất ngờ → 500 + ERROR")
        void register_unexpectedException_returns500() throws IOException {
            doThrow(new RuntimeException("DB connection failed"))
                    .when(authService).registerBidder(any(RegisterBidderRequestDTO.class));

            when(exchange.getRequestMethod()).thenReturn("POST");
            when(exchange.getRequestBody()).thenReturn(bodyOf(
                    "username=alice&password=Pass1234&fullName=Alice&email=alice@example.com" +
                    "&shippingAddress=&phoneNumber="));

            handler.handle(exchange);

            verify(exchange).sendResponseHeaders(eq(500), anyLong());
            String resp = responseBody.toString(StandardCharsets.UTF_8);
            assertTrue(resp.contains("ERROR"));
        }
    }
}
