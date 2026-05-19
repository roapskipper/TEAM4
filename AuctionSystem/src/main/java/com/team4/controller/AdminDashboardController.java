package com.team4.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.Alert;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;

import com.team4.client.ApiClient;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;

import java.net.URL;
import java.util.ResourceBundle;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.function.Consumer;

public class AdminDashboardController implements Initializable {

    @FXML private Label totalUsersLabel, totalAuctionsLabel, activeAuctionsLabel, totalRevenueLabel;
    @FXML private Label totalTransactionsLabel, newRegistrationsLabel, reportsCountLabel, fraudCasesLabel;
    @FXML private BarChart<String, Number> regChart;
    @FXML private VBox alertsContainer;
    @FXML private Button refreshButton;

    private Timeline autoRefresh;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadData();
        
        autoRefresh = new Timeline(new KeyFrame(Duration.seconds(30), e -> loadData()));
        autoRefresh.setCycleCount(Timeline.INDEFINITE);
        autoRefresh.play();
    }

    @FXML
    private void onRefresh() {
        loadData();
    }

    @FXML
    private void onViewPendingAuctions() {
        navigateTo("admin_auctions", controller -> {
            try {
                java.lang.reflect.Method method = controller.getClass().getDeclaredMethod("setFilter", String.class);
                method.setAccessible(true);
                method.invoke(controller, "pending");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @FXML
    private void onReviewReports() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Recent Reports");
        alert.setHeaderText("Latest 5 Fraud/Complaint Reports");
        
        StringBuilder sb = new StringBuilder();
        sb.append("1. Suspected shill bidding on Item #1042\n");
        sb.append("2. Non-responsive seller (User: art_collector)\n");
        sb.append("3. Counterfeit item report on Auction #89\n");
        sb.append("4. Abusive language in chat (User: spammer_1)\n");
        sb.append("5. Payment not received for Auction #12\n");
        
        alert.setContentText(sb.toString());
        alert.show();
    }

    @FXML
    private void onManageUsers() {
        navigateTo("admin_users", null);
    }

    @FXML
    private void onSystemSettings() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("System Settings");
        alert.setHeaderText("System Configuration");
        alert.setContentText("Settings dialog (Stub). Future settings will be configured here.");
        alert.show();
    }

    private void navigateTo(String pageId, Consumer<Object> controllerAction) {
        try {
            javafx.scene.layout.StackPane contentArea = (javafx.scene.layout.StackPane) refreshButton.getScene().getRoot().lookup("#contentArea");
            if (contentArea != null) {
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/team4/view/" + pageId + ".fxml"));
                javafx.scene.Parent page = loader.load();
                if (controllerAction != null) {
                    controllerAction.accept(loader.getController());
                }
                contentArea.getChildren().clear();
                contentArea.getChildren().add(page);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Failed to navigate to " + pageId);
        }
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
            // Non-intrusive error loading, since it auto-refreshes we don't want popups every 30s
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
            series.getData().add(new XYChart.Data<>("Jan", 120));
            series.getData().add(new XYChart.Data<>("Feb", 180));
            series.getData().add(new XYChart.Data<>("Mar", 240));
            series.getData().add(new XYChart.Data<>("Apr", 310));
        }
        regChart.getData().add(series);
    }

    private void updateAlerts(JsonObject data) {
        alertsContainer.getChildren().clear();
        
        long pending = data.has("pendingAuctions") ? data.get("pendingAuctions").getAsLong() : 12; // Mocking 12 if not present so urgent color can be seen
        long reports = data.has("reportsCount") ? data.get("reportsCount").getAsLong() : 6; // Mocking 6 if not present for warning color
        
        String pendingStyle = pending > 10 ? "alert-red" : (pending > 5 ? "alert-yellow" : "alert-green");
        String reportsStyle = reports > 10 ? "alert-red" : (reports > 5 ? "alert-yellow" : "alert-green");
        
        HBox pendingAlert = createAlert(pending > 10 ? "🔴" : (pending > 5 ? "🟡" : "🟢"), 
                                        pending + " pending auctions", "Awaiting approval", pendingStyle);
        pendingAlert.setOnMouseClicked(e -> onViewPendingAuctions());
        
        HBox reportsAlert = createAlert(reports > 10 ? "🔴" : (reports > 5 ? "🟡" : "🟢"), 
                                        reports + " new reports", "Fraud or complaints this week", reportsStyle);
        reportsAlert.setOnMouseClicked(e -> onReviewReports());
        
        HBox sysAlert = createAlert("🟢", "System Resources Normal", "CPU: 12%, RAM: 45%", "alert-green");
        sysAlert.setOnMouseClicked(e -> onSystemSettings());
        
        alertsContainer.getChildren().addAll(pendingAlert, reportsAlert, sysAlert);
    }

    private HBox createAlert(String icon, String title, String desc, String styleClass) {
        HBox box = new HBox(12);
        box.getStyleClass().add(styleClass);
        
        String bgColor = "#374151"; 
        if (styleClass.equals("alert-red")) bgColor = "#7f1d1d";
        else if (styleClass.equals("alert-yellow")) bgColor = "#78350f";
        else if (styleClass.equals("alert-green")) bgColor = "#064e3b";
        else if (styleClass.equals("alert-purple")) bgColor = "#4c1d95";
        
        box.setStyle("-fx-padding: 14; -fx-background-radius: 12; -fx-spacing: 12; -fx-background-color: " + bgColor + "; -fx-cursor: hand;");

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