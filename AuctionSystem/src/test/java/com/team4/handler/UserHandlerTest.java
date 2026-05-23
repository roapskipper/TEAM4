package com.team4.handler;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.team4.dao.impl.AuctionDAOImpl;
import com.team4.dao.impl.ItemDAOImpl;
import com.team4.dto.auth.UserResponseDTO;
import com.team4.dto.auction.BidTransactionResponseDTO;
import com.team4.model.Art;
import com.team4.model.Auction;
import com.team4.model.Item;
import com.team4.model.User;
import com.team4.server.Server;
import com.team4.service.AuthenticationService;
import com.team4.service.BiddingService;
import com.team4.service.UserService;
import com.team4.util.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("Kiểm thử UserHandler")
public class UserHandlerTest {

    @Mock private ItemDAOImpl itemDAO;
    @Mock private AuctionDAOImpl auctionDAO;
    @Mock private UserService userService;
    @Mock private BiddingService biddingService;
    @Mock private AuthenticationService authenticationService;
    @Mock private HttpExchange exchange;

    private UserHandler handler;
    private ByteArrayOutputStream responseBody;
    private MockedStatic<Server> mockedServer;
    private final Gson gson = new Gson();

    @BeforeEach
    void setUp() throws Exception {
        handler = new UserHandler();

        // Inject mock DAOs
        Field itemDaoField = UserHandler.class.getDeclaredField("itemDAO");
        itemDaoField.setAccessible(true);
        itemDaoField.set(handler, itemDAO);

        Field auctionDaoField = UserHandler.class.getDeclaredField("auctionDAO");
        auctionDaoField.setAccessible(true);
        auctionDaoField.set(handler, auctionDAO);

        // Mock static Server class
        mockedServer = mockStatic(Server.class);
        mockedServer.when(Server::getUserService).thenReturn(userService);
        mockedServer.when(Server::getBiddingService).thenReturn(biddingService);
        mockedServer.when(Server::getAuthenticationService).thenReturn(authenticationService);
        mockedServer.when(Server::getGson).thenReturn(gson);

        responseBody = new ByteArrayOutputStream();
        Headers headers = new Headers();
        lenient().when(exchange.getResponseHeaders()).thenReturn(headers);
        lenient().when(exchange.getResponseBody()).thenReturn(responseBody);
        lenient().doNothing().when(exchange).sendResponseHeaders(anyInt(), anyLong());
    }

    @AfterEach
    void tearDown() {
        mockedServer.close();
    }

    @Test
    @DisplayName("OPTIONS → 204")
    void options_returns204() throws IOException {
        when(exchange.getRequestMethod()).thenReturn("OPTIONS");

        handler.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(204), anyLong());
    }

    @Test
    @DisplayName("Đường dẫn không hợp lệ (< 5 phần) → 400")
    void invalidPath_returns400() throws IOException {
        when(exchange.getRequestMethod()).thenReturn("GET");
        when(exchange.getRequestURI()).thenReturn(URI.create("/api/user/1")); // Chỉ có 4 phần sau khi split "/"

        handler.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(400), anyLong());
        String resp = responseBody.toString(StandardCharsets.UTF_8);
        assertTrue(resp.contains("ERROR"));
        assertTrue(resp.contains("Invalid path"));
    }

    @Nested
    @DisplayName("GET /api/user/{userId}/profile")
    class GetProfileTests {

        @Test
        @DisplayName("Lấy profile thành công → 200 + SUCCESS")
        void getProfile_success() throws IOException {
            UserResponseDTO userDTO = new UserResponseDTO("123", "test_user", "Test User", "test@test.com", User.Role.BIDDER, BigDecimal.ZERO, "2026-05-22T23:00:00");
            userDTO.setPhoneNumber("0912345678");
            when(userService.getUserById("123")).thenReturn(userDTO);

            when(exchange.getRequestMethod()).thenReturn("GET");
            when(exchange.getRequestURI()).thenReturn(URI.create("/api/user/123/profile"));

            handler.handle(exchange);

            verify(exchange).sendResponseHeaders(eq(200), anyLong());
            String resp = responseBody.toString(StandardCharsets.UTF_8);
            assertTrue(resp.contains("SUCCESS"));
            assertTrue(resp.contains("Test User"));
            assertTrue(resp.contains("test@test.com"));
        }

        @Test
        @DisplayName("Không tìm thấy user → 400 + ERROR")
        void getProfile_userNotFound() throws IOException {
            when(userService.getUserById("nonexistent")).thenThrow(new BusinessException("User not found"));

            when(exchange.getRequestMethod()).thenReturn("GET");
            when(exchange.getRequestURI()).thenReturn(URI.create("/api/user/nonexistent/profile"));

            handler.handle(exchange);

            verify(exchange).sendResponseHeaders(eq(400), anyLong());
            String resp = responseBody.toString(StandardCharsets.UTF_8);
            assertTrue(resp.contains("ERROR"));
            assertTrue(resp.contains("User not found"));
        }
    }

    @Nested
    @DisplayName("GET /api/user/{userId}/bid-history")
    class GetBidHistoryTests {

        @Test
        @DisplayName("Lấy lịch sử đấu giá thành công → 200 + SUCCESS")
        void getBidHistory_success() throws IOException {
            // Mock user validation
            UserResponseDTO userDTO = new UserResponseDTO("123", "test_user", "Test User", "test@test.com", User.Role.BIDDER, BigDecimal.ZERO, "2026-05-22T23:00:00");
            userDTO.setPhoneNumber("0912345678");
            when(userService.getUserById("123")).thenReturn(userDTO);

            // Mock history return
            BidTransactionResponseDTO bidDTO = new BidTransactionResponseDTO("bid-1", "auc-1", "123", new BigDecimal("150.00"), "2026-05-22T23:00:00");
            when(biddingService.getBidHistoryByBidder("123")).thenReturn(List.of(bidDTO));

            when(exchange.getRequestMethod()).thenReturn("GET");
            when(exchange.getRequestURI()).thenReturn(URI.create("/api/user/123/bid-history"));

            handler.handle(exchange);

            verify(exchange).sendResponseHeaders(eq(200), anyLong());
            String resp = responseBody.toString(StandardCharsets.UTF_8);
            assertTrue(resp.contains("SUCCESS"));
            assertTrue(resp.contains("auc-1"));
            assertTrue(resp.contains("150.00"));
        }
    }

    @Nested
    @DisplayName("GET /api/user/{userId}/owned-items")
    class GetOwnedItemsTests {

        @Test
        @DisplayName("Người dùng là Seller yêu cầu xem owned-items → 400 + ERROR")
        void getOwnedItems_sellerRequest_returns400() throws IOException {
            UserResponseDTO sellerDTO = new UserResponseDTO("456", "seller_user", "Seller User", "seller@test.com", User.Role.SELLER, BigDecimal.ZERO, "2026-05-22T23:00:00");
            sellerDTO.setPhoneNumber("0912345678");
            when(userService.getUserById("456")).thenReturn(sellerDTO);

            when(exchange.getRequestMethod()).thenReturn("GET");
            when(exchange.getRequestURI()).thenReturn(URI.create("/api/user/456/owned-items"));

            handler.handle(exchange);

            verify(exchange).sendResponseHeaders(eq(400), anyLong());
            String resp = responseBody.toString(StandardCharsets.UTF_8);
            assertTrue(resp.contains("ERROR"));
            assertTrue(resp.contains("Only bidders have won items"));
        }

        @Test
        @DisplayName("Lấy danh sách đồ đã sở hữu thành công → 200 + SUCCESS")
        void getOwnedItems_success() throws IOException {
            UserResponseDTO bidderDTO = new UserResponseDTO("123", "test_user", "Test User", "test@test.com", User.Role.BIDDER, BigDecimal.ZERO, "2026-05-22T23:00:00");
            bidderDTO.setPhoneNumber("0912345678");
            when(userService.getUserById("123")).thenReturn(bidderDTO);

            Item item = new Art("item-1", LocalDateTime.now(), "Test Item", new BigDecimal("100.00"), "Desc", "123", "Artist", 2020, Art.Medium.OIL_PAINT, "30x40 cm");
            when(itemDAO.findOwnedByBidderId("123")).thenReturn(List.of(item));

            Auction auction = new Auction("auc-1", LocalDateTime.now(), "item-1", "seller-1", "123", new BigDecimal("100.00"), new BigDecimal("150.00"), new BigDecimal("10.00"), LocalDateTime.now(), LocalDateTime.now().minusHours(1), Auction.AuctionStatus.FINISHED);
            when(auctionDAO.findAll()).thenReturn(List.of(auction));

            when(exchange.getRequestMethod()).thenReturn("GET");
            when(exchange.getRequestURI()).thenReturn(URI.create("/api/user/123/owned-items"));

            handler.handle(exchange);

            verify(exchange).sendResponseHeaders(eq(200), anyLong());
            String resp = responseBody.toString(StandardCharsets.UTF_8);
            assertTrue(resp.contains("SUCCESS"));
            assertTrue(resp.contains("Test Item"));
            assertTrue(resp.contains("wonPrice"));
        }
    }

    @Nested
    @DisplayName("PUT /api/user/{userId}/profile")
    class UpdateProfileTests {

        @Test
        @DisplayName("Cập nhật profile thành công → 200 + SUCCESS")
        void updateProfile_success() throws IOException {
            UserResponseDTO updatedDTO = new UserResponseDTO("123", "test_user", "New Name", "new@test.com", User.Role.BIDDER, BigDecimal.ZERO, "2026-05-22T23:00:00");
            updatedDTO.setPhoneNumber("0912345678");
            when(userService.updateProfile("123", "New Name", "new@test.com", "0912345678")).thenReturn(updatedDTO);

            String body = "fullName=New+Name&email=new%40test.com&phone=0912345678";
            ByteArrayInputStream requestBodyStream = new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
            when(exchange.getRequestBody()).thenReturn(requestBodyStream);

            when(exchange.getRequestMethod()).thenReturn("PUT");
            when(exchange.getRequestURI()).thenReturn(URI.create("/api/user/123/profile"));

            handler.handle(exchange);

            verify(exchange).sendResponseHeaders(eq(200), anyLong());
            String resp = responseBody.toString(StandardCharsets.UTF_8);
            assertTrue(resp.contains("SUCCESS"));
            assertTrue(resp.contains("New Name"));
        }

        @Test
        @DisplayName("Thiếu thông tin bắt buộc → 400 + ERROR")
        void updateProfile_missingFields() throws IOException {
            String body = "fullName=New+Name"; // thiếu email
            ByteArrayInputStream requestBodyStream = new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
            when(exchange.getRequestBody()).thenReturn(requestBodyStream);

            when(exchange.getRequestMethod()).thenReturn("PUT");
            when(exchange.getRequestURI()).thenReturn(URI.create("/api/user/123/profile"));

            handler.handle(exchange);

            verify(exchange).sendResponseHeaders(eq(400), anyLong());
            String resp = responseBody.toString(StandardCharsets.UTF_8);
            assertTrue(resp.contains("ERROR"));
        }
    }

    @Nested
    @DisplayName("PUT /api/user/{userId}/password")
    class ChangePasswordTests {

        @Test
        @DisplayName("Thay đổi mật khẩu thành công → 200 + SUCCESS")
        void changePassword_success() throws IOException {
            doNothing().when(authenticationService).changePassword("123", "old123", "new123");

            String body = "oldPassword=old123&newPassword=new123";
            ByteArrayInputStream requestBodyStream = new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
            when(exchange.getRequestBody()).thenReturn(requestBodyStream);

            when(exchange.getRequestMethod()).thenReturn("PUT");
            when(exchange.getRequestURI()).thenReturn(URI.create("/api/user/123/password"));

            handler.handle(exchange);

            verify(exchange).sendResponseHeaders(eq(200), anyLong());
            String resp = responseBody.toString(StandardCharsets.UTF_8);
            assertTrue(resp.contains("SUCCESS"));
            assertTrue(resp.contains("Password changed successfully"));
        }
    }
}
