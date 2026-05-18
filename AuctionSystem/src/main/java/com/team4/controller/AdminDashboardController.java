package com.team4.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;

import com.team4.client.ApiClient;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;

import java.net.URL;
import java.util.ResourceBundle;
import java.text.NumberFormat;
import java.util.Locale;

public class AdminDashboardController implements Initializable {

    @FXML private Label totalUsersLabel, totalAuctionsLabel, activeAuctionsLabel, totalRevenueLabel;
    @FXML private Label totalTransactionsLabel, newRegistrationsLabel, reportsCountLabel, fraudCasesLabel;
    @FXML private BarChart<String, Number> regChart;
    @FXML private VBox alertsContainer;
    @FXML private Button refreshButton;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadData();
    }

    @FXML
    private void onRefresh() {
        loadData();
    }

    private void loadData() {
        setLoadingState();
        Task<JsonObject> task = new Task<>() {
            @Override
            protected JsonObject call() throws Exception {
                ApiClient apiClient = new ApiClient();
                return apiClient.getDashboardStats();
            }
        };

        task.setOnSucceeded(e -> {
            JsonObject data = task.getValue();
            updateStats(data);
            updateChart(data);
            updateAlerts(data);
            refreshButton.setDisable(false);
        });

        task.setOnFailed(e -> {
            refreshButton.setDisable(false);
            showError("Failed to load dashboard data: " + task.getException().getMessage());
            setDefaultState();
        });

        new Thread(task).start();
    }

    private void setLoadingState() {
        refreshButton.setDisable(true);
        totalUsersLabel.setText("...");
        totalAuctionsLabel.setText("...");
        activeAuctionsLabel.setText("...");
        totalRevenueLabel.setText("...");
        totalTransactionsLabel.setText("...");
        newRegistrationsLabel.setText("...");
        reportsCountLabel.setText("...");
        fraudCasesLabel.setText("...");
    }

    private void setDefaultState() {
        totalUsersLabel.setText("0");
        totalAuctionsLabel.setText("0");
        activeAuctionsLabel.setText("0");
        totalRevenueLabel.setText("0 VND");
        totalTransactionsLabel.setText("0");
        newRegistrationsLabel.setText("0");
        reportsCountLabel.setText("0");
        fraudCasesLabel.setText("0");
    }

    private void updateStats(JsonObject data) {
        NumberFormat numberFormat = NumberFormat.getInstance(Locale.US);
        
        long totalUsers = data.has("totalUsers") ? data.get("totalUsers").getAsLong() : 0;
        long totalAuctions = data.has("totalAuctions") ? data.get("totalAuctions").getAsLong() : 0;
        long activeAuctions = data.has("activeAuctions") ? data.get("activeAuctions").getAsLong() : 0;
        double totalRevenue = data.has("totalRevenue") ? data.get("totalRevenue").getAsDouble() : 0.0;
        long totalTransactions = data.has("totalTransactions") ? data.get("totalTransactions").getAsLong() : 0;
        long newRegistrations = data.has("newRegistrations") ? data.get("newRegistrations").getAsLong() : 0;
        long reportsCount = data.has("reportsCount") ? data.get("reportsCount").getAsLong() : 0;
        long fraudCases = data.has("fraudCases") ? data.get("fraudCases").getAsLong() : 0;

        totalUsersLabel.setText(numberFormat.format(totalUsers));
        totalAuctionsLabel.setText(numberFormat.format(totalAuctions));
        activeAuctionsLabel.setText(numberFormat.format(activeAuctions));
        totalRevenueLabel.setText(numberFormat.format(totalRevenue) + " VND");
        totalTransactionsLabel.setText(numberFormat.format(totalTransactions));
        newRegistrationsLabel.setText(numberFormat.format(newRegistrations));
        reportsCountLabel.setText(numberFormat.format(reportsCount));
        fraudCasesLabel.setText(numberFormat.format(fraudCases));
    }

    private void updateChart(JsonObject data) {
        regChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("New Users");

        if (data.has("registrationChart") && data.get("registrationChart").isJsonArray()) {
            JsonArray chartData = data.getAsJsonArray("registrationChart");
            for (JsonElement el : chartData) {
                JsonObject point = el.getAsJsonObject();
                String month = point.has("month") ? point.get("month").getAsString() : "";
                int count = point.has("count") ? point.get("count").getAsInt() : 0;
                series.getData().add(new XYChart.Data<>(month, count));
            }
        } else {
            // Fallback mock data if API doesn't provide chart data
            series.getData().add(new XYChart.Data<>("Jan", 120));
            series.getData().add(new XYChart.Data<>("Feb", 180));
            series.getData().add(new XYChart.Data<>("Mar", 240));
            series.getData().add(new XYChart.Data<>("Apr", 310));
        }
        regChart.getData().add(series);
    }

    private void updateAlerts(JsonObject data) {
        alertsContainer.getChildren().clear();
        
        if (data.has("alerts") && data.get("alerts").isJsonArray()) {
            JsonArray alerts = data.getAsJsonArray("alerts");
            for (JsonElement el : alerts) {
                JsonObject alert = el.getAsJsonObject();
                String icon = alert.has("icon") ? alert.get("icon").getAsString() : "⚠️";
                String title = alert.has("title") ? alert.get("title").getAsString() : "Alert";
                String desc = alert.has("desc") ? alert.get("desc").getAsString() : "";
                String style = alert.has("style") ? alert.get("style").getAsString() : "alert-yellow";
                alertsContainer.getChildren().add(createAlert(icon, title, desc, style));
            }
        } else {
            // Fallback mock alerts if API doesn't provide them
            alertsContainer.getChildren().add(createAlert("🔴", "3 auctions reported", "Needs review and action", "alert-red"));
            alertsContainer.getChildren().add(createAlert("🟡", "8 auctions pending approval", "Need approval to start", "alert-yellow"));
            alertsContainer.getChildren().add(createAlert("🟣", "12 accounts locked", "Currently restricted", "alert-purple"));
        }
    }

    private HBox createAlert(String icon, String title, String desc, String styleClass) {
        HBox box = new HBox(12);
        box.getStyleClass().add(styleClass);
        box.setStyle("-fx-padding: 14; -fx-background-radius: 12; -fx-spacing: 12;");

        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size: 20;");

        VBox text = new VBox(4);
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: white;");
        Label descLbl = new Label(desc);
        descLbl.setStyle("-fx-font-size: 12; -fx-text-fill: #9ca3af;");
        
        text.getChildren().addAll(titleLbl, descLbl);

        box.getChildren().addAll(iconLbl, text);
        return box;
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.show();
    }
}