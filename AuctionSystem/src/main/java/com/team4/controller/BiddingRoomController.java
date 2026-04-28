package com.team4.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

public class BiddingRoomController implements Initializable {

    private static final double MAX_BID = 50000000;

    @FXML private Label itemName, itemCategory, itemCondition, sellerName, sellerRating;
    @FXML private Label currentPrice, bidCount, timeLeft, minBidLabel, bidError;
    @FXML private AreaChart<Number, Number> priceChart;
    @FXML private ListView<String> bidHistoryList;
    @FXML private TextField bidAmountField;
    @FXML private Button quick100k, quick200k, quick500k, quick1m, placeBidBtn;
    @FXML private ToggleButton autoBidToggle;
    @FXML private VBox autoBidPanel;
    @FXML private TextField autoBidMax, autoBidIncrement;
    @FXML private ListView<String> chatList;
    @FXML private TextField chatInput;

    private double currentBid = 28500000;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadChart();
        loadBidHistory();
        loadChat();
        updateBidInfo();
    }

    private void updateBidInfo() {
        currentPrice.setText(formatPrice(currentBid));
        double minNext = currentBid + 100000;
        bidAmountField.setText(String.valueOf((int)minNext));
        minBidLabel.setText(formatPrice(minNext));
    }

    @FXML private void onQuickBid() {
        int increment = 100000;
        double newBid = currentBid + increment;
        
        if (newBid > MAX_BID) {
            showBidError("Gia dat khong duoc vuot qua " + formatPrice(MAX_BID));
            return;
        }
        
        placeBid(newBid);
    }

    @FXML private void onPlaceBid() {
        try {
            double bid = Double.parseDouble(bidAmountField.getText());
            if (bid > MAX_BID) {
                showBidError("Gia dat khong duoc vuot qua " + formatPrice(MAX_BID));
                return;
            }
            if (bid <= currentBid) {
                showBidError("Gia dat phai lon hon gia hien tai");
                return;
            }
            placeBid(bid);
        } catch (NumberFormatException e) {
            showBidError("Vui long nhap so hop le");
        }
    }

    private void placeBid(double amount) {
        currentBid = amount;
        updateBidInfo();
        hideBidError();
        addBidToHistory("Ban", amount);
        addChartPoint(amount);
    }

    @FXML private void onAutoBidToggle() {
        boolean active = autoBidToggle.isSelected();
        autoBidPanel.setManaged(active);
        autoBidPanel.setVisible(active);
    }

    @FXML private void onSendChat() {
        String msg = chatInput.getText().trim();
        if (!msg.isEmpty()) {
            chatList.getItems().add(0, "Ban: " + msg);
            chatInput.clear();
        }
    }

    private void loadChart() {
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Gia dat");
        series.getData().add(new XYChart.Data<>(0, 25000000));
        series.getData().add(new XYChart.Data<>(1, 25500000));
        series.getData().add(new XYChart.Data<>(2, 26000000));
        series.getData().add(new XYChart.Data<>(3, 27000000));
        series.getData().add(new XYChart.Data<>(4, 27500000));
        series.getData().add(new XYChart.Data<>(5, 28000000));
        series.getData().add(new XYChart.Data<>(6, 28500000));
        priceChart.getData().add(series);
    }

    private void addChartPoint(double price) {
        int nextIndex = priceChart.getData().get(0).getData().size();
        priceChart.getData().get(0).getData().add(
            new XYChart.Data<>(nextIndex, price)
        );
    }

    private void loadBidHistory() {
        bidHistoryList.getItems().addAll(
            "🥇 Ban - 28,500,000 - Vua xong",
            "🥈 Tran Thi B - 28,000,000 - 5 phut truoc",
            "🥉 Le Van C - 27,500,000 - 8 phut truoc",
            "4. Pham Thi D - 27,000,000 - 12 phut truoc",
            "5. Hoang Van E - 26,000,000 - 15 phut truoc"
        );
    }

    private void addBidToHistory(String bidder, double amount) {
        bidHistoryList.getItems().add(0, "🥇 " + bidder + " - " + formatPrice(amount) + " - Vua xong");
    }

    private void loadChat() {
        chatList.getItems().addAll(
            "Nguyen Van A: San pham dep qua!",
            "Tran Thi B: Gia nay hop ly roi",
            "Le Van C: Con ai dau khong?"
        );
    }

    private void showBidError(String msg) {
        bidError.setText("⚠ " + msg);
        bidError.setManaged(true);
        bidError.setVisible(true);
    }

    private void hideBidError() {
        bidError.setManaged(false);
        bidError.setVisible(false);
    }

    private String formatPrice(double price) {
        return String.format("%,d VND", (int)price);
    }
}
