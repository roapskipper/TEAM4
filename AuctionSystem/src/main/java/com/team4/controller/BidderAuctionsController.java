package com.team4.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import java.net.URL;
import java.util.ResourceBundle;

public class BidderAuctionsController implements Initializable {

    @FXML private Label liveCount, upcomingCount, endedCount, totalBids;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortBox;
    @FXML private FlowPane auctionsGrid;
    @FXML private HBox categoryPills, statusPills;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadStats();
        setupSort();
        loadAuctionCards();
    }

    private void loadStats() {
        liveCount.setText("12");
        upcomingCount.setText("8");
        endedCount.setText("156");
        totalBids.setText("2.4K");
    }

    private void setupSort() {
        sortBox.getItems().addAll("Moi nhat", "Ket thuc som", "Gia tang dan", "Gia giam dan");
        sortBox.getSelectionModel().selectFirst();
    }

    private void loadAuctionCards() {
    }
}
