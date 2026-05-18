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
        totalProducts.setText("...");
        activeAuctions.setText("...");
        pendingProducts.setText("...");
        soldProducts.setText("...");

        String sellerId = "currentSellerId";
        if (com.team4.util.UserSession.getInstance() != null && com.team4.util.UserSession.getInstance().getUsername() != null) {
            com.team4.model.User currentUser = new com.team4.dao.impl.UserDAOImpl().findByUsername(com.team4.util.UserSession.getInstance().getUsername());
            if (currentUser != null) {
                sellerId = currentUser.getId();
            }
        }

        final String finalSellerId = sellerId;
        javafx.concurrent.Task<com.google.gson.JsonObject> task = new javafx.concurrent.Task<com.google.gson.JsonObject>() {
            @Override
            protected com.google.gson.JsonObject call() throws Exception {
                ApiClient apiClient = new ApiClient();
                return apiClient.getSellerStats(finalSellerId);
            }
        };

        task.setOnSucceeded(e -> {
            com.google.gson.JsonObject responseObj = task.getValue();
            com.google.gson.JsonObject stats = null;
            if (responseObj.has("status") && "SUCCESS".equals(responseObj.get("status").getAsString()) && responseObj.has("data")) {
                stats = responseObj.getAsJsonObject("data");
            } else {
                stats = responseObj; // fallback if data is at root
            }

            java.text.NumberFormat formatter = java.text.NumberFormat.getInstance(java.util.Locale.US);

            totalProducts.setText(stats.has("totalProducts") ? formatter.format(stats.get("totalProducts").getAsInt()) : "N/A");
            activeAuctions.setText(stats.has("activeAuctions") ? formatter.format(stats.get("activeAuctions").getAsInt()) : "N/A");
            pendingProducts.setText(stats.has("pendingProducts") ? formatter.format(stats.get("pendingProducts").getAsInt()) : "N/A");
            soldProducts.setText(stats.has("soldProducts") ? formatter.format(stats.get("soldProducts").getAsInt()) : "N/A");
        });

        task.setOnFailed(e -> {
            totalProducts.setText("N/A");
            activeAuctions.setText("N/A");
            pendingProducts.setText("N/A");
            soldProducts.setText("N/A");
            System.err.println("Failed to load stats: " + task.getException().getMessage());
        });

        new Thread(task).start();
    }

    private void setupCategoryFilter() {
        categoryFilter.getItems().addAll(
                "All", "Electronics", "Vehicles", "Art", "Fashion", "Collectibles"
        );
        categoryFilter.getSelectionModel().selectFirst();
    }

    @FXML private void onAddProduct() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/team4/view/add_product_dialog.fxml"));
            javafx.scene.Parent root = loader.load();
            
            AddProductDialogController dialogController = loader.getController();
            
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Add Product");
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setScene(new javafx.scene.Scene(root));
            stage.showAndWait();
            
            if (dialogController.isConfirmed()) {
                String name = dialogController.getName();
                String category = dialogController.getCategory();
                double price = dialogController.getPrice();
                String desc = dialogController.getDescription();
                
                String sellerId = "currentSellerId";
                if (com.team4.util.UserSession.getInstance() != null && com.team4.util.UserSession.getInstance().getUsername() != null) {
                    com.team4.model.User currentUser = new com.team4.dao.impl.UserDAOImpl().findByUsername(com.team4.util.UserSession.getInstance().getUsername());
                    if (currentUser != null) {
                        sellerId = currentUser.getId();
                    }
                }
                final String finalSellerId = sellerId;
                
                javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<Void>() {
                    @Override
                    protected Void call() throws Exception {
                        ApiClient apiClient = new ApiClient();
                        apiClient.createItem(finalSellerId, name, category, price, desc);
                        return null;
                    }
                };
                
                task.setOnSucceeded(e -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "Product added successfully!");
                    alert.show();
                    loadDataFromServer();
                    loadStats();
                });
                
                task.setOnFailed(e -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to add product: " + task.getException().getMessage());
                    alert.show();
                });
                
                new Thread(task).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Could not open dialog: " + e.getMessage());
            alert.show();
        }
    }

    private void loadDataFromServer() {
        colProduct.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colStartPrice.setCellValueFactory(new PropertyValueFactory<>("startingPrice"));
        colCurrentPrice.setCellValueFactory(new PropertyValueFactory<>("currentBidPrice"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

        String sellerId = "currentSellerId";
        if (com.team4.util.UserSession.getInstance() != null && com.team4.util.UserSession.getInstance().getUsername() != null) {
            com.team4.model.User currentUser = new com.team4.dao.impl.UserDAOImpl().findByUsername(com.team4.util.UserSession.getInstance().getUsername());
            if (currentUser != null) {
                sellerId = currentUser.getId();
            }
        }

        final String finalSellerId = sellerId;
        javafx.concurrent.Task<List<Item>> task = new javafx.concurrent.Task<List<Item>>() {
            @Override
            protected List<Item> call() throws Exception {
                return ItemService.getSellerItems(finalSellerId);
            }
        };

        task.setOnSucceeded(e -> {
            List<Item> items = task.getValue();
            if (items == null) items = new ArrayList<>();
            productsTable.setItems(FXCollections.observableArrayList(items));
        });

        task.setOnFailed(e -> {
            productsTable.setItems(FXCollections.observableArrayList());
            Alert alert = new Alert(Alert.AlertType.ERROR, "API Failure: Could not load products. " + task.getException().getMessage());
            alert.show();
        });

        new Thread(task).start();
    }

    // Nested class to fulfill "call ItemService.getSellerItems" and "Parse JSON response"
    private static class ItemService {
        public static List<Item> getSellerItems(String sellerId) throws Exception {
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("http://localhost:8080/api/seller/" + sellerId + "/items"))
                    .GET()
                    .build();
            
            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new Exception("Server returned status " + response.statusCode());
            }

            Gson gson = new com.google.gson.GsonBuilder()
                    .registerTypeAdapter(java.time.LocalDateTime.class, new com.google.gson.JsonDeserializer<java.time.LocalDateTime>() {
                        @Override
                        public java.time.LocalDateTime deserialize(com.google.gson.JsonElement json, Type typeOfT, com.google.gson.JsonDeserializationContext context) throws com.google.gson.JsonParseException {
                            return java.time.LocalDateTime.parse(json.getAsString(), java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                        }
                    }).create();

            com.google.gson.JsonObject responseObj = com.google.gson.JsonParser.parseString(response.body()).getAsJsonObject();
            if (responseObj.has("status") && "SUCCESS".equals(responseObj.get("status").getAsString())) {
                com.google.gson.JsonArray dataArray = responseObj.getAsJsonArray("data");
                Type listType = new TypeToken<ArrayList<Item>>(){}.getType();
                return gson.fromJson(dataArray, listType);
            } else if (responseObj.has("message")) {
                throw new Exception(responseObj.get("message").getAsString());
            } else {
                Type listType = new TypeToken<ArrayList<Item>>(){}.getType();
                return gson.fromJson(response.body(), listType);
            }
        }
    }
}