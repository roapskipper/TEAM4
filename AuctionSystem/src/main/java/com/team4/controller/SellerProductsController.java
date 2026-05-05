package com.team4.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;

public class SellerProductsController implements Initializable {

    @FXML private Label totalProducts, activeAuctions, pendingProducts, soldProducts;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private Button addProductBtn;
    @FXML private TableView<?> productsTable;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadStats();
        setupCategoryFilter();
    }

    private void loadStats() {
        totalProducts.setText("24");
        activeAuctions.setText("8");
        pendingProducts.setText("3");
        soldProducts.setText("156");
    }

    private void setupCategoryFilter() {
        categoryFilter.getItems().addAll(
            "Tat ca", "Dien tu", "Xe co", "Nghe thuat", "Thoi trang", "Do suu tam"
        );
        categoryFilter.getSelectionModel().selectFirst();
    }

    @FXML private void onAddProduct() {
        System.out.println("Add product clicked");
    }
}
