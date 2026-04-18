package auctionsystemclient.controller;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

public class DashboardController {

    @FXML
    private TilePane auctionGrid;

    @FXML
    public void initialize() {
        String[] products = {"MacBook Pro M3", "iPhone 15 Pro Max", "Đồng hồ Rolex", "Giày Jordan 1", "Màn hình LG 27inch", "Bàn phím cơ Keychron"};
        String[] prices = {"45.000.000 đ", "29.990.000 đ", "350.000.000 đ", "4.500.000 đ", "7.200.000 đ", "1.800.000 đ"};

        for (int i = 0; i < products.length; i++) {
            VBox card = createProductCard(products[i], prices[i]);
            auctionGrid.getChildren().add(card);
        }
    }

    private VBox createProductCard(String name, String price) {
        VBox card = new VBox(15);
        card.setStyle("-fx-background-color: #13131a; -fx-background-radius: 12; -fx-border-color: #27273a; -fx-border-radius: 12; -fx-padding: 15; -fx-cursor: hand;");
        card.setPrefWidth(260);
        StackPane imagePlaceHolder = new StackPane();
        imagePlaceHolder.setPrefHeight(160);
        imagePlaceHolder.setStyle("-fx-background-color: linear-gradient(to bottom right, #2a2a35, #1a1a24); -fx-background-radius: 8;");
        Label icon = new Label("📦");
        icon.setStyle("-fx-font-size: 40;");
        imagePlaceHolder.getChildren().add(icon);

        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16; -fx-font-weight: bold;");

        HBox priceBox = new HBox();
        priceBox.setAlignment(Pos.CENTER_LEFT);

        Label priceLabel = new Label(price);
        priceLabel.setStyle("-fx-text-fill: #ec4899; -fx-font-size: 18; -fx-font-weight: bold;");

        Label spacer = new Label();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        spacer.setMaxWidth(Double.MAX_VALUE);

        Label timeLabel = new Label("⏱ 02:15:30");
        timeLabel.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 12;");

        priceBox.getChildren().addAll(priceLabel, spacer, timeLabel);

        Button bidBtn = new Button("Đấu giá ngay");
        bidBtn.setMaxWidth(Double.MAX_VALUE);
        bidBtn.setStyle("-fx-background-color: rgba(139, 92, 246, 0.2); -fx-text-fill: #a78bfa; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 12; -fx-cursor: hand;");

        card.setOnMouseEntered(e -> {
            card.setStyle("-fx-background-color: #1a1a24; -fx-background-radius: 12; -fx-border-color: #8b5cf6; -fx-border-radius: 12; -fx-padding: 15; -fx-cursor: hand;");
            bidBtn.setStyle("-fx-background-color: #8b5cf6; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 12; -fx-cursor: hand;");
        });

        card.setOnMouseExited(e -> {
            card.setStyle("-fx-background-color: #13131a; -fx-background-radius: 12; -fx-border-color: #27273a; -fx-border-radius: 12; -fx-padding: 15; -fx-cursor: hand;");
            bidBtn.setStyle("-fx-background-color: rgba(139, 92, 246, 0.2); -fx-text-fill: #a78bfa; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 12; -fx-cursor: hand;");
        });

        card.getChildren().addAll(imagePlaceHolder, nameLabel, priceBox, bidBtn);
        return card;
    }
}