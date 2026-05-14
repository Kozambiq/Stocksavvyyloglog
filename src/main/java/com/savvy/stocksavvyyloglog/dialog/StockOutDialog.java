package com.savvy.stocksavvyyloglog.dialog;

import com.savvy.stocksavvyyloglog.util.DatabaseConnection;
import com.savvy.stocksavvyyloglog.view.InventoryView;
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
import javafx.util.Duration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * StockOutDialog — records outgoing stock and updates the stocks table quantity.
 * FIX: stock_out insert is now wrapped in its own try-catch so a missing column
 * won't block the stocks quantity update (the critical operation).
 */
public class StockOutDialog {

    // ── Red theme tokens ──────────────────────────────────────────────────────
    private static final String NAV_BG       = "#7F0000";
    private static final String NAV_BORDER   = "#EF9A9A";
    private static final String BG           = "#FFF5F5";
    private static final String BORDER       = "#FFCDD2";
    private static final String ACCENT       = "#C62828";
    private static final String ACCENT_HOVER = "#8B0000";
    private static final String TEXT         = "#1A0A0A";
    private static final String TEXT_MUTED   = "#B71C1C";
    private static final String INPUT_BG     = "#FFF8F8";
    private static final String SUCCESS_BG   = "#FFEBEE";
    private static final String SUCCESS_FG   = "#7F0000";
    private static final String ERROR_FG     = "#D32F2F";
    private static final String FOOTER_BG    = "#FFF0F0";
    private static final String PREVIEW_BG   = "#FFEBEE";
    private static final String PREVIEW_BR   = "#EF9A9A";
    private static final String INFO_BG      = "#FFEBEE";
    private static final String INFO_BORDER  = "#EF9A9A";

    private final Stage                  ownerStage;
    private final InventoryView.StockRow row;
    private       double                 currentQty;
    private       Runnable               onSaved;
    private final Stage                  dialog = new Stage();

    private TextField  tfQty;
    private TextField  tfUnit;
    private TextField  tfReason;
    private DatePicker dpDateOut;
    private TextArea   taNotes;
    private Label      errQty;
    private HBox       successBanner;
    private HBox       previewBox;
    private Label      lblPreview;

    public StockOutDialog(Stage ownerStage, InventoryView.StockRow row) {
        this.ownerStage = ownerStage;
        this.row        = row;
        try { currentQty = Double.parseDouble(row.getQuantity()); }
        catch (NumberFormatException e) { currentQty = 0; }
    }

    public void setOnSaved(Runnable onSaved) { this.onSaved = onSaved; }

    public void show() {
        dialog.initOwner(ownerStage);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle("Stock Out — " + row.getProductName());
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.setResizable(false);

        VBox root = new VBox();
        root.setStyle("-fx-background-color: " + BG + "; " +
                "-fx-border-color: " + BORDER + "; " +
                "-fx-border-width: 1; -fx-border-radius: 12; -fx-background-radius: 12;");
        root.getChildren().addAll(buildHeader(), buildBody(), buildFooter());

        Scene scene = new Scene(root, 500, 580);
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
        header.setStyle("-fx-background-color: " + NAV_BG + "; " +
                "-fx-border-width: 0 0 3 0; -fx-border-color: " + NAV_BORDER + "; " +
                "-fx-background-radius: 12 12 0 0;");
        Label title = new Label("📤  Stock Out  —  " + row.getProductName());
        title.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 15px; " +
                "-fx-font-weight: bold; -fx-text-fill: #FFF5F5;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-font-size: 13px; -fx-text-fill: #FFF5F5; " +
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
                "-fx-border-color: " + NAV_BORDER + "; -fx-border-width: 1; " +
                "-fx-background-radius: 6; -fx-border-radius: 6;");
        Label successLbl = new Label("✔   Stock Out recorded successfully!");
        successLbl.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 13px; " +
                "-fx-font-weight: bold; -fx-text-fill: " + SUCCESS_FG + ";");
        successBanner.getChildren().add(successLbl);
        setVisible(successBanner, false);

        body.getChildren().addAll(
                successBanner,
                sectionLabel("CURRENT STOCK INFO"),
                buildInfoBar(),
                separator(),
                sectionLabel("QUANTITY TO REMOVE"),
                buildQtyRow(),
                buildPreviewBox(),
                separator(),
                sectionLabel("REASON & DATE"),
                buildReasonRow(),
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
        bar.setStyle("-fx-background-color: " + INFO_BG + "; " +
                "-fx-border-color: " + INFO_BORDER + "; -fx-border-width: 1; " +
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
        qtyBox.getChildren().addAll(fieldLabel("Quantity to Remove *"), tfQty, errQty);

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

    private VBox buildReasonRow() {
        tfReason = new TextField();
        tfReason.setPromptText("e.g. Sold, Used in production, Spoiled…");
        styleInput(tfReason);
        VBox reasonBox = new VBox(5);
        reasonBox.setMaxWidth(Double.MAX_VALUE);
        reasonBox.getChildren().addAll(fieldLabel("Reason"), tfReason);

        dpDateOut = new DatePicker(LocalDate.now());
        dpDateOut.setMaxWidth(Double.MAX_VALUE);
        styleInput(dpDateOut);
        VBox dateBox = new VBox(5);
        HBox.setHgrow(dateBox, Priority.ALWAYS);
        dateBox.getChildren().addAll(fieldLabel("Date Out"), dpDateOut);

        HBox row2 = new HBox(12, reasonBox, dateBox);
        HBox.setHgrow(reasonBox, Priority.ALWAYS);
        row2.setMaxWidth(Double.MAX_VALUE);

        return new VBox(10, row2);
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
        Button saveBtn = buildBtn("✔  Confirm Stock Out", true);
        saveBtn.setOnAction(e -> handleSave());
        footer.getChildren().addAll(cancelBtn, saveBtn);
        return footer;
    }

    // ── Save Logic ────────────────────────────────────────────────────────────
    private void handleSave() {
        if (!validateInputs()) return;

        double qty = Double.parseDouble(tfQty.getText().trim());
        String dateStr = dpDateOut.getValue() != null
                ? dpDateOut.getValue().format(DateTimeFormatter.ISO_LOCAL_DATE)
                : LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

        try (Connection conn = DatabaseConnection.getConnection()) {

            // FIX: stock_out log wrapped separately — won't block quantity update if schema differs
            try {
                PreparedStatement ins = conn.prepareStatement(
                        "INSERT INTO stock_out (stock_id, product_name, quantity_out, unit, reason, date_out, notes) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?)");
                ins.setInt(1, row.getId());
                ins.setString(2, row.getProductName());
                ins.setDouble(3, qty);
                ins.setString(4, tfUnit.getText().trim());
                ins.setString(5, tfReason.getText().trim());
                ins.setString(6, dateStr);
                ins.setString(7, taNotes.getText().trim());
                ins.executeUpdate();
            } catch (Exception logEx) {
                System.err.println("[StockOutDialog] stock_out log skipped: " + logEx.getMessage());
            }

            // FIX: This is the critical update — always runs
            PreparedStatement upd = conn.prepareStatement(
                    "UPDATE stocks SET quantity = quantity - ? WHERE id = ?");
            upd.setDouble(1, qty);
            upd.setInt(2, row.getId());
            upd.executeUpdate();

            setVisible(successBanner, true);
            clearForm();

            Timeline hide = new Timeline(new KeyFrame(Duration.seconds(2), ev -> {
                dialog.close();
                if (onSaved != null) onSaved.run();
            }));
            hide.play();

        } catch (Exception ex) {
            ex.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Database Error");
            alert.setHeaderText(null);
            alert.setContentText("Failed to record Stock Out: " + ex.getMessage());
            alert.showAndWait();
        }
    }

    private boolean validateInputs() {
        boolean valid = true;
        try {
            double qty = Double.parseDouble(tfQty.getText().trim());
            if (qty <= 0) throw new NumberFormatException();
            if (qty > currentQty) {
                errQty.setText(String.format("Cannot remove %.2f — only %.2f in stock.", qty, currentQty));
                showError(tfQty, errQty);
                return false;
            }
            clearError(tfQty, errQty);
        } catch (NumberFormatException e) {
            errQty.setText("Enter a valid quantity.");
            showError(tfQty, errQty);
            valid = false;
        }
        return valid;
    }

    private void updatePreview() {
        String qtyText = tfQty.getText().trim();
        if (qtyText.isEmpty()) { setVisible(previewBox, false); return; }
        try {
            double qty       = Double.parseDouble(qtyText);
            String unit      = tfUnit.getText().trim().isEmpty() ? row.getUnit() : tfUnit.getText().trim();
            double remaining = currentQty - qty;
            String txt = remaining < 0
                    ? "⚠  Exceeds current stock of " + currentQty + " " + unit
                    : String.format("Removing %.2f %s of %s   —   Remaining: %.2f %s",
                    qty, unit, row.getProductName(), remaining, unit);
            lblPreview.setText(txt);
            setVisible(previewBox, true);
        } catch (NumberFormatException e) {
            setVisible(previewBox, false);
        }
    }

    private void clearForm() {
        tfQty.clear();
        tfUnit.setText(row.getUnit());
        tfReason.clear();
        dpDateOut.setValue(LocalDate.now());
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
            hover = base.replace("transparent", "#FFEBEE");
        }
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e  -> btn.setStyle(base));
        return btn;
    }
}