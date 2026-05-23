package com.team4.server;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import static org.mockito.Mockito.*;

/**
 * Unit tests cho các utility method của ApiServer.
 * Tập trung vào parseParam() – hàm parse form-urlencoded request body.
 */
@DisplayName("Kiểm thử tiện ích ApiServer (parseParam)")
public class ApiServerUtilTest {

    // =========================================================================
    // Trường hợp thành công
    // =========================================================================
    @Nested
    @DisplayName("parseParam – tìm thấy giá trị")
    class FoundTests {

        @Test
        @DisplayName("Body có 1 param → trả về giá trị đúng")
        void singleParam_found() {
            assertEquals("alice", ApiServer.parseParam("username=alice", "username"));
        }

        @Test
        @DisplayName("Body có nhiều param → trả về đúng giá trị của key")
        void multipleParams_correctValue() {
            String body = "username=alice&password=secret123&role=BIDDER";
            assertEquals("alice",      ApiServer.parseParam(body, "username"));
            assertEquals("secret123",  ApiServer.parseParam(body, "password"));
            assertEquals("BIDDER",     ApiServer.parseParam(body, "role"));
        }

        @Test
        @DisplayName("Giá trị có dấu '=' → trả về phần sau dấu '=' đầu tiên")
        void valueContainsEquals() {
            // split("=", 2) → chỉ split lần đầu
            String body = "token=abc=def";
            assertEquals("abc=def", ApiServer.parseParam(body, "token"));
        }
    }

    // =========================================================================
    // URL encoding
    // =========================================================================
    @Nested
    @DisplayName("parseParam – URL-encoded values")
    class UrlEncodingTests {

        @Test
        @DisplayName("Tên có dấu tiếng Việt được decode đúng")
        void urlEncoded_vietnamese() {
            // "Nguyễn" URL-encoded
            String body = "fullName=Nguy%E1%BB%85n+V%C4%83n+A";
            String result = ApiServer.parseParam(body, "fullName");
            assertEquals("Nguyễn Văn A", result);
        }

        @Test
        @DisplayName("Dấu '+' trong value được decode thành khoảng trắng")
        void urlEncoded_plusAsSpace() {
            String body = "address=123+ABC+Street";
            assertEquals("123 ABC Street", ApiServer.parseParam(body, "address"));
        }

        @Test
        @DisplayName("Ký tự đặc biệt %40 (@) được decode đúng")
        void urlEncoded_atSign() {
            String body = "email=user%40example.com";
            assertEquals("user@example.com", ApiServer.parseParam(body, "email"));
        }
    }

    // =========================================================================
    // Trường hợp không tìm thấy / biên
    // =========================================================================
    @Nested
    @DisplayName("parseParam – không tìm thấy / biên")
    class NotFoundTests {

        @Test
        @DisplayName("Key không tồn tại → null")
        void keyNotFound_returnsNull() {
            assertNull(ApiServer.parseParam("username=alice", "email"));
        }

        @Test
        @DisplayName("Body rỗng → null")
        void emptyBody_returnsNull() {
            assertNull(ApiServer.parseParam("", "username"));
        }

        @Test
        @DisplayName("Giá trị rỗng ('key=') → trả về chuỗi rỗng")
        void emptyValue_returnsEmpty() {
            assertEquals("", ApiServer.parseParam("username=", "username"));
        }

        @ParameterizedTest(name = "body={0}, key={1}")
        @CsvSource({
            "a=1&b=2,  c",         // key không tồn tại
            "username=,  password", // value rỗng ở key khác
            "&=value, username"     // key rỗng không khớp
        })
        @DisplayName("Các trường hợp biên – không tìm thấy → null")
        void edgeCases_returnsNull(String body, String key) {
            assertNull(ApiServer.parseParam(body.trim(), key.trim()));
        }
    }

    @Nested
    @DisplayName("getRequesterId – xác thực token / fallback")
    class GetRequesterIdTests {

        @Test
        @DisplayName("Authorization header absent → trả về fallback value")
        void authHeaderAbsent_returnsFallback() {
            HttpExchange exchange = mock(HttpExchange.class);
            Headers headers = new Headers();
            when(exchange.getRequestHeaders()).thenReturn(headers);

            String result = ApiServer.getRequesterId(exchange, "fallback-user");
            assertEquals("fallback-user", result);
        }

        @Test
        @DisplayName("Authorization header present & valid → trả về userId")
        void authHeaderValid_returnsUserId() {
            HttpExchange exchange = mock(HttpExchange.class);
            Headers headers = new Headers();
            
            // Create a valid token
            com.team4.model.User user = new com.team4.model.Bidder(
                "test-user-id", java.time.LocalDateTime.now(), "username", "hash", "fullname", "email@t.com", 
                java.math.BigDecimal.ZERO, "address", "0912345678"
            );
            String token = Server.getJwtService().generateToken(user);
            headers.add("Authorization", "Bearer " + token);
            when(exchange.getRequestHeaders()).thenReturn(headers);

            String result = ApiServer.getRequesterId(exchange, "fallback-user");
            assertEquals("test-user-id", result);
        }

        @Test
        @DisplayName("Authorization header present but invalid → ném BusinessException")
        void authHeaderInvalid_throwsException() {
            HttpExchange exchange = mock(HttpExchange.class);
            Headers headers = new Headers();
            headers.add("Authorization", "Bearer invalid-token-string");
            when(exchange.getRequestHeaders()).thenReturn(headers);

            assertThrows(com.team4.util.BusinessException.class, () -> 
                ApiServer.getRequesterId(exchange, "fallback-user")
            );
        }
    }
}
