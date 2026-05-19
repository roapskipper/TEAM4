package com.team4.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class AddProductDialogController implements Initializable {
    @FXML private TextField nameField;
    @FXML private ComboBox<String> categoryBox;
    @FXML private TextField priceField;
    @FXML private TextArea descArea;
    @FXML private Label errorLabel;

    private boolean confirmed = false;
    private String name, category, description;
    private double price;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        categoryBox.getItems().addAll("Electronics", "Art", "Fashion", "Collectible", "Vehicle");
        categoryBox.getSelectionModel().selectFirst();
    }

    @FXML
    private void onSave() {
        errorLabel.setVisible(false);
        String n = nameField.getText().trim();
        String c = categoryBox.getValue();
        String pStr = priceField.getText().trim();
        String d = descArea.getText().trim();

        if (n.isEmpty()) {
            showError("Product name is required.");
            return;
        }
        if (n.length() > 100) {
            showError("Product name cannot exceed 100 characters.");
            return;
        }
        if (c == null || c.isEmpty()) {
            showError("Please select a category.");
            return;
        }
        if (pStr.isEmpty()) {
            showError("Starting price is required.");
            return;
        }
        double p;
        try {
            p = Double.parseDouble(pStr);
            com.team4.service.ItemService dummyService = new com.team4.service.ItemService(null, null);
            dummyService.validatePrice(com.team4.model.Item.ItemCategory.valueOf(c.toUpperCase()), new java.math.BigDecimal(pStr));
        } catch (NumberFormatException e) {
            showError("Starting price must be a valid number.");
            return;
        } catch (com.team4.util.BusinessException be) {
            showError(be.getMessage());
            return;
        } catch (Exception e) {
            showError("Invalid category or price.");
            return;
        }
        if (d.length() > 500) {
            showError("Description cannot exceed 500 characters.");
            return;
        }

        this.name = n;
        this.category = c.toUpperCase();
        this.price = p;
        this.description = d;
        this.confirmed = true;
        
        closeDialog();
    }

    @FXML
    private void onCancel() {
        this.confirmed = false;
        closeDialog();
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
    }

    private void closeDialog() {
        Stage stage = (Stage) nameField.getScene().getWindow();
        stage.close();
    }

    public void setItemData(String n, String c, double p, String d, String status) {
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

        if (!"PENDING".equalsIgnoreCase(status)) {
            priceField.setDisable(true);
            if ("ACTIVE".equalsIgnoreCase(status) || "COMPLETED".equalsIgnoreCase(status)) {
                nameField.setDisable(true);
                categoryBox.setDisable(true);
                descArea.setDisable(true);
            }
        }
    }

    public boolean isConfirmed() { return confirmed; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public double getPrice() { return price; }
    public String getDescription() { return description; }
}
