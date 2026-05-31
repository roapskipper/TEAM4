package com.team4.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Kiểm thử LoginController")
public class LoginControllerTest {

    private LoginController controller;

    private VBox loginForm;
    private VBox registerForm;
    private Button loginTab;
    private Button registerTab;
    private Button loginBtn;
    private Button regBtn;
    private Label loginError;
    private TextField loginUsername;
    private PasswordField loginPassword;
    private VBox adminCodeBox;
    private PasswordField loginAdminCode;
    private Hyperlink toggleAdminCodeLink;

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
        controller = new LoginController();

        loginForm = new VBox();
        registerForm = new VBox();
        loginTab = new Button();
        registerTab = new Button();
        loginBtn = new Button();
        regBtn = new Button();
        loginError = new Label();
        loginUsername = new TextField();
        loginPassword = new PasswordField();
        adminCodeBox = new VBox();
        loginAdminCode = new PasswordField();
        toggleAdminCodeLink = new Hyperlink();

        setField(controller, "loginForm", loginForm);
        setField(controller, "registerForm", registerForm);
        setField(controller, "loginTab", loginTab);
        setField(controller, "registerTab", registerTab);
        setField(controller, "loginBtn", loginBtn);
        setField(controller, "regBtn", regBtn);
        setField(controller, "loginError", loginError);
        setField(controller, "loginUsername", loginUsername);
        setField(controller, "loginPassword", loginPassword);
        setField(controller, "adminCodeBox", adminCodeBox);
        setField(controller, "loginAdminCode", loginAdminCode);
        setField(controller, "toggleAdminCodeLink", toggleAdminCodeLink);
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
    @DisplayName("Kiểm tra chuyển sang tab Login")
    public void testOnLoginTabClicked() throws Exception {
        invokeMethod(controller, "onLoginTabClicked");

        assertTrue(loginForm.isVisible());
        assertTrue(loginForm.isManaged());
        assertFalse(registerForm.isVisible());
        assertFalse(registerForm.isManaged());
        assertTrue(loginBtn.isDefaultButton());
        assertFalse(regBtn.isDefaultButton());
    }

    @Test
    @DisplayName("Kiểm tra chuyển sang tab Register")
    public void testOnRegisterTabClicked() throws Exception {
        invokeMethod(controller, "onRegisterTabClicked");

        assertFalse(loginForm.isVisible());
        assertFalse(loginForm.isManaged());
        assertTrue(registerForm.isVisible());
        assertTrue(registerForm.isManaged());
        assertFalse(loginBtn.isDefaultButton());
        assertTrue(regBtn.isDefaultButton());
    }

    @Test
    @DisplayName("Kiểm tra bắt lỗi đăng nhập khi để trống trường")
    public void testOnLoginSubmit_EmptyFields() throws Exception {
        loginUsername.setText("");
        loginPassword.setText("");

        invokeMethod(controller, "onLoginSubmit");

        assertTrue(loginError.isVisible());
        assertEquals("Please enter your username and password!", loginError.getText());
    }

    @Test
    @DisplayName("Kiểm tra Toggle Admin Code hiển thị panel")
    public void testOnToggleAdminCode() throws Exception {
        adminCodeBox.setVisible(false);
        adminCodeBox.setManaged(false);

        invokeMethod(controller, "onToggleAdminCode");

        assertTrue(adminCodeBox.isVisible());
        assertTrue(adminCodeBox.isManaged());
        assertEquals("Hide admin code", toggleAdminCodeLink.getText());

        // Toggle again
        invokeMethod(controller, "onToggleAdminCode");

        assertFalse(adminCodeBox.isVisible());
        assertFalse(adminCodeBox.isManaged());
        assertEquals("Login as admin?", toggleAdminCodeLink.getText());
    }
}
