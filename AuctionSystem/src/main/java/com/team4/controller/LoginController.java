package com.team4.controller;

import com.team4.util.UserSession;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class LoginController {

    @FXML private TextField loginUsername;
    @FXML private PasswordField loginPassword;
    @FXML private Label loginError;
    @FXML private Button loginBtn;
    @FXML private Button loginTab;
    @FXML private Button registerTab;
    @FXML private VBox loginForm;
    @FXML private VBox registerForm;
    @FXML private ToggleButton roleBidder;
    @FXML private ToggleButton roleSeller;
    @FXML private VBox storeNameBox;
    @FXML private TextField regStoreName;
    @FXML private TextField regUsername;
    @FXML private TextField regEmail;
    @FXML private PasswordField regPassword;
    @FXML private PasswordField regConfirmPassword;
    @FXML private Label regError;
    @FXML private Button regBtn;

    private final String ADMIN_ACC = "admin";
    private final String ADMIN_PASS = "admin";
    private final String BIDDER_ACC = "bidder";
    private final String BIDDER_PASS = "bidder";
    private final String SELLER_ACC = "seller";
    private final String SELLER_PASS = "seller";

    @FXML
    public void onLoginSubmit(ActionEvent event) {
        String username = loginUsername.getText();
        String password = loginPassword.getText();

        if (username.equals(ADMIN_ACC) && password.equals(ADMIN_PASS)) {
            UserSession.createSession(username, "ADMIN");
            closeWindow();
        } else if ((username.equals(BIDDER_ACC) && password.equals(BIDDER_PASS)) ||
                (username.equals(SELLER_ACC) && password.equals(SELLER_PASS))) {
            UserSession.createSession(username, "USER");
            closeWindow();
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Lỗi");
            alert.setHeaderText(null);
            alert.setContentText("Sai tài khoản hoặc mật khẩu!");
            alert.showAndWait();
        }
    }

    private void closeWindow() {
        Stage stage = (Stage) loginUsername.getScene().getWindow();
        stage.close();
    }

    @FXML
    public void onLoginTabClicked(ActionEvent event) {
        loginForm.setVisible(true);
        loginForm.setManaged(true);
        registerForm.setVisible(false);
        registerForm.setManaged(false);
    }

    @FXML
    public void onRegisterTabClicked(ActionEvent event) {
        loginForm.setVisible(false);
        loginForm.setManaged(false);
        registerForm.setVisible(true);
        registerForm.setManaged(true);
    }

    @FXML
    public void onRoleChanged(ActionEvent event) {
        if (roleSeller.isSelected()) {
            storeNameBox.setVisible(true);
            storeNameBox.setManaged(true);
        } else {
            storeNameBox.setVisible(false);
            storeNameBox.setManaged(false);
        }
    }

    @FXML
    public void onRegisterSubmit(ActionEvent event) {
    }
}