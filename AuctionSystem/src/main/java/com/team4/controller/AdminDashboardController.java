package com.team4.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

public class AdminDashboardController implements Initializable {

    @FXML private Label pendingApprovalsLabel, openReportsLabel, suspiciousAccountsLabel, endingSoonLabel;
    @FXML private BarChart<String, Number> regChart;
    @FXML private VBox alertsContainer;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadStats();
        loadChart();
        loadAlerts();
    }

    private void loadStats() {
        pendingApprovalsLabel.setText("8");
        openReportsLabel.setText("5");
        suspiciousAccountsLabel.setText("3");
        endingSoonLabel.setText("6");
    }

    private void loadChart() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Reports");
        series.getData().add(new XYChart.Data<>("Mon", 4));
        series.getData().add(new XYChart.Data<>("Tue", 7));
        series.getData().add(new XYChart.Data<>("Wed", 3));
        series.getData().add(new XYChart.Data<>("Thu", 9));
        series.getData().add(new XYChart.Data<>("Fri", 6));
        series.getData().add(new XYChart.Data<>("Sat", 12));
        series.getData().add(new XYChart.Data<>("Sun", 5));
        regChart.getData().add(series);
    }

    private void loadAlerts() {
        alertsContainer.getChildren().clear();
        alertsContainer.getChildren().add(createAlert("🚩", "5 new violation reports", "Items flagged by bidders — review required", "alert-red"));
        alertsContainer.getChildren().add(createAlert("⏳", "8 auctions awaiting approval", "New sessions need admin approval to start", "alert-yellow"));
        alertsContainer.getChildren().add(createAlert("👁", "3 suspicious accounts", "Unusual bidding pattern detected", "alert-purple"));
    }

    private HBox createAlert(String icon, String title, String desc, String colorClass) {
        HBox box = new HBox();
        box.getStyleClass().addAll(colorClass, "alert-box");

        Label iconLbl = new Label(icon);
        iconLbl.getStyleClass().add("alert-icon");

        VBox text = new VBox(4);
        Label titleLbl = new Label(title);
        titleLbl.getStyleClass().add("alert-title");
        Label descLbl = new Label(desc);
        descLbl.getStyleClass().add("alert-desc");
        text.getChildren().addAll(titleLbl, descLbl);

        box.getChildren().addAll(iconLbl, text);
        return box;
    }
}