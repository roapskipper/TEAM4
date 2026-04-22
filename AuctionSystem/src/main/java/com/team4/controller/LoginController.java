package com.team4.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.io.IOException;

public class LoginController {

    @FXML private VBox loginForm, registerForm;
    @FXML private Button loginTab, registerTab;
    @FXML private TextField usernameField, registerUsernameField;
    @FXML private PasswordField passwordField, registerPasswordField;
    @FXML private Label errorLabel;

    @FXML
    public void handleLoginTab(ActionEvent event) {
        registerForm.setVisible(false);
        registerForm.setManaged(false);
        loginForm.setVisible(true);
        loginForm.setManaged(true);

        loginTab.setStyle("-fx-background-color: linear-gradient(to right, #8b5cf6, #ec4899); -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10;");
        registerTab.setStyle("-fx-background-color: transparent; -fx-text-fill: #9ca3af; -fx-font-weight: bold; -fx-background-radius: 10;");
    }

    @FXML
    public void handleRegisterTab(ActionEvent event) {
        loginForm.setVisible(false);
        loginForm.setManaged(false);
        registerForm.setVisible(true);
        registerForm.setManaged(true);

        registerTab.setStyle("-fx-background-color: linear-gradient(to right, #8b5cf6, #ec4899); -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10;");
        loginTab.setStyle("-fx-background-color: transparent; -fx-text-fill: #9ca3af; -fx-font-weight: bold; -fx-background-radius: 10;");
    }

    @FXML
    private void handleLogin() {
        String user = usernameField.getText();
        String pass = passwordField.getText();

        String jsonResponse = mockSocketServer("LOGIN", user, pass);

        Gson gson = new Gson();
        JsonObject response = gson.fromJson(jsonResponse, JsonObject.class);

        if (response.get("status").getAsString().equals("SUCCESS")) {
            switchToDashboard();
        } else {
            errorLabel.setText(response.get("message").getAsString());
            errorLabel.setStyle("-fx-text-fill: #ef4444;");
            errorLabel.setVisible(true);
        }
    }

    @FXML
    private void handleRegister() {
        String newUser = registerUsernameField.getText();
        String newPass = registerPasswordField.getText();

        if (newUser.isEmpty() || newPass.isEmpty()) {
            errorLabel.setText("Vui lòng không để trống thông tin đăng ký!");
            errorLabel.setStyle("-fx-text-fill: #ef4444;");
            errorLabel.setVisible(true);
            return;
        }

        System.out.println("Giả lập đăng ký: " + newUser);
        errorLabel.setText("Mô phỏng Đăng ký thành công!");
        errorLabel.setStyle("-fx-text-fill: #10b981;");
        errorLabel.setVisible(true);
    }

    private String mockSocketServer(String action, String u, String p) {
        JsonObject response = new JsonObject();

        if (action.equals("LOGIN")) {
            if (u.equals("tester_bidder") && p.equals("123456")) {
                response.addProperty("status", "SUCCESS");
                response.addProperty("message", "Đăng nhập thành công!");

                JsonObject data = new JsonObject();
                data.addProperty("username", u);
                response.add("data", data);
            } else {
                response.addProperty("status", "ERROR");
                response.addProperty("message", "Tài khoản hoặc mật khẩu không chính xác.");
            }
        }
        return new Gson().toJson(response);
    }

    private void switchToDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/team4/view/main.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("AuctionSpace - Trang chủ");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}