package com.team4.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import com.team4.client.ApiClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.concurrent.Task;

import java.net.URL;
import java.util.ResourceBundle;

public class AdminUsersController implements Initializable {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> roleFilter;
    @FXML private Label resultCount;
    @FXML private TableView<UserRow> usersTable;
    @FXML private TableColumn<UserRow, String> colUsername, colFullName, colRole, colEmail, colJoinDate, colStatus;

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