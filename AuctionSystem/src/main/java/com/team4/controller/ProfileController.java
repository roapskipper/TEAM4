package com.team4.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;

public class ProfileController implements Initializable {

    @FXML private Label avatarText, displayName, roleBadge, statusBadge, emailDisplay;
    @FXML private TextField editName, editEmail, editPhone;
    @FXML private PasswordField currentPassword, newPassword, confirmPassword;
    @FXML private Button saveBtn, cancelBtn, loginBtn, regBtn;
    @FXML private Label errorLabel, loginError, regError;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadProfile();
    }

    private void loadProfile() {
        avatarText.setText("U");
        displayName.setText("User Name");
        roleBadge.setText("BIDDER");
        emailDisplay.setText("user@email.com");

        editName.setText("User Name");
        editEmail.setText("user@email.com");
        editPhone.setText("0901234567");
    }

    @FXML private void onSaveProfile() {
        String name = editName.getText().trim();
        String email = editEmail.getText().trim();
        String phone = editPhone.getText().trim();
        String oldPass = currentPassword.getText();
        String newPass = newPassword.getText();
        String confirmPass = confirmPassword.getText();

        if (errorLabel != null) errorLabel.setText("");
        if (loginError != null) loginError.setText("");
        if (regError != null) regError.setText("");

        if (name.isEmpty() || email.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Missing Information", "Please enter your name and email");
            return;
        }

        if (!phone.isEmpty() && !phone.matches("^(0\\d{9}|\\+84\\d{9})$")) {
            showAlert(Alert.AlertType.WARNING, "Invalid Phone", "Phone number must be 10 digits starting with 0 or +84.");
            return;
        }

        if (!newPass.isEmpty()) {
            if (oldPass.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Missing Password", "Please enter your current password");
                return;
            }
            if (!newPass.equals(confirmPass)) {
                showAlert(Alert.AlertType.WARNING, "Mismatch", "New password and confirmation do not match");
                return;
            }
        }

        if (saveBtn != null) saveBtn.setDisable(true);
        if (cancelBtn != null) cancelBtn.setDisable(true);
        if (loginBtn != null) loginBtn.setDisable(true);
        if (regBtn != null) regBtn.setDisable(true);
        String originalSaveText = (saveBtn != null) ? saveBtn.getText() : "Save Changes";
        if (saveBtn != null) saveBtn.setText("Processing...");

        javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<Void>() {
            @Override
            protected Void call() throws Exception {
                String userId = "currentUserId";
                if (com.team4.util.UserSession.getInstance() != null && com.team4.util.UserSession.getInstance().getUsername() != null) {
                    com.team4.model.User currentUser = new com.team4.dao.impl.UserDAOImpl().findByUsername(com.team4.util.UserSession.getInstance().getUsername());
                    if (currentUser != null) {
                        userId = currentUser.getId();
                    }
                }

                com.team4.client.ApiClient apiClient = new com.team4.client.ApiClient();
                
                // Profile Update
                apiClient.updateProfile(userId, name, email, phone);
                com.team4.service.UserService userService = new com.team4.service.UserService(new com.team4.dao.impl.UserDAOImpl());
                userService.updateProfile(userId, name, email, phone);

                // Password Update
                if (!newPass.isEmpty()) {
                    apiClient.changePassword(userId, oldPass, newPass);
                    com.team4.service.AuthenticationService authService = new com.team4.service.AuthenticationService(new com.team4.dao.impl.UserDAOImpl());
                    authService.changePassword(userId, oldPass, newPass);
                }
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            if (saveBtn != null) {
                saveBtn.setDisable(false);
                saveBtn.setText(originalSaveText);
            }
            if (cancelBtn != null) cancelBtn.setDisable(false);
            if (loginBtn != null) loginBtn.setDisable(false);
            if (regBtn != null) regBtn.setDisable(false);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText(null);
            alert.setContentText(newPass.isEmpty() ? "Profile updated successfully!" : "Profile and password updated successfully!");
            alert.show();

            javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2));
            delay.setOnFinished(ev -> {
                alert.close();
                currentPassword.clear();
                newPassword.clear();
                confirmPassword.clear();
                // Requirement 6: Reload profile data after successful update
                loadProfile(); 
            });
            delay.play();
        });

        task.setOnFailed(e -> {
            if (saveBtn != null) {
                saveBtn.setDisable(false);
                saveBtn.setText(originalSaveText);
            }
            if (cancelBtn != null) cancelBtn.setDisable(false);
            if (loginBtn != null) loginBtn.setDisable(false);
            if (regBtn != null) regBtn.setDisable(false);

            Throwable ex = task.getException();
            if (ex instanceof com.team4.util.BusinessException) {
                showAlert(Alert.AlertType.ERROR, "Update Failed", ex.getMessage());
            } else {
                showAlert(Alert.AlertType.ERROR, "Network Error", "Could not connect to server: " + (ex != null ? ex.getMessage() : "Unknown"));
            }
        });

        new Thread(task).start();
    }

    @FXML private void onCancel() {
        loadProfile();
        currentPassword.clear();
        newPassword.clear();
        confirmPassword.clear();
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}