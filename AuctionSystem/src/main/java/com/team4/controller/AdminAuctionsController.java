package com.team4.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.util.ResourceBundle;

public class AdminAuctionsController implements Initializable {

    @FXML private TextField searchField;
    @FXML private Button filterAll, filterPending, filterLive, filterViolated;
    @FXML private Label resultCount;
    @FXML private TableView<AuctionRow> auctionsTable;
    @FXML private TableColumn<AuctionRow, String> colItem, colSeller, colStartPrice, colStatus, colReports, colAction;

    private ObservableList<AuctionRow> allAuctions = FXCollections.observableArrayList();
    private String currentFilter = "all";

    public void initialize(URL url, ResourceBundle rb) {
        setupTable();
        loadDataFromServer();
        setupFilters();
    }

    private void setupTable() {
        colItem.setCellValueFactory(new PropertyValueFactory<>("itemInfo"));
        colSeller.setCellValueFactory(new PropertyValueFactory<>("seller"));
        colStartPrice.setCellValueFactory(new PropertyValueFactory<>("startPrice"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colReports.setCellValueFactory(new PropertyValueFactory<>("reports"));
        colAction.setCellValueFactory(new PropertyValueFactory<>("action"));
    }

    private void loadDataFromServer() {
        auctionsTable.setPlaceholder(new Label("Loading..."));
        javafx.concurrent.Task<com.google.gson.JsonArray> task = new javafx.concurrent.Task<>() {
            @Override
            protected com.google.gson.JsonArray call() throws Exception {
                com.team4.client.ApiClient apiClient = new com.team4.client.ApiClient();
                return apiClient.getAuctions(currentFilter);
            }
        };

        task.setOnSucceeded(e -> {
            allAuctions.clear();
            com.google.gson.JsonArray array = task.getValue();
            java.text.NumberFormat formatter = java.text.NumberFormat.getInstance(java.util.Locale.US);

            for (com.google.gson.JsonElement el : array) {
                com.google.gson.JsonObject obj = el.getAsJsonObject();
                String id = obj.has("id") ? obj.get("id").getAsString() : "";
                String name = obj.has("itemName") ? obj.get("itemName").getAsString() : "";
                String seller = obj.has("sellerName") ? obj.get("sellerName").getAsString() : "";
                
                String startPrice = "0";
                if (obj.has("startPrice")) {
                    double price = obj.get("startPrice").getAsDouble();
                    startPrice = formatter.format(price);
                }
                
                String status = obj.has("status") ? obj.get("status").getAsString() : "PENDING_APPROVAL";
                String reportCount = obj.has("reportCount") ? obj.get("reportCount").getAsString() : "0";
                
                allAuctions.add(new AuctionRow(id, name, seller, startPrice, status, reportCount, "Action"));
            }
            
            if (allAuctions.isEmpty()) {
                auctionsTable.setPlaceholder(new Label("No auctions found."));
            }
            applyFilter();
        });

        task.setOnFailed(e -> {
            allAuctions.clear();
            auctionsTable.setPlaceholder(new Label("Failed to load: " + task.getException().getMessage()));
            applyFilter();
            Alert alert = new Alert(Alert.AlertType.ERROR, "API Failure: Could not load auctions. " + task.getException().getMessage());
            alert.show();
        });

        new Thread(task).start();
    }

    private void setupFilters() {
        searchField.textProperty().addListener((obs, old, val) -> applyFilter());
    }

    @FXML private void onFilterAll() { setFilter("all"); }
    @FXML private void onFilterPending() { setFilter("pending"); }
    @FXML private void onFilterLive() { setFilter("live"); }
    @FXML private void onFilterViolated() { setFilter("rejected"); }

    private void setFilter(String f) {
        currentFilter = f;
        updateFilterButtons();
        loadDataFromServer();
    }

    private void updateFilterButtons() {
        filterAll.getStyleClass().setAll("admin-filter-active".equals(getFilterStyle("all")) ? "admin-filter-active" : "admin-filter");
        filterPending.getStyleClass().setAll("admin-filter-active".equals(getFilterStyle("pending")) ? "admin-filter-active" : "admin-filter");
        filterLive.getStyleClass().setAll("admin-filter-active".equals(getFilterStyle("live")) ? "admin-filter-active" : "admin-filter");
        filterViolated.getStyleClass().setAll("admin-filter-active".equals(getFilterStyle("rejected")) ? "admin-filter-active" : "admin-filter");
    }

    private String getFilterStyle(String f) {
        return currentFilter.equals(f) ? "admin-filter-active" : "admin-filter";
    }

    private void applyFilter() {
        String search = searchField.getText().toLowerCase();
        ObservableList<AuctionRow> filtered = allAuctions.filtered(a -> {
            return a.itemName.toLowerCase().contains(search);
        });
        auctionsTable.setItems(filtered);
        resultCount.setText(filtered.size() + " auctions");
    }

    @FXML private void onApprove(String id) {
        System.out.println("Approve auction: " + id);
    }

    @FXML private void onReject(String id) {
        System.out.println("Reject auction: " + id);
    }

    @FXML private void onDelete(String id) {
        allAuctions.removeIf(a -> a.id.equals(id));
        applyFilter();
    }

    public static class AuctionRow {
        String id, itemName, seller, startPrice, statusRaw, reports, action;
        public AuctionRow(String id, String n, String s, String p, String st, String r, String a) {
            this.id=id; itemName=n; seller=s; startPrice=p; statusRaw=st; reports=r; action=a;
        }
        public String getItemInfo() { return itemName; }
        public String getSeller() { return seller; }
        public String getStartPrice() { return startPrice; }
        public String getStatus() {
            return switch(statusRaw.toUpperCase()) {
                case "PENDING_APPROVAL" -> "Pending";
                case "APPROVED" -> "Approved";
                case "LIVE" -> "Live";
                case "REJECTED" -> "Rejected";
                case "ENDED" -> "Ended";
                default -> statusRaw;
            };
        }
        public String getReports() { return reports; }
        public String getAction() { return action; }
        public String getStatusRaw() { return statusRaw; }
    }
}
