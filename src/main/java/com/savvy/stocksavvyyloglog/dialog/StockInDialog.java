package com.savvy.stocksavvyyloglog.dialog;

import com.savvy.stocksavvyyloglog.util.DatabaseConnection;
import com.savvy.stocksavvyyloglog.view.InventoryView;
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
import javafx.stage.Window;
import javafx.util.Duration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * StockInDialog — records incoming stock and updates the stocks table quantity.
 * FIX: stock_in insert is now wrapped in its own try-catch so a missing column
 * won't block the stocks quantity update (the critical operation).
 */
public class StockInDialog {

    // ── Green theme tokens ────────────────────────────────────────────────────
    private static final String NAV_BG       = "#1B5E20";
    private static final String NAV_BORDER   = "#66BB6A";
    private static final String BG           = "#F1F8F2";
    private static final String BORDER       = "#C8E6C9";
    private static final String ACCENT       = "#2E7D32";
    private static final String ACCENT_HOVER = "#1B5E20";
    private static final String TEXT         = "#0A1F0C";
    private static final String TEXT_MUTED   = "#4A7C4E";
    private static final String INPUT_BG     = "#F4FAF4";
    private static final String SUCCESS_BG   = "#E8F5E9";
    private static final String SUCCESS_FG   = "#2E7D32";
    private static final String ERROR_FG     = "#D32F2F";
    private static final String FOOTER_BG    = "#EBF5EC";
    private static final String PREVIEW_BG   = "#E8F5E9";
    private static final String PREVIEW_BR   = "#C8E6C9";

    private final Stage              ownerStage;
    private final InventoryView.StockRow row;
    private       Runnable           onSaved;
    private final Stage              dialog = new Stage();

    private TextField  tfQty;
    private TextField  tfUnit;
    private TextField  tfSupplier;
    private DatePicker dpDateIn;
    private TextArea   taNotes;
    private Label      errQty;
    private HBox       successBanner;
    private HBox       previewBox;
    private Label      lblPreview;

    public StockInDialog(Stage ownerStage, InventoryView.StockRow row) {
        this.ownerStage = ownerStage;
        this.row        = row;
    }

    public void setOnSaved(Runnable onSaved) { this.onSaved = onSaved; }

    public void show() {
        dialog.initOwner(ownerStage);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle("Stock In — " + row.getProductName());
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.setResizable(false);

        VBox root = new VBox();
        root.setStyle(
                "-fx-background-color: " + BG + "; " +
                        "-fx-border-color: " + BORDER + "; " +
                        "-fx-border-width: 1; -fx-border-radius: 12; -fx-background-radius: 12;"
        );
        root.getChildren().addAll(buildHeader(), buildBody(), buildFooter());

        Scene scene = new Scene(root, 500, 560);
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

    private HBox buildHeader() {
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 20, 14, 20));
        header.setStyle(
                "-fx-background-color: " + NAV_BG + "; " +
                        "-fx-border-width: 0 0 3 0; -fx-border-color: " + NAV_BORDER + "; " +
                        "-fx-background-radius: 12 12 0 0;"
        );
        Label title = new Label("📥  Stock In  —  " + row.getProductName());
        title.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 15px; " +
                "-fx-font-weight: bold; -fx-text-fill: #F1F8F2;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-font-size: 13px; -fx-text-fill: #F1F8F2; " +
                "-fx-background-color: rgba(255,255,255,0.15); " +
                "-fx-border-color: rgba(255,255,255,0.3); -fx-border-width: 1; " +
                "-fx-background-radius: 6; -fx-border-radius: 6; -fx-cursor: hand; -fx-padding: 4 10;");
        closeBtn.setOnAction(e -> dialog.close());
        header.getChildren().addAll(title, spacer, closeBtn);
        return header;
    }

    private ScrollPane buildBody() {
        VBox body = new VBox(14);
        body.setPadding(new Insets(20));
        body.setStyle("-fx-background-color: " + BG + ";");

        successBanner = new HBox(8);
        successBanner.setAlignment(Pos.CENTER_LEFT);
        successBanner.setPadding(new Insets(10, 14, 10, 14));
        successBanner.setStyle("-fx-background-color: " + SUCCESS_BG + "; " +
                "-fx-border-color: #A5D6A7; -fx-border-width: 1; " +
                "-fx-background-radius: 6; -fx-border-radius: 6;");
        Label successLbl = new Label("✔   Stock In recorded successfully!");
        successLbl.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 13px; " +
                "-fx-font-weight: bold; -fx-text-fill: " + SUCCESS_FG + ";");
        successBanner.getChildren().add(successLbl);
        setVisible(successBanner, false);

        body.getChildren().addAll(
                successBanner,
                sectionLabel("CURRENT STOCK INFO"),
                buildInfoBar(),
                separator(),
                sectionLabel("QUANTITY TO ADD"),
                buildQtyRow(),
                buildPreviewBox(),
                separator(),
                sectionLabel("DELIVERY DETAILS"),
                buildDeliveryRow(),
                separator(),
                sectionLabel("NOTES"),
                buildNotesRow()
        );

        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: " + BG + "; -fx-background: " + BG + "; -fx-border-width: 0;");
        return scroll;
    }

    private HBox buildInfoBar() {
        HBox bar = new HBox(20);
        bar.setPadding(new Insets(12, 14, 12, 14));
        bar.setStyle("-fx-background-color: " + SUCCESS_BG + "; " +
                "-fx-border-color: " + NAV_BORDER + "; -fx-border-width: 1; " +
                "-fx-background-radius: 8; -fx-border-radius: 8;");
        bar.getChildren().addAll(
                infoChip("Current Qty", row.getQuantity() + " " + row.getUnit()),
                infoChip("Category",    row.getCategory()),
                infoChip("Supplier",    row.getSupplier().isEmpty() ? "—" : row.getSupplier())
        );
        return bar;
    }

    private HBox infoChip(String label, String value) {
        Label lbl = new Label(label + ": ");
        lbl.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 11px; -fx-text-fill: " + TEXT_MUTED + ";");
        Label val = new Label(value);
        val.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + TEXT + ";");
        return new HBox(2, lbl, val);
    }

    private HBox buildQtyRow() {
        tfQty = new TextField();
        tfQty.setPromptText("0.00");
        styleInput(tfQty);
        tfQty.textProperty().addListener((o, ov, nv) -> {
            updatePreview();
            clearError(tfQty, errQty);
        });
        errQty = errorLabel("Enter a valid quantity.");

        VBox qtyBox = new VBox(5);
        HBox.setHgrow(qtyBox, Priority.ALWAYS);
        qtyBox.getChildren().addAll(fieldLabel("Quantity to Add *"), tfQty, errQty);

        tfUnit = new TextField(row.getUnit());
        styleInput(tfUnit);
        VBox unitBox = new VBox(5);
        HBox.setHgrow(unitBox, Priority.ALWAYS);
        unitBox.getChildren().addAll(fieldLabel("Unit"), tfUnit);

        HBox row2 = new HBox(12, qtyBox, unitBox);
        row2.setMaxWidth(Double.MAX_VALUE);
        return row2;
    }

    private HBox buildPreviewBox() {
        previewBox = new HBox();
        previewBox.setPadding(new Insets(10, 14, 10, 14));
        previewBox.setStyle("-fx-background-color: " + PREVIEW_BG + "; " +
                "-fx-border-color: " + PREVIEW_BR + "; -fx-border-width: 1; " +
                "-fx-background-radius: 7; -fx-border-radius: 7;");
        lblPreview = new Label();
        lblPreview.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 12px; -fx-text-fill: " + TEXT + ";");
        lblPreview.setWrapText(true);
        previewBox.getChildren().add(lblPreview);
        setVisible(previewBox, false);
        return previewBox;
    }

    private HBox buildDeliveryRow() {
        tfSupplier = new TextField(row.getSupplier());
        tfSupplier.setPromptText("e.g. San Rafael Meats");
        styleInput(tfSupplier);
        VBox supplierBox = new VBox(5);
        HBox.setHgrow(supplierBox, Priority.ALWAYS);
        supplierBox.getChildren().addAll(fieldLabel("Supplier"), tfSupplier);

        dpDateIn = new DatePicker(LocalDate.now());
        dpDateIn.setMaxWidth(Double.MAX_VALUE);
        styleInput(dpDateIn);
        VBox dateBox = new VBox(5);
        HBox.setHgrow(dateBox, Priority.ALWAYS);
        dateBox.getChildren().addAll(fieldLabel("Date In"), dpDateIn);

        HBox row2 = new HBox(12, supplierBox, dateBox);
        row2.setMaxWidth(Double.MAX_VALUE);
        return row2;
    }

    private VBox buildNotesRow() {
        taNotes = new TextArea();
        taNotes.setPromptText("Optional: batch number, remarks…");
        taNotes.setPrefRowCount(3);
        taNotes.setWrapText(true);
        taNotes.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 13px; " +
                "-fx-background-color: " + INPUT_BG + "; " +
                "-fx-border-color: " + BORDER + "; -fx-border-radius: 7; -fx-background-radius: 7; " +
                "-fx-text-fill: " + TEXT + "; -fx-control-inner-background: " + INPUT_BG + ";");
        VBox box = new VBox(5);
        box.getChildren().addAll(fieldLabel("Notes"), taNotes);
        return box;
    }

    private HBox buildFooter() {
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(14, 20, 14, 20));
        footer.setStyle("-fx-background-color: " + FOOTER_BG + "; " +
                "-fx-border-width: 1 0 0 0; -fx-border-color: " + BORDER + "; " +
                "-fx-background-radius: 0 0 12 12;");
        Button cancelBtn = buildBtn("Cancel", false);
        cancelBtn.setOnAction(e -> dialog.close());
        Button saveBtn = buildBtn("✔  Confirm Stock In", true);
        saveBtn.setOnAction(e -> handleSave());
        footer.getChildren().addAll(cancelBtn, saveBtn);
        return footer;
    }

    // ── Save Logic ────────────────────────────────────────────────────────────
    private void handleSave() {
        if (!validateInputs()) return;

        double qty = Double.parseDouble(tfQty.getText().trim());
        String dateStr = dpDateIn.getValue() != null
                ? dpDateIn.getValue().format(DateTimeFormatter.ISO_LOCAL_DATE)
                : LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

        try (Connection conn = DatabaseConnection.getConnection()) {

            // Log to stock_in_log table
            try {
                PreparedStatement ins = conn.prepareStatement(
                        "INSERT INTO stock_in_log (product_name, quantity, supplier_name, created_at, unit, notes) " +
                                "VALUES (?, ?, ?, ?, ?, ?)");
                ins.setString(1, row.getProductName());
                ins.setDouble(2, qty);
                ins.setString(3, tfSupplier.getText().trim());
                ins.setString(4, dateStr);
                ins.setString(5, tfUnit.getText().trim());
                ins.setString(6, taNotes.getText().trim());
                ins.executeUpdate();
            } catch (Exception logEx) {
                System.err.println("[StockInDialog] stock_in_log skipped: " + logEx.getMessage());
            }

            // FIX: This is the critical update — always runs
            PreparedStatement upd = conn.prepareStatement(
                    "UPDATE stocks SET quantity = quantity + ? WHERE id = ?");
            upd.setDouble(1, qty);
            upd.setInt(2, row.getId());
            upd.executeUpdate();

            setVisible(successBanner, true);
            clearForm();

            javafx.animation.Timeline hide = new javafx.animation.Timeline(
                    new javafx.animation.KeyFrame(Duration.seconds(2), ev -> {
                        dialog.close();
                        if (onSaved != null) onSaved.run();
                    })
            );
            hide.play();

        } catch (Exception ex) {
            ex.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Database Error");
            alert.setHeaderText(null);
            alert.setContentText("Failed to record Stock In: " + ex.getMessage());
            alert.showAndWait();
        }
    }

    private boolean validateInputs() {
        boolean valid = true;
        try {
            double qty = Double.parseDouble(tfQty.getText().trim());
            if (qty <= 0) throw new NumberFormatException();
            clearError(tfQty, errQty);
        } catch (NumberFormatException e) {
            showError(tfQty, errQty);
            valid = false;
        }
        return valid;
    }

    private void updatePreview() {
        String qtyText = tfQty.getText().trim();
        if (qtyText.isEmpty()) { setVisible(previewBox, false); return; }
        try {
            double qty  = Double.parseDouble(qtyText);
            String unit = tfUnit.getText().trim().isEmpty() ? row.getUnit() : tfUnit.getText().trim();
            lblPreview.setText(String.format(
                    "Adding %.2f %s of %s   —   New total: %.2f %s",
                    qty, unit, row.getProductName(), row.getQuantity() + qty, unit));
            setVisible(previewBox, true);
        } catch (NumberFormatException e) {
            setVisible(previewBox, false);
        }
    }

    private void clearForm() {
        tfQty.clear();
        tfUnit.setText(row.getUnit());
        tfSupplier.setText(row.getSupplier());
        dpDateIn.setValue(LocalDate.now());
        taNotes.clear();
        setVisible(previewBox, false);
        clearError(tfQty, errQty);
    }

    // ── UI Helpers ────────────────────────────────────────────────────────────
    private Label fieldLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 11px; " +
                "-fx-font-weight: bold; -fx-text-fill: " + TEXT + "; -fx-letter-spacing: 0.04em;");
        return lbl;
    }

    private Label sectionLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 10px; " +
                "-fx-font-weight: bold; -fx-text-fill: " + TEXT_MUTED + "; -fx-letter-spacing: 0.07em;");
        return lbl;
    }

    private Label errorLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 11px; -fx-text-fill: " + ERROR_FG + ";");
        lbl.setVisible(false);
        lbl.setManaged(false);
        return lbl;
    }

    private void styleInput(Control control) {
        control.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 13px; " +
                "-fx-background-color: " + INPUT_BG + "; " +
                "-fx-border-color: " + BORDER + "; -fx-border-radius: 7; -fx-background-radius: 7; " +
                "-fx-padding: 7 10; -fx-text-fill: " + TEXT + ";");
    }

    private void showError(Control control, Label errLabel) {
        control.setStyle(control.getStyle() + " -fx-border-color: " + ERROR_FG + ";");
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
        sep.setStyle("-fx-background-color: " + BORDER + ";");
        return sep;
    }

    private Button buildBtn(String text, boolean primary) {
        Button btn = new Button(text);
        String base, hover;
        if (primary) {
            base  = "-fx-font-family: Sans Serif; -fx-font-size: 13px; -fx-font-weight: bold; " +
                    "-fx-text-fill: white; -fx-background-color: " + ACCENT + "; " +
                    "-fx-background-radius: 7; -fx-cursor: hand; -fx-padding: 8 20;";
            hover = base.replace(ACCENT, ACCENT_HOVER);
        } else {
            base  = "-fx-font-family: Sans Serif; -fx-font-size: 13px; " +
                    "-fx-text-fill: " + TEXT_MUTED + "; -fx-background-color: transparent; " +
                    "-fx-border-color: " + BORDER + "; -fx-border-width: 1; " +
                    "-fx-background-radius: 7; -fx-border-radius: 7; -fx-cursor: hand; -fx-padding: 8 18;";
            hover = base.replace("transparent", "#DCF0DC");
        }
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e  -> btn.setStyle(base));
        return btn;
    }
}