package com.team4.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

import java.net.URL;
import java.util.ResourceBundle;

public class AdminUsersController implements Initializable {

    @FXML private TextField searchField;
    @FXML private Button filterAll, filterActive, filterLocked;
    @FXML private Label resultCount;
    @FXML private TableView<UserRow> usersTable;
    @FXML private TableColumn<UserRow, String> colUser, colRole, colStatus, colJoined, colAuctions, colAction;

    private ObservableList<UserRow> allUsers = FXCollections.observableArrayList();
    private String currentFilter = "all";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTable();
        loadMockData();
        setupFilters();
    }

    private void setupTable() {
        colUser.setCellValueFactory(new PropertyValueFactory<>("userInfo"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colJoined.setCellValueFactory(new PropertyValueFactory<>("joined"));
        colAuctions.setCellValueFactory(new PropertyValueFactory<>("auctions"));
        colAction.setCellValueFactory(new PropertyValueFactory<>("action"));
    }

    private void loadMockData() {
        allUsers.add(new UserRow("bidder_a", "a@mail.com", "bidder", "active", "2024-01-15", "12"));
        allUsers.add(new UserRow("seller_pro", "seller@mail.com", "seller", "active", "2024-01-10", "8"));
        allUsers.add(new UserRow("spammer_1", "spam@mail.com", "bidder", "locked", "2024-02-01", "0"));
        allUsers.add(new UserRow("art_collector", "art@mail.com", "bidder", "active", "2024-01-20", "5"));
        allUsers.add(new UserRow("tech_store", "tech@mail.com", "seller", "active", "2024-01-05", "25"));
        applyFilter();
    }

    private void setupFilters() {
        searchField.textProperty().addListener((obs, old, val) -> applyFilter());
    }

    @FXML private void onFilterAll() { setFilter("all"); }
    @FXML private void onFilterActive() { setFilter("active"); }
    @FXML private void onFilterLocked() { setFilter("locked"); }

    private void setFilter(String f) {
        currentFilter = f;
        updateFilterButtons();
        applyFilter();
    }

    private void updateFilterButtons() {
        filterAll.getStyleClass().setAll(currentFilter.equals("all") ? "admin-filter-active" : "admin-filter");
        filterActive.getStyleClass().setAll(currentFilter.equals("active") ? "admin-filter-active" : "admin-filter");
        filterLocked.getStyleClass().setAll(currentFilter.equals("locked") ? "admin-filter-active" : "admin-filter");
    }

    private void applyFilter() {
        String search = searchField.getText().toLowerCase();
        ObservableList<UserRow> filtered = allUsers.filtered(u -> {
            boolean matchSearch = u.username.toLowerCase().contains(search);
            boolean matchFilter = currentFilter.equals("all") || u.statusRaw.equals(currentFilter);
            return matchSearch && matchFilter;
        });
        usersTable.setItems(filtered);
        resultCount.setText(filtered.size() + " users");
    }

    public static class UserRow {
        private String username, email, role, statusRaw, joined, auctions;
        public UserRow(String u, String e, String r, String s, String j, String a) {
            username=u; email=e; role=r; statusRaw=s; joined=j; auctions=a;
        }
        public String getUserInfo() { return username + "\n" + email; }
        public String getRole() { return "bidder".equals(role) ? "Buyer" : "Seller"; }
        public String getStatus() { return "active".equals(statusRaw) ? "🟢 Active" : "🔴 Locked"; }
        public String getJoined() { return joined; }
        public String getAuctions() { return auctions; }
        public String getAction() { return "active".equals(statusRaw) ? "🔒 Lock" : "🔓 Unlock"; }
        public String getStatusRaw() { return statusRaw; }
    }
}