package com.team4.controller;

import com.google.gson.JsonObject;
import com.team4.client.Client;
import com.team4.util.UserSession;
import javafx.application.Platform;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Kiểm thử BiddingRoomController")
public class BiddingRoomControllerTest {

    private BiddingRoomController controller;

    private TextField bidAmountField;
    private Button placeBidBtn;
    private Label bidError;
    private Label currentPrice;
    private ToggleButton autoBidToggle;
    private VBox autoBidPanel;
    private TextField autoBidMax;
    private Button applyAutoBidBtn;

    private MockedStatic<Client> mockedClientStatic;
    private MockedStatic<UserSession> mockedSessionStatic;
    private Client mockClient;
    private UserSession mockSession;

    @BeforeAll
    public static void initJFX() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // Toolkit already initialized
        }
    }

    @BeforeEach
    public void setUp() throws Exception {
        controller = new BiddingRoomController();

        // Khởi tạo các UI components
        bidAmountField = new TextField();
        placeBidBtn = new Button();
        bidError = new Label();
        currentPrice = new Label();
        autoBidToggle = new ToggleButton();
        autoBidPanel = new VBox();
        autoBidMax = new TextField();
        applyAutoBidBtn = new Button();
        
        Label itemName = new Label();
        Label minBidLabel = new Label();
        Label bidCount = new Label();
        Label timeLeft = new Label();
        ListView<String> bidHistoryList = new ListView<>();
        AreaChart<Number, Number> priceChart = new AreaChart<>(new NumberAxis(), new NumberAxis());

        // Tiêm (Inject) qua reflection
        setField(controller, "bidAmountField", bidAmountField);
        setField(controller, "placeBidBtn", placeBidBtn);
        setField(controller, "bidError", bidError);
        setField(controller, "currentPrice", currentPrice);
        setField(controller, "autoBidToggle", autoBidToggle);
        setField(controller, "autoBidPanel", autoBidPanel);
        setField(controller, "autoBidMax", autoBidMax);
        setField(controller, "applyAutoBidBtn", applyAutoBidBtn);
        setField(controller, "itemName", itemName);
        setField(controller, "minBidLabel", minBidLabel);
        setField(controller, "bidCount", bidCount);
        setField(controller, "timeLeft", timeLeft);
        setField(controller, "bidHistoryList", bidHistoryList);
        setField(controller, "priceChart", priceChart);

        // Mock Singletons
        mockClient = mock(Client.class);
        mockedClientStatic = mockStatic(Client.class);
        mockedClientStatic.when(Client::getInstance).thenReturn(mockClient);

        // Initialize real UserSession singleton for other threads (e.g. JavaFX thread)
        UserSession.createSession("user-1", "test_user", "BIDDER");

        mockSession = mock(UserSession.class);
        mockedSessionStatic = mockStatic(UserSession.class);
        mockedSessionStatic.when(UserSession::getInstance).thenReturn(mockSession);
    }

    @AfterEach
    public void tearDown() {
        mockedClientStatic.close();
        mockedSessionStatic.close();
        UserSession.clearSession();
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private void invokeMethod(Object target, String methodName) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        method.invoke(target);
    }

    @Test
    @DisplayName("Kiểm tra báo lỗi khi nhập số tiền không hợp lệ")
    public void testOnPlaceBid_InvalidNumber() throws Exception {
        bidAmountField.setText("abc"); // Không phải số
        invokeMethod(controller, "onPlaceBid");

        assertTrue(bidError.isVisible());
        assertEquals("Please enter a valid number", bidError.getText());
        verify(mockClient, never()).sendBid(anyString(), anyString(), anyDouble());
    }

    @Test
    @DisplayName("Kiểm tra Toggle Auto-Bid hiển thị/ẩn Panel")
    public void testOnAutoBidToggle() throws Exception {
        // Mô phỏng trạng thái đấu giá đang diễn ra
        setField(controller, "auctionStatus", "RUNNING");
        setField(controller, "auctionEndTime", java.time.LocalDateTime.now().plusDays(1));
        when(mockSession.getRole()).thenReturn("BIDDER");

        autoBidToggle.setSelected(true);
        invokeMethod(controller, "onAutoBidToggle");

        assertTrue(autoBidPanel.isVisible());
        assertTrue(autoBidPanel.isManaged());
        assertFalse(applyAutoBidBtn.isDisabled());

        autoBidToggle.setSelected(false);
        invokeMethod(controller, "onAutoBidToggle");

        assertFalse(autoBidPanel.isVisible());
        assertFalse(autoBidPanel.isManaged());
    }

    @Test
    @DisplayName("Kiểm tra gọi Client.sendBid thành công")
    public void testPlaceBid_Success() throws Exception {
        // Thiết lập trạng thái hợp lệ
        setField(controller, "auctionStatus", "RUNNING");
        setField(controller, "auctionEndTime", java.time.LocalDateTime.now().plusDays(1));
        setField(controller, "auctionId", "auction-123");
        setField(controller, "currentBid", 100000.0);
        setField(controller, "bidIncrement", 50000.0);
        setField(controller, "availableBalance", BigDecimal.valueOf(500000.0));

        when(mockSession.getUserId()).thenReturn("user-1");
        when(mockSession.getRole()).thenReturn("BIDDER");
        // Giả lập số dư > số tiền đấu giá
        setField(controller, "availableBalance", BigDecimal.valueOf(500000.0));

        when(mockClient.isConnected()).thenReturn(true);

        // Đặt số tiền đấu giá là 200,000 (Hợp lệ)
        bidAmountField.setText("200000");

        invokeMethod(controller, "onPlaceBid");

        // Nút đặt cược phải bị vô hiệu hóa để chống spam
        assertTrue(placeBidBtn.isDisabled());
        // Lỗi phải bị ẩn
        assertFalse(bidError.isVisible());
        // Hệ thống gọi Client.sendBid
        verify(mockClient, times(1)).sendBid("auction-123", "user-1", 200000.0);
    }

    @Test
    @DisplayName("Kiểm tra báo lỗi khi số dư không đủ")
    public void testPlaceBid_InsufficientBalance() throws Exception {
        setField(controller, "auctionStatus", "RUNNING");
        setField(controller, "auctionEndTime", java.time.LocalDateTime.now().plusDays(1));
        setField(controller, "auctionId", "auction-123");
        setField(controller, "currentBid", 100000.0);
        setField(controller, "bidIncrement", 50000.0);
        setField(controller, "availableBalance", BigDecimal.valueOf(10000.0));

        when(mockSession.getUserId()).thenReturn("user-1");
        when(mockSession.getRole()).thenReturn("BIDDER");
        // Giả lập số dư quá ít
        setField(controller, "availableBalance", BigDecimal.valueOf(10000.0));

        bidAmountField.setText("200000");

        invokeMethod(controller, "onPlaceBid");

        assertTrue(bidError.isVisible());
        assertTrue(bidError.getText().contains("Your balance is not enough"));
        verify(mockClient, never()).sendBid(anyString(), anyString(), anyDouble());
    }

    @Test
    @DisplayName("Kiểm tra xử lý thông báo Socket - BID_SUCCESS")
    public void testHandleSocketMessage_BidSuccess() throws Exception {
        // Mô phỏng hàm handleSocketMessage(String message)
        Method handleMessage = BiddingRoomController.class.getDeclaredMethod("handleSocketMessage", String.class);
        handleMessage.setAccessible(true);

        setField(controller, "auctionId", "auction-123");
        setField(controller, "auctionStatus", "RUNNING");
        setField(controller, "auctionEndTime", java.time.LocalDateTime.now().plusDays(1));

        JsonObject data = new JsonObject();
        data.addProperty("auctionId", "auction-123");
        data.addProperty("currentPrice", 300000.0);

        JsonObject json = new JsonObject();
        json.addProperty("action", "BID_SUCCESS");
        json.add("data", data);

        // Gọi UI update trong JavaFX thread qua Platform.runLater
        Platform.runLater(() -> {
            try {
                handleMessage.invoke(controller, json.toString());
            } catch (Exception e) {
                fail(e);
            }
        });

        // Chờ JavaFX thread chạy xong
        Thread.sleep(200);

        // Giá hiện tại phải được cập nhật trên giao diện
        assertEquals("300,000 VND", currentPrice.getText());
        assertFalse(bidError.isVisible());
        assertFalse(placeBidBtn.isDisabled());
    }
}
