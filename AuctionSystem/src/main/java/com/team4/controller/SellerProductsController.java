package com.team4.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

// Import các thư viện mạng và JSON
import com.team4.client.ApiClient;
import com.team4.model.Item; // Đảm bảo class Item của bạn nằm đúng thư mục này
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class SellerProductsController implements Initializable {

    @FXML private Label totalProducts, activeAuctions, pendingProducts, soldProducts;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private Button addProductBtn;
    @FXML private TableView<Item> productsTable;
    @FXML private TableColumn<Item, String> colProduct;
    @FXML private TableColumn<Item, String> colCategory;
    @FXML private TableColumn<Item, Double> colStartPrice;
    @FXML private TableColumn<Item, Double> colCurrentPrice;
    @FXML private TableColumn<Item, String> colStatus;
    @FXML private TableColumn<Item, String> colDate;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadStats();
        setupCategoryFilter();
        loadDataFromServer();
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

    private void loadDataFromServer() {
        colProduct.setCellValueFactory(new PropertyValueFactory<>("name"));
        colStartPrice.setCellValueFactory(new PropertyValueFactory<>("startingPrice"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colCurrentPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

        ApiClient apiClient = new ApiClient();
        List<Item> realDataFromServer = apiClient.getItems();
    }
}