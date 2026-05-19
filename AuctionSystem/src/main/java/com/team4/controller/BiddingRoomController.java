package com.team4.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.team4.client.ApiClient;
import com.team4.client.Client;
import com.team4.util.UserSession;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.math.BigDecimal;
import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.ResourceBundle;

public class BiddingRoomController implements Initializable {

    @FXML private Label itemName, itemCategory, itemCondition, sellerName, sellerRating, itemDescription;
    @FXML private Label currentPrice, startingPriceLabel, bidIncrementLabel, bidCount, timeLeft, minBidLabel, bidStepLabel, bidError;
    @FXML private AreaChart<Number, Number> priceChart;
    @FXML private ListView<String> bidHistoryList;
    @FXML private TextField bidAmountField;
    @FXML private Button quick100k, quick200k, quick500k, quick1m, placeBidBtn;
    @FXML private ToggleButton autoBidToggle;
    @FXML private VBox autoBidPanel;
    @FXML private TextField autoBidMax, autoBidIncrement;

    private static final NumberFormat MONEY_FORMAT = NumberFormat.getNumberInstance(Locale.US);
    private static final DateTimeFormatter HISTORY_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private String auctionId;
    private double currentBid;
    private double bidIncrement = 100000;
    private LocalDateTime auctionEndTime;
    private Timeline countdownTimeline;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        priceChart.setAnimated(false);
        priceChart.setLegendVisible(false);
        bidHistoryList.setPlaceholder(new Label("No bids yet"));
        hideBidError();
        setRoomLoadingState();
    }

    public void loadAuction(String auctionId) {
        this.auctionId = auctionId;
        setRoomLoadingState();

        Task<JsonObject> task = new Task<JsonObject>() {
            @Override
            protected JsonObject call() throws Exception {
                return new ApiClient().getAuctionDetail(auctionId);
            }
        };

        task.setOnSucceeded(e -> applyAuctionData(task.getValue()));
        task.setOnFailed(e -> showBidError("Could not load auction. " + ApiClient.toDisplayMessage(task.getException())));

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void setRoomLoadingState() {
        itemName.setText("Loading auction...");
        if (itemDescription != null) {
            itemDescription.setText("");
        }
        sellerName.setText("");
        currentPrice.setText("...");
        bidCount.setText("0 bids");
        timeLeft.setText("--:--:--");
        bidAmountField.clear();
        bidHistoryList.getItems().clear();
        priceChart.getData().clear();
    }

    private void applyAuctionData(JsonObject data) {
        if (data == null) {
            showBidError("Auction data is empty.");
            return;
        }

        auctionId = stringValue(data, "id", auctionId);
        currentBid = doubleValue(data, "currentPrice", 0);
        bidIncrement = doubleValue(data, "bidIncrement", 100000);
        auctionEndTime = parseTime(stringValue(data, "endTime", null));

        itemName.setText(stringValue(data, "itemName", "Untitled item"));
        itemCategory.setText(formatCategory(stringValue(data, "category", "Other")));
        itemCondition.setText(formatStatus(stringValue(data, "status", "")));
        sellerName.setText("Seller: " + stringValue(data, "sellerName", "Unknown Seller"));
        if (sellerRating != null) {
            double rating = doubleValue(data, "sellerRating", 0);
            sellerRating.setText(rating > 0 ? String.format(Locale.US, "%.1f Rating", rating) : "");
        }
        if (itemDescription != null) {
            itemDescription.setText(stringValue(data, "itemDescription", ""));
        }

        currentPrice.setText(formatPrice(currentBid));
        if (startingPriceLabel != null) {
            startingPriceLabel.setText(formatPrice(doubleValue(data, "startingPrice", currentBid)));
        }
        if (bidIncrementLabel != null) {
            bidIncrementLabel.setText(formatPrice(bidIncrement));
        }
        if (bidStepLabel != null) {
            bidStepLabel.setText(formatPrice(bidIncrement));
        }

        JsonArray history = data.has("bidHistory") && data.get("bidHistory").isJsonArray()
                ? data.getAsJsonArray("bidHistory")
                : new JsonArray();

        bidCount.setText(history.size() + " bids");
        updateBidInfo();
        updateQuickBidButtons();
        renderBidHistory(history);
        renderPriceChart(data, history);
        startCountdown();
        ensureSocketListener();
    }

    private void startCountdown() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
        KeyFrame frame = new KeyFrame(Duration.seconds(1), event -> updateCountdownLabel());
        countdownTimeline = new Timeline(frame);
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
        updateCountdownLabel();
    }

    private void updateCountdownLabel() {
        if (auctionEndTime == null) {
            timeLeft.setText("--:--:--");
            return;
        }
        long secondsBetween = ChronoUnit.SECONDS.between(LocalDateTime.now(), auctionEndTime);
        if (secondsBetween <= 0) {
            timeLeft.setText("00:00:00 (Ended)");
            placeBidBtn.setDisable(true);
            return;
        }

        long days = secondsBetween / 86400;
        long h = (secondsBetween % 86400) / 3600;
        long m = (secondsBetween % 3600) / 60;
        long s = secondsBetween % 60;
        if (days > 0) {
            timeLeft.setText(String.format("%dd %02d:%02d:%02d", days, h, m, s));
        } else {
            timeLeft.setText(String.format("%02d:%02d:%02d", h, m, s));
        }
        placeBidBtn.setDisable(false);
    }

    private void ensureSocketListener() {
        Client client = Client.getInstance();
        if (!client.isConnected()) {
            if (!client.connect()) {
                showBidError("Cannot connect to bidding server. Please start the server and try again.");
                return;
            }
            UserSession session = UserSession.getInstance();
            if (session != null && session.getUserId() != null) {
                client.sendLogin(session.getUserId());
            }
        }
        client.startListening(message -> Platform.runLater(() -> handleSocketMessage(message)));
    }

    private void handleSocketMessage(String message) {
        try {
            JsonObject json = JsonParser.parseString(message).getAsJsonObject();
            String action = stringValue(json, "action", "");
            JsonObject data = json.has("data") && json.get("data").isJsonObject()
                    ? json.getAsJsonObject("data")
                    : new JsonObject();

            String messageAuctionId = stringValue(data, "auctionId", "");
            if (!messageAuctionId.isEmpty() && auctionId != null && !auctionId.equals(messageAuctionId)) {
                return;
            }

            if ("BID_FAILED".equals(action)) {
                placeBidBtn.setDisable(false);
                showBidError(stringValue(json, "message", "Bid failed."));
                return;
            }

            if ("BID_SUCCESS".equals(action) || "BID_UPDATE".equals(action)) {
                placeBidBtn.setDisable(false);
                hideBidError();
                if (data.has("currentPrice")) {
                    currentBid = data.get("currentPrice").getAsDouble();
                    currentPrice.setText(formatPrice(currentBid));
                    updateBidInfo();
                } else if (data.has("amount")) {
                    currentBid = data.get("amount").getAsDouble();
                    currentPrice.setText(formatPrice(currentBid));
                    updateBidInfo();
                }
                if (data.has("endTime")) {
                    auctionEndTime = parseTime(data.get("endTime").getAsString());
                    startCountdown();
                }
                if (auctionId != null) {
                    loadAuction(auctionId);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void updateBidInfo() {
        double minNext = currentBid + bidIncrement;
        bidAmountField.setText(String.format(Locale.US, "%.0f", minNext));
        minBidLabel.setText(formatPrice(minNext));
    }

    private void updateQuickBidButtons() {
        quick100k.setText("+" + compactMoney(bidIncrement));
        quick200k.setText("+" + compactMoney(bidIncrement * 2));
        quick500k.setText("+" + compactMoney(bidIncrement * 5));
        quick1m.setText("+" + compactMoney(bidIncrement * 10));
    }

    @FXML private void onQuickBid(ActionEvent event) {
        double multiplier = 1;
        Object source = event.getSource();
        if (source == quick200k) {
            multiplier = 2;
        } else if (source == quick500k) {
            multiplier = 5;
        } else if (source == quick1m) {
            multiplier = 10;
        }
        placeBid(currentBid + bidIncrement * multiplier);
    }

    @FXML private void onPlaceBid() {
        try {
            double bid = Double.parseDouble(bidAmountField.getText().trim());
            if (bid < currentBid + bidIncrement) {
                showBidError("Bid must be at least " + formatPrice(currentBid + bidIncrement));
                return;
            }
            placeBid(bid);
        } catch (NumberFormatException e) {
            showBidError("Please enter a valid number");
        }
    }

    private void placeBid(double amount) {
        UserSession session = UserSession.getInstance();
        if (session == null || session.getUserId() == null || session.getUserId().isBlank()) {
            showBidError("Please log in as a bidder before placing a bid.");
            return;
        }
        if (auctionId == null || auctionId.isBlank()) {
            showBidError("Auction is not ready yet.");
            return;
        }

        hideBidError();
        placeBidBtn.setDisable(true);
        ensureSocketListener();
        if (Client.getInstance().isConnected()) {
            Client.getInstance().sendBid(auctionId, session.getUserId(), amount);
        } else {
            placeBidBtn.setDisable(false);
        }
    }

    @FXML private void onAutoBidToggle() {
        boolean active = autoBidToggle.isSelected();
        autoBidPanel.setManaged(active);
        autoBidPanel.setVisible(active);
    }

    private void renderBidHistory(JsonArray history) {
        bidHistoryList.getItems().clear();
        for (int i = history.size() - 1; i >= 0; i--) {
            JsonObject bid = history.get(i).getAsJsonObject();
            String bidder = stringValue(bid, "bidderName", "Unknown Bidder");
            double amount = doubleValue(bid, "bidAmount", 0);
            LocalDateTime bidTime = parseTime(stringValue(bid, "bidTime", null));
            String time = bidTime == null ? "" : " - " + bidTime.format(HISTORY_TIME_FORMAT);
            bidHistoryList.getItems().add(bidder + " - " + formatPrice(amount) + time);
        }
    }

    private void renderPriceChart(JsonObject auction, JsonArray history) {
        priceChart.getData().clear();
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Bid price");

        double startPrice = doubleValue(auction, "startingPrice", currentBid);
        series.getData().add(new XYChart.Data<>(0, startPrice));

        for (int i = 0; i < history.size(); i++) {
            JsonObject bid = history.get(i).getAsJsonObject();
            series.getData().add(new XYChart.Data<>(i + 1, doubleValue(bid, "bidAmount", startPrice)));
        }

        if (history.isEmpty() && currentBid != startPrice) {
            series.getData().add(new XYChart.Data<>(1, currentBid));
        }

        priceChart.getData().add(series);
    }

    private void showBidError(String msg) {
        bidError.setText(msg);
        bidError.setManaged(true);
        bidError.setVisible(true);
    }

    private void hideBidError() {
        bidError.setManaged(false);
        bidError.setVisible(false);
    }

    private String formatPrice(double price) {
        return MONEY_FORMAT.format(BigDecimal.valueOf(price)) + " VND";
    }

    private String compactMoney(double value) {
        if (value >= 1_000_000) {
            double millions = value / 1_000_000;
            return millions == Math.floor(millions)
                    ? String.format(Locale.US, "%.0fM", millions)
                    : String.format(Locale.US, "%.1fM", millions);
        }
        if (value >= 1_000) {
            return String.format(Locale.US, "%.0fK", value / 1_000);
        }
        return String.format(Locale.US, "%.0f", value);
    }

    private String formatCategory(String value) {
        if (value == null || value.isBlank()) {
            return "Other";
        }
        String lower = value.replace("_", " ").toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private String formatStatus(String value) {
        if ("RUNNING".equalsIgnoreCase(value)) {
            return "Live";
        }
        if ("FINISHED".equalsIgnoreCase(value)) {
            return "Ended";
        }
        return value == null ? "" : value;
    }

    private String stringValue(JsonObject obj, String key, String fallback) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : fallback;
    }

    private double doubleValue(JsonObject obj, String key, double fallback) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsDouble() : fallback;
    }

    private LocalDateTime parseTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value);
        } catch (Exception ignored) {
            return null;
        }
    }
}
