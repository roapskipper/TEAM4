package com.team4.controller;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.team4.client.ApiClient;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

public class AdminUsersController implements Initializable {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> roleFilter;
    @FXML private Label resultCount;
    @FXML private TableView<UserRow> usersTable;
    @FXML private TableColumn<UserRow, String> colUsername, colFullName, colRole, colEmail, colJoinDate, colStatus;
    @FXML private TableColumn<UserRow, Void> colAction;

    private ObservableList<UserRow> allUsers = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTable();
        setupFilters();
        loadRealData();
    }

    private void setupTable() {
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colFullName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colJoinDate.setCellValueFactory(new PropertyValueFactory<>("joinDate"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        colStatus.setCellFactory(param -> new TableCell<UserRow, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    String status = item.toUpperCase();
                    Label statusLabel = new Label(status);
                    statusLabel.setStyle("-fx-font-weight: bold;");

                    if ("ACTIVE".equals(status)) statusLabel.setStyle(statusLabel.getStyle() + " -fx-text-fill: #10b981;");
                    else if ("SUSPENDED".equals(status)) statusLabel.setStyle(statusLabel.getStyle() + " -fx-text-fill: #f59e0b;");
                    else if ("BANNED".equals(status)) statusLabel.setStyle(statusLabel.getStyle() + " -fx-text-fill: #ef4444;");

                    setGraphic(statusLabel);
                    setText(null);
                }
            }
        });

        colAction.setCellFactory(param -> new TableCell<UserRow, Void>() {
            private final Button suspendBtn = new Button("Suspend");
            private final Button banBtn = new Button("Ban");
            private final Button unsuspendBtn = new Button("Unsuspend");
            private final HBox actionBox = new HBox(10);

            {
                actionBox.setAlignment(Pos.CENTER_LEFT);
                suspendBtn.setStyle("-fx-font-size: 11px;");
                banBtn.setStyle("-fx-font-size: 11px; -fx-text-fill: #ef4444;");
                unsuspendBtn.setStyle("-fx-font-size: 11px; -fx-text-fill: #10b981;");
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    UserRow user = getTableRow().getItem();
                    String status = user.getStatus() != null ? user.getStatus().toUpperCase() : "ACTIVE";
                    actionBox.getChildren().clear();

                    if ("ACTIVE".equals(status)) {
                        suspendBtn.setOnAction(e -> handleSuspend(user));
                        banBtn.setOnAction(e -> handleBan(user));
                        actionBox.getChildren().addAll(suspendBtn, banBtn);
                    } else if ("SUSPENDED".equals(status)) {
                        unsuspendBtn.setOnAction(e -> handleUnsuspend(user));
                        banBtn.setOnAction(e -> handleBan(user));
                        actionBox.getChildren().addAll(unsuspendBtn, banBtn);
                    }

                    setGraphic(actionBox);
                    setText(null);
                }
            }
        });
    }

    private void loadRealData() {
        usersTable.setPlaceholder(new Label("Loading users..."));
        Task<JsonArray> task = new Task<>() {
            @Override
            protected JsonArray call() throws Exception {
                ApiClient apiClient = new ApiClient();
                return apiClient.getAllUsers();
            }
        };

        task.setOnSucceeded(e -> {
            allUsers.clear();
            JsonArray array = task.getValue();

            for (JsonElement el : array) {
                JsonObject obj = el.getAsJsonObject();
                String id = obj.has("id") ? obj.get("id").getAsString() : "";
                String username = obj.has("username") ? obj.get("username").getAsString() : "";
                String fullName = obj.has("fullName") ? obj.get("fullName").getAsString() : "";
                String role = obj.has("role") ? obj.get("role").getAsString() : "BIDDER";
                String email = obj.has("email") ? obj.get("email").getAsString() : "";
                String joinDate = obj.has("createdAt") ? obj.get("createdAt").getAsString() : "";
                String status = obj.has("status") ? obj.get("status").getAsString() : "ACTIVE";
                
                allUsers.add(new UserRow(id, username, fullName, role, email, joinDate, status));
            }
            
            if (allUsers.isEmpty()) {
                usersTable.setPlaceholder(new Label("No users found."));
            }
            applyFilter();
        });

        task.setOnFailed(e -> {
            allUsers.clear();
            usersTable.setPlaceholder(new Label("Failed to load: " + task.getException().getMessage()));
            applyFilter();
        });

        new Thread(task).start();
    }

    private void setupFilters() {
        roleFilter.setItems(FXCollections.observableArrayList("All Users", "Bidders Only", "Sellers Only", "Admins Only"));
        roleFilter.setValue("All Users");
        roleFilter.valueProperty().addListener((obs, old, val) -> applyFilter());
        searchField.textProperty().addListener((obs, old, val) -> applyFilter());
    }

    private void applyFilter() {
        String search = searchField.getText().toLowerCase().trim();
        String roleStr = roleFilter.getValue();
        
        ObservableList<UserRow> filtered = allUsers.filtered(u -> {
            boolean matchSearch = u.getUsername().toLowerCase().contains(search) || 
                                  u.getEmail().toLowerCase().contains(search);
            boolean matchFilter = true;
            if ("Bidders Only".equals(roleStr)) {
                matchFilter = "BIDDER".equalsIgnoreCase(u.getRole());
            } else if ("Sellers Only".equals(roleStr)) {
                matchFilter = "SELLER".equalsIgnoreCase(u.getRole());
            } else if ("Admins Only".equals(roleStr)) {
                matchFilter = "ADMIN".equalsIgnoreCase(u.getRole());
            }
            return matchSearch && matchFilter;
        });
        usersTable.setItems(filtered);
        resultCount.setText(filtered.size() + " users");
    }

    private void handleSuspend(UserRow user) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Suspend User");
        dialog.setHeaderText("Suspend User: " + user.getUsername());
        dialog.setContentText("Reason (optional):");
        
        dialog.showAndWait().ifPresent(reason -> {
            disableButtonsAndCallApi(() -> {
                ApiClient apiClient = new ApiClient();
                return apiClient.suspendUser(user.getUserId(), reason);
            }, "User suspended successfully!");
        });
    }

    private void handleBan(UserRow user) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Ban User");
        dialog.setHeaderText("Ban User: " + user.getUsername());
        dialog.setContentText("Reason (mandatory):");
        
        dialog.showAndWait().ifPresent(reason -> {
            if (reason.trim().isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Error", "Ban reason is mandatory.");
                return;
            }
            disableButtonsAndCallApi(() -> {
                ApiClient apiClient = new ApiClient();
                return apiClient.banUser(user.getUserId(), reason);
            }, "User banned successfully!");
        });
    }

    private void handleUnsuspend(UserRow user) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to unsuspend " + user.getUsername() + "?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                disableButtonsAndCallApi(() -> {
                    ApiClient apiClient = new ApiClient();
                    return apiClient.unsuspendUser(user.getUserId());
                }, "User unsuspended successfully!");
            }
        });
    }

    private void disableButtonsAndCallApi(Callable<String> apiCall, String successMsg) {
        usersTable.setDisable(true);
        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return apiCall.call();
            }
        };

        task.setOnSucceeded(e -> {
            usersTable.setDisable(false);
            showAlert(Alert.AlertType.INFORMATION, "Success", successMsg);
            loadRealData();
        });

        task.setOnFailed(e -> {
            usersTable.setDisable(false);
            showAlert(Alert.AlertType.ERROR, "Error", "Action failed: " + task.getException().getMessage());
        });

        new Thread(task).start();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type, message);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.show();
    }

    public static class UserRow {
        private String userId, username, fullName, role, email, joinDate, status;
        public UserRow(String userId, String username, String fullName, String role, String email, String joinDate, String status) {
            this.userId = userId;
            this.username = username;
            this.fullName = fullName;
            this.role = role;
            this.email = email;
            this.joinDate = joinDate;
            this.status = status;
        }
        public String getUserId() { return userId; }
        public String getUsername() { return username; }
        public String getFullName() { return fullName; }
        public String getRole() { return role; }
        public String getEmail() { return email; }
        public String getJoinDate() { return joinDate; }
        public String getStatus() { return status; }
    }
}
