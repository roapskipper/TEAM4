package com.team4.controller;

import com.team4.factory.ItemRequest;
import com.team4.model.*;
import com.team4.util.ProductFormLabels;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class AddProductDialogController implements Initializable {

    // Common
    @FXML private TextField nameField;
    @FXML private ComboBox<String> categoryBox;
    @FXML private TextField priceField;
    @FXML private TextArea descArea;
    @FXML private Label errorLabel;
    @FXML private Label categoryHintLabel;

    // Panels
    @FXML private VBox artPanel;
    @FXML private VBox collectiblePanel;
    @FXML private VBox electronicsPanel;
    @FXML private VBox fashionPanel;
    @FXML private VBox vehiclePanel;

    // Art
    @FXML private ComboBox<Art.Medium> artMediumBox;
    @FXML private TextField artArtistField;
    @FXML private TextField artYearField;
    @FXML private TextField artDimensionsField;

    // Collectible
    @FXML private ComboBox<Collectible.RarityLevel> collectibleRarityBox;
    @FXML private ComboBox<Collectible.ConditionGrade> collectibleConditionBox;
    @FXML private TextField collectibleYearField;
    @FXML private TextField collectibleOriginField;
    @FXML private CheckBox collectibleCertCheck;

    // Electronics
    @FXML private ComboBox<Electronics.ConditionGrade> electronicsConditionBox;
    @FXML private TextField electronicsWarrantyField;
    @FXML private TextField electronicsBrandField;
    @FXML private TextField electronicsModelField;
    @FXML private CheckBox electronicsFunctionalCheck;

    // Fashion
    @FXML private ComboBox<Fashion.Size> fashionSizeBox;
    @FXML private ComboBox<Fashion.ConditionGrade> fashionConditionBox;
    @FXML private ComboBox<Fashion.Gender> fashionGenderBox;
    @FXML private TextField fashionBrandField;
    @FXML private TextField fashionMaterialField;
    @FXML private TextField fashionColorField;
    @FXML private CheckBox fashionAuthenticCheck;

    // Vehicle
    @FXML private ComboBox<Vehicle.EngineType> vehicleEngineBox;
    @FXML private ComboBox<Vehicle.Transmission> vehicleTransmissionBox;
    @FXML private TextField vehicleOdoField;
    @FXML private TextField vehicleYearField;
    @FXML private TextField vehicleBrandField;
    @FXML private TextField vehicleModelField;
    @FXML private TextField vehicleColorField;
    @FXML private CheckBox vehicleLegalCheck;

    private boolean confirmed;
    private boolean editMode;
    private ItemRequest builtRequest;

    private static final Map<String, String> CATEGORY_HINTS = new LinkedHashMap<>();

    static {
        CATEGORY_HINTS.put("Art",
                "Bắt buộc: Chất liệu. Tùy chọn: Tác giả (trống → Unknown), Năm sáng tác (0 = chưa rõ, từ -3000 đến nay), Kích thước.");
        CATEGORY_HINTS.put("Collectible",
                "Bắt buộc: Độ hiếm, Tình trạng. Tùy chọn: Năm xuất xứ (0), Xuất xứ (trống → Unknown), Chứng chỉ.");
        CATEGORY_HINTS.put("Electronics",
                "Bắt buộc: Tình trạng, Số tháng bảo hành (≥ 0). Tùy chọn: Thương hiệu/Model (trống → Unknown), Hoạt động bình thường.");
        CATEGORY_HINTS.put("Fashion",
                "Bắt buộc: Kích cỡ, Tình trạng, Đối tượng (mặc định UNISEX). Tùy chọn: Thương hiệu, Chất liệu, Màu, Chính hãng.");
        CATEGORY_HINTS.put("Vehicle",
                "Bắt buộc: Động cơ, Hộp số (mặc định OTHER), Số km (0–1.000.000). Tùy chọn: Năm SX (1886–nay), Thương hiệu/Model (trống → Unknown), Màu, Giấy tờ.");
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        categoryBox.getItems().setAll("Electronics", "Art", "Fashion", "Collectible", "Vehicle");
        categoryBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> showCategoryPanel(newVal));

        setupEnumCombo(artMediumBox, Art.Medium.values(), ProductFormLabels::artMedium);
        setupEnumCombo(collectibleRarityBox, Collectible.RarityLevel.values(), ProductFormLabels::collectibleRarity);
        setupEnumCombo(collectibleConditionBox, Collectible.ConditionGrade.values(),
                g -> ProductFormLabels.conditionGrade("", g.name()));
        setupEnumCombo(electronicsConditionBox, Electronics.ConditionGrade.values(), ProductFormLabels::electronicsCondition);
        setupEnumCombo(fashionSizeBox, Fashion.Size.values(), ProductFormLabels::fashionSize);
        setupEnumCombo(fashionConditionBox, Fashion.ConditionGrade.values(),
                g -> ProductFormLabels.conditionGrade("", g.name()));
        setupEnumCombo(fashionGenderBox, Fashion.Gender.values(), ProductFormLabels::fashionGender);
        fashionGenderBox.getSelectionModel().select(Fashion.Gender.UNISEX);
        setupEnumCombo(vehicleEngineBox, Vehicle.EngineType.values(), ProductFormLabels::vehicleEngine);
        setupEnumCombo(vehicleTransmissionBox, Vehicle.Transmission.values(), ProductFormLabels::vehicleTransmission);
        vehicleTransmissionBox.getSelectionModel().select(Vehicle.Transmission.OTHER);

        showCategoryPanel(categoryBox.getValue());
    }

    private <E extends Enum<E>> void setupEnumCombo(ComboBox<E> combo, E[] values, java.util.function.Function<E, String> labelFn) {
        combo.setItems(FXCollections.observableArrayList(values));
        combo.setConverter(enumConverter(labelFn));
    }

    private <E extends Enum<E>> StringConverter<E> enumConverter(java.util.function.Function<E, String> labelFn) {
        return new StringConverter<>() {
            @Override
            public String toString(E value) {
                return value == null ? "" : labelFn.apply(value);
            }

            @Override
            public E fromString(String string) {
                return null;
            }
        };
    }

    private void showCategoryPanel(String category) {
        if (editMode) {
            hideAllCategoryPanels();
            categoryHintLabel.setText("Chế độ chỉnh sửa: chỉ cập nhật tên, giá (nếu được phép) và mô tả.");
            return;
        }
        hideAllCategoryPanels();
        if (category == null) {
            categoryHintLabel.setText("Chọn danh mục để hiển thị các trường bắt buộc và tùy chọn.");
            return;
        }
        categoryHintLabel.setText(CATEGORY_HINTS.getOrDefault(category, ""));
        switch (category) {
            case "Art" -> setPanelVisible(artPanel, true);
            case "Collectible" -> setPanelVisible(collectiblePanel, true);
            case "Electronics" -> setPanelVisible(electronicsPanel, true);
            case "Fashion" -> setPanelVisible(fashionPanel, true);
            case "Vehicle" -> setPanelVisible(vehiclePanel, true);
            default -> { }
        }
    }

    private void hideAllCategoryPanels() {
        setPanelVisible(artPanel, false);
        setPanelVisible(collectiblePanel, false);
        setPanelVisible(electronicsPanel, false);
        setPanelVisible(fashionPanel, false);
        setPanelVisible(vehiclePanel, false);
    }

    private void setPanelVisible(VBox panel, boolean visible) {
        panel.setVisible(visible);
        panel.setManaged(visible);
    }

    @FXML
    private void onSave() {
        hideError();
        try {
            builtRequest = buildItemRequest(null);
            confirmed = true;
            closeDialog();
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        } catch (Exception ex) {
            showError(ex.getMessage() != null ? ex.getMessage() : "Dữ liệu không hợp lệ.");
        }
    }

    /**
     * Builds and validates an {@link ItemRequest}. Used by the dialog and seller flow.
     */
    public ItemRequest buildItemRequest(String ownerId) {
        String name = trim(nameField.getText());
        String categoryLabel = categoryBox.getValue();
        String priceStr = trim(priceField.getText());
        String description = trim(descArea.getText());

        if (name.isEmpty()) {
            throw new IllegalArgumentException(Item.ValidationMessages.NAME_REQUIRED);
        }
        if (name.length() > 100) {
            throw new IllegalArgumentException("Tên sản phẩm tối đa 100 ký tự trên giao diện.");
        }
        if (categoryLabel == null || categoryLabel.isEmpty()) {
            throw new IllegalArgumentException(Item.ValidationMessages.CATEGORY_REQUIRED);
        }
        if (priceStr.isEmpty()) {
            throw new IllegalArgumentException(Item.ValidationMessages.STARTING_PRICE_REQUIRED);
        }
        BigDecimal price;
        try {
            price = new BigDecimal(priceStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Giá khởi điểm phải là số hợp lệ.");
        }
        if (description.isEmpty()) {
            throw new IllegalArgumentException(Item.ValidationMessages.DESCRIPTION_REQUIRED);
        }
        if (description.length() > 500) {
            throw new IllegalArgumentException("Mô tả tối đa 500 ký tự trên giao diện.");
        }

        Item.ItemCategory category = Item.ItemCategory.valueOf(categoryLabel.toUpperCase());
        try {
            new com.team4.service.ItemService(null, null)
                    .validatePrice(category, price);
        } catch (com.team4.util.BusinessException be) {
            throw new IllegalArgumentException(be.getMessage());
        }

        ItemRequest req = new ItemRequest();
        req.setName(name);
        req.setDescription(description);
        req.setStartingPrice(price);
        req.setCategory(category);
        if (ownerId != null) {
            req.setOwnerId(ownerId);
        }

        if (!editMode) {
            fillCategoryFields(req, categoryLabel);
        }

        return req;
    }

    private void fillCategoryFields(ItemRequest req, String categoryLabel) {
        switch (categoryLabel) {
            case "Art" -> fillArt(req);
            case "Collectible" -> fillCollectible(req);
            case "Electronics" -> fillElectronics(req);
            case "Fashion" -> fillFashion(req);
            case "Vehicle" -> fillVehicle(req);
            default -> throw new IllegalArgumentException(Item.ValidationMessages.CATEGORY_REQUIRED);
        }
    }

    private void fillArt(ItemRequest req) {
        if (artMediumBox.getValue() == null) {
            throw new IllegalArgumentException("Chất liệu (Medium) là bắt buộc.");
        }
        req.setMedium(artMediumBox.getValue());
        req.setArtist(trimToNull(artArtistField.getText()));
        req.setCreationYear(parseOptionalYear(artYearField.getText(), "Năm sáng tác"));
        req.setDimensions(trimToNull(artDimensionsField.getText()));
    }

    private void fillCollectible(ItemRequest req) {
        if (collectibleRarityBox.getValue() == null) {
            throw new IllegalArgumentException(Collectible.ValidationMessages.RARITY_REQUIRED);
        }
        if (collectibleConditionBox.getValue() == null) {
            throw new IllegalArgumentException(Collectible.ValidationMessages.CONDITION_REQUIRED);
        }
        req.setRarityLevel(collectibleRarityBox.getValue());
        req.setConditionGrade(collectibleConditionBox.getValue());
        req.setYearOfOrigin(parseOptionalYear(collectibleYearField.getText(), "Năm xuất xứ"));
        req.setOrigin(trimToNull(collectibleOriginField.getText()));
        req.setHasCertificate(collectibleCertCheck.isSelected());
    }

    private void fillElectronics(ItemRequest req) {
        if (electronicsConditionBox.getValue() == null) {
            throw new IllegalArgumentException("Tình trạng (Condition) là bắt buộc.");
        }
        req.setItemCondition(electronicsConditionBox.getValue());
        req.setWarrantyMonths(parseRequiredNonNegativeInt(electronicsWarrantyField.getText(), "Số tháng bảo hành"));
        req.setBrand(trimToNull(electronicsBrandField.getText()));
        req.setModel(trimToNull(electronicsModelField.getText()));
        req.setFullyFunctional(electronicsFunctionalCheck.isSelected());
    }

    private void fillFashion(ItemRequest req) {
        if (fashionSizeBox.getValue() == null) {
            throw new IllegalArgumentException("Kích cỡ (Size) là bắt buộc.");
        }
        if (fashionConditionBox.getValue() == null) {
            throw new IllegalArgumentException("Tình trạng (Condition) là bắt buộc.");
        }
        req.setSize(fashionSizeBox.getValue());
        req.setCondition(fashionConditionBox.getValue());
        req.setGender(fashionGenderBox.getValue() != null ? fashionGenderBox.getValue() : Fashion.Gender.UNISEX);
        req.setBrand(trimToNull(fashionBrandField.getText()));
        req.setMaterial(trimToNull(fashionMaterialField.getText()));
        req.setColor(trimToNull(fashionColorField.getText()));
        req.setAuthentic(fashionAuthenticCheck.isSelected());
    }

    private void fillVehicle(ItemRequest req) {
        if (vehicleEngineBox.getValue() == null) {
            throw new IllegalArgumentException("Loại động cơ (Engine) là bắt buộc.");
        }
        req.setEngineType(vehicleEngineBox.getValue());
        req.setTransmission(vehicleTransmissionBox.getValue() != null
                ? vehicleTransmissionBox.getValue()
                : Vehicle.Transmission.OTHER);
        req.setOdo(parseRequiredNonNegativeInt(vehicleOdoField.getText(), "Số km đã đi (Odo)"));
        if (req.getOdo() > 1_000_000) {
            throw new IllegalArgumentException("Số km không được vượt quá 1.000.000.");
        }
        req.setManufacturingYear(parseOptionalYear(vehicleYearField.getText(), "Năm sản xuất"));
        req.setBrand(trimToNull(vehicleBrandField.getText()));
        req.setModel(trimToNull(vehicleModelField.getText()));
        req.setColor(trimToNull(vehicleColorField.getText()));
        req.setHasLegalPapers(vehicleLegalCheck.isSelected());
    }

    private static int parseOptionalYear(String raw, String fieldLabel) {
        if (raw == null || raw.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldLabel + " phải là số nguyên.");
        }
    }

    private static int parseRequiredNonNegativeInt(String raw, String fieldLabel) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException(fieldLabel + " là bắt buộc.");
        }
        try {
            int value = Integer.parseInt(raw.trim());
            if (value < 0) {
                throw new IllegalArgumentException(fieldLabel + " phải ≥ 0.");
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldLabel + " phải là số nguyên.");
        }
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static String trimToNull(String value) {
        String t = trim(value);
        return t.isEmpty() ? null : t;
    }

    @FXML
    private void onCancel() {
        confirmed = false;
        builtRequest = null;
        closeDialog();
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setManaged(true);
        errorLabel.setVisible(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private void closeDialog() {
        Stage stage = (Stage) nameField.getScene().getWindow();
        stage.close();
    }

    public void setItemData(String n, String c, double p, String d, String status) {
        editMode = true;
        nameField.setText(n != null ? n : "");
        if (c != null) {
            for (String item : categoryBox.getItems()) {
                if (item.equalsIgnoreCase(c)) {
                    categoryBox.setValue(item);
                    break;
                }
            }
        }
        priceField.setText(String.format(java.util.Locale.US, "%.0f", p));
        descArea.setText(d != null ? d : "");
        hideAllCategoryPanels();

        if (!"PENDING".equalsIgnoreCase(status)) {
            priceField.setDisable(true);
            if ("ACTIVE".equalsIgnoreCase(status) || "COMPLETED".equalsIgnoreCase(status)) {
                nameField.setDisable(true);
                categoryBox.setDisable(true);
                descArea.setDisable(true);
            }
        }
        showCategoryPanel(categoryBox.getValue());
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public ItemRequest getItemRequest() {
        return builtRequest;
    }

    /** @deprecated use {@link #getItemRequest()} */
    public String getName() {
        return builtRequest != null ? builtRequest.getName() : null;
    }

    public String getCategory() {
        return builtRequest != null && builtRequest.getCategory() != null
                ? builtRequest.getCategory().name()
                : null;
    }

    public double getPrice() {
        return builtRequest != null && builtRequest.getStartingPrice() != null
                ? builtRequest.getStartingPrice().doubleValue()
                : 0;
    }

    public String getDescription() {
        return builtRequest != null ? builtRequest.getDescription() : null;
    }
}
