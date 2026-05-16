package com.savvy.stocksavvyyloglog.controller;

import com.savvy.stocksavvyyloglog.model.NewSaleModel;
import com.savvy.stocksavvyyloglog.model.SaleDAO;
import com.savvy.stocksavvyyloglog.view.NewSaleView;
import javafx.scene.control.Alert;
import javafx.scene.control.RadioButton;
import javafx.stage.Window;

import java.time.format.DateTimeFormatter;

/**
 * Controller for the New Sale modal.
 * Usage: new NewSaleController(stage, currentUser).show();
 */
public class NewSaleController {

    private final NewSaleView  view;
    private final NewSaleModel model;
    private final SaleDAO      dao;
    private final Window       owner;
    private final String       soldBy;

    // ── Constructor ───────────────────────────────────────────────────────────

    public NewSaleController(Window owner, String soldBy) {
        this.owner  = owner;
        this.soldBy = soldBy != null ? soldBy : "admin";
        this.model  = new NewSaleModel();
        this.dao    = new SaleDAO();
        this.view   = new NewSaleView() {
            @Override
            public void handleConfirm() {
                NewSaleController.this.handleConfirm();
            }
        };
        // NOTE: loadDropdowns() is called in show(), NOT here,
        // because view fields (cbProduct, cbCustomerName) are only
        // initialized after NewSaleView.show(owner) builds the UI.
    }

    // ── Backward-compatible constructor (no soldBy) ───────────────────────────

    public NewSaleController(Window owner) {
        this(owner, "admin");
    }

    // ── Show ──────────────────────────────────────────────────────────────────

    public void show() {
        view.show(owner);   // builds the UI and initializes all fields
        loadDropdowns();    // safe to populate now
    }

    // ── Load dropdowns from DB ────────────────────────────────────────────────

    private void loadDropdowns() {
        // Products from stocks table
        java.util.List<String> products = dao.getProductNames();
        if (!products.isEmpty()) {
            view.cbProduct.getItems().setAll(products);
        }

        // Customers from customers table
        java.util.List<String> customers = dao.getCustomerNames();
        if (!customers.isEmpty()) {
            view.cbCustomerName.getItems().setAll(customers);
        }

        // Auto-fill unit price when product is selected
        view.cbProduct.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isEmpty()) {
                double price = dao.getUnitPrice(newVal);
                if (price > 0) {
                    view.tfUnitPrice.setText(String.format("%.2f", price));
                }
            }
        });
    }

    // ── Confirm ───────────────────────────────────────────────────────────────

    private void handleConfirm() {
        if (!view.validateInputs()) return;

        collectFormData();

        String errors = model.validate();
        if (!errors.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Incomplete Form");
            alert.setHeaderText("Please fix the following:");
            alert.setContentText(errors.trim());
            alert.initOwner(view.getDialog());
            alert.showAndWait();
            return;
        }

        saveSale();
    }

    // ── Collect form → model ──────────────────────────────────────────────────

    private void collectFormData() {
        model.setCustomerName(view.cbCustomerName.getValue());
        model.setProduct(view.cbProduct.getValue());
        model.setDeliveryDate(view.dpDeliveryDate.getValue());
        model.setNotes(view.taNotes.getText().trim());

        try {
            model.setQuantity(Integer.parseInt(view.tfQuantity.getText().trim()));
        } catch (NumberFormatException e) {
            model.setQuantity(1);
        }

        try {
            model.setUnitPrice(Double.parseDouble(view.tfUnitPrice.getText().trim()));
        } catch (NumberFormatException e) {
            model.setUnitPrice(0);
        }

        try {
            String disc = view.tfDiscount.getText().trim();
            model.setDiscountPercent(disc.isEmpty() ? 0.0 : Double.parseDouble(disc));
        } catch (NumberFormatException e) {
            model.setDiscountPercent(0);
        }

        try {
            String fee = view.tfDeliveryFee.getText().trim();
            model.setDeliveryFee(fee.isEmpty() ? 0.0 : Double.parseDouble(fee));
        } catch (NumberFormatException e) {
            model.setDeliveryFee(0);
        }

        RadioButton selected = (RadioButton) view.paymentGroup.getSelectedToggle();
        if (selected != null) model.setPaymentMethod(selected.getText());
        
        // Handle Order Type
        model.setOrderType(view.cbOrderType.getValue());
    }

    // ── Save to database ──────────────────────────────────────────────────────

    private void saveSale() {
        String saleDate = model.getDeliveryDate()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        boolean saved = dao.recordSale(
                model.getCustomerName(),
                model.getProduct(),
                model.getQuantity(),
                model.getUnitPrice(),
                model.getDiscountPercent(),
                model.getDeliveryFee(),
                model.getPaymentMethod(),
                model.getOrderType(),
                saleDate,
                model.getNotes(),
                soldBy
        );

        if (saved) {
            view.showSuccess();
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Save Failed");
            alert.setHeaderText(null);
            alert.setContentText("Could not save the sale. Please check your database connection and try again.");
            alert.initOwner(view.getDialog());
            alert.showAndWait();
        }
    }
}