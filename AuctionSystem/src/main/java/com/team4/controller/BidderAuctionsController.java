package com.team4.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.team4.client.ApiClient;
import com.team4.util.UserSession;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

import java.math.BigDecimal;
import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Locale;
import java.util.ResourceBundle;

public class BidderAuctionsController implements Initializable {

    @FXML private Label liveCount, upcomingCount, endedCount, totalBids;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortBox;
    @FXML private FlowPane auctionsGrid;
    @FXML private HBox categoryPills, statusPills;

    private static final NumberFormat MONEY_FORMAT = NumberFormat.getNumberInstance(Locale.US);
    private static final DateTimeFormatter CARD_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM HH:mm");
    private final ObservableList<AuctionCardData> auctions = FXCollections.observableArrayList();

    private String selectedCategory = "All";
    private String selectedStatus = "All";
    private MainController mainController;

    public void setMainController(MainController mc) {
        this.mainController = mc;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupSort();
        setupFilters();
        loadAuctionCards();
    }

    private void setupSort() {
        sortBox.getItems().setAll("Ending soon", "Newest", "Price: Low to High", "Price: High to Low");
        sortBox.getSelectionModel().selectFirst();
        sortBox.valueProperty().addListener((obs, oldValue, newValue) -> renderAuctions());
    }

    private void setupFilters() {
        searchField.textProperty().addListener((obs, oldValue, newValue) -> renderAuctions());
        bindFilterButtons(categoryPills, true);
        bindFilterButtons(statusPills, false);
    }

    private void bindFilterButtons(HBox container, boolean categoryFilter) {
        for (javafx.scene.Node node : container.getChildren()) {
            if (node instanceof Button) {
                Button button = (Button) node;
                button.setOnAction(e -> {
                    if (categoryFilter) {
                        selectedCategory = button.getText();
                    } else {
                        selectedStatus = button.getText();
                    }
                    updateFilterStyles(container, button);
                    renderAuctions();
                });
            }
        }
    }

    private void updateFilterStyles(HBox container, Button activeButton) {
        for (javafx.scene.Node node : container.getChildren()) {
            if (node instanceof Button) {
                Button button = (Button) node;
                button.getStyleClass().removeAll("filter-pill-active", "filter-pill");
                button.getStyleClass().add(button == activeButton ? "filter-pill-active" : "filter-pill");
            }
        }
    }

    private void loadAuctionCards() {
        showLoading("Loading auctions...");

        Task<JsonArray> task = new Task<JsonArray>() {
            @Override
            protected JsonArray call() throws Exception {
                return new ApiClient().getPublicAuctions();
            }
        };

        task.setOnSucceeded(e -> {
            auctions.clear();
            JsonArray data = task.getValue();
            if (data != null) {
                UserSession session = UserSession.getInstance();
                boolean isSeller = session != null && "seller".equalsIgnoreCase(session.getRole());
                String currentUserId = session != null ? session.getUserId() : "";

                for (JsonElement element : data) {
                    if (element.isJsonObject()) {
                        AuctionCardData cardData = new AuctionCardData(element.getAsJsonObject());
                        if (!isSeller || currentUserId.equals(cardData.sellerId)) {
                            auctions.add(cardData);
                        }
                    }
                }
            }
            updateStats();
            renderAuctions();
        });

        task.setOnFailed(e -> {
            auctions.clear();
            updateStats();
            showLoading("Could not load auctions. " + ApiClient.toDisplayMessage(task.getException()));
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void updateStats() {
        long ongoing = auctions.stream().filter(this::isOngoing).count();
        long upcoming = auctions.stream().filter(this::isUpcoming).count();
        long ended = auctions.stream().filter(this::isEnded).count();
        long bids = auctions.stream().mapToLong(a -> a.bidCount).sum();

        liveCount.setText(String.valueOf(ongoing));
        upcomingCount.setText(String.valueOf(upcoming));
        endedCount.setText(String.valueOf(ended));
        totalBids.setText(String.valueOf(bids));
    }

    private void renderAuctions() {
        auctionsGrid.getChildren().clear();

        auctions.stream()
                .filter(this::matchesFilters)
                .sorted(currentSort())
                .forEach(auction -> auctionsGrid.getChildren().add(createAuctionCard(auction)));

        if (auctionsGrid.getChildren().isEmpty()) {
            showLoading("No auctions found");
        }
    }

    private boolean matchesFilters(AuctionCardData auction) {
        String keyword = searchField.getText() == null
                ? ""
                : searchField.getText().trim().toLowerCase(Locale.ROOT);

        boolean matchesKeyword = keyword.isEmpty()
                || auction.itemName.toLowerCase(Locale.ROOT).contains(keyword)
                || auction.sellerName.toLowerCase(Locale.ROOT).contains(keyword)
                || auction.category.toLowerCase(Locale.ROOT).contains(keyword);

        boolean matchesCategory = "All".equals(selectedCategory)
                || normalizeCategory(selectedCategory).equalsIgnoreCase(auction.category);

        boolean matchesStatus = true;
        if (!"All".equals(selectedStatus)) {
            if ("Ongoing".equalsIgnoreCase(selectedStatus)) {
                matchesStatus = isOngoing(auction);
            } else if ("Pending".equalsIgnoreCase(selectedStatus) || "Upcoming".equalsIgnoreCase(selectedStatus)) {
                matchesStatus = isUpcoming(auction);
            } else if ("Ended".equalsIgnoreCase(selectedStatus)) {
                matchesStatus = isEnded(auction);
            }
        }

        return matchesKeyword && matchesCategory && matchesStatus;
    }

    private Comparator<AuctionCardData> currentSort() {
        String sort = sortBox.getValue();
        if ("Newest".equals(sort)) {
            return Comparator.comparing((AuctionCardData a) -> a.startTime, Comparator.nullsLast(Comparator.naturalOrder())).reversed();
        }
        if ("Price: Low to High".equals(sort)) {
            return Comparator.comparing(a -> a.currentPrice);
        }
        if ("Price: High to Low".equals(sort)) {
            return Comparator.comparing((AuctionCardData a) -> a.currentPrice).reversed();
        }
        return Comparator.comparing(a -> a.endTime, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private VBox createAuctionCard(AuctionCardData auction) {
        VBox card = new VBox(12);
        card.getStyleClass().add("auction-card");
        card.setPrefWidth(260);
        card.setMinWidth(260);

        StackPane visual = new StackPane();
        visual.getStyleClass().add("auction-card-visual");
        VBox visualContent = new VBox(3);
        visualContent.setAlignment(Pos.CENTER);
        Label visualText = new Label(productInitials(auction.itemName));
        visualText.getStyleClass().add("auction-card-visual-text");
        visualText.setTextAlignment(TextAlignment.CENTER);
        visualText.setAlignment(Pos.CENTER);
        Label visualCategory = new Label(formatCategory(auction.category).toUpperCase(Locale.ROOT));
        visualCategory.getStyleClass().add("auction-card-visual-category");
        visualContent.getChildren().addAll(visualText, visualCategory);
        visual.getChildren().add(visualContent);

        HBox topLine = new HBox(8);
        topLine.setAlignment(Pos.CENTER_LEFT);
        Label category = new Label(formatCategory(auction.category));
        category.getStyleClass().addAll("badge", categoryBadge(auction.category));
        Label status = new Label(formatStatus(auction));
        status.getStyleClass().addAll("badge", statusBadge(auction));
        topLine.getChildren().addAll(category, status);

        Label title = new Label(auction.itemName);
        title.getStyleClass().add("auction-card-title");
        title.setWrapText(true);

        Label desc = new Label(auction.description);
        desc.getStyleClass().add("auction-card-desc");
        desc.setWrapText(true);
        desc.setMaxHeight(42);

        Label seller = new Label("Seller: " + auction.sellerName);
        seller.getStyleClass().add("muted-text");
        seller.setWrapText(true);

        VBox priceBox = new VBox(3);
        Label currentLabel = new Label("Current price");
        currentLabel.getStyleClass().add("price-mini-label");
        Label currentPrice = new Label(formatMoney(auction.currentPrice));
        currentPrice.getStyleClass().add("auction-card-price");
        priceBox.getChildren().addAll(currentLabel, currentPrice);

        VBox details = new VBox(5);
        details.getStyleClass().add("auction-card-details");
        details.getChildren().addAll(
                createCardInfoRow("Start", formatMoney(auction.startingPrice)),
                createCardInfoRow("Step", formatMoney(auction.bidIncrement)),
                createCardInfoRow("Leader", auction.currentLeaderName.isBlank() ? "No bids yet" : auction.currentLeaderName),
                createCardInfoRow("Store", auction.sellerStoreName.isBlank() ? "Not provided" : auction.sellerStoreName),
                createCardInfoRow("Ends", formatCardTime(auction.endTime))
        );

        HBox meta = new HBox(10);
        meta.setAlignment(Pos.CENTER_LEFT);
        Label bids = new Label(auction.bidCount + " bids");
        bids.getStyleClass().add("muted-text-sm");
        Label time = new Label(timeLeftText(auction.endTime));
        time.getStyleClass().add("timer-urgent");
        meta.getChildren().addAll(bids, new Label("-"), time);

        Button joinBtn = new Button("Enter Room");
        joinBtn.getStyleClass().add("btn-primary");
        joinBtn.setMaxWidth(Double.MAX_VALUE);
        joinBtn.setMouseTransparent(true);

        card.setOnMouseClicked(e -> openBiddingRoom(auction.id));
        card.getChildren().addAll(visual, topLine, title, desc, seller, priceBox, details, meta, joinBtn);
        return card;
    }

    private HBox createCardInfoRow(String labelText, String valueText) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        Label label = new Label(labelText);
        label.getStyleClass().add("auction-card-info-label");
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        Label value = new Label(valueText);
        value.getStyleClass().add("auction-card-info-value");
        value.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
        value.setMaxWidth(138);
        row.getChildren().addAll(label, spacer, value);
        return row;
    }

    private void openBiddingRoom(String auctionId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/team4/view/bidding_room.fxml"));
            Parent page = loader.load();
            BiddingRoomController controller = loader.getController();
            controller.setMainController(mainController);
            controller.loadAuction(auctionId);

            if (mainController != null) {
                // Path 1: Navigate via MainController (proper way)
                mainController.navigateTo(page, "Auction Room", "Review auction details and bids");
            } else {
                // Path 2: Fallback — look up contentArea from scene (may be null if scene not ready)
                javafx.scene.Scene scene = auctionsGrid.getScene();
                if (scene == null) {
                    throw new IllegalStateException("Scene not ready - cannot navigate");
                }
                StackPane contentArea = (StackPane) scene.getRoot().lookup("#contentArea");
                if (contentArea == null) {
                    throw new IllegalStateException("Cannot find #contentArea in scene");
                }
                contentArea.getChildren().setAll(page);

                Label pageTitle = (Label) scene.getRoot().lookup("#pageTitle");
                Label pageSubtitle = (Label) scene.getRoot().lookup("#pageSubtitle");
                if (pageTitle != null) pageTitle.setText("Auction Room");
                if (pageSubtitle != null) pageSubtitle.setText("Review auction details and bids");
            }
        } catch (Exception ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Auction Room");
            alert.setHeaderText(null);
            alert.setContentText("Could not open auction room. " + ApiClient.toDisplayMessage(ex));
            alert.show();
        }
    }

    private void showLoading(String text) {
        auctionsGrid.getChildren().clear();
        VBox empty = new VBox(10);
        empty.setAlignment(Pos.CENTER);
        empty.setPrefWidth(860);
        empty.setPrefHeight(240);
        empty.getStyleClass().add("empty-state");

        boolean loading = text != null && text.toLowerCase(Locale.ROOT).contains("loading");
        boolean error = text != null && text.toLowerCase(Locale.ROOT).contains("could not");
        Label mark = new Label(loading ? "..." : "No Data");
        mark.getStyleClass().add("empty-state-mark");
        Label title = new Label(loading ? "Loading auctions" : error ? "Auctions unavailable" : "No auctions found");
        title.getStyleClass().add("empty-state-title");
        Label message = new Label(loading
                ? "Fetching the latest auction list."
                : error ? text : "Try another keyword, category, or status filter.");
        message.getStyleClass().add("empty-state-text");
        message.setWrapText(true);
        message.setMaxWidth(420);

        empty.getChildren().addAll(mark, title, message);
        auctionsGrid.getChildren().add(empty);
    }

    private String normalizeCategory(String value) {
        if ("Vehicles".equals(value)) {
            return "VEHICLE";
        }
        if ("Collectibles".equals(value)) {
            return "COLLECTIBLE";
        }
        return value == null ? "" : value.toUpperCase(Locale.ROOT);
    }

    private String normalizeStatus(String value) {
        if ("Ongoing".equals(value)) {
            return "RUNNING";
        }
        if ("Ended".equals(value)) {
            return "FINISHED";
        }
        if ("Pending".equals(value)) {
            return "PENDING";
        }
        return value == null ? "" : value.toUpperCase(Locale.ROOT);
    }

    private String canonicalStatus(String value) {
        if (isPendingStatus(value)) {
            return "PENDING";
        }
        if (isOngoingStatus(value)) {
            return "RUNNING";
        }
        if (isEndedStatus(value)) {
            return "FINISHED";
        }
        return value == null ? "" : value.toUpperCase(Locale.ROOT);
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

    private String statusBadge(String value) {
        if (isOngoingStatus(value)) {
            return "badge-green";
        }
        if (isEndedStatus(value)) {
            return "badge-red";
        }
        if (isPendingStatus(value)) {
            return "badge-yellow";
        }
        return "badge-purple";
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

    private boolean isOngoing(AuctionCardData a) {
        return isOngoingStatus(a.status) && (a.startTime == null || !LocalDateTime.now().isBefore(a.startTime)) && !isEnded(a);
    }

    private boolean isUpcoming(AuctionCardData a) {
        return isPendingStatus(a.status) || (isOngoingStatus(a.status) && a.startTime != null && LocalDateTime.now().isBefore(a.startTime));
    }

    private boolean isEnded(AuctionCardData a) {
        return isEndedStatus(a.status) || (a.endTime != null && LocalDateTime.now().isAfter(a.endTime));
    }

    private String formatStatus(AuctionCardData a) {
        if (isUpcoming(a)) {
            return "Upcoming";
        }
        if (isOngoing(a)) {
            return "Ongoing";
        }
        if (isEnded(a)) {
            return "Ended";
        }
        return formatStatus(a.status);
    }

    private String statusBadge(AuctionCardData a) {
        if (isUpcoming(a)) {
            return "badge-yellow";
        }
        if (isOngoing(a)) {
            return "badge-green";
        }
        if (isEnded(a)) {
            return "badge-red";
        }
        return statusBadge(a.status);
    }

    private String categoryBadge(String category) {
        if ("ELECTRONICS".equalsIgnoreCase(category)) {
            return "badge-blue";
        }
        if ("FASHION".equalsIgnoreCase(category)) {
            return "badge-purple";
        }
        if ("VEHICLE".equalsIgnoreCase(category)) {
            return "badge-yellow";
        }
        if ("ART".equalsIgnoreCase(category) || "COLLECTIBLE".equalsIgnoreCase(category)) {
            return "badge-red";
        }
        return "badge-purple";
    }

    private String formatMoney(BigDecimal value) {
        if (value == null) {
            return "0 VND";
        }
        return MONEY_FORMAT.format(value) + " VND";
    }

    private String formatCardTime(LocalDateTime value) {
        return value == null ? "Not scheduled" : value.format(CARD_TIME_FORMAT);
    }

    private String productInitials(String itemName) {
        if (itemName == null || itemName.isBlank()) {
            return "IT";
        }
        String[] parts = itemName.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();
        for (String part : parts) {
            if (!part.isBlank()) {
                initials.append(part.substring(0, 1).toUpperCase(Locale.ROOT));
            }
            if (initials.length() == 3) {
                break;
            }
        }
        return initials.length() == 0 ? "IT" : initials.toString();
    }

    private String timeLeftText(LocalDateTime endTime) {
        if (endTime == null) {
            return "No end time";
        }
        long seconds = ChronoUnit.SECONDS.between(LocalDateTime.now(), endTime);
        if (seconds <= 0) {
            return "Ended";
        }
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        if (days > 0) {
            return days + "d " + hours + "h left";
        }
        long minutes = (seconds % 3600) / 60;
        return String.format("%02dh %02dm left", hours, minutes);
    }

    private static class AuctionCardData {
        private final String id;
        private final String itemName;
        private final String description;
        private final String category;
        private final String sellerName;
        private final String sellerStoreName;
        private final String sellerId;
        private final String status;
        private final String currentLeaderName;
        private final BigDecimal startingPrice;
        private final BigDecimal currentPrice;
        private final BigDecimal bidIncrement;
        private final long bidCount;
        private final LocalDateTime startTime;
        private final LocalDateTime endTime;

        private AuctionCardData(JsonObject obj) {
            this.id = stringValue(obj, "id");
            this.itemName = fallback(stringValue(obj, "itemName"), "Untitled item");
            this.description = fallback(stringValue(obj, "itemDescription"), "No description provided.");
            this.category = stringValue(obj, "category");
            this.sellerName = fallback(stringValue(obj, "sellerName"), "Unknown Seller");
            this.sellerStoreName = stringValue(obj, "sellerStoreName");
            this.sellerId = stringValue(obj, "sellerId");
            this.status = fallback(stringValue(obj, "status"), "RUNNING");
            this.currentLeaderName = stringValue(obj, "currentHighestBidderName");
            this.startingPrice = moneyValue(obj, "startingPrice");
            this.currentPrice = moneyValue(obj, "currentPrice");
            this.bidIncrement = moneyValue(obj, "bidIncrement");
            this.bidCount = longValue(obj, "bidCount");
            this.startTime = timeValue(obj, "startTime");
            this.endTime = timeValue(obj, "endTime");
        }

        private static String stringValue(JsonObject obj, String key) {
            return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : "";
        }

        private static String fallback(String value, String fallback) {
            return value == null || value.isBlank() ? fallback : value;
        }

        private static BigDecimal moneyValue(JsonObject obj, String key) {
            return obj.has(key) && !obj.get(key).isJsonNull()
                    ? obj.get(key).getAsBigDecimal()
                    : BigDecimal.ZERO;
        }

        private static long longValue(JsonObject obj, String key) {
            return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsLong() : 0;
        }

        private static LocalDateTime timeValue(JsonObject obj, String key) {
            if (!obj.has(key) || obj.get(key).isJsonNull()) {
                return null;
            }
            try {
                return LocalDateTime.parse(obj.get(key).getAsString());
            } catch (Exception ignored) {
                return null;
            }
        }
    }
}
