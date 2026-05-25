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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BiddingRoomController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(BiddingRoomController.class);

    @FXML private Label itemName, itemCategory, itemCondition, sellerName, sellerRating, itemDescription;
    @FXML private Label currentPrice, startingPriceLabel, bidIncrementLabel, bidCount, timeLeft, minBidLabel, bidStepLabel, bidError;
    @FXML private Label walletBalance;
    @FXML private AreaChart<Number, Number> priceChart;
    @FXML private ListView<String> bidHistoryList;
    @FXML private TextField bidAmountField;
    @FXML private Button quick100k, quick200k, quick500k, quick1m, placeBidBtn;
    @FXML private ToggleButton autoBidToggle;
    @FXML private VBox bidControlsCard, autoBidPanel;
    @FXML private TextField autoBidMax;
    @FXML private Button applyAutoBidBtn;

    private boolean autoBidActive = false;

    private static final NumberFormat MONEY_FORMAT = NumberFormat.getNumberInstance(Locale.US);
    private static final DateTimeFormatter HISTORY_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private String auctionId;
    private String currentLeaderId;
    private double currentBid;
    private double bidIncrement = 100000;
    private BigDecimal availableBalance = BigDecimal.ZERO;
    private LocalDateTime auctionEndTime;
    private String auctionStatus = "";
    private Timeline countdownTimeline;
    private MainController mainController;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        priceChart.setAnimated(false);
        priceChart.setLegendVisible(false);
        bidHistoryList.setPlaceholder(new Label("No bids yet"));
        hideBidError();
        updateWalletBalanceLabel();
        setRoomLoadingState();
        applyRoleVisibility();
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
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
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
        auctionEndTime = null;
        auctionStatus = "";
        itemName.setText("Loading auction...");
        applyRoleVisibility();
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
        setBiddingControlsDisabled(true);
    }

    private void applyAuctionData(JsonObject data) {
        if (data == null) {
            showBidError("Auction data is empty.");
            return;
        }

        auctionId = stringValue(data, "id", auctionId);
        currentLeaderId = stringValue(data, "currentHighestBidderId", currentLeaderId);
        currentBid = doubleValue(data, "currentPrice", 0);
        bidIncrement = doubleValue(data, "bidIncrement", 100000);
        auctionEndTime = parseTime(stringValue(data, "endTime", null));
        auctionStatus = stringValue(data, "status", "");

        itemName.setText(stringValue(data, "itemName", "Untitled item"));
        applyRoleVisibility();
        itemCategory.setText(formatCategory(stringValue(data, "category", "Other")));
        itemCondition.setText(formatStatus(auctionStatus));
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
        refreshWalletBalance();
        ensureSocketListener();
        if (isAuctionOpenForBidding()) {
            fetchAutoBidStatus();
        } else {
            resetAutoBidUi();
        }
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
            setBiddingControlsDisabled(true);
            return;
        }
        if (!isOngoingStatus(auctionStatus)) {
            timeLeft.setText(isPendingStatus(auctionStatus) ? "Pending" : "00:00:00 (Ended)");
            setBiddingControlsDisabled(true);
            return;
        }
        long secondsBetween = ChronoUnit.SECONDS.between(LocalDateTime.now(), auctionEndTime);
        if (secondsBetween <= 0) {
            timeLeft.setText("00:00:00 (Ended)");
            setBiddingControlsDisabled(true);
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
        setBiddingControlsDisabled(false);
    }

    private void setBiddingControlsDisabled(boolean disabled) {
        boolean shouldDisable = disabled || !isBidderRole();
        if (bidAmountField != null) bidAmountField.setDisable(shouldDisable);
        if (placeBidBtn != null) placeBidBtn.setDisable(shouldDisable);
        if (quick100k != null) quick100k.setDisable(shouldDisable);
        if (quick200k != null) quick200k.setDisable(shouldDisable);
        if (quick500k != null) quick500k.setDisable(shouldDisable);
        if (quick1m != null) quick1m.setDisable(shouldDisable);
        if (autoBidToggle != null) autoBidToggle.setDisable(shouldDisable);
        if (applyAutoBidBtn != null && shouldDisable) applyAutoBidBtn.setDisable(true);
        if (shouldDisable && !isAuctionOpenForBidding()) {
            resetAutoBidUi();
        }
    }

    private void applyRoleVisibility() {
        boolean seller = isSellerRole();
        if (bidControlsCard != null) {
            bidControlsCard.setManaged(!seller);
            bidControlsCard.setVisible(!seller);
        }
        if (seller) {
            setBiddingControlsDisabled(true);
            hideBidError();
        }
    }

    private boolean isSellerRole() {
        UserSession session = UserSession.getInstance();
        return session != null && "seller".equalsIgnoreCase(session.getRole());
    }

    private boolean isBidderRole() {
        UserSession session = UserSession.getInstance();
        return session != null && "bidder".equalsIgnoreCase(session.getRole());
    }

    private boolean isAuctionOpenForBidding() {
        return isBidderRole()
                && isOngoingStatus(auctionStatus)
                && auctionEndTime != null
                && ChronoUnit.SECONDS.between(LocalDateTime.now(), auctionEndTime) > 0;
    }

    private void resetAutoBidUi() {
        autoBidActive = false;
        if (autoBidToggle != null) {
            autoBidToggle.setSelected(false);
        }
        if (autoBidPanel != null) {
            autoBidPanel.setManaged(false);
            autoBidPanel.setVisible(false);
        }
        if (autoBidMax != null) {
            autoBidMax.clear();
            autoBidMax.setDisable(true);
        }
        if (applyAutoBidBtn != null) {
            applyAutoBidBtn.setText("Enable Auto Bid");
            applyAutoBidBtn.setDisable(true);
        }
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
                setBiddingControlsDisabled(!isAuctionOpenForBidding());
                showBidError(stringValue(json, "message", "Bid failed."));
                return;
            }

            if ("BID_SUCCESS".equals(action) || "BID_UPDATE".equals(action)) {
                setBiddingControlsDisabled(!isAuctionOpenForBidding());
                hideBidError();
                if (data.has("currentHighestBidderId")) {
                    currentLeaderId = stringValue(data, "currentHighestBidderId", currentLeaderId);
                }
                if ("BID_SUCCESS".equals(action) && data.has("balance") && !data.get("balance").isJsonNull()) {
                    updateSessionBalance(data.get("balance").getAsBigDecimal());
                } else {
                    refreshWalletBalance();
                }
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
        } catch (Exception e) {
            logger.warn("Failed to process socket message", e);
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
        if (!isBidderRole()) {
            showBidError("Only bidders can place bids.");
            return;
        }
        if (session == null || session.getUserId() == null || session.getUserId().isBlank()) {
            showBidError("Please log in as a bidder before placing a bid.");
            return;
        }
        if (auctionId == null || auctionId.isBlank()) {
            showBidError("Auction is not ready yet.");
            return;
        }
        if (!isAuctionOpenForBidding()) {
            showBidError("Bidding is only available for ongoing auctions.");
            return;
        }

        BigDecimal requestedBid = BigDecimal.valueOf(amount);
        BigDecimal spendable = availableForBid(session);
        if (requestedBid.compareTo(spendable) > 0) {
            showBidError("Your balance is not enough. Available: " + formatPrice(spendable.doubleValue()));
            return;
        }

        hideBidError();
        placeBidBtn.setDisable(true);
        ensureSocketListener();
        if (Client.getInstance().isConnected()) {
            Client.getInstance().sendBid(auctionId, session.getUserId(), amount);
        } else {
            setBiddingControlsDisabled(!isAuctionOpenForBidding());
        }
    }

    @FXML private void onAutoBidToggle() {
        if (!isAuctionOpenForBidding()) {
            resetAutoBidUi();
            setBiddingControlsDisabled(true);
            return;
        }
        boolean active = autoBidToggle.isSelected();
        autoBidPanel.setManaged(active);
        autoBidPanel.setVisible(active);
        if (active) {
            autoBidMax.setDisable(autoBidActive);
            applyAutoBidBtn.setDisable(false);
        }
        if (active) {
            fetchAutoBidStatus();
        }
    }

    @FXML private void onApplyAutoBid() {
        UserSession session = UserSession.getInstance();
        if (session == null || session.getUserId() == null || session.getUserId().isBlank()) {
            showBidError("Please sign in to use Auto Bid.");
            return;
        }
        if (auctionId == null || auctionId.isBlank()) {
            showBidError("The auction is not ready yet.");
            return;
        }
        if (!isAuctionOpenForBidding()) {
            showBidError("Auto Bid is only available for ongoing auctions.");
            resetAutoBidUi();
            setBiddingControlsDisabled(true);
            return;
        }

        hideBidError();

        if (autoBidActive) {
            applyAutoBidBtn.setDisable(true);
            Task<Void> task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    new ApiClient().disableAutoBid(auctionId, session.getUserId());
                    return null;
                }
            };
            task.setOnSucceeded(e -> {
                applyAutoBidBtn.setDisable(false);
                autoBidActive = false;
                autoBidMax.setDisable(false);
                applyAutoBidBtn.setText("Enable Auto Bid");
                showBidError("Auto Bid has been disabled.");
            });
            task.setOnFailed(e -> {
                applyAutoBidBtn.setDisable(false);
                showBidError("Failed to disable Auto Bid: " + ApiClient.toDisplayMessage(task.getException()));
            });
            Thread thread = new Thread(task);
            thread.setDaemon(true);
            thread.start();
        } else {
            String maxStr = autoBidMax.getText().trim();
            if (maxStr.isEmpty()) {
                showBidError("Please enter a maximum bid.");
                return;
            }
            double maxAmount;
            try {
                maxAmount = Double.parseDouble(maxStr);
            } catch (NumberFormatException e) {
                showBidError("Maximum bid is invalid.");
                return;
            }

            if (maxAmount <= currentBid) {
                showBidError("Maximum bid must be greater than the current price: " + formatPrice(currentBid));
                return;
            }

            applyAutoBidBtn.setDisable(true);
            Task<JsonObject> task = new Task<JsonObject>() {
                @Override
                protected JsonObject call() throws Exception {
                    return new ApiClient().enableAutoBid(auctionId, session.getUserId(), maxAmount);
                }
            };
            task.setOnSucceeded(e -> {
                applyAutoBidBtn.setDisable(false);
                autoBidActive = true;
                autoBidMax.setDisable(true);
                applyAutoBidBtn.setText("Disable Auto Bid");
                showBidError("Auto Bid has been enabled.");
            });
            task.setOnFailed(e -> {
                applyAutoBidBtn.setDisable(false);
                showBidError("Failed to enable Auto Bid: " + ApiClient.toDisplayMessage(task.getException()));
            });
            Thread thread = new Thread(task);
            thread.setDaemon(true);
            thread.start();
        }
    }

    private void fetchAutoBidStatus() {
        UserSession session = UserSession.getInstance();
        if (session == null || session.getUserId() == null || auctionId == null) {
            return;
        }
        if (!isAuctionOpenForBidding()) {
            resetAutoBidUi();
            return;
        }

        Task<JsonObject> task = new Task<JsonObject>() {
            @Override
            protected JsonObject call() throws Exception {
                return new ApiClient().getAutoBidStatus(auctionId, session.getUserId());
            }
        };

        task.setOnSucceeded(e -> {
            if (!isAuctionOpenForBidding()) {
                resetAutoBidUi();
                return;
            }
            JsonObject status = task.getValue();
            if (status != null && status.has("active") && status.get("active").getAsBoolean()) {
                autoBidActive = true;
                double maxAmount = status.get("maxAmount").getAsDouble();
                autoBidMax.setText(String.format(Locale.US, "%.0f", maxAmount));
                applyAutoBidBtn.setText("Disable Auto Bid");
                applyAutoBidBtn.setDisable(false);
                autoBidMax.setDisable(true);

                autoBidToggle.setSelected(true);
                autoBidPanel.setManaged(true);
                autoBidPanel.setVisible(true);
            } else {
                autoBidActive = false;
                if (!autoBidToggle.isSelected()) {
                    autoBidMax.clear();
                }
                applyAutoBidBtn.setText("Enable Auto Bid");
                applyAutoBidBtn.setDisable(false);
                autoBidMax.setDisable(false);
            }
        });

        task.setOnFailed(e -> {
            if (!isAuctionOpenForBidding()) {
                resetAutoBidUi();
                return;
            }
            autoBidActive = false;
            applyAutoBidBtn.setText("Enable Auto Bid");
            applyAutoBidBtn.setDisable(false);
            autoBidMax.setDisable(false);
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
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

    private void refreshWalletBalance() {
        UserSession session = UserSession.getInstance();
        if (session == null || session.getUserId() == null) {
            availableBalance = BigDecimal.ZERO;
            updateWalletBalanceLabel();
            return;
        }

        updateSessionBalance(session.getBalance());

        Task<JsonObject> task = new Task<JsonObject>() {
            @Override
            protected JsonObject call() throws Exception {
                return new ApiClient().getUserProfile(session.getUserId());
            }
        };
        task.setOnSucceeded(e -> {
            JsonObject profile = task.getValue();
            if (profile != null && profile.has("balance") && !profile.get("balance").isJsonNull()) {
                updateSessionBalance(profile.get("balance").getAsBigDecimal());
            }
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private BigDecimal availableForBid(UserSession session) {
        BigDecimal spendable = availableBalance != null ? availableBalance : BigDecimal.ZERO;
        if (session != null && session.getUserId() != null && session.getUserId().equals(currentLeaderId)) {
            spendable = spendable.add(BigDecimal.valueOf(currentBid));
        }
        return spendable;
    }

    private void updateSessionBalance(BigDecimal balance) {
        BigDecimal safeBalance = balance != null ? balance : BigDecimal.ZERO;
        availableBalance = safeBalance;
        UserSession session = UserSession.getInstance();
        if (session != null) {
            session.setBalance(safeBalance);
        }
        updateWalletBalanceLabel();
        if (mainController != null) {
            mainController.refreshUserBalance();
        }
    }

    private void updateWalletBalanceLabel() {
        if (walletBalance != null) {
            walletBalance.setText("Wallet: " + formatPrice((availableBalance != null ? availableBalance : BigDecimal.ZERO).doubleValue()));
        }
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
        if (isPendingStatus(value)) {
            return "Pending";
        }
        if (isOngoingStatus(value)) {
            return "Ongoing";
        }
        if (isEndedStatus(value)) {
            return "Ended";
        }
        if (value == null || value.isBlank()) {
            return "";
        }
        return formatCategory(value);
    }

    private boolean isPendingStatus(String value) {
        return hasStatus(value, "PENDING", "PENDING_APPROVAL");
    }

    private boolean isOngoingStatus(String value) {
        return hasStatus(value, "RUNNING", "LIVE", "ACTIVE", "APPROVED", "ONGOING");
    }

    private boolean isEndedStatus(String value) {
        return hasStatus(value, "FINISHED", "ENDED", "COMPLETED", "PAID", "SOLD");
    }

    private boolean hasStatus(String value, String... candidates) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String normalized = value.trim().replace(' ', '_').toUpperCase(Locale.ROOT);
        for (String candidate : candidates) {
            if (candidate.equals(normalized)) {
                return true;
            }
        }
        return false;
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
