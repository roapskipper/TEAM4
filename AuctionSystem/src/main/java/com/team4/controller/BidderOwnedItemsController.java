package com.team4.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.team4.client.ApiClient;
import com.team4.util.UserSession;

import javafx.beans.binding.Bindings;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ResourceBundle;

public class BidderOwnedItemsController implements Initializable {

    @FXML private Label ownedCount;
    @FXML private Label totalValue;
    @FXML private Label latestItem;
    @FXML private FlowPane ownedItemsGrid;

    private static final NumberFormat MONEY_FORMAT = NumberFormat.getNumberInstance(Locale.US);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        if (ownedItemsGrid != null) {
            ownedItemsGrid.setAlignment(Pos.TOP_CENTER);
        }
        loadOwnedItems();
    }

    private void loadOwnedItems() {
        showMessage("Loading owned items...");

        UserSession session = UserSession.getInstance();
        if (session == null || session.getUserId() == null || session.getUserId().isBlank()) {
            showMessage("Please log in as a bidder to view owned items.");
            return;
        }

        Task<JsonArray> task = new Task<JsonArray>() {
            @Override
            protected JsonArray call() throws Exception {
                return new ApiClient().getOwnedItems(session.getUserId());
            }
        };

        task.setOnSucceeded(e -> renderOwnedItems(task.getValue()));
        task.setOnFailed(e -> {
            ownedCount.setText("0");
            totalValue.setText("0 VND");
            latestItem.setText("--");
            showMessage("Could not load owned items. " + ApiClient.toDisplayMessage(task.getException()));
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void renderOwnedItems(JsonArray items) {
        ownedItemsGrid.getChildren().clear();

        int count = items == null ? 0 : items.size();
        ownedCount.setText(String.valueOf(count));

        BigDecimal total = BigDecimal.ZERO;
        String newestName = "--";
        LocalDateTime newestTime = null;

        if (items != null) {
            for (JsonElement element : items) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject item = element.getAsJsonObject();
                total = total.add(moneyValue(item, "wonPrice", moneyValue(item, "startingPrice", BigDecimal.ZERO)));
                LocalDateTime wonAt = timeValue(item, "wonAt");
                if (wonAt != null && (newestTime == null || wonAt.isAfter(newestTime))) {
                    newestTime = wonAt;
                    newestName = stringValue(item, "name", "--");
                }
                ownedItemsGrid.getChildren().add(createOwnedItemCard(item));
            }
        }

        totalValue.setText(formatMoney(total));
        latestItem.setText(newestName);

        if (count == 0) {
            showMessage("You have not won any items yet.");
        }
    }

    private VBox createOwnedItemCard(JsonObject item) {
        VBox card = new VBox(12);
        card.getStyleClass().add("owned-item-card");
        card.setPrefWidth(270);
        card.setMinWidth(270);

        HBox topLine = new HBox(8);
        topLine.setAlignment(Pos.CENTER_LEFT);
        Label category = new Label(formatCategory(stringValue(item, "category", "Other")));
        category.getStyleClass().addAll("badge", categoryBadge(stringValue(item, "category", "")));
        Label owned = new Label("Owned");
        owned.getStyleClass().addAll("badge", "badge-green");
        topLine.getChildren().addAll(category, owned);

        Label title = new Label(stringValue(item, "name", "Untitled item"));
        title.getStyleClass().add("auction-card-title");
        title.setWrapText(true);

        Label description = new Label(stringValue(item, "description", "No description provided."));
        description.getStyleClass().add("auction-card-desc");
        description.setWrapText(true);
        description.setMaxHeight(46);

        VBox priceBox = new VBox(3);
        Label priceLabel = new Label(item.has("wonPrice") ? "Winning price" : "Estimated value");
        priceLabel.getStyleClass().add("price-mini-label");
        Label price = new Label(formatMoney(moneyValue(item, "wonPrice", moneyValue(item, "startingPrice", BigDecimal.ZERO))));
        price.getStyleClass().add("auction-card-price");
        priceBox.getChildren().addAll(priceLabel, price);

        HBox meta = new HBox(8);
        meta.setAlignment(Pos.CENTER_LEFT);
        Label date = new Label(formatWonAt(timeValue(item, "wonAt")));
        date.getStyleClass().add("muted-text-sm");
        meta.getChildren().add(date);

        Region spacer = new Region();
        VBox.setVgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        card.getChildren().addAll(topLine, title, description, spacer, priceBox, meta);
        return card;
    }

    private void showMessage(String text) {
        ownedItemsGrid.getChildren().clear();
        VBox empty = new VBox(10);
        empty.setAlignment(Pos.CENTER);
        empty.prefWidthProperty().bind(Bindings.createDoubleBinding(
                () -> Math.max(420, ownedItemsGrid.getWidth() - 8),
                ownedItemsGrid.widthProperty()));
        empty.setPrefHeight(240);
        empty.setMaxWidth(Double.MAX_VALUE);
        empty.getStyleClass().add("empty-state");

        boolean loading = text != null && text.toLowerCase(Locale.ROOT).contains("loading");
        boolean error = text != null && text.toLowerCase(Locale.ROOT).contains("could not");
        Label mark = new Label(loading ? "..." : "No Data");
        mark.getStyleClass().add("empty-state-mark");
        Label title = new Label(loading ? "Loading owned items" : error ? "Owned items unavailable" : "No owned items yet");
        title.getStyleClass().add("empty-state-title");
        title.setMaxWidth(Double.MAX_VALUE);
        title.setAlignment(Pos.CENTER);
        Label message = new Label(loading
                ? "Fetching the items you have won."
                : error ? text : "Won auction items will appear here after completed auctions.");
        message.getStyleClass().add("empty-state-text");
        message.setWrapText(true);
        message.setMaxWidth(Double.MAX_VALUE);
        message.setAlignment(Pos.CENTER);

        empty.getChildren().addAll(mark, title, message);
        ownedItemsGrid.getChildren().add(empty);
    }

    private String formatWonAt(LocalDateTime value) {
        return value == null ? "Won date unavailable" : "Won at " + value.format(DATE_FORMAT);
    }

    private String formatMoney(BigDecimal value) {
        return MONEY_FORMAT.format(value != null ? value : BigDecimal.ZERO) + " VND";
    }

    private String formatCategory(String value) {
        if (value == null || value.isBlank()) {
            return "Other";
        }
        String lower = value.replace("_", " ").toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
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

    private String stringValue(JsonObject obj, String key, String fallback) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : fallback;
    }

    private BigDecimal moneyValue(JsonObject obj, String key, BigDecimal fallback) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsBigDecimal() : fallback;
    }

    private LocalDateTime timeValue(JsonObject obj, String key) {
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
