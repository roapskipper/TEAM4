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
                "Required: Medium. Optional: Artist (blank -> Unknown), creation year (0 = unknown, from -3000 to present), dimensions.");
        CATEGORY_HINTS.put("Collectible",
                "Required: Rarity and condition. Optional: year of origin (0), origin (blank -> Unknown), certificate.");
        CATEGORY_HINTS.put("Electronics",
                "Required: Condition and warranty months (>= 0). Optional: brand/model (blank -> Unknown), fully functional.");
        CATEGORY_HINTS.put("Fashion",
                "Required: Size, condition, gender (default UNISEX). Optional: brand, material, color, authenticity.");
        CATEGORY_HINTS.put("Vehicle",
                "Required: Engine, transmission (default OTHER), odometer (0-1,000,000). Optional: manufacturing year (1886-present), brand/model (blank -> Unknown), color, legal papers.");
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
            categoryHintLabel.setText("Edit mode: only name, price (if allowed), and description can be updated.");
            return;
        }
        hideAllCategoryPanels();
        if (category == null) {
            categoryHintLabel.setText("Select a category to show required and optional fields.");
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
            showError(ex.getMessage() != null ? ex.getMessage() : "Invalid data.");
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
            throw new IllegalArgumentException("Product name cannot exceed 100 UI characters.");
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
            throw new IllegalArgumentException("Starting price must be a valid number.");
        }
        if (description.isEmpty()) {
            throw new IllegalArgumentException(Item.ValidationMessages.DESCRIPTION_REQUIRED);
        }
        if (description.length() > 500) {
            throw new IllegalArgumentException("Description cannot exceed 500 UI characters.");
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
            throw new IllegalArgumentException("Medium is required.");
        }
        req.setMedium(artMediumBox.getValue());
        req.setArtist(trimToNull(artArtistField.getText()));
        req.setCreationYear(parseOptionalYear(artYearField.getText(), "Creation year"));
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
        req.setYearOfOrigin(parseOptionalYear(collectibleYearField.getText(), "Year of origin"));
        req.setOrigin(trimToNull(collectibleOriginField.getText()));
        req.setHasCertificate(collectibleCertCheck.isSelected());
    }

    private void fillElectronics(ItemRequest req) {
        if (electronicsConditionBox.getValue() == null) {
            throw new IllegalArgumentException("Condition is required.");
        }
        req.setItemCondition(electronicsConditionBox.getValue());
        req.setWarrantyMonths(parseRequiredNonNegativeInt(electronicsWarrantyField.getText(), "Warranty months"));
        req.setBrand(trimToNull(electronicsBrandField.getText()));
        req.setModel(trimToNull(electronicsModelField.getText()));
        req.setFullyFunctional(electronicsFunctionalCheck.isSelected());
    }

    private void fillFashion(ItemRequest req) {
        if (fashionSizeBox.getValue() == null) {
            throw new IllegalArgumentException("Size is required.");
        }
        if (fashionConditionBox.getValue() == null) {
            throw new IllegalArgumentException("Condition is required.");
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
            throw new IllegalArgumentException("Engine type is required.");
        }
        req.setEngineType(vehicleEngineBox.getValue());
        req.setTransmission(vehicleTransmissionBox.getValue() != null
                ? vehicleTransmissionBox.getValue()
                : Vehicle.Transmission.OTHER);
        req.setOdo(parseRequiredNonNegativeInt(vehicleOdoField.getText(), "Odometer"));
        if (req.getOdo() > 1_000_000) {
            throw new IllegalArgumentException("Odometer cannot exceed 1,000,000.");
        }
        req.setManufacturingYear(parseOptionalYear(vehicleYearField.getText(), "Manufacturing year"));
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
            throw new IllegalArgumentException(fieldLabel + " must be an integer.");
        }
    }

    private static int parseRequiredNonNegativeInt(String raw, String fieldLabel) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException(fieldLabel + " is required.");
        }
        try {
            int value = Integer.parseInt(raw.trim());
            if (value < 0) {
                throw new IllegalArgumentException(fieldLabel + " must be >= 0.");
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldLabel + " must be an integer.");
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
