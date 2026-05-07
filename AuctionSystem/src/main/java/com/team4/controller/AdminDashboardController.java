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

    @FXML private Label totalUsersLabel, lockedUsersLabel, pendingAuctionsLabel, liveAuctionsLabel;
    @FXML private BarChart<String, Number> regChart;
    @FXML private VBox alertsContainer;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadStats();
        loadChart();
        loadAlerts();
    }

    private void loadStats() {
        totalUsersLabel.setText("1,245");
        lockedUsersLabel.setText("12");
        pendingAuctionsLabel.setText("8");
        liveAuctionsLabel.setText("24");
    }

    private void loadChart() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("New Users");
        series.getData().add(new XYChart.Data<>("Jan", 120));
        series.getData().add(new XYChart.Data<>("Feb", 180));
        series.getData().add(new XYChart.Data<>("Mar", 240));
        series.getData().add(new XYChart.Data<>("Apr", 310));
        regChart.getData().add(series);
    }

    private void loadAlerts() {
        alertsContainer.getChildren().clear();
        alertsContainer.getChildren().add(createAlert("🔴", "3 auctions reported", "Needs review and action", "alert-red"));
        alertsContainer.getChildren().add(createAlert("🟡", "8 auctions pending approval", "Need approval to start", "alert-yellow"));
        alertsContainer.getChildren().add(createAlert("🟣", "12 accounts locked", "Currently restricted", "alert-purple"));
    }

    private HBox createAlert(String icon, String title, String desc, String styleClass) {
        HBox box = new HBox(12);
        box.getStyleClass().add(styleClass);
        box.setStyle("-fx-padding: 14; -fx-background-radius: 12; -fx-spacing: 12;");

        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size: 20;");

        VBox text = new VBox(4);
        text.getChildren().add(new Label(title) {{ setStyle("-fx-font-weight: bold; -fx-text-fill: white;"); }});
        text.getChildren().add(new Label(desc) {{ setStyle("-fx-font-size: 12; -fx-text-fill: #9ca3af;"); }});

        box.getChildren().addAll(iconLbl, text);
        return box;
    }
}