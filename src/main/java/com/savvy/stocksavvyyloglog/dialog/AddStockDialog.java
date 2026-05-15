package com.savvy.stocksavvyyloglog.dialog;

import com.savvy.stocksavvyyloglog.model.Stock;
import com.savvy.stocksavvyyloglog.model.StockDAO;
import com.savvy.stocksavvyyloglog.util.DatabaseConnection;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
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
import javafx.stage.Window;
import javafx.util.Duration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Modal dialog for adding new stock entries.
 * Matches the StockSavy visual theme from DashboardApplication.
 * Supports both light and dark modes — token system mirrors NewOrderDialog.
 *
 * FIX: handleSave() now also calls syncToProducts() so that every stock entry
 * is mirrored into the `products` table. This allows SalesOrderDAO to resolve
 * product_id correctly and confirmOrder() to deduct stock successfully.
 */
public class AddStockDialog {

    // ── Theme tokens — light ──────────────────────────────────────────────────
    private static final String L_NAV_BG     = "#C04A10";
    private static final String L_NAV_BORDER = "#C8A96E";
    private static final String L_BG         = "#FDF5EC";
    private static final String L_BORDER     = "#E8D8C0";
    private static final String L_ACCENT     = "#C04A10";
    private static final String L_TEXT       = "#2A1A08";
    private static final String L_TEXT_MUTED = "#9E8050";
    private static final String L_INPUT_BG   = "#FDF8F4";
    private static final String L_SUCCESS_BG = "#E8F5E9";
    private static final String L_SUCCESS_FG = "#2E7D32";
    private static final String L_ERROR_FG   = "#D32F2F";

    // ── Theme tokens — dark ───────────────────────────────────────────────────
    private static final String D_NAV_BG     = "#1A0F07";
    private static final String D_NAV_BORDER = "#7A5A30";
    private static final String D_BG         = "#1A1208";
    private static final String D_BORDER     = "#4A3420";
    private static final String D_ACCENT     = "#E8622A";
    private static final String D_TEXT       = "#FAF0E6";
    private static final String D_TEXT_MUTED = "#C8A96E";
    private static final String D_INPUT_BG   = "#221808";
    private static final String D_SUCCESS_BG = "#1A2E1A";
    private static final String D_SUCCESS_FG = "#66BB6A";
    private static final String D_ERROR_FG   = "#EF5350";

    // ── Mode ──────────────────────────────────────────────────────────────────
    private final boolean darkMode;

    // ── Token accessors ───────────────────────────────────────────────────────
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

    // ── Fields ────────────────────────────────────────────────────────────────
    private TextField        tfProductName;
    private ComboBox<String> cbCategory;
    private TextField        tfQuantity;
    private ComboBox<String> cbUnit;
    private TextField        tfCostPerUnit;
    private TextField        tfSupplier;
    private DatePicker       dpDateReceived;
    private DatePicker       dpExpiryDate;
    private TextField        tfLowStockThreshold;
    private TextArea         taNotes;

    private Label            lblPreview;
    private HBox             previewBox;
    private HBox             successBanner;
    private Label            errProduct;
    private Label            errQty;

    private final StockDAO stockDAO = new StockDAO();
    private final Stage    dialog   = new Stage();
    private Runnable       onSaved;

    // ── Constructors ──────────────────────────────────────────────────────────
    public AddStockDialog() {
        this(false);
    }

    public AddStockDialog(boolean darkMode) {
        this.darkMode = darkMode;
    }

    public void setOnSaved(Runnable onSaved) {
        this.onSaved = onSaved;
    }

    // ── Show ──────────────────────────────────────────────────────────────────
    public void show(Window owner) {
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Add Stock — StockSavy");
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.setResizable(false);

        VBox root = new VBox();
        root.setStyle(
                "-fx-background-color: " + bg() + "; " +
                        "-fx-border-color: " + border() + "; " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 12; " +
                        "-fx-background-radius: 12;"
        );
        root.getChildren().addAll(buildHeader(), buildBody(), buildFooter());

        Scene scene = new Scene(root, 500, 680);
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

        loadDropdownValues();

        dialog.show();
    }

    private void loadDropdownValues() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            PreparedStatement psCat = conn.prepareStatement(
                    "SELECT DISTINCT category FROM stocks WHERE category IS NOT NULL AND category != '' ORDER BY category");
            ResultSet rsCat = psCat.executeQuery();
            while (rsCat.next()) {
                cbCategory.getItems().add(rsCat.getString("category"));
            }

            PreparedStatement psUnit = conn.prepareStatement(
                    "SELECT DISTINCT unit FROM stocks WHERE unit IS NOT NULL AND unit != '' ORDER BY unit");
            ResultSet rsUnit = psUnit.executeQuery();
            while (rsUnit.next()) {
                cbUnit.getItems().add(rsUnit.getString("unit"));
            }
        } catch (Exception e) {
            System.err.println("[AddStockDialog] Failed to load dropdown values: " + e.getMessage());
        }
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

        Label title = new Label("\uD83D\uDCE6  Add Stock");
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
        Label successLbl = new Label("\u2714   Stock added successfully!");
        successLbl.setStyle(
                "-fx-font-family: Sans Serif; -fx-font-size: 13px; " +
                        "-fx-font-weight: bold; -fx-text-fill: " + successFg() + ";"
        );
        successBanner.getChildren().add(successLbl);
        successBanner.setVisible(false);
        successBanner.setManaged(false);

        body.getChildren().addAll(
                successBanner,
                sectionLabel("PRODUCT DETAILS"),
                buildProductRow(),
                separator(),
                sectionLabel("STOCK QUANTITY"),
                buildQuantityRow(),
                buildPreviewBox(),
                separator(),
                sectionLabel("DELIVERY INFO"),
                buildDeliveryRow(),
                separator(),
                sectionLabel("EXPIRY & ALERT"),
                buildExpiryRow(),
                buildNotesRow()
        );

        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.setStyle(
                "-fx-background-color: " + bg() + "; " +
                        "-fx-background: " + bg() + "; -fx-border-width: 0;"
        );
        return scroll;
    }

    // ── Product Row ───────────────────────────────────────────────────────────
    private HBox buildProductRow() {
        tfProductName = new TextField();
        tfProductName.setPromptText("Enter product name");
        tfProductName.setMaxWidth(Double.MAX_VALUE);
        styleInput(tfProductName);
        tfProductName.textProperty().addListener((o, ov, nv) -> {
            updatePreview();
            clearError(tfProductName, errProduct);
        });

        errProduct = errorLabel("Please enter a product name.");

        VBox productBox = new VBox(5);
        HBox.setHgrow(productBox, Priority.ALWAYS);
        productBox.getChildren().addAll(fieldLabel("Product Name *"), tfProductName, errProduct);

        cbCategory = new ComboBox<>();
        cbCategory.setEditable(true);
        HBox customCategory = createCustomComboBox(cbCategory, "Enter or select category");

        VBox categoryBox = new VBox(5);
        HBox.setHgrow(categoryBox, Priority.ALWAYS);
        categoryBox.getChildren().addAll(fieldLabel("Category"), customCategory);

        HBox row = new HBox(12, productBox, categoryBox);
        row.setMaxWidth(Double.MAX_VALUE);
        return row;
    }

    // ── Quantity Row ──────────────────────────────────────────────────────────
    private HBox buildQuantityRow() {
        tfQuantity = new TextField();
        tfQuantity.setPromptText("0.00");
        styleInput(tfQuantity);
        tfQuantity.textProperty().addListener((o, ov, nv) -> {
            updatePreview();
            clearError(tfQuantity, errQty);
        });

        cbUnit = new ComboBox<>();
        cbUnit.setEditable(true);
        cbUnit.setPrefWidth(90);
        HBox customUnit = createCustomComboBox(cbUnit, "Unit");
        cbUnit.valueProperty().addListener((o, ov, nv) -> updatePreview());

        HBox qtyInner = new HBox(6, tfQuantity, customUnit);
        HBox.setHgrow(tfQuantity, Priority.ALWAYS);

        errQty = errorLabel("Enter a valid quantity.");

        VBox qtyBox = new VBox(5);
        HBox.setHgrow(qtyBox, Priority.ALWAYS);
        qtyBox.getChildren().addAll(fieldLabel("Quantity *"), qtyInner, errQty);

        tfCostPerUnit = new TextField();
        tfCostPerUnit.setPromptText("0.00");
        styleInput(tfCostPerUnit);
        tfCostPerUnit.textProperty().addListener((o, ov, nv) -> updatePreview());

        VBox costBox = new VBox(5);
        HBox.setHgrow(costBox, Priority.ALWAYS);
        costBox.getChildren().addAll(fieldLabel("Cost per Unit (\u20B1)"), tfCostPerUnit);

        HBox row = new HBox(12, qtyBox, costBox);
        row.setMaxWidth(Double.MAX_VALUE);
        return row;
    }

    // ── Preview Box ───────────────────────────────────────────────────────────
    private HBox buildPreviewBox() {
        previewBox = new HBox();
        previewBox.setPadding(new Insets(10, 14, 10, 14));
        previewBox.setStyle(
                "-fx-background-color: " + (darkMode ? "#2C2016" : "#FEF0E6") + "; " +
                        "-fx-border-color: "      + (darkMode ? "#4A3420" : "#E8CFC4") + "; -fx-border-width: 1; " +
                        "-fx-background-radius: 7; -fx-border-radius: 7;"
        );
        lblPreview = new Label();
        lblPreview.setStyle(
                "-fx-font-family: Sans Serif; -fx-font-size: 12px; " +
                        "-fx-text-fill: " + text() + ";"
        );
        lblPreview.setWrapText(true);
        previewBox.getChildren().add(lblPreview);
        previewBox.setVisible(false);
        previewBox.setManaged(false);
        return previewBox;
    }

    // ── Delivery Row ──────────────────────────────────────────────────────────
    private HBox buildDeliveryRow() {
        tfSupplier = new TextField();
        tfSupplier.setPromptText("e.g. San Rafael Meats");
        styleInput(tfSupplier);

        VBox supplierBox = new VBox(5);
        HBox.setHgrow(supplierBox, Priority.ALWAYS);
        supplierBox.getChildren().addAll(fieldLabel("Supplier"), tfSupplier);

        dpDateReceived = new DatePicker(LocalDate.now());
        dpDateReceived.setMaxWidth(Double.MAX_VALUE);
        styleInput(dpDateReceived);

        VBox dateBox = new VBox(5);
        HBox.setHgrow(dateBox, Priority.ALWAYS);
        dateBox.getChildren().addAll(fieldLabel("Date Received"), dpDateReceived);

        HBox row = new HBox(12, supplierBox, dateBox);
        row.setMaxWidth(Double.MAX_VALUE);
        return row;
    }

    // ── Expiry & Low Stock Row ────────────────────────────────────────────────
    private HBox buildExpiryRow() {
        dpExpiryDate = new DatePicker();
        dpExpiryDate.setPromptText("Optional");
        dpExpiryDate.setMaxWidth(Double.MAX_VALUE);
        styleInput(dpExpiryDate);

        VBox expiryBox = new VBox(5);
        HBox.setHgrow(expiryBox, Priority.ALWAYS);
        expiryBox.getChildren().addAll(fieldLabel("Expiry Date"), dpExpiryDate);

        tfLowStockThreshold = new TextField("5");
        tfLowStockThreshold.setPromptText("e.g. 5");
        styleInput(tfLowStockThreshold);

        VBox thresholdBox = new VBox(5);
        HBox.setHgrow(thresholdBox, Priority.ALWAYS);
        thresholdBox.getChildren().addAll(fieldLabel("Low Stock Alert At"), tfLowStockThreshold);

        HBox row = new HBox(12, expiryBox, thresholdBox);
        row.setMaxWidth(Double.MAX_VALUE);
        return row;
    }

    // ── Notes Row ─────────────────────────────────────────────────────────────
    private VBox buildNotesRow() {
        taNotes = new TextArea();
        taNotes.setPromptText("Optional: batch number, condition, remarks\u2026");
        taNotes.setPrefRowCount(3);
        taNotes.setWrapText(true);
        taNotes.setStyle(
                "-fx-font-family: Sans Serif; -fx-font-size: 13px; " +
                        "-fx-background-color: " + inputBg() + "; " +
                        "-fx-border-color: " + border() + "; -fx-border-radius: 7; -fx-background-radius: 7; " +
                        "-fx-text-fill: " + text() + "; " +
                        "-fx-control-inner-background: " + inputBg() + ";"
        );

        VBox box = new VBox(5);
        box.getChildren().addAll(fieldLabel("Notes"), taNotes);
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

        Button saveBtn = buildBtn("+ Add Stock", true);
        saveBtn.setOnAction(e -> handleSave());

        footer.getChildren().addAll(clearBtn, spacer, cancelBtn, saveBtn);
        return footer;
    }

    // ── Save Logic ────────────────────────────────────────────────────────────
    private void handleSave() {
        if (!validateInputs()) return;

        int threshold = 5;
        try {
            threshold = Integer.parseInt(tfLowStockThreshold.getText().trim());
            if (threshold < 0) threshold = 0;
        } catch (NumberFormatException ignored) {}

        String expiry = null;
        if (dpExpiryDate.getValue() != null) {
            expiry = dpExpiryDate.getValue().format(DateTimeFormatter.ISO_LOCAL_DATE);
        }

        String productName = tfProductName.getText().trim();

        double costPerUnit = tfCostPerUnit.getText().trim().isEmpty()
                ? 0 : Double.parseDouble(tfCostPerUnit.getText().trim());

        String catVal = cbCategory.isEditable()
                ? cbCategory.getEditor().getText().trim()
                : cbCategory.getValue();
        String category = (catVal != null && !catVal.isEmpty()) ? catVal.toUpperCase() : null;

        String unitVal = cbUnit.isEditable()
                ? cbUnit.getEditor().getText().trim()
                : cbUnit.getValue();
        String unit = (unitVal != null && !unitVal.isEmpty()) ? unitVal.toUpperCase() : null;

        Stock stock = new Stock(
                productName,
                category,
                Double.parseDouble(tfQuantity.getText().trim()),
                unit,
                costPerUnit,
                tfSupplier.getText().trim(),
                dpDateReceived.getValue() != null
                        ? dpDateReceived.getValue().format(DateTimeFormatter.ISO_LOCAL_DATE)
                        : LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                taNotes.getText().trim(),
                expiry,
                threshold
        );

        boolean saved = stockDAO.addStock(stock);

        if (saved) {
            // FIX: mirror into products table so SalesOrderDAO can resolve product_id
            syncToProducts(productName, unit, costPerUnit);

            setVisible(successBanner, true);
            clearForm();
            Timeline hide = new Timeline(new KeyFrame(Duration.seconds(2),
                    e -> setVisible(successBanner, false)));
            hide.play();
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Database Error");
            alert.setHeaderText(null);
            alert.setContentText("Failed to save stock. Check your database connection.");
            alert.showAndWait();
        }
    }

    /**
     * FIX: Inserts or updates the product in the `products` table.
     * This ensures SalesOrderDAO.findProductId() and confirmOrder()
     * can always resolve the product and deduct stock correctly.
     */
    private void syncToProducts(String productName, String unit, double price) {
        String checkSql  = "SELECT id FROM products WHERE name = ? LIMIT 1";
        String insertSql = "INSERT INTO products (name, unit, price_per_unit) VALUES (?, ?, ?)";
        String updateSql = "UPDATE products SET unit = ?, price_per_unit = ? WHERE name = ?";

        try (Connection conn = DatabaseConnection.getConnection()) {
            boolean exists = false;
            try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                ps.setString(1, productName);
                exists = ps.executeQuery().next();
            }

            if (exists) {
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setString(1, unit);
                    ps.setDouble(2, price);
                    ps.setString(3, productName);
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                    ps.setString(1, productName);
                    ps.setString(2, unit);
                    ps.setDouble(3, price);
                    ps.executeUpdate();
                }
            }

        } catch (Exception e) {
            System.err.println("[AddStockDialog] syncToProducts failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── Validate ──────────────────────────────────────────────────────────────
    private boolean validateInputs() {
        boolean valid = true;

        String productVal = tfProductName.getText().trim();
        if (productVal.isEmpty()) {
            showError(tfProductName, errProduct);
            valid = false;
        }

        try {
            double qty = Double.parseDouble(tfQuantity.getText().trim());
            if (qty <= 0) throw new NumberFormatException();
            clearError(tfQuantity, errQty);
        } catch (NumberFormatException e) {
            showError(tfQuantity, errQty);
            valid = false;
        }

        return valid;
    }

    // ── Live Preview ──────────────────────────────────────────────────────────
    private void updatePreview() {
        String product  = tfProductName.getText().trim();
        String qtyText  = tfQuantity.getText().trim();
        String unit     = cbUnit.getValue();
        String costText = tfCostPerUnit.getText().trim();

        if (product == null || product.isEmpty() || qtyText.isEmpty()) {
            setVisible(previewBox, false);
            return;
        }
        try {
            double qty  = Double.parseDouble(qtyText);
            double cost = costText.isEmpty() ? 0 : Double.parseDouble(costText);
            String txt  = String.format("Adding %.2f %s of %s", qty, unit, product);
            if (cost > 0) txt += String.format("   \u2014   Total cost: \u20B1%.2f", qty * cost);
            lblPreview.setText(txt);
            setVisible(previewBox, true);
        } catch (NumberFormatException e) {
            setVisible(previewBox, false);
        }
    }

    // ── Clear ─────────────────────────────────────────────────────────────────
    private void clearForm() {
        tfProductName.clear();
        cbCategory.setValue(null);
        tfQuantity.clear();
        cbUnit.setValue("kg");
        tfCostPerUnit.clear();
        tfSupplier.clear();
        dpDateReceived.setValue(LocalDate.now());
        dpExpiryDate.setValue(null);
        tfLowStockThreshold.setText("5");
        taNotes.clear();
        setVisible(previewBox, false);
        clearError(tfProductName, errProduct);
        clearError(tfQuantity, errQty);
    }

    // ── UI Helpers ────────────────────────────────────────────────────────────
    private Label fieldLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle(
                "-fx-font-family: Sans Serif; -fx-font-size: 11px; " +
                        "-fx-font-weight: bold; -fx-text-fill: " + text() + "; " +
                        "-fx-letter-spacing: 0.04em;"
        );
        return lbl;
    }

    private Label sectionLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle(
                "-fx-font-family: Sans Serif; -fx-font-size: 10px; " +
                        "-fx-font-weight: bold; -fx-text-fill: " + textMuted() + "; " +
                        "-fx-letter-spacing: 0.07em;"
        );
        return lbl;
    }

    private Label errorLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle(
                "-fx-font-family: Sans Serif; -fx-font-size: 11px; " +
                        "-fx-text-fill: " + errorFg() + ";"
        );
        lbl.setVisible(false);
        lbl.setManaged(false);
        return lbl;
    }

    private void styleInput(Control control) {
        control.setStyle(
                "-fx-font-family: Sans Serif; -fx-font-size: 13px; " +
                        "-fx-background-color: " + inputBg() + "; " +
                        "-fx-border-color: " + border() + "; -fx-border-radius: 7; -fx-background-radius: 7; " +
                        "-fx-padding: 7 10; -fx-text-fill: " + text() + ";"
        );
    }

    @SuppressWarnings("unchecked")
    private void styleComboBox(ComboBox<String> comboBox) {
        // Hide default arrow button by making it transparent and zero width
        comboBox.setStyle(
                "-fx-font-family: Sans Serif; -fx-font-size: 13px; " +
                "-fx-background-color: transparent; " +
                "-fx-border-color: transparent; " +
                "-fx-padding: 0; -fx-text-fill: " + text() + ";"
        );

        if (comboBox.isEditable()) {
            comboBox.getEditor().setStyle(
                    "-fx-background-color: transparent; " +
                    "-fx-padding: 7 10; -fx-text-fill: " + text() + ";"
            );
        }

        comboBox.setCellFactory(listView -> new javafx.scene.control.ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    setTextFill(textColor());
                    setStyle("-fx-background-color: " + inputBg() + ";");
                }
            }
        });
    }

    private HBox createCustomComboBox(ComboBox<String> comboBox, String prompt) {
        comboBox.setPromptText(prompt);
        styleComboBox(comboBox);
        comboBox.setMaxWidth(Double.MAX_VALUE);

        Label arrow = new Label("\u25BC");
        arrow.setStyle("-fx-text-fill: " + textMuted() + "; -fx-font-size: 10px; -fx-cursor: hand; -fx-padding: 0 10 0 0;");
        arrow.setOnMouseClicked(e -> {
            if (comboBox.isShowing()) comboBox.hide();
            else comboBox.show();
        });

        StackPane wrapper = new StackPane(comboBox, arrow);
        StackPane.setAlignment(arrow, Pos.CENTER_RIGHT);
        wrapper.setStyle(
                "-fx-background-color: " + inputBg() + "; " +
                "-fx-border-color: " + border() + "; " +
                "-fx-border-radius: 7; -fx-background-radius: 7;"
        );

        HBox container = new HBox(wrapper);
        container.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(wrapper, Priority.ALWAYS);
        return container;
    }

    private javafx.scene.paint.Paint textColor() {
        return darkMode ? javafx.scene.paint.Color.web(D_TEXT) : javafx.scene.paint.Color.web(L_TEXT);
    }

    private void showError(Control control, Label errLabel) {
        control.setStyle(control.getStyle() + " -fx-border-color: " + errorFg() + ";");
        setVisible(errLabel, true);
    }

    private void clearError(Control control, Label errLabel) {
        styleInput(control);
        setVisible(errLabel, false);
    }

    private void setVisible(javafx.scene.Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
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
}