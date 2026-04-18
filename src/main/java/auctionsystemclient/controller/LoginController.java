package auctionsystemclient.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

public class LoginController {

    @FXML private Button loginTab;
    @FXML private Button registerTab;
    @FXML private VBox loginForm;
    @FXML private VBox registerForm;

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    @FXML private ToggleButton bidderRole;
    @FXML private ToggleButton sellerRole;
    @FXML private TextField regUsernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField regPasswordField;
    @FXML private PasswordField confirmPasswordField;

    @FXML private VBox storeNameContainer;
    @FXML private TextField storeNameField;

    @FXML
    public void initialize() {
        ToggleGroup roleGroup = new ToggleGroup();
        bidderRole.setToggleGroup(roleGroup);
        sellerRole.setToggleGroup(roleGroup);
        roleGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == sellerRole) {
                storeNameContainer.setVisible(true);
                storeNameContainer.setManaged(true);
                sellerRole.setStyle("-fx-background-color: rgba(139, 92, 246, 0.2); -fx-border-color: #8b5cf6; -fx-border-radius: 10; -fx-background-radius: 10; -fx-text-fill: white; -fx-padding: 15; -fx-cursor: hand;");
                bidderRole.setStyle("-fx-background-color: transparent; -fx-border-color: #374151; -fx-border-radius: 10; -fx-background-radius: 10; -fx-text-fill: #9ca3af; -fx-padding: 15; -fx-cursor: hand;");
            } else {
                storeNameContainer.setVisible(false);
                storeNameContainer.setManaged(false);
                bidderRole.setStyle("-fx-background-color: rgba(139, 92, 246, 0.2); -fx-border-color: #8b5cf6; -fx-border-radius: 10; -fx-background-radius: 10; -fx-text-fill: white; -fx-padding: 15; -fx-cursor: hand;");
                sellerRole.setStyle("-fx-background-color: transparent; -fx-border-color: #374151; -fx-border-radius: 10; -fx-background-radius: 10; -fx-text-fill: #9ca3af; -fx-padding: 15; -fx-cursor: hand;");
            }
        });
    }

    @FXML
    public void handleLoginTab(ActionEvent event) {
        registerForm.setVisible(false);
        registerForm.setManaged(false);
        loginForm.setVisible(true);
        loginForm.setManaged(true);

        loginTab.setStyle("-fx-background-color: linear-gradient(to right, #8b5cf6, #ec4899); -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 12 30;");
        registerTab.setStyle("-fx-background-color: transparent; -fx-text-fill: #9ca3af; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 12 30;");
    }

    @FXML
    public void handleRegisterTab(ActionEvent event) {
        loginForm.setVisible(false);
        loginForm.setManaged(false);
        registerForm.setVisible(true);
        registerForm.setManaged(true);

        registerTab.setStyle("-fx-background-color: linear-gradient(to right, #8b5cf6, #ec4899); -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 12 30;");
        loginTab.setStyle("-fx-background-color: transparent; -fx-text-fill: #9ca3af; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 12 30;");
    }

    @FXML
    public void handleLogin(ActionEvent event) {
        System.out.println("--- GỬI YÊU CẦU ĐĂNG NHẬP ---");
        System.out.println("Tài khoản: " + usernameField.getText());
        System.out.println("Mật khẩu: " + passwordField.getText());
    }

    @FXML
    public void handleRegister(ActionEvent event) {
        System.out.println("--- GỬI YÊU CẦU ĐĂNG KÝ ---");
        System.out.println("Tài khoản: " + regUsernameField.getText());
        System.out.println("Email: " + emailField.getText());
        System.out.println("Vai trò: " + (sellerRole.isSelected() ? "Người bán" : "Người mua"));
        if(sellerRole.isSelected()) {
            System.out.println("Tên cửa hàng: " + storeNameField.getText());
        }
    }
}