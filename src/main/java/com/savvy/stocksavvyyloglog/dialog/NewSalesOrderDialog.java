package com.savvy.stocksavvyyloglog.dialog;

import com.savvy.stocksavvyyloglog.model.SalesOrderDAO;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.List;

/**
 * NewSalesOrderDialog
 * --------------------
 * Modal dialog for creating a new Sales Order.
 * Fields: Customer, Product, Qty, Unit Price, Delivery Date, Notes.
 */
public class NewSalesOrderDialog {

    // ── Theme tokens (match SalesViewEnhanced) ────────────────────────────────
    private static final String L_BG         = "#FDF5EC";
    private static final String L_CARD_BG    = "#FDFAF0";
    private static final String L_BORDER     = "#E8D8C0";
    private static final String L_ACCENT     = "#C04A10";
    private static final String L_ACCENT2    = "#C8A96E";
    private static final String L_TEXT       = "#2A1A08";
    private static final String L_TEXT_MUTED = "#9E8050";
    private static final String L_SUCCESS    = "#4A7C4E";

    private final Stage          ownerStage;
    private final String         currentUser;
    private final SalesOrderDAO  dao = new SalesOrderDAO();

    private boolean saved = false;

    public NewSalesOrderDialog(Stage ownerStage, String currentUser) {
        this.ownerStage  = ownerStage;
        this.currentUser = currentUser;
    }

    public boolean isSaved() { return saved; }

    public void show() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(ownerStage);
        dialog.setTitle("New Sales Order");
        dialog.setResizable(false);

        VBox root = new VBox(20);
        root.setPadding(new Insets(28, 32, 28, 32));
        root.setStyle("-fx-background-color: " + L_BG + ";");
        root.setPrefWidth(460);

        // ── Title ─────────────────────────────────────────────────────────────
        Label title = new Label("📋  New Sales Order");
        title.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 20px; " +
                "-fx-font-weight: bold; -fx-text-fill: " + L_ACCENT + ";");

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: " + L_BORDER + ";");

        // ── Form grid ─────────────────────────────────────────────────────────
        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(14);
        grid.setStyle("-fx-background-color: " + L_CARD_BG + "; " +
                "-fx-border-color: " + L_BORDER + "; -fx-border-width: 1; " +
                "-fx-background-radius: 8; -fx-border-radius: 8; -fx-padding: 20;");

        ColumnConstraints col1 = new ColumnConstraints(120);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col1, col2);

        // Customer
        List<String> customers = dao.getCustomerNames();
        TextField customerField = styledField("Select or type customer…");
        customerField.setMaxWidth(Double.MAX_VALUE);
        setupAutocomplete(customerField, customers);

        // Unit Price (auto-filled on product select)
        TextField priceField = styledField("e.g. 150.00");

        // Product
        TextField productInput = styledField("Search product…");
        MenuButton productDropdown = new MenuButton();
        productDropdown.setStyle("-fx-background-color: transparent; -fx-border-color: " + L_BORDER + "; -fx-border-radius: 0 6 6 0;");
        
        for (String p : dao.getProductionProductNames()) {
            MenuItem item = new MenuItem(p);
            item.setOnAction(e -> {
                productInput.setText(p);
                double price = dao.getProductionPrice(p);
                priceField.setText(price > 0 ? String.format("%.2f", price) : "");
            });
            productDropdown.getItems().add(item);
        }

        // Delivery Date
        DatePicker deliveryPicker = new DatePicker(LocalDate.now().plusDays(3));
        deliveryPicker.setMaxWidth(Double.MAX_VALUE);
        deliveryPicker.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 12px;");

        // Notes
        TextArea notesArea = new TextArea();
        notesArea.setPromptText("Optional notes…");
        notesArea.setPrefRowCount(3);
        notesArea.setWrapText(true);
        notesArea.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 12px; " +
                "-fx-background-color: " + L_BG + "; -fx-border-color: " + L_BORDER + "; " +
                "-fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6;");

        HBox productBox = new HBox(0, productInput, productDropdown);
        HBox.setHgrow(productInput, Priority.ALWAYS);
        productInput.setStyle("-fx-background-color: " + L_BG + "; -fx-border-color: " + L_BORDER + "; -fx-border-width: 1 0 1 1; -fx-border-radius: 6 0 0 6; -fx-padding: 8 12;");

        // Qty
        TextField qtyField = styledField("e.g. 10");

        // Add rows
        addRow(grid, 0, "Customer *",      customerField);
        addRow(grid, 1, "Product *",       productBox);
        addRow(grid, 2, "Quantity *",      qtyField);
        addRow(grid, 3, "Unit Price (₱) *", priceField);
        addRow(grid, 4, "Delivery Date",   deliveryPicker);
        addRow(grid, 5, "Notes",           notesArea);

        // ── Buttons ───────────────────────────────────────────────────────────
        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 12px; " +
                "-fx-text-fill: " + L_TEXT_MUTED + "; -fx-background-color: transparent; " +
                "-fx-border-color: " + L_BORDER + "; -fx-border-width: 1; " +
                "-fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand; " +
                "-fx-padding: 8 20;");
        cancelBtn.setOnAction(e -> dialog.close());

        Button saveBtn = new Button("✔  Create Order");
        String saveBtnBase = "-fx-font-family: Sans Serif; -fx-font-size: 12px; " +
                "-fx-text-fill: white; -fx-background-color: " + L_ACCENT + "; " +
                "-fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 8 20; " +
                "-fx-font-weight: bold;";
        saveBtn.setStyle(saveBtnBase);
        saveBtn.setOnMouseEntered(e -> saveBtn.setStyle(saveBtnBase.replace(L_ACCENT, "#A03A0A")));
        saveBtn.setOnMouseExited (e -> saveBtn.setStyle(saveBtnBase));

        saveBtn.setOnAction(e -> {
            // Validate
            String customer = customerField.getText().trim();
            String product  = productInput.getText().trim();
            String qtyStr   = qtyField.getText().trim();
            String priceStr = priceField.getText().trim();

            if (customer.isBlank()) { showError("Customer is required."); return; }
            if (product.isBlank())  { showError("Product is required.");  return; }
            if (qtyStr.isBlank())   { showError("Quantity is required.");   return; }
            if (priceStr.isBlank()) { showError("Unit price is required."); return; }

            double qty, price;
            try { qty   = Double.parseDouble(qtyStr);   } catch (Exception ex) { showError("Invalid quantity.");   return; }
            try { price = Double.parseDouble(priceStr);  } catch (Exception ex) { showError("Invalid unit price."); return; }
            if (qty <= 0)   { showError("Quantity must be greater than 0.");   return; }
            if (price <= 0) { showError("Unit price must be greater than 0."); return; }

            String orderDate    = LocalDate.now().toString();
            String deliveryDate = deliveryPicker.getValue() != null
                    ? deliveryPicker.getValue().toString() : null;
            String notes = notesArea.getText().trim();

            boolean ok = dao.createOrder(customer, product, qty, price,
                    orderDate, deliveryDate, notes, currentUser);
            if (ok) {
                saved = true;
                dialog.close();
            } else {
                showError("Failed to create order. Please try again.");
            }
        });

        HBox buttons = new HBox(12, cancelBtn, saveBtn);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(title, sep, grid, buttons);

        Scene scene = new Scene(root);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private void addRow(GridPane grid, int row, String labelText, javafx.scene.Node control) {
        Label lbl = new Label(labelText);
        lbl.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 12px; " +
                "-fx-text-fill: " + L_TEXT_MUTED + "; -fx-font-weight: bold;");
        lbl.setAlignment(Pos.CENTER_RIGHT);
        lbl.setMaxWidth(Double.MAX_VALUE);
        grid.add(lbl,     0, row);
        grid.add(control, 1, row);
    }

    private TextField styledField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 12px; " +
                "-fx-background-color: " + L_BG + "; -fx-border-color: " + L_BORDER + "; " +
                "-fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6; " +
                "-fx-padding: 8 12; -fx-text-fill: " + L_TEXT + ";");
        return tf;
    }

    private void setupAutocomplete(TextField tf, List<String> items) {
        ContextMenu suggestionsMenu = new ContextMenu();
        suggestionsMenu.setPrefWidth(200);

        tf.textProperty().addListener((obs, oldV, newV) -> {
            String filter = newV == null ? "" : newV.toLowerCase().trim();
            if (filter.isEmpty()) {
                suggestionsMenu.hide();
                return;
            }

            java.util.List<MenuItem> matches = new java.util.ArrayList<>();
            for (String s : items) {
                if (s.toLowerCase().contains(filter)) {
                    MenuItem item = new MenuItem(s);
                    item.setOnAction(e -> {
                        tf.setText(s);
                        tf.positionCaret(s.length());
                        suggestionsMenu.hide();
                    });
                    matches.add(item);
                }
            }

            if (!matches.isEmpty()) {
                suggestionsMenu.getItems().setAll(matches);
                if (!suggestionsMenu.isShowing()) {
                    suggestionsMenu.show(tf, javafx.geometry.Side.BOTTOM, 0, 0);
                }
            } else {
                suggestionsMenu.hide();
            }
        });

        tf.focusedProperty().addListener((obs, oldV, newVal) -> {
            if (!newVal) suggestionsMenu.hide();
        });
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Validation Error");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}