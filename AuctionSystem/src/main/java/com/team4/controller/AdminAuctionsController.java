package com.team4.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import com.team4.util.UserSession;
import com.team4.client.ApiClient;

import java.net.URL;
import java.util.ResourceBundle;

public class AdminAuctionsController implements Initializable {

    @FXML private TextField searchField;
    @FXML private Button filterAll, filterPending, filterLive, filterViolated;
    @FXML private Label resultCount;
    @FXML private TableView<AuctionRow> auctionsTable;
    @FXML private TableColumn<AuctionRow, String> colItem, colSeller, colStartPrice, colStatus, colReports;
    @FXML private TableColumn<AuctionRow, Void> colAction;

    private ObservableList<AuctionRow> allAuctions = FXCollections.observableArrayList();
    private String currentFilter = "all";

    public void initialize(URL url, ResourceBundle rb) {
        setupTable();
        loadDataFromServer();
        setupFilters();
    }

    private void setupTable() {
        auctionsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        colItem.setCellValueFactory(new PropertyValueFactory<>("itemInfo"));
        colSeller.setCellValueFactory(new PropertyValueFactory<>("seller"));
        colStartPrice.setCellValueFactory(new PropertyValueFactory<>("startPrice"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colReports.setCellValueFactory(new PropertyValueFactory<>("reports"));

        colAction.setCellFactory(param -> new TableCell<AuctionRow, Void>() {
            private final Button approveBtn = new Button("Approve");
            private final Button rejectBtn = new Button("Reject");
            private final HBox actionBox = new HBox(6);

            {
                actionBox.setAlignment(Pos.CENTER);
                styleActionButton(approveBtn, "admin-action-grant");
                styleActionButton(rejectBtn, "admin-action-revoke");
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    AuctionRow row = getTableRow().getItem();
                    actionBox.getChildren().clear();

                    if ("PENDING".equalsIgnoreCase(row.getStatusRaw()) || "PENDING_APPROVAL".equalsIgnoreCase(row.getStatusRaw())) {
                        approveBtn.setOnAction(e -> handleApprove(row));
                        rejectBtn.setOnAction(e -> handleReject(row));
                        actionBox.getChildren().addAll(approveBtn, rejectBtn);
                    } else {
                        Label noAction = new Label("No actions");
                        noAction.getStyleClass().add("muted-text-sm");
                        actionBox.getChildren().add(noAction);
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
        button.setMaxWidth(Region.USE_PREF_SIZE);
    }

    private void loadDataFromServer() {
        auctionsTable.setPlaceholder(createTableEmptyState("Loading auctions", "Fetching auction review data."));
        javafx.concurrent.Task<com.google.gson.JsonArray> task = new javafx.concurrent.Task<>() {
            @Override
            protected com.google.gson.JsonArray call() throws Exception {
                ApiClient apiClient = new ApiClient();
                return apiClient.getAuctions(currentFilter, currentUserId());
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
                
                allAuctions.add(new AuctionRow(id, name, seller, startPrice, status, reportCount));
            }
            
            if (allAuctions.isEmpty()) {
                auctionsTable.setPlaceholder(createTableEmptyState("No auctions found", "Auctions awaiting review will appear here."));
            }
            applyFilter();
        });

        task.setOnFailed(e -> {
            allAuctions.clear();
            auctionsTable.setItems(FXCollections.observableArrayList());
            auctionsTable.setPlaceholder(createTableEmptyState(
                    "Auctions unavailable",
                    "Could not load auctions. " + task.getException().getMessage()));
            resultCount.setText("0 auctions");
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
        if (filtered.isEmpty()) {
            auctionsTable.setPlaceholder(createTableEmptyState(
                    allAuctions.isEmpty() ? "No auctions found" : "No matching auctions",
                    allAuctions.isEmpty()
                            ? "Auctions awaiting review will appear here."
                            : "Adjust the search keyword or status filter."));
        }
        resultCount.setText(filtered.size() + " auctions");
    }

    private VBox createTableEmptyState(String title, String message) {
        VBox empty = new VBox(10);
        empty.setAlignment(Pos.CENTER);
        empty.setPrefHeight(220);
        empty.getStyleClass().add("empty-state");

        Label mark = new Label(title.toLowerCase().contains("loading") ? "..." : "No Data");
        mark.getStyleClass().add("empty-state-mark");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("empty-state-title");
        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("empty-state-text");
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(380);

        empty.getChildren().addAll(mark, titleLabel, messageLabel);
        return empty;
    }

    private void handleApprove(AuctionRow row) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Approve auction for " + row.getItemInfo() + "?",
                ButtonType.YES,
                ButtonType.NO);
        confirm.setTitle("Approve Auction");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                disableTableAndCallApi(() -> {
                    ApiClient apiClient = new ApiClient();
                    return apiClient.approveAuction(row.id, currentUserId());
                }, "Auction approved successfully!");
            }
        });
    }

    private void handleReject(AuctionRow row) {
        TextInputDialog dialog = new TextInputDialog("Invalid item description");
        dialog.setTitle("Reject Auction");
        dialog.setHeaderText("Reject auction for " + row.getItemInfo());
        dialog.setContentText("Reason for rejection:");
        dialog.showAndWait().ifPresent(reason -> {
            if (reason.trim().isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Error", "Reason cannot be empty.");
                return;
            }
            disableTableAndCallApi(() -> {
                ApiClient apiClient = new ApiClient();
                return apiClient.rejectAuction(row.id, currentUserId(), reason.trim());
            }, "Auction rejected successfully!");
        });
    }

    private void disableTableAndCallApi(java.util.concurrent.Callable<String> apiCall, String successMsg) {
        auctionsTable.setDisable(true);
        javafx.concurrent.Task<String> task = new javafx.concurrent.Task<>() {
            @Override
            protected String call() throws Exception {
                return apiCall.call();
            }
        };

        task.setOnSucceeded(e -> {
            auctionsTable.setDisable(false);
            showAlert(Alert.AlertType.INFORMATION, "Success", successMsg);
            loadDataFromServer();
        });

        task.setOnFailed(e -> {
            auctionsTable.setDisable(false);
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

    private String currentUserId() {
        UserSession session = UserSession.getInstance();
        return session != null && session.getUserId() != null ? session.getUserId() : "";
    }

    public static class AuctionRow {
        String id, itemName, seller, startPrice, statusRaw, reports;
        public AuctionRow(String id, String n, String s, String p, String st, String r) {
            this.id=id; itemName=n; seller=s; startPrice=p; statusRaw=st; reports=r;
        }
        public String getItemInfo() { return itemName; }
        public String getSeller() { return seller; }
        public String getStartPrice() { return startPrice; }
        public String getStatus() {
            String value = statusRaw == null ? "" : statusRaw.trim().toUpperCase();
            return switch(value) {
                case "PENDING", "PENDING_APPROVAL" -> "Pending";
                case "RUNNING", "LIVE", "ACTIVE", "APPROVED", "ONGOING" -> "Ongoing";
                case "FINISHED", "ENDED", "COMPLETED", "PAID", "SOLD" -> "Ended";
                case "REJECTED" -> "Rejected";
                case "CANCELLED" -> "Cancelled";
                default -> statusRaw;
            };
        }
        public String getReports() { return reports; }
        public String getStatusRaw() { return statusRaw; }
    }
}
