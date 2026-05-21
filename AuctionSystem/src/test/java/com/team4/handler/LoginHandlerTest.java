package com.team4.handler;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.team4.dto.auth.LoginRequestDTO;
import com.team4.dto.auth.LoginResponseDTO;
import com.team4.model.User;
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
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests cho LoginHandler.
 *
 * Chiến lược:
 * - Mock HttpExchange để tránh phụ thuộc vào HTTP server thật.
 * - Mock AuthenticationService (inject qua reflection) để kiểm soát kết quả.
 * - Kiểm tra status code HTTP và nội dung response JSON thông qua OutputStream đã capture.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Kiểm thử LoginHandler")
public class LoginHandlerTest {

    @Mock
    private AuthenticationService authService;

    @Mock
    private HttpExchange exchange;

    private LoginHandler handler;

    // OutputStream để capture response ghi ra
    private ByteArrayOutputStream responseBody;

    @BeforeEach
    void setUp() throws Exception {
        handler = new LoginHandler();

        // Inject authService mock vào handler thông qua reflection (field private)
        Field field = LoginHandler.class.getDeclaredField("authService");
        field.setAccessible(true);
        field.set(handler, authService);

        // Dùng ByteArrayOutputStream để bắt nội dung mà handler ghi vào
        responseBody = new ByteArrayOutputStream();

        // Stub các header và outputStream mà ApiServer.sendResponse cần
        Headers headers = new Headers();
        lenient().when(exchange.getResponseHeaders()).thenReturn(headers);
        lenient().when(exchange.getResponseBody()).thenReturn(responseBody);
        // stub sendResponseHeaders để tránh NPE khi handler gọi ApiServer.sendResponse
        lenient().doNothing().when(exchange).sendResponseHeaders(anyInt(), anyLong());
    }

    // -------------------------------------------------------------------------
    // Helper: tạo request body dạng form-urlencoded
    // -------------------------------------------------------------------------
    private InputStream bodyOf(String body) {
        return new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
    }

    // -------------------------------------------------------------------------
    // Helper: tạo LoginResponseDTO giả cho một Bidder
    // -------------------------------------------------------------------------
    private LoginResponseDTO fakeBidderResponse(String userId) {
        return new LoginResponseDTO(userId, "testuser", User.Role.BIDDER,
                "Nguyen Van A", null, BigDecimal.ZERO, null);
    }

    // =========================================================================
    // OPTIONS – CORS pre-flight
    // =========================================================================
    @Nested
    @DisplayName("OPTIONS request (CORS pre-flight)")
    class OptionsTests {

        @Test
        @DisplayName("OPTIONS → trả về 204 và không gọi service")
        void options_returns204() throws IOException {
            when(exchange.getRequestMethod()).thenReturn("OPTIONS");
            Headers headers = new Headers();
            when(exchange.getResponseHeaders()).thenReturn(headers);

            handler.handle(exchange);

            // ApiServer.sendResponse gửi body rỗng → sendResponseHeaders(204, 0L)
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
        @DisplayName("GET → trả về 405")
        void get_returns405() throws IOException {
            when(exchange.getRequestMethod()).thenReturn("GET");

            handler.handle(exchange);

            verify(exchange).sendResponseHeaders(405, -1);
            verifyNoInteractions(authService);
        }
    }

    // =========================================================================
    // POST – thiếu thông tin
    // =========================================================================
    @Nested
    @DisplayName("POST – thiếu username / password")
    class MissingParamTests {

        @Test
        @DisplayName("Body rỗng → trả về 400")
        void emptyBody_returns400() throws IOException {
            when(exchange.getRequestMethod()).thenReturn("POST");
            when(exchange.getRequestBody()).thenReturn(bodyOf(""));

            handler.handle(exchange);

            verify(exchange).sendResponseHeaders(eq(400), anyLong());
            String resp = responseBody.toString(StandardCharsets.UTF_8);
            assertTrue(resp.contains("ERROR"), "Response phải chứa ERROR");
            verifyNoInteractions(authService);
        }

        @Test
        @DisplayName("Chỉ có username, thiếu password → trả về 400")
        void missingPassword_returns400() throws IOException {
            when(exchange.getRequestMethod()).thenReturn("POST");
            when(exchange.getRequestBody()).thenReturn(bodyOf("username=alice"));

            handler.handle(exchange);

            verify(exchange).sendResponseHeaders(eq(400), anyLong());
            verifyNoInteractions(authService);
        }
    }

    // =========================================================================
    // POST – đăng nhập thường (không có adminCode)
    // =========================================================================
    @Nested
    @DisplayName("POST – đăng nhập Bidder/Seller thông thường")
    class NormalLoginTests {

        @Test
        @DisplayName("Đăng nhập thành công → 200 + userId / role trong JSON")
        void login_success_returns200() throws IOException {
            // AuthService mới dùng loginBidder(LoginRequestDTO) → LoginResponseDTO
            when(authService.loginBidder(any(LoginRequestDTO.class)))
                    .thenReturn(fakeBidderResponse("bidder-001"));

            when(exchange.getRequestMethod()).thenReturn("POST");
            when(exchange.getRequestBody()).thenReturn(bodyOf("username=testuser&password=correct_pass"));

            handler.handle(exchange);

            verify(exchange).sendResponseHeaders(eq(200), anyLong());
            String resp = responseBody.toString(StandardCharsets.UTF_8);
            assertTrue(resp.contains("SUCCESS"), "Phải chứa SUCCESS");
            assertTrue(resp.contains("userId"), "Phải chứa userId");
            assertTrue(resp.contains("BIDDER"), "Phải chứa role BIDDER");
        }

        @Test
        @DisplayName("Sai mật khẩu → 401 + ERROR")
        void login_wrongPassword_returns401() throws IOException {
            when(authService.loginBidder(any(LoginRequestDTO.class)))
                    .thenThrow(new BusinessException("Mật khẩu không đúng"));

            when(exchange.getRequestMethod()).thenReturn("POST");
            when(exchange.getRequestBody()).thenReturn(bodyOf("username=testuser&password=wrong_pass"));

            handler.handle(exchange);

            verify(exchange).sendResponseHeaders(eq(401), anyLong());
            String resp = responseBody.toString(StandardCharsets.UTF_8);
            assertTrue(resp.contains("ERROR"));
            assertTrue(resp.contains("Mật khẩu không đúng"));
        }

        @Test
        @DisplayName("Username không tồn tại → 401 + ERROR")
        void login_userNotFound_returns401() throws IOException {
            when(authService.loginBidder(any(LoginRequestDTO.class)))
                    .thenThrow(new BusinessException("Tên đăng nhập không tồn tại"));

            when(exchange.getRequestMethod()).thenReturn("POST");
            when(exchange.getRequestBody()).thenReturn(bodyOf("username=unknown&password=any_pass"));

            handler.handle(exchange);

            verify(exchange).sendResponseHeaders(eq(401), anyLong());
        }
    }

    // =========================================================================
    // POST – đăng nhập admin (có adminCode)
    // =========================================================================
    @Nested
    @DisplayName("POST – đăng nhập Admin (có adminCode)")
    class AdminLoginTests {

        @Test
        @DisplayName("adminCode sai → 401 + ERROR")
        void login_wrongAdminCode_returns401() throws IOException {
            when(authService.loginAdmin(any(LoginRequestDTO.class)))
                    .thenThrow(new BusinessException("Mã admin không đúng"));

            when(exchange.getRequestMethod()).thenReturn("POST");
            when(exchange.getRequestBody()).thenReturn(
                    bodyOf("username=admin&password=pass123&adminCode=wrong_code"));

            handler.handle(exchange);

            verify(exchange).sendResponseHeaders(eq(401), anyLong());
            String resp = responseBody.toString(StandardCharsets.UTF_8);
            assertTrue(resp.contains("Mã admin không đúng"));
        }
    }
}
