package com.team4.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import com.team4.client.ApiClient;

public class LoginController {

    @FXML private VBox loginForm;
    @FXML private TextField loginUsername;
    @FXML private PasswordField loginPassword;
    @FXML private VBox adminCodeBox;
    @FXML private PasswordField loginAdminCode;
    @FXML private Hyperlink toggleAdminCodeLink;
    @FXML private Label loginError;

    @FXML private javafx.scene.control.Button loginTab;
    @FXML private javafx.scene.control.Button registerTab;
    @FXML private ToggleButton roleBidder;
    @FXML private ToggleButton roleSeller;
    @FXML private VBox registerForm;
    @FXML private VBox storeNameBox;
    @FXML private TextField regStoreName;
    @FXML private TextField regUsername;
    @FXML private TextField regEmail;
    @FXML private TextField regPhone;
    @FXML private PasswordField regPassword;
    @FXML private PasswordField regConfirmPassword;
    @FXML private Label regError;
    @FXML private javafx.scene.control.Button loginBtn;
    @FXML private javafx.scene.control.Button regBtn;

    private ToggleGroup roleGroup;

    @FXML
    public void initialize() {
        roleGroup = new ToggleGroup();

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

        roleGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null) {
                oldToggle.setSelected(true);
            } else {
                updateRoleStyle();
            }
        });

        updateRoleStyle();
    }

    @FXML
    private void onRoleChanged(ActionEvent event) {
        updateRoleStyle();
    }

    private void updateRoleStyle() {
        if (roleGroup == null || roleBidder == null || roleSeller == null) return;

        Toggle selected = roleGroup.getSelectedToggle();

        roleBidder.getStyleClass().removeAll("role-btn-active", "role-btn-inactive");
        roleSeller.getStyleClass().removeAll("role-btn-active", "role-btn-inactive");

        if (selected == roleBidder) {
            roleBidder.getStyleClass().add("role-btn-active");
            roleSeller.getStyleClass().add("role-btn-inactive");
            if (storeNameBox != null) {
                storeNameBox.setVisible(false);
                storeNameBox.setManaged(false);
            }
        } else {
            roleSeller.getStyleClass().add("role-btn-active");
            roleBidder.getStyleClass().add("role-btn-inactive");
            if (storeNameBox != null) {
                storeNameBox.setVisible(true);
                storeNameBox.setManaged(true);
            }
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
    private void onToggleAdminCode() {
        if (adminCodeBox == null) return;
        boolean show = !adminCodeBox.isVisible();
        adminCodeBox.setVisible(show);
        adminCodeBox.setManaged(show);

        if (show) {
            if (toggleAdminCodeLink != null) toggleAdminCodeLink.setText("Hide admin code");
            if (loginAdminCode != null) loginAdminCode.requestFocus();
        } else {
            if (toggleAdminCodeLink != null) toggleAdminCodeLink.setText("Login as admin?");
            if (loginAdminCode != null) loginAdminCode.clear();
        }
    }

    @FXML
    private void onLoginSubmit() {
        String username = loginUsername.getText();
        String password = loginPassword.getText();
        String adminCode = "";
        if (adminCodeBox != null && adminCodeBox.isVisible()
                && loginAdminCode != null && loginAdminCode.getText() != null) {
            adminCode = loginAdminCode.getText().trim();
        }

        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            showError(loginError, "Please enter your username and password!");
            return;
        }

        try {
            ApiClient apiClient = new ApiClient();
            String response = apiClient.login(username, password, adminCode);
            if (response != null) {
                showError(loginError, "Login successful!");
                loginError.setStyle("-fx-text-fill: #10b981;");
                System.out.println("Server response: " + response);

                com.team4.client.Client socketClient = com.team4.client.Client.getInstance();
                if (socketClient.connect()) {
                    System.out.println("Socket connected successfully!");
                    try {
                        com.google.gson.JsonObject resObj = com.google.gson.JsonParser.parseString(response).getAsJsonObject();
                        String userId = resObj.getAsJsonObject("data").get("userId").getAsString();
                        socketClient.sendLogin(userId);
                        
                        socketClient.setOnForceLogout(() -> {
                            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                            alert.setTitle("Warning");
                            alert.setHeaderText("Forced logout");
                            alert.setContentText("Your account has just signed in from another device. The application will close.");
                            alert.showAndWait();
                            javafx.application.Platform.exit();
                            System.exit(0);
                        });

                        socketClient.startListening(null);
                    } catch (Exception e) {
                        System.out.println("Loi parse userId tu response: " + e.getMessage());
                    }
                } else {
                    System.out.println("Failed to connect to Socket server!");
                }

                try {
                    javafx.stage.Stage stage = (javafx.stage.Stage) loginForm.getScene().getWindow();
                    javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/team4/view/main.fxml"));
                    javafx.scene.Parent root = loader.load();

                    MainController mainController = loader.getController();

                    String role = "bidder";
                    com.google.gson.JsonObject jsonResponse = com.google.gson.JsonParser.parseString(response).getAsJsonObject();
                    if (jsonResponse.has("data")) {
                        com.google.gson.JsonObject data = jsonResponse.getAsJsonObject("data");
                        String r = data.has("role") ? data.get("role").getAsString().toUpperCase() : "";
                        if ("SELLER".equals(r)) role = "seller";
                        else if ("ADMIN".equals(r)) {
                            boolean isSuperAdmin = data.has("accessLevel") && data.get("accessLevel").getAsInt() == 2;
                            role = isSuperAdmin ? "admin_super" : "admin_regular";
                        }
                        
                        String userId = data.has("userId") ? data.get("userId").getAsString() : null;
                        String uName = data.has("username") ? data.get("username").getAsString() : username;
                        if (userId != null) {
                            java.math.BigDecimal balance = data.has("balance") && !data.get("balance").isJsonNull()
                                    ? data.get("balance").getAsBigDecimal()
                                    : java.math.BigDecimal.ZERO;
                            com.team4.util.UserSession.createSession(userId, uName, role, balance);
                        }
                    }

                    mainController.setUserRole(role);

                    javafx.scene.Scene scene = new javafx.scene.Scene(root);
                    String cssPath = getClass().getResource("/com/team4/view/style.css").toExternalForm();
                    scene.getStylesheets().add(cssPath);

                    stage.setScene(scene);
                    stage.setTitle("Dashboard - AuctionSpace");
                    stage.setWidth(1200);
                    stage.setHeight(800);
                    stage.centerOnScreen();
                    stage.show();

                } catch (Exception ex) {
                    System.out.println("Scene transition error: " + ex.getMessage());
                    ex.printStackTrace();
                }

            } else {
                showError(loginError, "Invalid username or password!");
                loginError.setStyle("-fx-text-fill: #ef4444;");
            }

        } catch (Exception e) {
            showError(loginError, "Login failed. " + ApiClient.toDisplayMessage(e));
            loginError.setStyle("-fx-text-fill: #ef4444;");
            e.printStackTrace();
        }
    }

    @FXML
    private void onTermsClicked() {
        try {
            java.awt.Desktop.getDesktop().browse(
                    new java.net.URI("https://docs.google.com/document/d/1NeyQwm6vGFmt-QHZG8rcC1JXk22AHrWb7CUSLj8QLqM/edit?usp=sharing")
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onRegisterSubmit() {
        String username = regUsername.getText();
        String email = regEmail.getText();
        String phone = regPhone.getText() == null ? "" : regPhone.getText().trim();
        String password = regPassword.getText();
        String confirmPass = regConfirmPassword.getText();

        if (username.isEmpty() || email.isEmpty() || phone.isEmpty()
                || password.isEmpty() || confirmPass.isEmpty()) {
            showError(regError, "Please fill in all required fields!");
            return;
        }
        if (!phone.matches("^(0\\d{9}|\\+84\\d{9})$")) {
            showError(regError, "Invalid phone number! Use format 0xxxxxxxxx or +84xxxxxxxxx.");
            return;
        }
        if (!password.equals(confirmPass)) {
            showError(regError, "Passwords do not match!");
            return;
        }

        boolean isSeller = roleSeller.isSelected();
        String storeName = regStoreName.getText();

        if (isSeller && storeName.isEmpty()) {
            showError(regError, "Please enter your store name!");
            return;
        }

        try {
            ApiClient client = new ApiClient();
            String response;
            if (isSeller) {
                response = client.registerSeller(username, password, username, email, storeName);
            } else {
                response = client.registerBidder(username, password, username, email, "Not updated", phone);
            }

            if (response != null) {
                showError(regError, "Registration successful! Please switch to the Login tab.");
                regError.setStyle("-fx-text-fill: #10b981;");
                loginUsername.setText(username);
                loginPassword.setText(password);
            } else {
                showError(regError, "Registration failed! Username may already exist.");
                regError.setStyle("-fx-text-fill: #ef4444;");
            }

        } catch (Exception e) {
            showError(regError, "Registration failed. " + ApiClient.toDisplayMessage(e));
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
