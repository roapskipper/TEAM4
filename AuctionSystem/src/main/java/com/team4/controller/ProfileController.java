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

        if (name.isEmpty() || email.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Missing Information", "Please enter your name and email");
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

            try {
                String userId = "currentUserId";
                if (com.team4.util.UserSession.getInstance() != null && com.team4.util.UserSession.getInstance().getUsername() != null) {
                    com.team4.model.User currentUser = new com.team4.dao.impl.UserDAOImpl().findByUsername(com.team4.util.UserSession.getInstance().getUsername());
                    if (currentUser != null) {
                        userId = currentUser.getId();
                    }
                }

                com.team4.client.ApiClient apiClient = new com.team4.client.ApiClient();
                apiClient.changePassword(userId, oldPass, newPass);

                com.team4.service.AuthenticationService authService = new com.team4.service.AuthenticationService(new com.team4.dao.impl.UserDAOImpl());
                authService.changePassword(userId, oldPass, newPass);

                showAlert(Alert.AlertType.INFORMATION, "Success", "Password changed successfully!");
                currentPassword.clear();
                newPassword.clear();
                confirmPassword.clear();
            } catch (com.team4.util.BusinessException be) {
                showAlert(Alert.AlertType.ERROR, "Update Failed", be.getMessage());
                return;
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Network Error", "Could not connect to server: " + e.getMessage());
                return;
            }
        }

        showAlert(Alert.AlertType.INFORMATION, "Success", "Your information has been updated!");
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