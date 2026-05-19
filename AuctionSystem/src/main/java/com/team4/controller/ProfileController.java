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
        String userId = null;
        if (com.team4.util.UserSession.getInstance() != null) {
            userId = com.team4.util.UserSession.getInstance().getUserId();
        }
        if (userId == null) {
            return;
        }

        final String finalUserId = userId;
        javafx.concurrent.Task<com.google.gson.JsonObject> task = new javafx.concurrent.Task<>() {
            @Override
            protected com.google.gson.JsonObject call() throws Exception {
                com.team4.client.ApiClient apiClient = new com.team4.client.ApiClient();
                return apiClient.getUserProfile(finalUserId);
            }
        };

        task.setOnSucceeded(e -> {
            com.google.gson.JsonObject profile = task.getValue();
            if (profile != null && profile.has("id")) {
                String name = profile.has("fullName") && !profile.get("fullName").isJsonNull() ? profile.get("fullName").getAsString() : "User Name";
                String email = profile.has("email") && !profile.get("email").isJsonNull() ? profile.get("email").getAsString() : "user@email.com";
                String role = profile.has("role") && !profile.get("role").isJsonNull() ? profile.get("role").getAsString() : "BIDDER";
                String phone = profile.has("phoneNumber") && !profile.get("phoneNumber").isJsonNull() ? profile.get("phoneNumber").getAsString() : "";
                
                String initial = name.isEmpty() ? "U" : name.substring(0, 1).toUpperCase();
                
                avatarText.setText(initial);
                displayName.setText(name);
                roleBadge.setText(role);
                emailDisplay.setText(email);

                editName.setText(name);
                editEmail.setText(email);
                editPhone.setText(phone);
            }
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            System.err.println("Failed to load profile: " + (ex != null ? ex.getMessage() : "Unknown"));
        });

        new Thread(task).start();
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
                String userId = null;
                if (com.team4.util.UserSession.getInstance() != null && com.team4.util.UserSession.getInstance().getUserId() != null) {
                    userId = com.team4.util.UserSession.getInstance().getUserId();
                }

                if (userId == null) {
                    throw new Exception("User not logged in or invalid session.");
                }

                com.team4.client.ApiClient apiClient = new com.team4.client.ApiClient();
                
                // Profile Update
                apiClient.updateProfile(userId, name, email, phone);

                // Password Update
                if (!newPass.isEmpty()) {
                    apiClient.changePassword(userId, oldPass, newPass);
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