package com.team4.controller;

import com.google.gson.JsonObject;
import com.team4.client.ApiClient;
import com.team4.util.UserSession;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ResourceBundle;

public class ProfileController implements Initializable {

    @FXML private Label avatarText, displayName, roleBadge, statusBadge, emailDisplay;
    @FXML private Label summaryRole, summaryBalance, summaryJoined, summaryContact, summaryRoleDetail;
    @FXML private TextField editName, editEmail, editPhone;
    @FXML private PasswordField currentPassword, newPassword, confirmPassword;
    @FXML private Button saveBtn, cancelBtn, loginBtn, regBtn;
    @FXML private Label errorLabel, loginError, regError;

    private static final DateTimeFormatter PROFILE_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final NumberFormat MONEY_FORMAT = NumberFormat.getNumberInstance(Locale.US);

    static {
        MONEY_FORMAT.setMaximumFractionDigits(0);
        MONEY_FORMAT.setMinimumFractionDigits(0);
        MONEY_FORMAT.setRoundingMode(RoundingMode.HALF_UP);
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadProfile();
    }

    private void loadProfile() {
        String userId = null;
        if (UserSession.getInstance() != null) {
            userId = UserSession.getInstance().getUserId();
        }
        if (userId == null) {
            return;
        }

        final String finalUserId = userId;
        javafx.concurrent.Task<JsonObject> task = new javafx.concurrent.Task<>() {
            @Override
            protected JsonObject call() throws Exception {
                ApiClient apiClient = new ApiClient();
                return apiClient.getUserProfile(finalUserId);
            }
        };

        task.setOnSucceeded(e -> {
            JsonObject profile = task.getValue();
            if (profile != null && profile.has("id")) {
                String name = jsonString(profile, "fullName", "User Name");
                String email = jsonString(profile, "email", "user@email.com");
                String role = jsonString(profile, "role", "BIDDER");
                String phone = jsonString(profile, "phoneNumber", "");
                
                String initial = name.isEmpty() ? "U" : name.substring(0, 1).toUpperCase();
                UserSession session = UserSession.getInstance();
                if (session != null) {
                    session.setFullName(name);
                }
                
                avatarText.setText(initial);
                displayName.setText(name);
                roleBadge.setText(formatRole(role));
                if (statusBadge != null) {
                    statusBadge.setText("Active");
                }
                emailDisplay.setText(email);

                editName.setText(name);
                editEmail.setText(email);
                editPhone.setText(phone);
                updateAccountSummary(profile, role, email, phone);
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
                if (UserSession.getInstance() != null && UserSession.getInstance().getUserId() != null) {
                    userId = UserSession.getInstance().getUserId();
                }

                if (userId == null) {
                    throw new Exception("User not logged in or invalid session.");
                }

                ApiClient apiClient = new ApiClient();
                
                apiClient.updateProfile(userId, name, email, phone);

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

    private void updateAccountSummary(JsonObject profile, String role, String email, String phone) {
        setText(summaryRole, formatRole(role));

        BigDecimal balance = jsonMoney(profile, "balance", sessionBalance());
        UserSession session = UserSession.getInstance();
        if (session != null) {
            session.setBalance(balance);
        }
        setText(summaryBalance, formatMoney(balance));
        if (summaryBalance != null) {
            summaryBalance.setStyle("-fx-text-fill: #722F37; -fx-font-weight: bold;");
        }
        setText(summaryJoined, formatDateTime(jsonString(profile, "createdAt", "")));
        setText(summaryContact, fallback(phone, email));
        setText(summaryRoleDetail, roleDetail(profile, role));
    }

    private String roleDetail(JsonObject profile, String role) {
        String normalized = role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
        if ("SELLER".equals(normalized)) {
            return "Store: " + fallback(jsonString(profile, "storeName", ""), "Not provided");
        }
        if ("BIDDER".equals(normalized)) {
            return "Shipping: " + fallback(jsonString(profile, "shippingAddress", ""), "Not provided");
        }
        if ("ADMIN".equals(normalized)) {
            int level = jsonInt(profile, "accessLevelCode", 0);
            if (level == 2) {
                return "Access: Super Admin";
            }
            if (level == 1) {
                return "Access: Admin";
            }
            return "Access: Standard";
        }
        return "Account details";
    }

    private String formatRole(String role) {
        if (role == null || role.isBlank()) {
            return "User";
        }
        String raw = role.replace("_", " ").toLowerCase(Locale.ROOT);
        StringBuilder result = new StringBuilder();
        for (String part : raw.split(" ")) {
            if (!part.isBlank()) {
                if (result.length() > 0) {
                    result.append(' ');
                }
                result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
            }
        }
        return result.toString();
    }

    private String formatMoney(BigDecimal amount) {
        return MONEY_FORMAT.format(amount != null ? amount : BigDecimal.ZERO) + " VND";
    }

    private String formatDateTime(String raw) {
        if (raw == null || raw.isBlank()) {
            return "Not available";
        }
        try {
            return LocalDateTime.parse(raw).format(PROFILE_DATE_FORMAT);
        } catch (Exception ignored) {
            return raw.replace('T', ' ');
        }
    }

    private BigDecimal sessionBalance() {
        UserSession session = UserSession.getInstance();
        return session != null && session.getBalance() != null ? session.getBalance() : BigDecimal.ZERO;
    }

    private String jsonString(JsonObject obj, String key, String fallback) {
        return obj != null && obj.has(key) && !obj.get(key).isJsonNull()
                ? obj.get(key).getAsString()
                : fallback;
    }

    private BigDecimal jsonMoney(JsonObject obj, String key, BigDecimal fallback) {
        return obj != null && obj.has(key) && !obj.get(key).isJsonNull()
                ? obj.get(key).getAsBigDecimal()
                : fallback;
    }

    private int jsonInt(JsonObject obj, String key, int fallback) {
        return obj != null && obj.has(key) && !obj.get(key).isJsonNull()
                ? obj.get(key).getAsInt()
                : fallback;
    }

    private String fallback(String primary, String fallback) {
        return primary == null || primary.isBlank() ? fallback : primary;
    }

    private void setText(Label label, String value) {
        if (label != null) {
            label.setText(value);
        }
    }
}
