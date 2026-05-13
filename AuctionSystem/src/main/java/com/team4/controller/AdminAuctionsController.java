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

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTable();
        loadMockData();
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

    private void loadMockData() {
        allAuctions.add(new AuctionRow("A1", "iPhone 15 Pro Max", "tech_store", "25,000,000", "pending", "0", "Pending approval"));
        allAuctions.add(new AuctionRow("A2", "Buc tranh son dau", "art_collector", "5,000,000", "pending", "0", "Pending approval"));
        allAuctions.add(new AuctionRow("A3", "Honda Civic 2020", "seller_pro", "450,000,000", "live", "0", "Live"));
        allAuctions.add(new AuctionRow("A4", "San pham la", "spammer_1", "1,000", "rejected", "5", "Violation"));
        allAuctions.add(new AuctionRow("A5", "Rolex Datejust", "seller_pro", "120,000,000", "approved", "0", "Approved"));
        applyFilter();
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
        applyFilter();
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
            boolean matchSearch = a.itemName.toLowerCase().contains(search);
            boolean matchFilter = currentFilter.equals("all") || a.statusRaw.equals(currentFilter);
            return matchSearch && matchFilter;
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
            return switch(statusRaw) {
                case "pending" -> "Pending";
                case "approved" -> "Approved";
                case "live" -> "Live";
                case "rejected" -> "Violation";
                default -> "⚪ " + statusRaw;
            };
        }
        public String getReports() { return reports; }
        public String getAction() { return action; }
        public String getStatusRaw() { return statusRaw; }
    }
}