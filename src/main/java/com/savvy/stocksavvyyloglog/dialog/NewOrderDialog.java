package com.savvy.stocksavvyyloglog.dialog;

import com.savvy.stocksavvyyloglog.util.DatabaseConnection;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class NewOrderDialog {

    private final Stage owner;
    private final boolean darkMode;
    private Stage dialog;

    // ── Theme tokens ──────────────────────────────────────────────────────────
    private static final String L_NAV_BG      = "#C04A10";
    private static final String L_NAV_BORDER  = "#C8A96E";
    private static final String L_BG          = "#FDF5EC";
    private static final String L_BORDER      = "#E8D8C0";
    private static final String L_ACCENT      = "#C04A10";
    private static final String L_TEXT        = "#2A1A08";
    private static final String L_TEXT_MUTED  = "#9E8050";
    private static final String L_INPUT_BG    = "#FDF8F4";
    private static final String L_SUCCESS_BG  = "#E8F5E9";
    private static final String L_SUCCESS_FG  = "#2E7D32";
    private static final String L_ERROR_FG    = "#D32F2F";

    private static final String D_NAV_BG      = "#1A0F07";
    private static final String D_NAV_BORDER  = "#7A5A30";
    private static final String D_BG          = "#1A1208";
    private static final String D_BORDER      = "#4A3420";
    private static final String D_ACCENT      = "#E8622A";
    private static final String D_TEXT        = "#FAF0E6";
    private static final String D_TEXT_MUTED  = "#C8A96E";
    private static final String D_INPUT_BG    = "#221808";
    private static final String D_SUCCESS_BG  = "#1A2E1A";
    private static final String D_SUCCESS_FG  = "#66BB6A";
    private static final String D_ERROR_FG    = "#EF5350";

    private String navBg()     { return darkMode ? D_NAV_BG     : L_NAV_BG;     }
    private String navBorder() { return darkMode ? D_NAV_BORDER : L_NAV_BORDER; }
    private String bg()        { return darkMode ? D_BG         : L_BG;         }
    private String border()    { return darkMode ? D_BORDER     : L_BORDER;     }
    private String accent()    { return darkMode ? D_ACCENT     : L_ACCENT;     }
    private String text()      { return darkMode ? D_TEXT       : L_TEXT;       }
    private String textMuted() { return darkMode ? D_TEXT_MUTED : L_TEXT_MUTED; }
    private String inputBg()   { return darkMode ? D_INPUT_BG   : L_INPUT_BG;   }
    private String successBg() { return darkMode ? D_SUCCESS_BG : L_SUCCESS_BG; }
    private String successFg() { return darkMode ? D_SUCCESS_FG : L_SUCCESS_FG; }
    private String errorFg()   { return darkMode ? D_ERROR_FG   : L_ERROR_FG;   }

    // ── Order item model ──────────────────────────────────────────────────────
    private static class OrderItem {
        int    productId;
        String productName;
        String unit;
        double unitPrice;
        int    quantity;

        OrderItem(int productId, String productName, String unit, double unitPrice, int quantity) {
            this.productId   = productId;
            this.productName = productName;
            this.unit        = unit;
            this.unitPrice   = unitPrice;
            this.quantity    = quantity;
        }

        double subtotal() { return unitPrice * quantity; }
    }

    private final List<OrderItem> orderItems = new ArrayList<>();
    private VBox             itemsContainer;
    private Label            totalLabel;
    private ComboBox<String> customerCombo;
    private DatePicker       deliveryDatePicker;
    private TextArea         notesArea;
    private Label            errorLabel;
    private HBox             successBanner;

    public NewOrderDialog(Stage owner, boolean darkMode) {
        this.owner    = owner;
        this.darkMode = darkMode;
    }

    // ── Show ──────────────────────────────────────────────────────────────────
    public void show() {
        dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.setTitle("New Sales Order — StockSavy");

        VBox root = new VBox();
        root.setStyle(
                "-fx-background-color: " + bg() + "; " +
                        "-fx-border-color: " + border() + "; " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 12; " +
                        "-fx-background-radius: 12;"
        );
        root.getChildren().addAll(buildHeader(), buildBody(), buildFooter());

        Scene scene = new Scene(root, 760, 640);
        scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene);
        dialog.centerOnScreen();

        root.setOpacity(0);
        root.setTranslateY(18);
        FadeTransition ft = new FadeTransition(Duration.millis(220), root);
        ft.setFromValue(0); ft.setToValue(1);
        TranslateTransition tt = new TranslateTransition(Duration.millis(220), root);
        tt.setFromY(18); tt.setToY(0);
        ft.play(); tt.play();

        dialog.show();
    }

    // ── Header ────────────────────────────────────────────────────────────────
    private HBox buildHeader() {
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 20, 14, 20));
        header.setStyle(
                "-fx-background-color: " + navBg() + "; " +
                        "-fx-border-width: 0 0 3 0; -fx-border-color: " + navBorder() + "; " +
                        "-fx-background-radius: 12 12 0 0;"
        );

        Label title = new Label("\uD83D\uDCCB  New Sales Order");
        title.setStyle(
                "-fx-font-family: Sans Serif; -fx-font-size: 15px; " +
                        "-fx-font-weight: bold; -fx-text-fill: #FAF0E6;"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = new Button("\u2715");
        closeBtn.setStyle(
                "-fx-font-size: 13px; -fx-text-fill: #FAF0E6; " +
                        "-fx-background-color: rgba(255,255,255,0.15); " +
                        "-fx-border-color: rgba(255,255,255,0.3); -fx-border-width: 1; " +
                        "-fx-background-radius: 6; -fx-border-radius: 6; " +
                        "-fx-cursor: hand; -fx-padding: 4 10;"
        );
        closeBtn.setOnAction(e -> dialog.close());

        header.getChildren().addAll(title, spacer, closeBtn);
        return header;
    }

    // ── Body ──────────────────────────────────────────────────────────────────
    private ScrollPane buildBody() {
        VBox body = new VBox(14);
        body.setPadding(new Insets(20));
        body.setStyle("-fx-background-color: " + bg() + ";");

        successBanner = new HBox(8);
        successBanner.setAlignment(Pos.CENTER_LEFT);
        successBanner.setPadding(new Insets(10, 14, 10, 14));
        successBanner.setStyle(
                "-fx-background-color: " + successBg() + "; " +
                        "-fx-border-color: " + (darkMode ? "#2E6030" : "#A5D6A7") + "; -fx-border-width: 1; " +
                        "-fx-background-radius: 6; -fx-border-radius: 6;"
        );
        Label successLbl = new Label("\u2714   Order placed successfully!");
        successLbl.setStyle(
                "-fx-font-family: Sans Serif; -fx-font-size: 13px; " +
                        "-fx-font-weight: bold; -fx-text-fill: " + successFg() + ";"
        );
        successBanner.getChildren().add(successLbl);
        successBanner.setVisible(false);
        successBanner.setManaged(false);

        errorLabel = new Label();
        errorLabel.setStyle(
                "-fx-font-family: Sans Serif; -fx-font-size: 11px; " +
                        "-fx-text-fill: " + errorFg() + ";"
        );
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        body.getChildren().addAll(
                successBanner,
                sectionLabel("ORDER DETAILS"),
                buildOrderDetailsRow(),
                separator(),
                sectionLabel("ORDER ITEMS"),
                buildItemsSection(),
                separator(),
                sectionLabel("NOTES"),
                buildNotesRow(),
                errorLabel
        );

        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.setStyle(
                "-fx-background-color: " + bg() + "; " +
                        "-fx-background: " + bg() + "; -fx-border-width: 0;"
        );
        return scroll;
    }

    // ── Order Details Row ─────────────────────────────────────────────────────
    private HBox buildOrderDetailsRow() {
        customerCombo = new ComboBox<>();
        customerCombo.setPromptText("— Select customer —");
        customerCombo.setMaxWidth(Double.MAX_VALUE);
        styleInput(customerCombo);
        loadCustomers();

        VBox customerBox = new VBox(5);
        HBox.setHgrow(customerBox, Priority.ALWAYS);
        customerBox.getChildren().addAll(fieldLabel("Customer *"), customerCombo);

        deliveryDatePicker = new DatePicker(LocalDate.now().plusDays(1));
        deliveryDatePicker.setMaxWidth(Double.MAX_VALUE);
        styleInput(deliveryDatePicker);

        VBox dateBox = new VBox(5);
        HBox.setHgrow(dateBox, Priority.ALWAYS);
        dateBox.getChildren().addAll(fieldLabel("Delivery Date *"), deliveryDatePicker);

        HBox row = new HBox(12, customerBox, dateBox);
        row.setMaxWidth(Double.MAX_VALUE);
        return row;
    }

    // ── Items Section ─────────────────────────────────────────────────────────
    private VBox buildItemsSection() {
        VBox section = new VBox(8);

        HBox itemsHeader = new HBox();
        itemsHeader.setAlignment(Pos.CENTER_LEFT);
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Button addItemBtn = buildBtn("+ Add Item", true);
        addItemBtn.setOnAction(e -> showAddItemRow(section));
        itemsHeader.getChildren().addAll(sp, addItemBtn);

        HBox colHeaders = new HBox(0);
        colHeaders.setPadding(new Insets(6, 8, 6, 8));
        colHeaders.setStyle("-fx-background-color: " + navBg() + "; -fx-background-radius: 6 6 0 0;");
        colHeaders.getChildren().addAll(
                colHeader("Product",    220),
                colHeader("Unit",        80),
                colHeader("Unit Price", 110),
                colHeader("Qty",         60),
                colHeader("Subtotal",   110),
                colHeader("",            40)
        );

        itemsContainer = new VBox(0);
        itemsContainer.setStyle(
                "-fx-border-color: " + border() + "; -fx-border-width: 0 1 1 1; " +
                        "-fx-border-radius: 0 0 6 6;"
        );
        addEmptyNote();

        HBox totalRow = new HBox();
        totalRow.setAlignment(Pos.CENTER_RIGHT);
        totalRow.setPadding(new Insets(8, 8, 0, 0));
        totalLabel = new Label("Total: \u20B10.00");
        totalLabel.setStyle(
                "-fx-font-family: Sans Serif; -fx-font-size: 14px; " +
                        "-fx-font-weight: bold; -fx-text-fill: " + accent() + ";"
        );
        totalRow.getChildren().add(totalLabel);

        section.getChildren().addAll(itemsHeader, colHeaders, itemsContainer, totalRow);
        return section;
    }

    // ── Inline add-item row ───────────────────────────────────────────────────
    private void showAddItemRow(VBox itemsSection) {
        itemsContainer.getChildren().removeIf(n ->
                n instanceof Label && ((Label) n).getText().startsWith("No items")
        );

        HBox addRow = new HBox(8);
        addRow.setPadding(new Insets(6, 8, 6, 8));
        addRow.setAlignment(Pos.CENTER_LEFT);
        addRow.setStyle(
                "-fx-background-color: " + (darkMode ? "#2C2016" : "#FEF0E6") + "; " +
                        "-fx-border-color: " + (darkMode ? "#4A3420" : "#E8CFC4") + "; " +
                        "-fx-border-width: 0 0 1 0;"
        );

        ComboBox<String> productCombo = new ComboBox<>();
        productCombo.setPromptText("— Select product —");
        productCombo.setPrefWidth(220);
        styleInput(productCombo);

        // FIX: Load products from stocks table (not products table which may be stale)
        List<Integer> stockIds    = new ArrayList<>();
        List<Double>  stockPrices = new ArrayList<>();
        List<String>  stockUnits  = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id, product_name, unit, cost_per_unit FROM stocks ORDER BY product_name");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                productCombo.getItems().add(rs.getString("product_name"));
                stockIds.add(rs.getInt("id"));
                stockPrices.add(rs.getDouble("cost_per_unit"));
                stockUnits.add(rs.getString("unit"));
            }
        } catch (Exception e) { e.printStackTrace(); }

        Label unitLbl = new Label("-");
        unitLbl.setPrefWidth(80);
        unitLbl.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 12px; -fx-text-fill: " + textMuted() + ";");

        TextField priceField = new TextField();
        priceField.setPromptText("0.00");
        priceField.setPrefWidth(110);
        styleInput(priceField);

        TextField qtyField = new TextField("1");
        qtyField.setPrefWidth(60);
        styleInput(qtyField);

        Label subtotalLbl = new Label("\u20B10.00");
        subtotalLbl.setPrefWidth(110);
        subtotalLbl.setStyle(
                "-fx-font-family: Sans Serif; -fx-font-size: 12px; " +
                        "-fx-font-weight: bold; -fx-text-fill: " + text() + ";"
        );

        productCombo.setOnAction(e -> {
            int idx = productCombo.getSelectionModel().getSelectedIndex();
            if (idx >= 0) {
                unitLbl.setText(stockUnits.get(idx));
                priceField.setText(String.format("%.2f", stockPrices.get(idx)));
                updateSubtotal(priceField, qtyField, subtotalLbl);
            }
        });

        priceField.textProperty().addListener((o, ov, nv) -> updateSubtotal(priceField, qtyField, subtotalLbl));
        qtyField.textProperty().addListener((o, ov, nv)   -> updateSubtotal(priceField, qtyField, subtotalLbl));

        Button confirmBtn = buildSmallBtn("\u2714", "#4A7C4E", "#3A6040");
        confirmBtn.setOnAction(e -> {
            int idx = productCombo.getSelectionModel().getSelectedIndex();
            if (idx < 0) { showError("Please select a product."); return; }
            int qty; double price;
            try { qty   = Integer.parseInt(qtyField.getText().trim());    } catch (Exception ex) { showError("Invalid quantity.");  return; }
            try { price = Double.parseDouble(priceField.getText().trim()); } catch (Exception ex) { showError("Invalid price.");     return; }
            if (qty <= 0) { showError("Quantity must be at least 1."); return; }

            // FIX: resolve product_id from products table (synced by AddStockDialog)
            int resolvedProductId = resolveProductId(productCombo.getValue(), stockIds.get(idx));

            OrderItem item = new OrderItem(
                    resolvedProductId,
                    productCombo.getValue(),
                    stockUnits.get(idx),
                    price, qty
            );
            orderItems.add(item);
            itemsContainer.getChildren().remove(addRow);
            renderItemRow(item);
            refreshTotal();
            setVisible(errorLabel, false);
        });

        Button cancelBtn = buildSmallBtn("\u2715", textMuted(), darkMode ? "#9E8050" : "#7E6040");
        cancelBtn.setOnAction(e -> {
            itemsContainer.getChildren().remove(addRow);
            if (itemsContainer.getChildren().isEmpty()) addEmptyNote();
        });

        addRow.getChildren().addAll(productCombo, unitLbl, priceField, qtyField, subtotalLbl, confirmBtn, cancelBtn);
        itemsContainer.getChildren().add(addRow);
    }

    /**
     * FIX: Resolve product_id from the products table (synced by AddStockDialog.syncToProducts).
     * Falls back to the stocks row id if not found.
     */
    private int resolveProductId(String productName, int fallbackId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id FROM products WHERE name = ? LIMIT 1")) {
            ps.setString(1, productName);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) { e.printStackTrace(); }
        return fallbackId;
    }

    private void updateSubtotal(TextField priceField, TextField qtyField, Label subtotalLbl) {
        try {
            double price = Double.parseDouble(priceField.getText().trim());
            int    qty   = Integer.parseInt(qtyField.getText().trim());
            subtotalLbl.setText(String.format("\u20B1%.2f", price * qty));
        } catch (Exception ignored) {
            subtotalLbl.setText("\u20B10.00");
        }
    }

    private void renderItemRow(OrderItem item) {
        HBox row = new HBox(0);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(7, 8, 7, 8));
        row.setStyle(
                "-fx-background-color: " + (darkMode ? "#221808" : "#FDFAF0") + "; " +
                        "-fx-border-color: " + border() + "; -fx-border-width: 0 0 1 0;"
        );
        row.getChildren().addAll(
                rowCell(item.productName,                                220, true),
                rowCell(item.unit,                                        80, false),
                rowCell(String.format("\u20B1%.2f", item.unitPrice),     110, false),
                rowCell(String.valueOf(item.quantity),                    60, false),
                rowCell(String.format("\u20B1%.2f", item.subtotal()),    110, true),
                buildRemoveBtn(item, row)
        );
        itemsContainer.getChildren().add(row);
    }

    private Label rowCell(String text, double width, boolean bold) {
        Label lbl = new Label(text);
        lbl.setPrefWidth(width);
        lbl.setStyle(
                "-fx-font-family: Sans Serif; -fx-font-size: 12px; " +
                        "-fx-text-fill: " + text() + "; " +
                        (bold ? "-fx-font-weight: bold;" : "")
        );
        return lbl;
    }

    private Button buildRemoveBtn(OrderItem item, HBox row) {
        Button btn = new Button("\u2715");
        btn.setStyle(
                "-fx-font-size: 11px; -fx-text-fill: " + errorFg() + "; " +
                        "-fx-background-color: transparent; -fx-cursor: hand; " +
                        "-fx-border-width: 0; -fx-padding: 2 6;"
        );
        btn.setOnAction(e -> {
            orderItems.remove(item);
            itemsContainer.getChildren().remove(row);
            refreshTotal();
            if (itemsContainer.getChildren().isEmpty()) addEmptyNote();
        });
        return btn;
    }

    private void addEmptyNote() {
        Label empty = new Label("No items added yet. Click '+ Add Item' to begin.");
        empty.setStyle(
                "-fx-font-family: Sans Serif; -fx-font-size: 11px; " +
                        "-fx-text-fill: " + textMuted() + "; -fx-padding: 14 0 14 0;"
        );
        empty.setMaxWidth(Double.MAX_VALUE);
        empty.setAlignment(Pos.CENTER);
        itemsContainer.getChildren().add(empty);
    }

    private void refreshTotal() {
        double total = orderItems.stream().mapToDouble(OrderItem::subtotal).sum();
        totalLabel.setText(String.format("Total: \u20B1%.2f", total));
    }

    // ── Notes Row ─────────────────────────────────────────────────────────────
    private VBox buildNotesRow() {
        notesArea = new TextArea();
        notesArea.setPromptText("Optional: special instructions, remarks\u2026");
        notesArea.setPrefRowCount(3);
        notesArea.setWrapText(true);
        notesArea.setStyle(
                "-fx-font-family: Sans Serif; -fx-font-size: 13px; " +
                        "-fx-background-color: " + inputBg() + "; " +
                        "-fx-border-color: " + border() + "; -fx-border-radius: 7; -fx-background-radius: 7; " +
                        "-fx-text-fill: " + text() + "; " +
                        "-fx-control-inner-background: " + inputBg() + ";"
        );
        VBox box = new VBox(5);
        box.getChildren().addAll(fieldLabel("Notes"), notesArea);
        return box;
    }

    // ── Footer ────────────────────────────────────────────────────────────────
    private HBox buildFooter() {
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(14, 20, 14, 20));
        footer.setStyle(
                "-fx-background-color: " + (darkMode ? "#221410" : "#FDF0E8") + "; " +
                        "-fx-border-width: 1 0 0 0; -fx-border-color: " + border() + "; " +
                        "-fx-background-radius: 0 0 12 12;"
        );

        Button clearBtn = buildBtn("\u21BA   Clear", false);
        clearBtn.setOnAction(e -> clearForm());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button cancelBtn = buildBtn("Cancel", false);
        cancelBtn.setOnAction(e -> dialog.close());

        Button placeBtn = buildBtn("Place Order", true);
        placeBtn.setOnAction(e -> submitOrder());

        footer.getChildren().addAll(clearBtn, spacer, cancelBtn, placeBtn);
        return footer;
    }

    // ── Submit ────────────────────────────────────────────────────────────────
    private void submitOrder() {
        if (customerCombo.getValue() == null || customerCombo.getValue().trim().isEmpty()) {
            showError("Please select a customer."); return;
        }
        if (deliveryDatePicker.getValue() == null) {
            showError("Please select a delivery date."); return;
        }
        if (orderItems.isEmpty()) {
            showError("Please add at least one order item."); return;
        }

        int customerId = getCustomerIdByName(customerCombo.getValue());
        if (customerId == -1) { showError("Customer not found in database."); return; }

        double    total = orderItems.stream().mapToDouble(OrderItem::subtotal).sum();
        String    notes = notesArea.getText().trim();
        LocalDate dDate = deliveryDatePicker.getValue();

        // FIX: Use first item's product_id for the sales_orders.product_id column
        // (sales_orders is a single-product order table; multi-item uses sales_items)
        OrderItem firstItem = orderItems.get(0);

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String orderSql =
                        "INSERT INTO sales_orders (customer_id, product_id, quantity, unit_price, total_amount, " +
                                "status, order_date, delivery_date, notes, created_by) " +
                                "VALUES (?, ?, ?, ?, ?, 'Pending', CURDATE(), ?, ?, ?)";

                int orderId;
                try (PreparedStatement ps = conn.prepareStatement(orderSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, customerId);
                    ps.setInt(2, firstItem.productId);
                    ps.setDouble(3, firstItem.quantity);
                    ps.setDouble(4, firstItem.unitPrice);
                    ps.setDouble(5, total);
                    ps.setDate(6, java.sql.Date.valueOf(dDate));
                    ps.setString(7, notes.isEmpty() ? null : notes);
                    ps.setString(8, "admin");
                    ps.executeUpdate();
                    ResultSet keys = ps.getGeneratedKeys();
                    if (!keys.next()) throw new Exception("Failed to get order ID.");
                    orderId = keys.getInt(1);
                }

                // Also insert into sales_items for multi-item tracking
                try {
                    String itemSql =
                            "INSERT INTO sales_items (sale_id, product_id, quantity, unit_price) " +
                                    "VALUES (?, ?, ?, ?)";
                    try (PreparedStatement ps = conn.prepareStatement(itemSql)) {
                        for (OrderItem item : orderItems) {
                            ps.setInt(1, orderId);
                            ps.setInt(2, item.productId);
                            ps.setInt(3, item.quantity);
                            ps.setDouble(4, item.unitPrice);
                            ps.addBatch();
                        }
                        ps.executeBatch();
                    }
                } catch (Exception ignored) {
                    // sales_items table may not exist — order is still saved
                }

                conn.commit();
                showSuccessBanner(orderId, total);

            } catch (Exception ex) {
                conn.rollback();
                ex.printStackTrace();
                showError("Failed to save order: " + ex.getMessage());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Database connection error.");
        }
    }

    private void showSuccessBanner(int orderId, double total) {
        Label lbl = (Label) successBanner.getChildren().get(0);
        lbl.setText(String.format("\u2714   Order #%d placed! Total: \u20B1%.2f", orderId, total));
        setVisible(successBanner, true);
        clearForm();
        Timeline hide = new Timeline(new KeyFrame(Duration.seconds(3), e -> {
            setVisible(successBanner, false);
            dialog.close();
        }));
        hide.play();
    }

    private int getCustomerIdByName(String displayName) {
        String name = displayName.contains(" \u2014 ")
                ? displayName.split(" \u2014 ")[0].trim()
                : displayName.trim();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id FROM customers WHERE name = ? LIMIT 1")) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) { e.printStackTrace(); }
        return -1;
    }

    private void loadCustomers() {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT name, phone FROM customers ORDER BY name");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String phone = rs.getString("phone");
                String label = rs.getString("name") +
                        (phone != null && !phone.trim().isEmpty() ? " \u2014 " + phone : "");
                customerCombo.getItems().add(label);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void clearForm() {
        customerCombo.setValue(null);
        deliveryDatePicker.setValue(LocalDate.now().plusDays(1));
        orderItems.clear();
        itemsContainer.getChildren().clear();
        addEmptyNote();
        refreshTotal();
        notesArea.clear();
        setVisible(errorLabel, false);
    }

    // ── UI Helpers ────────────────────────────────────────────────────────────
    private Label sectionLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 10px; -fx-font-weight: bold; " +
                "-fx-text-fill: " + textMuted() + "; -fx-letter-spacing: 0.07em;");
        return lbl;
    }

    private Label fieldLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 11px; -fx-font-weight: bold; " +
                "-fx-text-fill: " + text() + "; -fx-letter-spacing: 0.04em;");
        return lbl;
    }

    private Label colHeader(String text, double width) {
        Label lbl = new Label(text);
        lbl.setPrefWidth(width);
        lbl.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 11px; " +
                "-fx-font-weight: bold; -fx-text-fill: #FAF0E6;");
        return lbl;
    }

    private void styleInput(Control control) {
        control.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 13px; " +
                "-fx-background-color: " + inputBg() + "; " +
                "-fx-border-color: " + border() + "; -fx-border-radius: 7; -fx-background-radius: 7; " +
                "-fx-padding: 7 10; -fx-text-fill: " + text() + ";");
    }

    private Region separator() {
        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setStyle("-fx-background-color: " + border() + ";");
        return sep;
    }

    private Button buildBtn(String text, boolean primary) {
        Button btn = new Button(text);
        String accentColor = accent();
        String base, hover;
        if (primary) {
            base  = "-fx-font-family: Sans Serif; -fx-font-size: 13px; -fx-font-weight: bold; " +
                    "-fx-text-fill: white; -fx-background-color: " + accentColor + "; " +
                    "-fx-background-radius: 7; -fx-cursor: hand; -fx-padding: 8 20;";
            hover = base.replace(accentColor, darkMode ? "#FF7A3A" : "#A03A0A");
        } else {
            base  = "-fx-font-family: Sans Serif; -fx-font-size: 13px; " +
                    "-fx-text-fill: " + textMuted() + "; -fx-background-color: transparent; " +
                    "-fx-border-color: " + border() + "; -fx-border-width: 1; " +
                    "-fx-background-radius: 7; -fx-border-radius: 7; -fx-cursor: hand; -fx-padding: 8 18;";
            hover = base.replace("transparent", darkMode ? "#3A2010" : "#F3EBE7");
        }
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e  -> btn.setStyle(base));
        return btn;
    }

    private Button buildSmallBtn(String text, String bg, String hoverBg) {
        Button btn = new Button(text);
        String base  = "-fx-font-family: Sans Serif; -fx-font-size: 11px; -fx-text-fill: white; " +
                "-fx-background-color: " + bg + "; -fx-background-radius: 5; " +
                "-fx-border-width: 0; -fx-cursor: hand; -fx-padding: 5 12;";
        String hover = base.replace(bg, hoverBg);
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e  -> btn.setStyle(base));
        return btn;
    }

    private void showError(String msg) {
        errorLabel.setText("\u26A0  " + msg);
        setVisible(errorLabel, true);
    }

    private void setVisible(javafx.scene.Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }
}