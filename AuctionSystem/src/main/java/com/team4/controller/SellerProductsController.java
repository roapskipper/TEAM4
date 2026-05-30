package com.team4.controller;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.team4.client.ApiClient;
import com.team4.factory.ItemRequest;
import com.team4.model.Art;
import com.team4.model.Collectible;
import com.team4.model.Electronics;
import com.team4.model.Fashion;
import com.team4.model.Item;
import com.team4.model.Vehicle;
import com.team4.util.UserSession;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.application.Platform;
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
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
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
    @FXML private VBox productDetailPanel;
    @FXML private Label detailTitle;
    @FXML private Label detailSubtitle;
    @FXML private Label detailStatus;
    @FXML private Label detailCategory;
    @FXML private Label detailStartPrice;
    @FXML private Label detailCurrentPrice;
    @FXML private Label detailCreatedAt;
    @FXML private Label detailDescription;
    @FXML private FlowPane detailAttributes;

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
        productsTable.setPlaceholder(createProductsEmptyState(
                "Loading products",
                "Fetching your product list from the server.",
                false));
        productsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        productsTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldItem, newItem) -> showProductDetails(newItem));
        Platform.runLater(this::styleProductTableHeader);

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

    private void styleProductTableHeader() {
        styleLookup(".column-header-background",
                "-fx-background-color: #722F37; -fx-background-radius: 12 12 0 0;");
        styleLookup(".nested-column-header",
                "-fx-background-color: #722F37; -fx-background-insets: 0;");
        styleLookup(".column-header",
                "-fx-background-color: #722F37; -fx-background-insets: 0; "
                        + "-fx-border-color: transparent rgba(255,255,255,0.18) transparent transparent;");
        styleLookup(".filler",
                "-fx-background-color: #722F37; -fx-background-radius: 0 12 0 0;");
        styleLookup(".show-hide-columns-button",
                "-fx-background-color: #722F37; -fx-background-radius: 0 12 0 0;");

        for (Node label : productsTable.lookupAll(".column-header .label")) {
            label.setStyle("-fx-text-fill: #FFFFFF; -fx-font-family: 'Lato'; "
                    + "-fx-font-weight: bold; -fx-font-size: 13px;");
        }
    }

    private void styleLookup(String selector, String style) {
        for (Node node : productsTable.lookupAll(selector)) {
            node.setStyle(style);
        }
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
            private final HBox actions = new HBox(8, editBtn, deleteBtn);

            {
                actions.setAlignment(Pos.CENTER);

                editBtn.getStyleClass().addAll("table-action-btn", "table-action-edit");
                editBtn.setFocusTraversable(false);
                editBtn.setMinWidth(56);
                editBtn.setPrefWidth(56);
                editBtn.setOnAction(event -> {
                    Item selected = getTableView().getItems().get(getIndex());
                    onEditProduct(selected);
                });

                deleteBtn.getStyleClass().addAll("table-action-btn", "table-action-delete");
                deleteBtn.setFocusTraversable(false);
                deleteBtn.setMinWidth(64);
                deleteBtn.setPrefWidth(64);
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
        updateProductPlaceholder();
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
                        dialogController.getItemRequest().getName(),
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
            showProductDetails(null);
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
            productsTable.setPlaceholder(createProductsEmptyState(
                    "Seller account required",
                    "Only sellers can manage registered products.",
                    false));
        }
        showProductDetails(null);
        if (totalProducts != null) totalProducts.setText("N/A");
        if (activeAuctions != null) activeAuctions.setText("N/A");
        if (pendingProducts != null) pendingProducts.setText("N/A");
        if (soldProducts != null) soldProducts.setText("N/A");
    }

    private void showProductDetails(Item item) {
        if (productDetailPanel == null) {
            return;
        }

        boolean visible = item != null;
        productDetailPanel.setVisible(visible);
        productDetailPanel.setManaged(visible);
        if (!visible) {
            return;
        }

        String status = readStatus(item);
        detailTitle.setText(nullSafe(item.getName()));
        detailSubtitle.setText(formatCategory(item.getCategory()) + " product");
        detailStatus.setText(status);
        detailStatus.getStyleClass().setAll("badge", statusClass(status));
        detailCategory.setText(formatCategory(item.getCategory()));
        detailStartPrice.setText(formatPrice(item.getStartingPrice()));
        detailCurrentPrice.setText(formatPrice(item.getStartingPrice()));
        detailCreatedAt.setText(formatDate(item.getCreatedAt()));
        detailDescription.setText(nullSafe(item.getDescription()).isBlank()
                ? "No description provided."
                : item.getDescription());

        detailAttributes.getChildren().clear();
        populateAttributePills(item);
        if (detailAttributes.getChildren().isEmpty()) {
            Label fallback = new Label("No category-specific attributes.");
            fallback.getStyleClass().add("detail-pill-muted");
            detailAttributes.getChildren().add(fallback);
        }
    }

    private void populateAttributePills(Item item) {
        if (item instanceof Art art) {
            addAttribute("Medium", formatEnum(art.getMedium()));
            addAttribute("Artist", art.getArtist());
            addAttribute("Year", formatYear(art.getCreationYear()));
            addAttribute("Dimensions", art.getDimensions());
            return;
        }
        if (item instanceof Collectible collectible) {
            addAttribute("Rarity", formatEnum(collectible.getRarityLevel()));
            addAttribute("Condition", formatEnum(collectible.getConditionGrade()));
            addAttribute("Year", formatYear(collectible.getYearOfOrigin()));
            addAttribute("Origin", collectible.getOrigin());
            addAttribute("Certificate", collectible.isHasCertificate() ? "Yes" : "No");
            return;
        }
        if (item instanceof Electronics electronics) {
            addAttribute("Condition", formatEnum(electronics.getItemCondition()));
            addAttribute("Warranty", electronics.getWarrantyMonths() + " months");
            addAttribute("Brand", electronics.getBrand());
            addAttribute("Model", electronics.getModel());
            addAttribute("Functional", electronics.isFullyFunctional() ? "Yes" : "No");
            return;
        }
        if (item instanceof Fashion fashion) {
            addAttribute("Size", formatEnum(fashion.getSize()));
            addAttribute("Condition", formatEnum(fashion.getCondition()));
            addAttribute("Gender", formatEnum(fashion.getGender()));
            addAttribute("Brand", fashion.getBrand());
            addAttribute("Material", fashion.getMaterial());
            addAttribute("Color", fashion.getColor());
            addAttribute("Authentic", fashion.isAuthentic() ? "Yes" : "No");
            return;
        }
        if (item instanceof Vehicle vehicle) {
            addAttribute("Engine", formatEnum(vehicle.getEngineType()));
            addAttribute("Transmission", formatEnum(vehicle.getTransmission()));
            addAttribute("Odometer", NUMBER_FORMAT.format(vehicle.getOdo()) + " km");
            addAttribute("Year", formatYear(vehicle.getManufacturingYear()));
            addAttribute("Brand", vehicle.getBrand());
            addAttribute("Model", vehicle.getModel());
            addAttribute("Color", vehicle.getColor());
            addAttribute("Legal papers", vehicle.hasLegalPapers() ? "Yes" : "No");
        }
    }

    private void addAttribute(String label, String value) {
        String displayValue = value == null || value.isBlank() ? "Not provided" : value;
        Label pill = new Label(label + ": " + displayValue);
        pill.getStyleClass().add("detail-pill");
        detailAttributes.getChildren().add(pill);
    }

    private void updateProductPlaceholder() {
        if (productsTable == null || filteredProducts == null) {
            return;
        }
        boolean hasProducts = !productSource.isEmpty();
        productsTable.setPlaceholder(createProductsEmptyState(
                hasProducts ? "No matching products" : "No products yet",
                hasProducts
                        ? "Adjust the search keyword or category filter to find a product."
                        : "Registered products will appear here after they are created.",
                !hasProducts && isSellerSession()));
    }

    private VBox createProductsEmptyState(String title, String message, boolean showAction) {
        VBox empty = new VBox(10);
        empty.setAlignment(Pos.CENTER);
        empty.setPrefHeight(220);
        empty.getStyleClass().add("empty-state");

        Label mark = new Label(title.toLowerCase(Locale.ROOT).contains("loading") ? "..." : "No Data");
        mark.getStyleClass().add("empty-state-mark");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("empty-state-title");
        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("empty-state-text");
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(360);

        empty.getChildren().addAll(mark, titleLabel, messageLabel);
        if (showAction) {
            Button action = new Button("Add Product");
            action.getStyleClass().add("empty-state-action");
            action.setOnAction(e -> onAddProduct());
            empty.getChildren().add(action);
        }
        return empty;
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

    private String formatEnum(Object value) {
        if (value == null) {
            return "Not provided";
        }
        String raw = value.toString().replace("_", " ").toLowerCase(Locale.ROOT);
        StringBuilder result = new StringBuilder();
        for (String part : raw.split(" ")) {
            if (!part.isBlank()) {
                if (result.length() > 0) {
                    result.append(' ');
                }
                result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
            }
        }
        return result.toString();
    }

    private String formatYear(int year) {
        return year == 0 ? "Unknown" : NUMBER_FORMAT.format(year);
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
