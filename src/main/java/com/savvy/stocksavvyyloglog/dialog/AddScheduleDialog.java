package com.savvy.stocksavvyyloglog.dialog;

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
import javafx.util.Duration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;

public class AddScheduleDialog {

    // ── Theme tokens ──────────────────────────────────────────────────────────
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

    private String navBg()     { return L_NAV_BG;     }
    private String navBorder() { return L_NAV_BORDER; }
    private String bg()        { return L_BG;         }
    private String border()    { return L_BORDER;     }
    private String accent()    { return L_ACCENT;     }
    private String text()      { return L_TEXT;       }
    private String textMuted() { return L_TEXT_MUTED; }
    private String inputBg()   { return L_INPUT_BG;   }
    private String successBg() { return L_SUCCESS_BG; }
    private String successFg() { return L_SUCCESS_FG; }
    private String errorFg()   { return L_ERROR_FG;   }

    // ── Fields ────────────────────────────────────────────────────────────────
    private TextField        tfTitle;
    private ComboBox<String> cbEventType;
    private DatePicker       dpEventDate;
    private TextArea         taDescription;

    private Label errTitle;
    private Label errDate;

    private HBox  successBanner;
    private HBox  previewBox;
    private Label lblPreview;

    private final Stage  dialog    = new Stage();
    private final Stage  owner;
    private final String createdBy; // logged-in username

    // ── Constructor ───────────────────────────────────────────────────────────
    public AddScheduleDialog(Stage owner, String currentUser) {
        this.owner     = owner;
        this.createdBy = currentUser;
    }

    // ── Show ──────────────────────────────────────────────────────────────────
    public void show() {
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Add Schedule — StockSavy");
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

        Scene scene = new Scene(root, 500, 540);
        scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene);
        dialog.centerOnScreen();

        // Entrance animation
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

        Label title = new Label("\uD83D\uDCC5  Add Schedule");
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

        // ── Success banner ────────────────────────────────────────────────────
        successBanner = new HBox(8);
        successBanner.setAlignment(Pos.CENTER_LEFT);
        successBanner.setPadding(new Insets(10, 14, 10, 14));
        successBanner.setStyle(
                "-fx-background-color: " + successBg() + "; " +
                        "-fx-border-color: #A5D6A7; -fx-border-width: 1; " +
                        "-fx-background-radius: 6; -fx-border-radius: 6;"
        );
        Label successLbl = new Label("\u2714   Schedule added successfully!");
        successLbl.setStyle(
                "-fx-font-family: Sans Serif; -fx-font-size: 13px; " +
                        "-fx-font-weight: bold; -fx-text-fill: " + successFg() + ";"
        );
        successBanner.getChildren().add(successLbl);
        setVisible(successBanner, false);

        body.getChildren().addAll(
                successBanner,
                sectionLabel("EVENT DETAILS"),
                buildTitleRow(),
                buildTypeAndDateRow(),
                buildPreviewBox(),
                separator(),
                sectionLabel("DESCRIPTION"),
                buildDescriptionRow()
        );

        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.setStyle(
                "-fx-background-color: " + bg() + "; " +
                        "-fx-background: " + bg() + "; -fx-border-width: 0;"
        );
        return scroll;
    }

    // ── Title Row ─────────────────────────────────────────────────────────────
    private VBox buildTitleRow() {
        tfTitle = new TextField();
        tfTitle.setPromptText("e.g. Deliver Vigan to Aling Nena");
        styleInput(tfTitle);
        tfTitle.textProperty().addListener((o, ov, nv) -> {
            updatePreview();
            setVisible(errTitle, false);
        });

        errTitle = errorLabel("Event title is required.");

        VBox box = new VBox(5, fieldLabel("Event Title *"), tfTitle, errTitle);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    // ── Type & Date Row ───────────────────────────────────────────────────────
    private HBox buildTypeAndDateRow() {
        // Event Type
        cbEventType = new ComboBox<>();
        cbEventType.getItems().addAll("Delivery", "Production", "Holiday", "Other");
        cbEventType.setValue("Delivery");
        cbEventType.setMaxWidth(Double.MAX_VALUE);
        styleInput(cbEventType);
        cbEventType.valueProperty().addListener((o, ov, nv) -> updatePreview());

        VBox typeBox = new VBox(5, fieldLabel("Event Type *"), cbEventType);
        HBox.setHgrow(typeBox, Priority.ALWAYS);

        // Event Date
        dpEventDate = new DatePicker(LocalDate.now());
        dpEventDate.setMaxWidth(Double.MAX_VALUE);
        styleInput(dpEventDate);
        dpEventDate.valueProperty().addListener((o, ov, nv) -> {
            updatePreview();
            setVisible(errDate, false);
        });

        errDate = errorLabel("Please select a valid date.");

        VBox dateBox = new VBox(5, fieldLabel("Event Date *"), dpEventDate, errDate);
        HBox.setHgrow(dateBox, Priority.ALWAYS);

        HBox row = new HBox(12, typeBox, dateBox);
        row.setMaxWidth(Double.MAX_VALUE);
        return row;
    }

    // ── Preview Box ───────────────────────────────────────────────────────────
    private HBox buildPreviewBox() {
        previewBox = new HBox();
        previewBox.setPadding(new Insets(10, 14, 10, 14));
        previewBox.setStyle(
                "-fx-background-color: #FEF0E6; " +
                        "-fx-border-color: #E8CFC4; -fx-border-width: 1; " +
                        "-fx-background-radius: 7; -fx-border-radius: 7;"
        );
        lblPreview = new Label();
        lblPreview.setStyle(
                "-fx-font-family: Sans Serif; -fx-font-size: 12px; -fx-text-fill: " + text() + ";"
        );
        lblPreview.setWrapText(true);
        previewBox.getChildren().add(lblPreview);
        setVisible(previewBox, false);
        return previewBox;
    }

    // ── Description Row ───────────────────────────────────────────────────────
    private VBox buildDescriptionRow() {
        taDescription = new TextArea();
        taDescription.setPromptText("Optional: additional notes or details\u2026");
        taDescription.setPrefRowCount(3);
        taDescription.setWrapText(true);
        taDescription.setStyle(
                "-fx-font-family: Sans Serif; -fx-font-size: 13px; " +
                        "-fx-background-color: " + inputBg() + "; " +
                        "-fx-border-color: " + border() + "; -fx-border-radius: 7; -fx-background-radius: 7; " +
                        "-fx-text-fill: " + text() + "; " +
                        "-fx-control-inner-background: " + inputBg() + ";"
        );

        VBox box = new VBox(5, fieldLabel("Description"), taDescription);
        return box;
    }

    // ── Footer ────────────────────────────────────────────────────────────────
    private HBox buildFooter() {
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(14, 20, 14, 20));
        footer.setStyle(
                "-fx-background-color: #FDF0E8; " +
                        "-fx-border-width: 1 0 0 0; -fx-border-color: " + border() + "; " +
                        "-fx-background-radius: 0 0 12 12;"
        );

        Button clearBtn = buildBtn("\u21BA   Clear", false);
        clearBtn.setOnAction(e -> clearForm());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button cancelBtn = buildBtn("Cancel", false);
        cancelBtn.setOnAction(e -> dialog.close());

        Button saveBtn = buildBtn("+ Add Schedule", true);
        saveBtn.setOnAction(e -> handleSave(saveBtn));

        footer.getChildren().addAll(clearBtn, spacer, cancelBtn, saveBtn);
        return footer;
    }

    // ── Save Logic ────────────────────────────────────────────────────────────
    private void handleSave(Button saveBtn) {
        if (!validateInputs()) return;

        String title       = tfTitle.getText().trim();
        String eventType   = cbEventType.getValue();
        LocalDate eventDate = dpEventDate.getValue();
        String description = taDescription.getText().trim();

        saveBtn.setDisable(true);
        saveBtn.setText("Saving\u2026");

        try {
            // Look up created_by user ID from username
            int userId = getUserId(createdBy);

            String sql = "INSERT INTO schedule (title, description, event_date, event_type, created_by) " +
                    "VALUES (?, ?, ?, ?, ?)";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, title);
                ps.setString(2, description.isEmpty() ? null : description);
                ps.setDate(3, java.sql.Date.valueOf(eventDate));
                ps.setString(4, eventType);
                ps.setInt(5, userId);
                ps.executeUpdate();
            }

            setVisible(successBanner, true);
            clearForm();
            Timeline hide = new Timeline(new KeyFrame(Duration.seconds(2),
                    e -> setVisible(successBanner, false)));
            hide.play();

        } catch (Exception ex) {
            ex.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Database Error");
            alert.setHeaderText(null);
            alert.setContentText("Failed to save schedule. Check your database connection.\n" + ex.getMessage());
            alert.showAndWait();
        } finally {
            saveBtn.setDisable(false);
            saveBtn.setText("+ Add Schedule");
        }
    }

    // ── Get user ID from username ─────────────────────────────────────────────
    private int getUserId(String username) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id FROM users WHERE username = ?")) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("id");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 1; // fallback to admin
    }

    // ── Validate ──────────────────────────────────────────────────────────────
    private boolean validateInputs() {
        boolean valid = true;

        if (tfTitle.getText().trim().isEmpty()) {
            setVisible(errTitle, true);
            tfTitle.setStyle(tfTitle.getStyle() + " -fx-border-color: " + errorFg() + ";");
            valid = false;
        }

        if (dpEventDate.getValue() == null) {
            setVisible(errDate, true);
            valid = false;
        }

        return valid;
    }

    // ── Live Preview ──────────────────────────────────────────────────────────
    private void updatePreview() {
        String title = tfTitle.getText().trim();
        LocalDate date = dpEventDate.getValue();
        String type = cbEventType.getValue();

        if (title.isEmpty() || date == null) {
            setVisible(previewBox, false);
            return;
        }

        String typeIcon = getTypeIcon(type);
        lblPreview.setText(typeIcon + "  " + title + "  \u2014  " +
                date.getMonth().getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH) +
                " " + date.getDayOfMonth() + ", " + date.getYear());
        setVisible(previewBox, true);
    }

    private String getTypeIcon(String type) {
        if (type == null) return "\uD83D\uDCC5";
        switch (type) {
            case "Delivery":   return "\uD83D\uDE9A";
            case "Production": return "\uD83C\uDFED";
            case "Holiday":    return "\uD83C\uDF89";
            default:           return "\uD83D\uDCC5";
        }
    }

    // ── Clear ─────────────────────────────────────────────────────────────────
    private void clearForm() {
        tfTitle.clear();
        cbEventType.setValue("Delivery");
        dpEventDate.setValue(LocalDate.now());
        taDescription.clear();
        setVisible(previewBox, false);
        setVisible(errTitle, false);
        setVisible(errDate, false);
        styleInput(tfTitle);
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
        setVisible(lbl, false);
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
        String base, hover;
        if (primary) {
            base  = "-fx-font-family: Sans Serif; -fx-font-size: 13px; -fx-font-weight: bold; " +
                    "-fx-text-fill: white; -fx-background-color: " + accent() + "; " +
                    "-fx-background-radius: 7; -fx-cursor: hand; -fx-padding: 8 20;";
            hover = base.replace(accent(), "#A03A0A");
        } else {
            base  = "-fx-font-family: Sans Serif; -fx-font-size: 13px; " +
                    "-fx-text-fill: " + textMuted() + "; -fx-background-color: transparent; " +
                    "-fx-border-color: " + border() + "; -fx-border-width: 1; " +
                    "-fx-background-radius: 7; -fx-border-radius: 7; -fx-cursor: hand; -fx-padding: 8 18;";
            hover = base.replace("transparent", "#F3EBE7");
        }
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e  -> btn.setStyle(base));
        return btn;
    }
}