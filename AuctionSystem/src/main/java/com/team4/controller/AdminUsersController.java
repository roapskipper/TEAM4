package com.team4.controller;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.team4.client.ApiClient;
import com.team4.util.UserSession;

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
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class AdminUsersController implements Initializable {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> roleFilter;
    @FXML private Label resultCount;
    @FXML private TableView<UserRow> usersTable;
    @FXML private TableColumn<UserRow, String> colUsername, colFullName, colRole, colEmail, colJoinDate;
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

        colAction.setCellFactory(param -> new TableCell<UserRow, Void>() {
            private final Button grantBtn = new Button("Grant Admin");
            private final Button revokeBtn = new Button("Remove Admin");
            private final HBox actionBox = new HBox(6);

            {
                actionBox.setAlignment(Pos.CENTER_LEFT);
                styleActionButton(grantBtn, "admin-action-grant");
                styleActionButton(revokeBtn, "admin-action-revoke");
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    UserRow user = getTableRow().getItem();
                    actionBox.getChildren().clear();

                    boolean self = currentUserId().equals(user.getUserId());
                    if (self) {
                        Label current = new Label("Current account");
                        current.getStyleClass().add("muted-text-sm");
                        actionBox.getChildren().add(current);
                    } else {
                        boolean adminRole = "ADMIN".equalsIgnoreCase(user.getRole()) || "MODERATOR".equalsIgnoreCase(user.getRole());
                        if (canGrantAdmin() && adminRole && user.getAccessLevelCode() != 2) {
                            revokeBtn.setOnAction(e -> handleRevokeAdmin(user));
                            actionBox.getChildren().add(revokeBtn);
                        } else if (canGrantAdmin() && !adminRole && !"SUPER_ADMIN".equalsIgnoreCase(user.getRole())) {
                            grantBtn.setOnAction(e -> handleGrantAdmin(user));
                            actionBox.getChildren().add(grantBtn);
                        }
                    }

                    setGraphic(actionBox);
                    setText(null);
                }
            }
        });
    }

    private void styleActionButton(Button button, String styleClass) {
        button.getStyleClass().setAll("admin-action-btn", styleClass);
        button.setMinWidth(Region.USE_PREF_SIZE);
    }

    private void loadRealData() {
        usersTable.setPlaceholder(new Label("Loading users..."));
        Task<JsonArray> task = new Task<>() {
            @Override
            protected JsonArray call() throws Exception {
                ApiClient apiClient = new ApiClient();
                return apiClient.getAllUsers(currentUserId());
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
                int accessLevelCode = obj.has("accessLevelCode") ? obj.get("accessLevelCode").getAsInt() : 0;
                
                if ("ADMIN".equalsIgnoreCase(role)) {
                    if (accessLevelCode == 1) {
                        role = "MODERATOR";
                    } else if (accessLevelCode == 2) {
                        role = "SUPER_ADMIN";
                    }
                }
                
                allUsers.add(new UserRow(id, username, fullName, role, email, joinDate, accessLevelCode));
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
        if (canGrantAdmin()) {
            roleFilter.setItems(FXCollections.observableArrayList("All Users", "Admins Only", "Sellers Only", "Bidders Only"));
        } else {
            roleFilter.setItems(FXCollections.observableArrayList("All Users", "Sellers Only", "Bidders Only"));
        }
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
                matchFilter = "ADMIN".equalsIgnoreCase(u.getRole()) || 
                              "MODERATOR".equalsIgnoreCase(u.getRole()) || 
                              "SUPER_ADMIN".equalsIgnoreCase(u.getRole());
            }
            return matchSearch && matchFilter;
        });
        usersTable.setItems(filtered);
        resultCount.setText(filtered.size() + " users");
    }



    private void handleRevokeAdmin(UserRow user) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Remove admin role from " + user.getUsername() + "?",
                ButtonType.YES,
                ButtonType.NO);
        confirm.setTitle("Remove Admin");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                disableButtonsAndCallApi(() -> {
                    ApiClient apiClient = new ApiClient();
                    return apiClient.revokeAdmin(user.getUserId(), currentUserId());
                }, "Admin privileges removed successfully!");
            }
        });
    }

    private void handleGrantAdmin(UserRow user) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Grant Admin");
        dialog.setHeaderText("Grant admin privileges to " + user.getUsername());
        ButtonType grantButtonType = new ButtonType("Grant", ButtonType.OK.getButtonData());
        dialog.getDialogPane().getButtonTypes().addAll(grantButtonType, ButtonType.CANCEL);

        PasswordField adminCodeField = new PasswordField();
        adminCodeField.setPromptText("New admin code");
        adminCodeField.getStyleClass().add("input-field");
        VBox content = new VBox(8, new Label("Set an admin code for this account"), adminCodeField);
        dialog.getDialogPane().setContent(content);
        dialog.setResultConverter(button -> button == grantButtonType ? adminCodeField.getText() : null);

        dialog.showAndWait().ifPresent(adminCode -> {
            if (adminCode == null || adminCode.trim().length() < 8) {
                showAlert(Alert.AlertType.ERROR, "Error", "Admin code must be at least 8 characters long.");
                return;
            }
            disableButtonsAndCallApi(() -> {
                ApiClient apiClient = new ApiClient();
                return apiClient.grantAdmin(user.getUserId(), currentUserId(), adminCode.trim());
            }, "Admin privileges granted successfully!");
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

    private boolean canGrantAdmin() {
        UserSession session = UserSession.getInstance();
        return session != null && "admin_super".equals(session.getRole());
    }

    private String currentUserId() {
        UserSession session = UserSession.getInstance();
        return session != null && session.getUserId() != null ? session.getUserId() : "";
    }

    public static class UserRow {
        private String userId, username, fullName, role, email, joinDate;
        private int accessLevelCode;
        public UserRow(String userId, String username, String fullName, String role, String email, String joinDate, int accessLevelCode) {
            this.userId = userId;
            this.username = username;
            this.fullName = fullName;
            this.role = role;
            this.email = email;
            this.joinDate = joinDate;
            this.accessLevelCode = accessLevelCode;
        }
        public String getUserId() { return userId; }
        public String getUsername() { return username; }
        public String getFullName() { return fullName; }
        public String getRole() { return role; }
        public String getEmail() { return email; }
        public String getJoinDate() { return joinDate; }
        public int getAccessLevelCode() { return accessLevelCode; }
    }
}
