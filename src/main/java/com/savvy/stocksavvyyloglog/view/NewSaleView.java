package com.savvy.stocksavvyyloglog.view;

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

import java.time.LocalDate;

/**
 * Modal dialog for recording a new sale.
 * Design mirrors AddStockDialog / NewOrderDialog — same header, sections,
 * field helpers, preview box, footer buttons, success banner, and dark mode support.
 */
public class NewSaleView {

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

    // ── Form fields (public for controller access across packages) ────────────
    public ComboBox<String> cbCustomerName;
    public ComboBox<String> cbProduct;
    public ComboBox<String> cbOrderType;
    public TextField        tfQuantity;
    public TextField        tfUnitPrice;
    public TextField        tfDiscount;
    public ToggleGroup      paymentGroup;
    public RadioButton      rbCash, rbGCash, rbBankTransfer, rbCredit;
    public DatePicker       dpDeliveryDate;
    public Label            lblDeliveryDate;
    public TextArea         taNotes;

    public Label            lblPreview;
    public HBox             previewBox;
    public HBox             successBanner;
    public Label            errCustomer;
    public Label            errProduct;
    public Label            errPrice;
    public Label            errOrderType;

    public Label            totalLabel;

    private final Stage dialog = new Stage();

    // ── Constructors ──────────────────────────────────────────────────────────
    /** Light-mode default — keeps existing call sites working. */
    public NewSaleView() {
        this(false);
    }

    public NewSaleView(boolean darkMode) {
        this.darkMode = darkMode;
    }

    // ── Show ──────────────────────────────────────────────────────────────────
    public void show(Window owner) {
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("New Sale — StockSavy");
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

        Scene scene = new Scene(root, 500, 660);
        scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene);
        dialog.centerOnScreen();

        // Entrance animation — mirrors NewOrderDialog / AddStockDialog
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

        Label title = new Label("\uD83D\uDCB5  New Sale");
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

        // Success banner (hidden by default)
        successBanner = new HBox(8);
        successBanner.setAlignment(Pos.CENTER_LEFT);
        successBanner.setPadding(new Insets(10, 14, 10, 14));
        successBanner.setStyle(
                "-fx-background-color: " + successBg() + "; " +
                        "-fx-border-color: " + (darkMode ? "#2E6030" : "#A5D6A7") + "; -fx-border-width: 1; " +
                        "-fx-background-radius: 6; -fx-border-radius: 6;"
        );
        Label successLbl = new Label("\u2714   Sale recorded successfully!");
        successLbl.setStyle(
                "-fx-font-family: Sans Serif; -fx-font-size: 13px; " +
                        "-fx-font-weight: bold; -fx-text-fill: " + successFg() + ";"
        );
        successBanner.getChildren().add(successLbl);
        successBanner.setVisible(false);
        successBanner.setManaged(false);

        body.getChildren().addAll(
                successBanner,
                sectionLabel("CUSTOMER INFO"),
                buildCustomerRow(),
                separator(),
                sectionLabel("PRODUCT & PRICING"),
                buildProductRow(),
                buildPriceRow(),
                buildPreviewBox(),
                separator(),
                sectionLabel("ORDER & PAYMENT"),
                buildOrderTypeRow(),
                buildPaymentRow(),
                buildDeliveryRow(),
                separator(),
                sectionLabel("NOTES"),
                buildNotesRow()
        );

        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.setStyle(
                "-fx-background-color: " + bg() + "; " +
                        "-fx-background: " + bg() + "; " +
                        "-fx-border-width: 0;"
        );
        return scroll;
    }

    // ── Customer Row ──────────────────────────────────────────────────────────
    private VBox buildCustomerRow() {
        cbCustomerName = new ComboBox<>();
        cbCustomerName.setEditable(true);
        cbCustomerName.getItems().addAll(
                "Ate Nena", "Jollibee", "Robinsons Supermarket",
                "SM Supermarket", "Walk-in Customer"
        );
        cbCustomerName.setPromptText("— Select or type customer —");
        cbCustomerName.setMaxWidth(Double.MAX_VALUE);
        styleInput(cbCustomerName);
        cbCustomerName.valueProperty().addListener((o, ov, nv) ->
                clearError(cbCustomerName, errCustomer));

        errCustomer = errorLabel("Customer name is required.");

        VBox box = new VBox(5);
        box.getChildren().addAll(fieldLabel("Customer Name *"), cbCustomerName, errCustomer);
        return box;
    }

    // ── Product Row ───────────────────────────────────────────────────────────
    private HBox buildProductRow() {
        cbProduct = new ComboBox<>();
        cbProduct.getItems().addAll(
                "Longanisa (Garlic)", "Longanisa (Sweet)",
                "Tocino", "Tapa", "Longganisang Hamonado"
        );
        cbProduct.setPromptText("— Select product —");
        cbProduct.setMaxWidth(Double.MAX_VALUE);
        styleInput(cbProduct);
        cbProduct.valueProperty().addListener((o, ov, nv) -> {
            updatePreview();
            clearError(cbProduct, errProduct);
        });

        errProduct = errorLabel("Please select a product.");

        VBox productBox = new VBox(5);
        HBox.setHgrow(productBox, Priority.ALWAYS);
        productBox.getChildren().addAll(fieldLabel("Product *"), cbProduct, errProduct);

        tfQuantity = new TextField("1");
        styleInput(tfQuantity);
        tfQuantity.textProperty().addListener((o, ov, nv) -> updatePreview());

        VBox qtyBox = new VBox(5);
        qtyBox.setPrefWidth(110);
        qtyBox.getChildren().addAll(fieldLabel("Quantity"), tfQuantity);

        HBox row = new HBox(12, productBox, qtyBox);
        row.setMaxWidth(Double.MAX_VALUE);
        return row;
    }

    // ── Price Row ─────────────────────────────────────────────────────────────
    private HBox buildPriceRow() {
        tfUnitPrice = new TextField();
        tfUnitPrice.setPromptText("0.00");
        styleInput(tfUnitPrice);
        tfUnitPrice.textProperty().addListener((o, ov, nv) -> {
            updatePreview();
            clearError(tfUnitPrice, errPrice);
        });

        errPrice = errorLabel("Enter a valid unit price.");

        VBox priceBox = new VBox(5);
        HBox.setHgrow(priceBox, Priority.ALWAYS);
        priceBox.getChildren().addAll(fieldLabel("Unit Price (\u20B1) *"), tfUnitPrice, errPrice);

        tfDiscount = new TextField();
        tfDiscount.setPromptText("0");
        styleInput(tfDiscount);
        tfDiscount.textProperty().addListener((o, ov, nv) -> updatePreview());

        VBox discountBox = new VBox(5);
        discountBox.setPrefWidth(110);
        discountBox.getChildren().addAll(fieldLabel("Discount %"), tfDiscount);

        HBox row = new HBox(12, priceBox, discountBox);
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

    // ── Payment Row ───────────────────────────────────────────────────────────
    private VBox buildPaymentRow() {
        paymentGroup = new ToggleGroup();

        rbCash         = radioBtn("Cash",          true);
        rbGCash        = radioBtn("GCash",         false);
        rbBankTransfer = radioBtn("Bank Transfer", false);
        rbCredit       = radioBtn("Credit",        false);

        HBox radioRow = new HBox(16, rbCash, rbGCash, rbBankTransfer, rbCredit);
        radioRow.setAlignment(Pos.CENTER_LEFT);
        radioRow.setPadding(new Insets(4, 0, 0, 0));

        VBox box = new VBox(5);
        box.getChildren().addAll(fieldLabel("Payment Method"), radioRow);
        return box;
    }

    // ── Delivery Row ──────────────────────────────────────────────────────────
    private HBox buildDeliveryRow() {
        lblDeliveryDate = fieldLabel("Delivery Date");
        dpDeliveryDate = new DatePicker(LocalDate.now());
        dpDeliveryDate.setMaxWidth(Double.MAX_VALUE);
        styleInput(dpDeliveryDate);

        VBox dateBox = new VBox(5);
        HBox.setHgrow(dateBox, Priority.ALWAYS);
        dateBox.getChildren().addAll(lblDeliveryDate, dpDeliveryDate);

        totalLabel = new Label("Total: \u20B10.00");
        totalLabel.setStyle(
                "-fx-font-family: Sans Serif; -fx-font-size: 15px; " +
                        "-fx-font-weight: bold; -fx-text-fill: " + accent() + ";"
        );

        VBox totalBox = new VBox(5);
        totalBox.setAlignment(Pos.BOTTOM_RIGHT);
        totalBox.setPrefWidth(160);
        totalBox.getChildren().addAll(new Label(""), totalLabel);

        HBox row = new HBox(12, dateBox, totalBox);
        row.setMaxWidth(Double.MAX_VALUE);
        row.setAlignment(Pos.BOTTOM_LEFT);
        return row;
    }

    private VBox buildOrderTypeRow() {
        cbOrderType = new ComboBox<>();
        cbOrderType.getItems().addAll("pickup", "deliver");
        cbOrderType.setValue("pickup");
        cbOrderType.setMaxWidth(Double.MAX_VALUE);
        styleInput(cbOrderType);
        
        cbOrderType.valueProperty().addListener((o, ov, nv) -> {
            if ("pickup".equals(nv)) {
                lblDeliveryDate.setText("Pick-up Date");
            } else {
                lblDeliveryDate.setText("Delivery Date");
            }
            clearError(cbOrderType, errOrderType);
        });

        errOrderType = errorLabel("Please select order type.");

        VBox box = new VBox(5);
        box.getChildren().addAll(fieldLabel("Order Type *"), cbOrderType, errOrderType);
        return box;
    }

    // ── Notes Row ─────────────────────────────────────────────────────────────
    private VBox buildNotesRow() {
        taNotes = new TextArea();
        taNotes.setPromptText("Optional: special instructions, remarks\u2026");
        taNotes.setPrefRowCount(3);
        taNotes.setWrapText(true);
        taNotes.setStyle(
                "-fx-font-family: Sans Serif; -fx-font-size: 13px; " +
                        "-fx-background-color: " + inputBg() + "; " +
                        "-fx-border-color: " + border() + "; " +
                        "-fx-border-radius: 7; -fx-background-radius: 7; " +
                        "-fx-text-fill: " + text() + "; " +
                        "-fx-control-inner-background: " + inputBg() + ";"
        );

        VBox box = new VBox(5);
        box.getChildren().addAll(fieldLabel("Notes"), taNotes);
        return box;
    }

    // ── Footer ────────────────────────────────────────────────────────────────
    HBox buildFooter() {
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

        Button confirmBtn = buildBtn("\u2714  Confirm Sale", true);
        confirmBtn.setOnAction(e -> handleConfirm());

        footer.getChildren().addAll(clearBtn, spacer, cancelBtn, confirmBtn);
        return footer;
    }

    // ── Live preview ──────────────────────────────────────────────────────────
    public void updatePreview() {
        String product   = cbProduct.getValue();
        String qtyText   = tfQuantity.getText().trim();
        String priceText = tfUnitPrice.getText().trim();
        String discText  = tfDiscount.getText().trim();

        if (product == null || priceText.isEmpty()) {
            setVisible(previewBox, false);
            totalLabel.setText("Total: \u20B10.00");
            return;
        }
        try {
            int    qty      = qtyText.isEmpty() ? 1 : Integer.parseInt(qtyText);
            double price    = Double.parseDouble(priceText);
            double discount = discText.isEmpty() ? 0 : Double.parseDouble(discText);
            double total    = price * qty * (1.0 - discount / 100.0);

            String preview = String.format("%d \u00D7 %s @ \u20B1%.2f", qty, product, price);
            if (discount > 0) preview += String.format(" (%.0f%% off)", discount);
            preview += String.format("   \u2014   Total: \u20B1%.2f", total);

            lblPreview.setText(preview);
            totalLabel.setText(String.format("Total: \u20B1%.2f", total));
            setVisible(previewBox, true);
        } catch (NumberFormatException e) {
            setVisible(previewBox, false);
            totalLabel.setText("Total: \u20B1\u2014");
        }
    }

    // ── Validation ────────────────────────────────────────────────────────────
    public boolean validateInputs() {
        boolean valid = true;

        if (cbCustomerName.getValue() == null || cbCustomerName.getValue().trim().isEmpty()) {
            showError(cbCustomerName, errCustomer);
            valid = false;
        }

        if (cbProduct.getValue() == null || cbProduct.getValue().trim().isEmpty()) {
            showError(cbProduct, errProduct);
            valid = false;
        }

        if (cbOrderType.getValue() == null) {
            showError(cbOrderType, errOrderType);
            valid = false;
        }

        try {
            double price = Double.parseDouble(tfUnitPrice.getText().trim());
            if (price <= 0) throw new NumberFormatException();
            clearError(tfUnitPrice, errPrice);
        } catch (NumberFormatException e) {
            showError(tfUnitPrice, errPrice);
            valid = false;
        }

        return valid;
    }

    // ── Clear form ────────────────────────────────────────────────────────────
    public void clearForm() {
        cbCustomerName.setValue(null);
        cbProduct.setValue(null);
        cbOrderType.setValue("pickup");
        tfQuantity.setText("1");
        tfUnitPrice.clear();
        tfDiscount.clear();
        rbCash.setSelected(true);
        dpDeliveryDate.setValue(LocalDate.now());
        taNotes.clear();
        totalLabel.setText("Total: \u20B10.00");
        setVisible(previewBox, false);
        clearError(cbCustomerName, errCustomer);
        clearError(cbProduct, errProduct);
        clearError(cbOrderType, errOrderType);
        clearError(tfUnitPrice, errPrice);
    }

    // ── Show success banner ───────────────────────────────────────────────────
    public void showSuccess() {
        setVisible(successBanner, true);
        clearForm();
        Timeline hide = new Timeline(new KeyFrame(Duration.seconds(2),
                e -> setVisible(successBanner, false)));
        hide.play();
    }

    // ── Placeholder — overridden by controller ────────────────────────────────
    public void handleConfirm() { /* wired in NewSaleController */ }

    // ── UI Helpers ────────────────────────────────────────────────────────────

    public Label fieldLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle(
                "-fx-font-family: Sans Serif; -fx-font-size: 11px; " +
                        "-fx-font-weight: bold; -fx-text-fill: " + text() + "; " +
                        "-fx-letter-spacing: 0.04em;"
        );
        return lbl;
    }

    public Label sectionLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle(
                "-fx-font-family: Sans Serif; -fx-font-size: 10px; " +
                        "-fx-font-weight: bold; -fx-text-fill: " + textMuted() + "; " +
                        "-fx-letter-spacing: 0.07em;"
        );
        return lbl;
    }

    public Label errorLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle(
                "-fx-font-family: Sans Serif; -fx-font-size: 11px; " +
                        "-fx-text-fill: " + errorFg() + ";"
        );
        lbl.setVisible(false);
        lbl.setManaged(false);
        return lbl;
    }

    public void styleInput(Control control) {
        control.setStyle(
                "-fx-font-family: Sans Serif; -fx-font-size: 13px; " +
                        "-fx-background-color: " + inputBg() + "; " +
                        "-fx-border-color: " + border() + "; " +
                        "-fx-border-radius: 7; -fx-background-radius: 7; " +
                        "-fx-padding: 7 10; -fx-text-fill: " + text() + ";"
        );
    }

    public void showError(Control control, Label errLabel) {
        control.setStyle(control.getStyle() + " -fx-border-color: " + errorFg() + ";");
        setVisible(errLabel, true);
    }

    public void clearError(Control control, Label errLabel) {
        styleInput(control);
        setVisible(errLabel, false);
    }

    public void setVisible(javafx.scene.Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    public Region separator() {
        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setStyle("-fx-background-color: " + border() + ";");
        return sep;
    }

    public Button buildBtn(String text, boolean primary) {
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

    private RadioButton radioBtn(String text, boolean selected) {
        RadioButton rb = new RadioButton(text);
        rb.setToggleGroup(paymentGroup);
        rb.setSelected(selected);
        rb.setStyle(
                "-fx-font-family: Sans Serif; -fx-font-size: 13px; " +
                        "-fx-text-fill: " + text() + "; -fx-cursor: hand;"
        );
        return rb;
    }

    public Stage getDialog() { return dialog; }
}