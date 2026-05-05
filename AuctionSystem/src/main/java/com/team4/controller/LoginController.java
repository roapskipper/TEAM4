package com.team4.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import com.team4.client.ApiClient;

public class LoginController {

    @FXML private VBox loginForm;
    @FXML private TextField loginUsername;
    @FXML private PasswordField loginPassword;
    @FXML private Label loginError;

    @FXML private VBox registerForm;
    @FXML private VBox storeNameBox;
    @FXML private TextField regStoreName;
    @FXML private TextField regUsername;
    @FXML private TextField regEmail;
    @FXML private PasswordField regPassword;
    @FXML private PasswordField regConfirmPassword;
    @FXML private Label regError;

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
    }

    @FXML
    private void onRegisterTabClicked() {
        loginForm.setVisible(false);
        loginForm.setManaged(false);
        registerForm.setVisible(true);
        registerForm.setManaged(true);
        hideErrors();
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

    @FXML
    private void onRegisterSubmit() {
        System.out.println("Nút Tạo tài khoản được bấm!");
    }

    @FXML
    private void onRoleChanged() {
        System.out.println("Role đăng ký bị thay đổi!");
    }
}