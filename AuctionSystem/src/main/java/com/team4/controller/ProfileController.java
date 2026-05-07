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
            showAlert(Alert.AlertType.WARNING, "Thieu thong tin", "Vui long nhap ten va email");
            return;
        }

        if (!newPass.isEmpty()) {
            if (oldPass.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Thieu mat khau", "Vui long nhap mat khau hien tai");
                return;
            }
            if (!newPass.equals(confirmPass)) {
                showAlert(Alert.AlertType.WARNING, "Khong khop", "Mat khau moi va xac nhan khong khop");
                return;
            }
        }

        showAlert(Alert.AlertType.INFORMATION, "Thanh cong", "Thong tin da duoc cap nhat!");
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
