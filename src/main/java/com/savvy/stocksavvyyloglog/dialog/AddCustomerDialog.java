package com.savvy.stocksavvyyloglog.dialog;

import com.savvy.stocksavvyyloglog.util.DatabaseConnection;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AddCustomerDialog {

    // ── Theme tokens ───────────────────────────────────────────────────────────
    private static final String ACCENT        = "#C04A10";
    private static final String ACCENT_DARK   = "#A03A0A";
    private static final String CREAM_BG      = "#FDF5EC";
    private static final String CARD_BG       = "#FFFFFF";
    private static final String BORDER        = "#E8D8C0";
    private static final String TEXT          = "#2A1A08";
    private static final String TEXT_MUTED    = "#9E8050";
    private static final String FIELD_BG      = "#FDF8F4";
    private static final String SUCCESS_COLOR = "#4A7C4E";
    private static final String ERROR_COLOR   = "#D32F2F";

    private Stage owner;

    public AddCustomerDialog(Stage owner) {
        this.owner = owner;
    }

    public void show() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(owner);
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.setResizable(false);

        // ── Outer wrapper ──────────────────────────────────────────────────────
        StackPane wrapper = new StackPane();
        wrapper.setStyle("-fx-background-color: " + CREAM_BG + ";");
        wrapper.setPrefSize(480, 520);

        // ── Card ───────────────────────────────────────────────────────────────
        VBox card = new VBox(0);
        card.setMaxWidth(440);
        card.setStyle(
                "-fx-background-color: " + CARD_BG + "; " +
                        "-fx-background-radius: 16; " +
                        "-fx-border-color: " + BORDER + "; " +
                        "-fx-border-width: 1; -fx-border-radius: 16;"
        );
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.web("#2A1A08", 0.12));
        shadow.setRadius(30);
        shadow.setOffsetY(6);
        card.setEffect(shadow);

        // ── Header ─────────────────────────────────────────────────────────────
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 24, 20, 24));
        header.setStyle(
                "-fx-background-color: " + ACCENT + "; " +
                        "-fx-background-radius: 14 14 0 0;"
        );

        Text titleText = new Text("\uD83D\uDC64  Add New Customer");
        titleText.setFont(Font.font("Sans Serif", FontWeight.BOLD, 16));
        titleText.setFill(Color.web("#FAF0E6"));

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        Button closeBtn = new Button("\u2715");
        closeBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #FAF0E6; " +
                        "-fx-font-size: 14px; -fx-cursor: hand; -fx-border-width: 0; -fx-padding: 0 4;"
        );
        closeBtn.setOnAction(e -> dialog.close());

        header.getChildren().addAll(titleText, headerSpacer, closeBtn);

        // ── Form body ──────────────────────────────────────────────────────────
        VBox body = new VBox(16);
        body.setPadding(new Insets(28, 28, 24, 28));

        // Name
        VBox nameBox = buildField("FULL NAME *", "e.g. Maria Santos");
        TextField nameField = (TextField) ((VBox) nameBox).getChildren().get(1);

        // Phone
        VBox phoneBox = buildField("PHONE NUMBER", "e.g. 09171234567");
        TextField phoneField = (TextField) ((VBox) phoneBox).getChildren().get(1);

        // Address (TextArea)
        Label addressLabel = fieldLabel("ADDRESS");
        TextArea addressArea = new TextArea();
        addressArea.setPromptText("e.g. 123 Rizal St, Manila");
        addressArea.setPrefRowCount(3);
        addressArea.setWrapText(true);
        styleTextArea(addressArea);

        VBox addressBox = new VBox(6, addressLabel, addressArea);

        // ── Status label ───────────────────────────────────────────────────────
        Label statusLabel = new Label();
        statusLabel.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 12px;");
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);

        // ── Buttons ────────────────────────────────────────────────────────────
        Button saveBtn   = new Button("Save Customer");
        Button cancelBtn = new Button("Cancel");

        saveBtn.setMaxWidth(Double.MAX_VALUE);
        cancelBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(saveBtn,   Priority.ALWAYS);
        HBox.setHgrow(cancelBtn, Priority.ALWAYS);

        String saveBtnBase  = buildBtnStyle(ACCENT,        "#FAF0E6", true);
        String saveBtnHover = buildBtnStyle(ACCENT_DARK,   "#FAF0E6", true);
        String cancelBase   = buildBtnStyle("transparent", TEXT_MUTED, false);
        String cancelHover  = buildBtnStyle("#F0E8DC",     TEXT,      false);

        saveBtn.setStyle(saveBtnBase);
        saveBtn.setOnMouseEntered(e -> saveBtn.setStyle(saveBtnHover));
        saveBtn.setOnMouseExited(e  -> saveBtn.setStyle(saveBtnBase));

        cancelBtn.setStyle(cancelBase);
        cancelBtn.setOnMouseEntered(e -> cancelBtn.setStyle(cancelHover));
        cancelBtn.setOnMouseExited(e  -> cancelBtn.setStyle(cancelBase));
        cancelBtn.setOnAction(e -> dialog.close());

        HBox btnRow = new HBox(10, cancelBtn, saveBtn);
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        // ── Save action ────────────────────────────────────────────────────────
        saveBtn.setOnAction(e -> {
            String name    = nameField.getText().trim();
            String phone   = phoneField.getText().trim();
            String address = addressArea.getText().trim();

            // Validate
            if (name.isEmpty()) {
                showStatus(statusLabel, "Full name is required.", false);
                nameField.requestFocus();
                return;
            }
            if (!phone.isEmpty() && !phone.matches("^[0-9+\\-\\s()]{7,20}$")) {
                showStatus(statusLabel, "Please enter a valid phone number.", false);
                phoneField.requestFocus();
                return;
            }

            // Check duplicate name
            if (customerExists(name)) {
                showStatus(statusLabel, "A customer with this name already exists.", false);
                return;
            }

            // Insert into DB
            if (insertCustomer(name, phone.isEmpty() ? null : phone, address.isEmpty() ? null : address)) {
                showStatus(statusLabel, "\u2714 Customer \"" + name + "\" added successfully!", true);
                saveBtn.setDisable(true);
                // Close after short delay
                javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                        javafx.util.Duration.seconds(1.4));
                pause.setOnFinished(ev -> dialog.close());
                pause.play();
            } else {
                showStatus(statusLabel, "Failed to save customer. Please try again.", false);
            }
        });

        body.getChildren().addAll(nameBox, phoneBox, addressBox, statusLabel, btnRow);

        card.getChildren().addAll(header, body);
        wrapper.getChildren().add(card);
        StackPane.setAlignment(card, Pos.CENTER);
        StackPane.setMargin(card, new Insets(20));

        Scene scene = new Scene(wrapper);
        dialog.setScene(scene);
        dialog.centerOnScreen();
        dialog.show();
    }

    // ── DB operations ──────────────────────────────────────────────────────────
    private boolean customerExists(String name) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id FROM customers WHERE LOWER(name) = LOWER(?)")) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean insertCustomer(String name, String phone, String address) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO customers (name, phone, address) VALUES (?, ?, ?)")) {
            ps.setString(1, name);
            ps.setString(2, phone);
            ps.setString(3, address);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // ── UI helpers ─────────────────────────────────────────────────────────────
    private VBox buildField(String labelText, String prompt) {
        Label lbl = fieldLabel(labelText);
        TextField field = new TextField();
        field.setPromptText(prompt);
        styleField(field);
        return new VBox(6, lbl, field);
    }

    private Label fieldLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle(
                "-fx-font-family: Sans Serif; -fx-font-size: 10px; " +
                        "-fx-font-weight: bold; -fx-text-fill: " + TEXT_MUTED + ";"
        );
        return lbl;
    }

    private void styleField(TextField field) {
        String base = "-fx-font-family: Sans Serif; -fx-font-size: 13px; " +
                "-fx-background-color: " + FIELD_BG + "; -fx-text-fill: " + TEXT + "; " +
                "-fx-prompt-text-fill: #C8BAA8; " +
                "-fx-border-color: " + BORDER + "; -fx-border-radius: 8; " +
                "-fx-background-radius: 8; -fx-padding: 10 12;";
        field.setStyle(base);
        field.setOnMouseEntered(e -> field.setStyle(base.replace(BORDER, "#C8A96E")));
        field.setOnMouseExited(e  -> field.setStyle(base));
        field.focusedProperty().addListener((o, ov, nv) ->
                field.setStyle(nv ? base.replace(BORDER, ACCENT) : base));
    }

    private void styleTextArea(TextArea area) {
        String base = "-fx-font-family: Sans Serif; -fx-font-size: 13px; " +
                "-fx-background-color: " + FIELD_BG + "; -fx-text-fill: " + TEXT + "; " +
                "-fx-prompt-text-fill: #C8BAA8; " +
                "-fx-border-color: " + BORDER + "; -fx-border-radius: 8; " +
                "-fx-background-radius: 8; -fx-padding: 8 12;";
        area.setStyle(base);
        area.focusedProperty().addListener((o, ov, nv) ->
                area.setStyle(nv ? base.replace(BORDER, ACCENT) : base));
    }

    private String buildBtnStyle(String bg, String fg, boolean bold) {
        return "-fx-font-family: Sans Serif; -fx-font-size: 13px; " +
                (bold ? "-fx-font-weight: bold; " : "") +
                "-fx-text-fill: " + fg + "; -fx-background-color: " + bg + "; " +
                "-fx-background-radius: 9; -fx-cursor: hand; -fx-padding: 11 0; " +
                "-fx-border-color: " + BORDER + "; -fx-border-width: 1; -fx-border-radius: 9;";
    }

    private void showStatus(Label label, String message, boolean success) {
        label.setText(message);
        label.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 12px; -fx-text-fill: "
                + (success ? SUCCESS_COLOR : ERROR_COLOR) + ";");
        label.setVisible(true);
        label.setManaged(true);
    }
}