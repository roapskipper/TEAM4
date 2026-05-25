package com.team4.controller;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.team4.client.ApiClient;
import com.team4.factory.ItemRequest;
import com.team4.model.Item;
import com.team4.util.UserSession;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class SellerProductsController implements Initializable {

    @FXML private Label totalProducts, activeAuctions, pendingProducts, soldProducts;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private Button addProductBtn;
    @FXML private TableView<Item> productsTable;
    @FXML private TableColumn<Item, String> colProduct;
    @FXML private TableColumn<Item, String> colCategory;
    @FXML private TableColumn<Item, String> colStartPrice;
    @FXML private TableColumn<Item, String> colCurrentPrice;
    @FXML private TableColumn<Item, String> colStatus;
    @FXML private TableColumn<Item, String> colDate;
    @FXML private TableColumn<Item, Void> colAction;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getNumberInstance(Locale.US);

    private final ObservableList<Item> productSource = FXCollections.observableArrayList();
    private FilteredList<Item> filteredProducts;

    static {
        NUMBER_FORMAT.setMaximumFractionDigits(0);
        NUMBER_FORMAT.setMinimumFractionDigits(0);
        NUMBER_FORMAT.setRoundingMode(RoundingMode.HALF_UP);
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (!isSellerSession()) {
            disableSellerOnlyView();
            return;
        }
        setupCategoryFilter();
        setupTable();
        setupFiltering();
        loadStats();
        loadDataFromServer();
    }

    private void loadStats() {
        totalProducts.setText("...");
        activeAuctions.setText("...");
        pendingProducts.setText("...");
        soldProducts.setText("...");

        Task<JsonObject> task = new Task<JsonObject>() {
            @Override
            protected JsonObject call() throws Exception {
                return new ApiClient().getSellerStats(currentSellerId());
            }
        };

        task.setOnSucceeded(e -> {
            JsonObject responseObj = task.getValue();
            JsonObject stats = null;
            if (responseObj != null && responseObj.has("status")
                    && "SUCCESS".equals(responseObj.get("status").getAsString())
                    && responseObj.has("data") && responseObj.get("data").isJsonObject()) {
                stats = responseObj.getAsJsonObject("data");
            } else {
                stats = responseObj;
            }

            totalProducts.setText(formatStat(stats, "totalProducts"));
            activeAuctions.setText(formatStat(stats, "activeAuctions"));
            pendingProducts.setText(formatStat(stats, "pendingProducts"));
            soldProducts.setText(formatStat(stats, "soldProducts"));
        });

        task.setOnFailed(e -> {
            totalProducts.setText("N/A");
            activeAuctions.setText("N/A");
            pendingProducts.setText("N/A");
            soldProducts.setText("N/A");
            System.err.println("Failed to load stats: " + cleanMessage(task.getException()));
        });

        new Thread(task).start();
    }

    private String formatStat(JsonObject stats, String key) {
        if (stats == null || !stats.has(key) || stats.get(key).isJsonNull()) {
            return "N/A";
        }
        return NUMBER_FORMAT.format(stats.get(key).getAsInt());
    }

    private void setupCategoryFilter() {
        categoryFilter.getItems().setAll(
                "All", "Electronics", "Vehicle", "Art", "Fashion", "Collectible"
        );
        categoryFilter.getSelectionModel().selectFirst();
    }

    private void setupTable() {
        productsTable.setPlaceholder(new Label("No products found"));

        colProduct.setCellValueFactory(param -> new SimpleStringProperty(nullSafe(param.getValue().getName())));
        colCategory.setCellValueFactory(param -> new SimpleStringProperty(formatCategory(param.getValue().getCategory())));
        colStartPrice.setCellValueFactory(param -> new SimpleStringProperty(formatPrice(param.getValue().getStartingPrice())));
        colCurrentPrice.setCellValueFactory(param -> new SimpleStringProperty(formatPrice(param.getValue().getStartingPrice())));
        colStatus.setCellValueFactory(param -> new SimpleStringProperty(readStatus(param.getValue())));
        colDate.setCellValueFactory(param -> new SimpleStringProperty(formatDate(param.getValue().getCreatedAt())));

        colStartPrice.setCellFactory(column -> priceCell());
        colCurrentPrice.setCellFactory(column -> priceCell());
        colStatus.setCellFactory(column -> statusCell());
        setupActionColumn();
    }

    private TableCell<Item, String> priceCell() {
        return new TableCell<Item, String>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty ? null : value);
                setGraphic(null);
                setStyle(empty ? "" : "-fx-alignment: CENTER_RIGHT;");
            }
        };
    }

    private TableCell<Item, String> statusCell() {
        return new TableCell<Item, String>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                Label badge = new Label(value);
                badge.getStyleClass().addAll("badge", statusClass(value));
                setText(null);
                setGraphic(badge);
            }
        };
    }

    private void setupActionColumn() {
        colAction.setCellFactory(param -> new TableCell<Item, Void>() {
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final HBox actions = new HBox(6, editBtn, deleteBtn);

            {
                editBtn.getStyleClass().add("table-action-btn");
                editBtn.setFocusTraversable(false);
                editBtn.setOnAction(event -> {
                    Item selected = getTableView().getItems().get(getIndex());
                    onEditProduct(selected);
                });

                deleteBtn.getStyleClass().add("table-action-btn");
                deleteBtn.setFocusTraversable(false);
                deleteBtn.setOnAction(event -> {
                    Item selected = getTableView().getItems().get(getIndex());
                    onDeleteProduct(selected);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : actions);
                setText(null);
            }
        });
    }

    private void setupFiltering() {
        filteredProducts = new FilteredList<>(productSource, item -> true);
        productsTable.setItems(filteredProducts);

        searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        categoryFilter.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());
    }

    private void applyFilters() {
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        String category = categoryFilter.getValue();

        filteredProducts.setPredicate(item -> {
            if (item == null) {
                return false;
            }

            boolean matchesKeyword = keyword.isEmpty()
                    || nullSafe(item.getName()).toLowerCase(Locale.ROOT).contains(keyword)
                    || formatCategory(item.getCategory()).toLowerCase(Locale.ROOT).contains(keyword)
                    || nullSafe(item.getDescription()).toLowerCase(Locale.ROOT).contains(keyword);

            boolean matchesCategory = category == null || "All".equals(category)
                    || formatCategory(item.getCategory()).equalsIgnoreCase(category);

            return matchesKeyword && matchesCategory;
        });
    }

    @FXML
    private void onAddProduct() {
        if (!isSellerSession()) {
            showAlert(Alert.AlertType.ERROR, "Access Denied", "Only sellers can register products.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/team4/view/add_product_dialog.fxml"));
            Parent root = loader.load();
            AddProductDialogController dialogController = loader.getController();

            Stage stage = new Stage();
            stage.setTitle("Add Product");
            stage.initModality(Modality.APPLICATION_MODAL);
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setMinWidth(600);
            stage.setMinHeight(720);
            stage.showAndWait();

            if (dialogController.isConfirmed()) {
                createProduct(dialogController);
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Could not open product form. " + cleanMessage(e));
        }
    }

    private void createProduct(AddProductDialogController dialogController) {
        final String sellerId = currentSellerId();
        final ItemRequest request;
        try {
            request = dialogController.buildItemRequest(sellerId);
            request.setOwnerId(sellerId);
        } catch (IllegalArgumentException ex) {
            showAlert(Alert.AlertType.ERROR, "Invalid Data", ex.getMessage());
            return;
        }

        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                new ApiClient().createItem(sellerId, request);
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            showAlert(Alert.AlertType.INFORMATION, "Success", "Product created successfully.");
            loadDataFromServer();
            loadStats();
        });

        task.setOnFailed(e -> {
            showAlert(Alert.AlertType.ERROR, "Error",
                    "Could not create product. " + cleanMessage(task.getException()));
        });

        new Thread(task).start();
    }

    private void onEditProduct(Item item) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/team4/view/add_product_dialog.fxml"));
            Parent root = loader.load();
            AddProductDialogController dialogController = loader.getController();

            dialogController.setItemData(
                    item.getName(),
                    formatCategory(item.getCategory()),
                    item.getStartingPrice().doubleValue(),
                    item.getDescription(),
                    readStatus(item)
            );

            Stage stage = new Stage();
            stage.setTitle("Edit Product");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

            if (dialogController.isConfirmed()) {
                updateProduct(item, dialogController);
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Could not open product form. " + cleanMessage(e));
        }
    }

    private void updateProduct(Item item, AddProductDialogController dialogController) {
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                new ApiClient().updateItem(
                        currentSellerId(),
                        item.getId(),
                        dialogController.getName(),
                        dialogController.getCategory(),
                        dialogController.getPrice(),
                        dialogController.getDescription()
                );
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            showAlert(Alert.AlertType.INFORMATION, "Success", "Product updated successfully.");
            loadDataFromServer();
            loadStats();
        });

        task.setOnFailed(e -> showAlert(Alert.AlertType.ERROR, "Error",
                "Failed to update product. " + cleanMessage(task.getException())));

        new Thread(task).start();
    }

    private void onDeleteProduct(Item item) {
        Alert confirmation = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Delete \"" + nullSafe(item.getName()) + "\"?",
                ButtonType.OK,
                ButtonType.CANCEL);
        confirmation.setTitle("Delete Product");
        confirmation.setHeaderText(null);
        if (confirmation.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                new ApiClient().deleteItem(currentSellerId(), item.getId());
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            showAlert(Alert.AlertType.INFORMATION, "Success", "Product deleted successfully.");
            loadDataFromServer();
            loadStats();
        });

        task.setOnFailed(e -> showAlert(Alert.AlertType.ERROR, "Error",
                "Could not delete product. " + cleanMessage(task.getException())));

        new Thread(task).start();
    }

    private void loadDataFromServer() {
        Task<List<Item>> task = new Task<List<Item>>() {
            @Override
            protected List<Item> call() throws Exception {
                return new ApiClient().getSellerItems(currentSellerId());
            }
        };

        task.setOnSucceeded(e -> {
            List<Item> items = task.getValue();
            if (items == null) {
                items = new ArrayList<>();
            }
            productSource.setAll(items);
            applyFilters();
        });

        task.setOnFailed(e -> {
            productSource.clear();
            showAlert(Alert.AlertType.ERROR, "Error", "Could not load products. " + cleanMessage(task.getException()));
        });

        new Thread(task).start();
    }

    private String currentSellerId() {
        UserSession session = UserSession.getInstance();
        if (session != null && session.getUserId() != null) {
            return session.getUserId();
        }
        return "";
    }

    private boolean isSellerSession() {
        UserSession session = UserSession.getInstance();
        return session != null && "seller".equalsIgnoreCase(session.getRole())
                && session.getUserId() != null && !session.getUserId().isBlank();
    }

    private void disableSellerOnlyView() {
        if (addProductBtn != null) {
            addProductBtn.setDisable(true);
        }
        if (productsTable != null) {
            productsTable.setPlaceholder(new Label("Only sellers can manage products."));
        }
        if (totalProducts != null) totalProducts.setText("N/A");
        if (activeAuctions != null) activeAuctions.setText("N/A");
        if (pendingProducts != null) pendingProducts.setText("N/A");
        if (soldProducts != null) soldProducts.setText("N/A");
    }

    private String formatPrice(BigDecimal amount) {
        if (amount == null) {
            return "0 VND";
        }
        return NUMBER_FORMAT.format(amount.setScale(0, RoundingMode.HALF_UP)) + " VND";
    }

    private String formatDate(LocalDateTime value) {
        return value == null ? "" : value.format(DATE_FORMAT);
    }

    private String formatCategory(Item.ItemCategory category) {
        if (category == null) {
            return "";
        }
        String value = category.name().replace("_", " ").toLowerCase(Locale.ROOT);
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private String readStatus(Item item) {
        try {
            Object value = item.getClass().getMethod("getStatus").invoke(item);
            return formatStatus(value != null ? value.toString() : "PENDING");
        } catch (Exception ignored) {
            return "Pending";
        }
    }

    private String formatStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            return "Pending";
        }
        if (isPendingStatus(rawStatus)) {
            return "Pending";
        }
        if (isOngoingStatus(rawStatus)) {
            return "Ongoing";
        }
        if (isEndedStatus(rawStatus)) {
            return "Ended";
        }
        String value = rawStatus.replace("_", " ").toLowerCase(Locale.ROOT);
        StringBuilder result = new StringBuilder();
        for (String part : value.split(" ")) {
            if (!part.isEmpty()) {
                if (result.length() > 0) {
                    result.append(' ');
                }
                result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
            }
        }
        return result.toString();
    }

    private String statusClass(String status) {
        if (isOngoingStatus(status)) {
            return "badge-green";
        }
        if (isEndedStatus(status)) {
            return "badge-red";
        }
        if (isPendingStatus(status)) {
            return "badge-yellow";
        }
        String value = status.toLowerCase(Locale.ROOT);
        if (value.contains("reject") || value.contains("cancel")) {
            return "badge-red";
        }
        return "badge-yellow";
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

    private String cleanMessage(Throwable throwable) {
        String raw = throwable == null ? "" : throwable.getMessage();
        if (raw == null || raw.trim().isEmpty()) {
            return "Please try again.";
        }

        String message = raw.trim();
        try {
            JsonElement parsed = JsonParser.parseString(message);
            if (parsed.isJsonObject()) {
                JsonObject obj = parsed.getAsJsonObject();
                if (obj.has("message") && !obj.get("message").isJsonNull()) {
                    return obj.get("message").getAsString();
                }
                if (obj.has("error") && !obj.get("error").isJsonNull()) {
                    return obj.get("error").getAsString();
                }
            }
        } catch (Exception ignored) {
        }

        String lower = message.toLowerCase(Locale.ROOT);
        if (lower.contains("connection refused") || lower.contains("connect timed out")
                || lower.contains("no route to host")) {
            return "Cannot connect to server. Please start the server and try again.";
        }
        return message;
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }
}
