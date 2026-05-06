package com.team4.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import com.team4.client.ApiClient;
import javafx.scene.control.ToggleGroup;

public class LoginController {

    @FXML private VBox loginForm;
    @FXML private TextField loginUsername;
    @FXML private PasswordField loginPassword;
    @FXML private Label loginError;

    @FXML private javafx.scene.control.Button loginTab;
    @FXML private javafx.scene.control.Button registerTab;
    @FXML private javafx.scene.control.ToggleButton roleBidder;
    @FXML private javafx.scene.control.ToggleButton roleSeller;
    @FXML private VBox registerForm;
    @FXML private VBox storeNameBox;
    @FXML private TextField regStoreName;
    @FXML private TextField regUsername;
    @FXML private TextField regEmail;
    @FXML private PasswordField regPassword;
    @FXML private PasswordField regConfirmPassword;
    @FXML private Label regError;
    @FXML private javafx.scene.control.Button loginBtn;
    @FXML private javafx.scene.control.Button regBtn;

    @FXML
    private void onLoginSubmit() {
        // Lấy dữ liệu từ field
        String username = loginUsername.getText();
        String password = loginPassword.getText();

        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            showError(loginError, "Vui lòng nhập đầy đủ tài khoản và mật khẩu!");
            return;
        }

        try {
            ApiClient apiClient = new ApiClient();

            String response = apiClient.login(username, password);

            if (response != null) {
                showError(loginError, "Đăng nhập thành công!");
                loginError.setStyle("-fx-text-fill: #10b981;");
                System.out.println("Dữ liệu Server trả về: " + response);
                try {
                    javafx.stage.Stage stage = (javafx.stage.Stage) loginForm.getScene().getWindow();
                    javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/team4/view/seller_products.fxml"));
                    javafx.scene.Parent root = loader.load();

                    javafx.scene.Scene scene = new javafx.scene.Scene(root);
                    stage.setScene(scene);
                    stage.setTitle("Quản lý sản phẩm - AuctionSpace");
                    stage.show();

                } catch (Exception ex) {
                    System.out.println("Lỗi không tìm thấy file FXML để chuyển cảnh: " + ex.getMessage());
                    ex.printStackTrace();
                }

            } else {
                showError(loginError, "Sai tài khoản hoặc mật khẩu!");
                loginError.setStyle("-fx-text-fill: #ef4444;");
            }

        } catch (Exception e) {
            showError(loginError, "Lỗi kết nối Server: " + e.getMessage());
            loginError.setStyle("-fx-text-fill: #ef4444;");
            e.printStackTrace();
        }
    }

    @FXML
    private void onLoginTabClicked() {
        loginForm.setVisible(true);
        loginForm.setManaged(true);
        registerForm.setVisible(false);
        registerForm.setManaged(false);
        hideErrors();
        if (loginTab != null && registerTab != null) {
            loginTab.getStyleClass().removeAll("tab-inactive");
            loginTab.getStyleClass().add("tab-active");
            registerTab.getStyleClass().removeAll("tab-active");
            registerTab.getStyleClass().add("tab-inactive");
        }
        if (loginBtn != null) loginBtn.setDefaultButton(true);
        if (regBtn != null) regBtn.setDefaultButton(false);
    }

    @FXML
    private void onRegisterTabClicked() {
        loginForm.setVisible(false);
        loginForm.setManaged(false);
        registerForm.setVisible(true);
        registerForm.setManaged(true);
        hideErrors();
        if (loginTab != null && registerTab != null) {
            registerTab.getStyleClass().removeAll("tab-inactive");
            registerTab.getStyleClass().add("tab-active");
            loginTab.getStyleClass().removeAll("tab-active");
            loginTab.getStyleClass().add("tab-inactive");
        }
        if (loginBtn != null) loginBtn.setDefaultButton(false);
        if (regBtn != null) regBtn.setDefaultButton(true);
    }

    @FXML
    public void initialize() {
        ToggleGroup roleGroup = new ToggleGroup();

        if (roleBidder != null) {
            roleBidder.setToggleGroup(roleGroup);
            roleBidder.setFocusTraversable(false);
        }
        if (roleSeller != null) {
            roleSeller.setToggleGroup(roleGroup);
            roleSeller.setFocusTraversable(false);
        }

        if (roleBidder != null) {
            roleBidder.setSelected(true);
        }

        roleGroup.selectedToggleProperty().addListener((observable, oldToggle, newToggle) -> {
            if (newToggle == null) {
                if (oldToggle != null) {
                    oldToggle.setSelected(true);
                }
            } else {
                onRoleChanged();
            }
        });

        onRoleChanged();
    }

    @FXML
    private void onRoleChanged() {
        if (roleSeller == null || roleBidder == null) return;

        if (storeNameBox != null) {
            if (roleSeller.isSelected()) {
                storeNameBox.setVisible(true);
                storeNameBox.setManaged(true);
            } else {
                storeNameBox.setVisible(false);
                storeNameBox.setManaged(false);
            }
        }

        String activeStyle = "-fx-border-color: #a855f7; -fx-text-fill: white; -fx-border-radius: 5; -fx-background-color: transparent;";
        String inactiveStyle = "-fx-border-color: #4b5563; -fx-text-fill: #9ca3af; -fx-border-radius: 5; -fx-background-color: transparent;";

        if (roleSeller.isSelected()) {
            roleSeller.setStyle(activeStyle);
            roleBidder.setStyle(inactiveStyle);
        } else {
            roleBidder.setStyle(activeStyle);
            roleSeller.setStyle(inactiveStyle);
        }
    }

    @FXML
    private void onRegisterSubmit() {
        String username = regUsername.getText();
        String email = regEmail.getText();
        String password = regPassword.getText();
        String confirmPass = regConfirmPassword.getText();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPass.isEmpty()) {
            showError(regError, "Vui lòng điền đầy đủ thông tin!");
            return;
        }
        if (!password.equals(confirmPass)) {
            showError(regError, "Mật khẩu xác nhận không khớp!");
            return;
        }

        boolean isSeller = roleSeller.isSelected();
        String storeName = regStoreName.getText();

        if (isSeller && storeName.isEmpty()) {
            showError(regError, "Vui lòng nhập tên cửa hàng!");
            return;
        }

        try {
            ApiClient client = new ApiClient();
            String response;
            if (isSeller) {
                response = client.registerSeller(username, password, username, email, storeName);
            } else {
                response = client.registerBidder(username, password, username, email, "Chưa cập nhật", "09673761411");
            }

            if (response != null) {
                showError(regError, "Đăng ký thành công! Hãy chuyển sang tab Đăng nhập.");
                regError.setStyle("-fx-text-fill: #10b981;");
                loginUsername.setText(username);
                loginPassword.setText(password);

            } else {
                showError(regError, "Đăng ký thất bại! Tên đăng nhập có thể đã tồn tại.");
                regError.setStyle("-fx-text-fill: #ef4444;");
            }

        } catch (Exception e) {
            showError(regError, "Lỗi kết nối Server: " + e.getMessage());
            regError.setStyle("-fx-text-fill: #ef4444;");
            e.printStackTrace();
        }
    }

    private void showError(Label errorLabel, String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }

    private void hideErrors() {
        if (loginError != null) {
            loginError.setVisible(false);
            loginError.setManaged(false);
            loginError.setStyle("-fx-text-fill: #ef4444;");
        }
        if (regError != null) {
            regError.setVisible(false);
            regError.setManaged(false);
        }
    }

}