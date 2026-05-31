package com.team4.controller;

import com.team4.util.UserSession;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Kiểm thử MainController")
public class MainControllerTest {

    private MainController controller;

    private VBox navContainer;
    private Label userNameLabel;
    private Label userRoleBadge;
    private Label balanceValueLabel;
    private Button depositBtn;

    private MockedStatic<UserSession> mockedSessionStatic;
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
        controller = new MainController();

        // Khởi tạo các UI components
        BorderPane mainRoot = new BorderPane();
        navContainer = new VBox();
        StackPane contentArea = new StackPane();
        Label pageTitle = new Label();
        Label pageSubtitle = new Label();
        userNameLabel = new Label();
        userRoleBadge = new Label();
        Label userAvatarText = new Label();
        Label userAvatarRoleText = new Label();
        balanceValueLabel = new Label();
        StackPane userAvatarBg = new StackPane();
        Button notiBtn = new Button();
        Label notiBadge = new Label();
        depositBtn = new Button();

        // Tiêm (Inject) qua reflection
        setField(controller, "mainRoot", mainRoot);
        setField(controller, "navContainer", navContainer);
        setField(controller, "contentArea", contentArea);
        setField(controller, "pageTitle", pageTitle);
        setField(controller, "pageSubtitle", pageSubtitle);
        setField(controller, "userNameLabel", userNameLabel);
        setField(controller, "userRoleBadge", userRoleBadge);
        setField(controller, "userAvatarText", userAvatarText);
        setField(controller, "userAvatarRoleText", userAvatarRoleText);
        setField(controller, "balanceValueLabel", balanceValueLabel);
        setField(controller, "userAvatarBg", userAvatarBg);
        setField(controller, "notiBtn", notiBtn);
        setField(controller, "notiBadge", notiBadge);
        setField(controller, "depositBtn", depositBtn);

        // Mock UserSession
        mockSession = mock(UserSession.class);
        mockedSessionStatic = mockStatic(UserSession.class);
        mockedSessionStatic.when(UserSession::getInstance).thenReturn(mockSession);
    }

    @AfterEach
    public void tearDown() {
        mockedSessionStatic.close();
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
    @DisplayName("Kiểm tra thiết lập Sidebar cho Admin")
    public void testSetUserRole_Admin() {
        when(mockSession.getFullName()).thenReturn("Admin User");
        
        // Mock method updateUserInfo and refresh calls that might need more setup
        controller.setUserRole("admin_super");

        // Kiểm tra UI cập nhật
        assertEquals("SUPER ADMIN", userRoleBadge.getText());
        assertEquals("Admin User", userNameLabel.getText());
        
        // Admin Sidebar cần có Dashboard, Users, Auctions, Profile
        boolean hasDashboard = navContainer.getChildren().stream()
                .filter(node -> node instanceof Button)
                .map(node -> (Button) node)
                .anyMatch(btn -> "Dashboard".equals(btn.getText()));
        assertTrue(hasDashboard, "Sidebar Admin thiếu Dashboard");
    }

    @Test
    @DisplayName("Kiểm tra thiết lập Sidebar cho Bidder")
    public void testSetUserRole_Bidder() {
        when(mockSession.getFullName()).thenReturn("Bidder User");
        
        controller.setUserRole("bidder");

        assertEquals("BIDDER", userRoleBadge.getText());
        assertEquals("Bidder User", userNameLabel.getText());
        
        // Bidder Sidebar cần có Auction Room, Owned Items, Profile
        boolean hasAuctionRoom = navContainer.getChildren().stream()
                .filter(node -> node instanceof Button)
                .map(node -> (Button) node)
                .anyMatch(btn -> "Auction Room".equals(btn.getText()));
        assertTrue(hasAuctionRoom, "Sidebar Bidder thiếu Auction Room");
    }

    @Test
    @DisplayName("Kiểm tra hiển thị nút Deposit cho Bidder")
    public void testUpdateDepositVisibility_Bidder() throws Exception {
        setField(controller, "userRole", "bidder");
        invokeMethod(controller, "updateDepositVisibility");
        
        assertTrue(depositBtn.isVisible());
        assertTrue(depositBtn.isManaged());
    }

    @Test
    @DisplayName("Kiểm tra ẩn nút Deposit cho Admin")
    public void testUpdateDepositVisibility_Admin() throws Exception {
        setField(controller, "userRole", "admin_super");
        invokeMethod(controller, "updateDepositVisibility");
        
        assertFalse(depositBtn.isVisible());
        assertFalse(depositBtn.isManaged());
    }
}
