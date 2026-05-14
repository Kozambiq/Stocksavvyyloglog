package com.savvy.stocksavvyyloglog.view;

import com.savvy.stocksavvyyloglog.util.DatabaseConnection;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Admin-only User Management screen.
 * Restyled to match AddStockDialog polish — UNDECORATED, entrance animation,
 * section labels, styled inputs, warm footer.
 */
public class UserManagementView {

    // ── Theme tokens — light ──────────────────────────────────────────────────
    private static final String L_NAV_BG      = "#C04A10";
    private static final String L_NAV_BORDER  = "#C8A96E";
    private static final String L_BG          = "#FDF5EC";
    private static final String L_CARD_BG     = "#FDFAF0";
    private static final String L_BORDER      = "#E8D8C0";
    private static final String L_ACCENT      = "#C04A10";
    private static final String L_ACCENT2     = "#C8A96E";
    private static final String L_TEXT        = "#2A1A08";
    private static final String L_TEXT_MUTED  = "#9E8050";
    private static final String L_INPUT_BG    = "#FDF8F4";
    private static final String L_FOOTER_BG   = "#FDF0E8";
    private static final String L_DANGER      = "#B52A2A";
    private static final String L_DANGER_HOV  = "#8A1A1A";
    private static final String L_SUCCESS     = "#2E7D32";
    private static final String L_SUCCESS_HOV = "#1B5E20";
    private static final String L_ERROR_FG    = "#D32F2F";

    // ── Theme tokens — dark ───────────────────────────────────────────────────
    private static final String D_NAV_BG      = "#1A0F07";
    private static final String D_NAV_BORDER  = "#7A5A30";
    private static final String D_BG          = "#18130E";
    private static final String D_CARD_BG     = "#231B12";
    private static final String D_BORDER      = "#3D2E1E";
    private static final String D_ACCENT      = "#E8622A";
    private static final String D_ACCENT2     = "#C8A96E";
    private static final String D_TEXT        = "#F0E0CC";
    private static final String D_TEXT_MUTED  = "#8A7055";
    private static final String D_INPUT_BG    = "#120A06";
    private static final String D_FOOTER_BG   = "#1A1008";

    // ── Mode ──────────────────────────────────────────────────────────────────
    private final boolean darkMode;

    // ── Token accessors ───────────────────────────────────────────────────────
    private String navBg()     { return darkMode ? D_NAV_BG     : L_NAV_BG;     }
    private String navBorder() { return darkMode ? D_NAV_BORDER : L_NAV_BORDER; }
    private String bg()        { return darkMode ? D_BG         : L_BG;         }
    private String cardBg()    { return darkMode ? D_CARD_BG    : L_CARD_BG;    }
    private String border()    { return darkMode ? D_BORDER     : L_BORDER;     }
    private String accent()    { return darkMode ? D_ACCENT     : L_ACCENT;     }
    private String accent2()   { return darkMode ? D_ACCENT2    : L_ACCENT2;    }
    private String text()      { return darkMode ? D_TEXT       : L_TEXT;       }
    private String textMuted() { return darkMode ? D_TEXT_MUTED : L_TEXT_MUTED; }
    private String inputBg()   { return darkMode ? D_INPUT_BG   : L_INPUT_BG;   }
    private String footerBg()  { return darkMode ? D_FOOTER_BG  : L_FOOTER_BG;  }

    // ── State ─────────────────────────────────────────────────────────────────
    private final Stage ownerStage;
    private final Stage dialog = new Stage();
    private final ObservableList<UserRow> userList = FXCollections.observableArrayList();
    private TableView<UserRow> table;
    private Label statusLabel;

    public UserManagementView(Stage ownerStage, boolean darkMode) {
        this.ownerStage = ownerStage;
        this.darkMode   = darkMode;
    }

    // ── Show ──────────────────────────────────────────────────────────────────
    public void show() {
        dialog.setTitle("User Management — Admin");
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(ownerStage);
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

        Scene scene = new Scene(root, 860, 580);
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
        loadUsers();
    }

    // ── Header ────────────────────────────────────────────────────────────────
    private HBox buildHeader() {
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 20, 14, 20));
        header.setStyle(
                "-fx-background-color: " + navBg() + "; " +
                        "-fx-border-width: 0 0 3 0; -fx-border-color: " + accent2() + "; " +
                        "-fx-background-radius: 12 12 0 0;"
        );

        Label icon = new Label("🛡");
        icon.setStyle("-fx-font-size: 18px;");

        VBox titleBox = new VBox(2);
        Label title = new Label("User Management");
        title.setStyle(
                "-fx-font-family: Sans Serif; -fx-font-size: 15px; " +
                        "-fx-font-weight: bold; -fx-text-fill: #FAF0E6;"
        );
        Label subtitle = new Label("Admin access only  •  Manage staff accounts");
        subtitle.setStyle(
                "-fx-font-family: Sans Serif; -fx-font-size: 11px; " +
                        "-fx-text-fill: rgba(250,240,230,0.55);"
        );
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label badge = new Label("ADMIN");
        badge.setStyle(
                "-fx-font-family: Sans Serif; -fx-font-size: 10px; -fx-font-weight: bold; " +
                        "-fx-text-fill: " + accent2() + "; -fx-border-color: " + accent2() + "; " +
                        "-fx-border-width: 1; -fx-border-radius: 4; -fx-background-radius: 4; " +
                        "-fx-padding: 3 10;"
        );

        Button closeBtn = new Button("✕");
        closeBtn.setStyle(
                "-fx-font-size: 13px; -fx-text-fill: #FAF0E6; " +
                        "-fx-background-color: rgba(255,255,255,0.15); " +
                        "-fx-border-color: rgba(255,255,255,0.3); -fx-border-width: 1; " +
                        "-fx-background-radius: 6; -fx-border-radius: 6; " +
                        "-fx-cursor: hand; -fx-padding: 4 10;"
        );
        closeBtn.setOnAction(e -> dialog.close());

        header.getChildren().addAll(icon, titleBox, spacer, badge, closeBtn);
        return header;
    }

    // ── Body ──────────────────────────────────────────────────────────────────
    private VBox buildBody() {
        VBox body = new VBox(16);
        body.setPadding(new Insets(20, 24, 16, 24));
        body.setStyle("-fx-background-color: " + bg() + ";");
        VBox.setVgrow(body, Priority.ALWAYS);

        // ── Toolbar ───────────────────────────────────────────────────────────
        HBox toolbar = new HBox(10);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        Label sectionLbl = sectionLabel("ALL ACCOUNTS");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refreshBtn = buildBtn("↻  Refresh", false, false);
        refreshBtn.setOnAction(e -> loadUsers());

        Button addBtn = buildBtn("+ Add User", true, false);
        addBtn.setOnAction(e -> showAddUserDialog());

        toolbar.getChildren().addAll(sectionLbl, spacer, refreshBtn, addBtn);

        // ── Status label ──────────────────────────────────────────────────────
        statusLabel = new Label();
        statusLabel.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 12px;");
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);

        // ── Separator ─────────────────────────────────────────────────────────
        Region sep = separator();

        // ── Table ─────────────────────────────────────────────────────────────
        table = buildTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        body.getChildren().addAll(toolbar, statusLabel, sep, table);
        return body;
    }

    // ── Table ─────────────────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private TableView<UserRow> buildTable() {
        TableView<UserRow> tv = new TableView<>();
        tv.setItems(userList);
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tv.setStyle(
                "-fx-background-color: " + cardBg() + "; " +
                        "-fx-border-color: " + border() + "; -fx-border-width: 1; " +
                        "-fx-border-radius: 8; -fx-background-radius: 8; " +
                        "-fx-table-cell-border-color: " + border() + ";"
        );
        tv.setFixedCellSize(46);
        tv.setPrefHeight(360);

        // ID
        TableColumn<UserRow, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setMaxWidth(60); idCol.setMinWidth(60);
        styleColumn(idCol);

        // Username
        TableColumn<UserRow, String> userCol = new TableColumn<>("Username");
        userCol.setCellValueFactory(new PropertyValueFactory<>("username"));
        styleColumn(userCol);

        // Role pill
        TableColumn<UserRow, String> roleCol = new TableColumn<>("Role");
        roleCol.setCellValueFactory(new PropertyValueFactory<>("role"));
        roleCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String role, boolean empty) {
                super.updateItem(role, empty);
                if (empty || role == null) { setGraphic(null); return; }
                Label pill = new Label(role);
                String color = "Admin".equals(role) ? accent() : L_SUCCESS;
                pill.setStyle(
                        "-fx-font-family: Sans Serif; -fx-font-size: 11px; -fx-font-weight: bold; " +
                                "-fx-text-fill: white; -fx-background-color: " + color + "; " +
                                "-fx-background-radius: 4; -fx-padding: 3 10;"
                );
                setGraphic(pill);
                setText(null);
                setAlignment(Pos.CENTER_LEFT);
            }
        });
        styleColumn(roleCol);

        // Created At
        TableColumn<UserRow, String> createdCol = new TableColumn<>("Created At");
        createdCol.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        styleColumn(createdCol);

        // Action — Delete
        TableColumn<UserRow, Void> actionCol = new TableColumn<>("Action");
        actionCol.setMaxWidth(110); actionCol.setMinWidth(110);
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button deleteBtn = buildDangerBtn("Delete");
            {
                deleteBtn.setOnAction(e -> {
                    UserRow row = getTableView().getItems().get(getIndex());
                    confirmAndDelete(row);
                });
            }
            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty) { setGraphic(null); return; }
                UserRow row = getTableView().getItems().get(getIndex());
                deleteBtn.setDisable("Admin".equals(row.getRole()));
                setGraphic(deleteBtn);
            }
        });
        styleColumn(actionCol);

        tv.getColumns().addAll(idCol, userCol, roleCol, createdCol, actionCol);

        Label empty = new Label("No users found.");
        empty.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 13px; -fx-text-fill: " + textMuted() + ";");
        tv.setPlaceholder(empty);

        return tv;
    }

    private <T> void styleColumn(TableColumn<UserRow, T> col) {
        col.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 12px; -fx-text-fill: " + text() + ";");
        col.setSortable(true);
    }

    // ── Footer ────────────────────────────────────────────────────────────────
    private HBox buildFooter() {
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(12, 24, 12, 24));
        footer.setStyle(
                "-fx-background-color: " + navBg() + "; " +
                        "-fx-border-width: 2 0 0 0; -fx-border-color: " + accent2() + "; " +
                        "-fx-background-radius: 0 0 12 12;"
        );
        Label note = new Label(
                "🛡  Only Admin accounts can access this screen.  " +
                        "Admins cannot be deleted from this panel."
        );
        note.setStyle(
                "-fx-font-family: Sans Serif; -fx-font-size: 11px; " +
                        "-fx-text-fill: rgba(250,240,230,0.6);"
        );
        footer.getChildren().add(note);
        return footer;
    }

    // ── Add User Dialog ───────────────────────────────────────────────────────
    private void showAddUserDialog() {
        Stage addDialog = new Stage();
        addDialog.setTitle("Add New User");
        addDialog.initModality(Modality.APPLICATION_MODAL);
        addDialog.initOwner(dialog);
        addDialog.initStyle(StageStyle.UNDECORATED);
        addDialog.setResizable(false);

        VBox root = new VBox();
        root.setStyle(
                "-fx-background-color: " + bg() + "; " +
                        "-fx-border-color: " + border() + "; " +
                        "-fx-border-width: 1; -fx-border-radius: 12; -fx-background-radius: 12;"
        );

        // Header
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 20, 14, 20));
        header.setStyle(
                "-fx-background-color: " + navBg() + "; " +
                        "-fx-border-width: 0 0 3 0; -fx-border-color: " + accent2() + "; " +
                        "-fx-background-radius: 12 12 0 0;"
        );
        Label dialogTitle = new Label("👤  Add New User");
        dialogTitle.setStyle(
                "-fx-font-family: Sans Serif; -fx-font-size: 15px; " +
                        "-fx-font-weight: bold; -fx-text-fill: #FAF0E6;"
        );
        Region hSpacer = new Region();
        HBox.setHgrow(hSpacer, Priority.ALWAYS);
        Button closeBtn = new Button("✕");
        closeBtn.setStyle(
                "-fx-font-size: 13px; -fx-text-fill: #FAF0E6; " +
                        "-fx-background-color: rgba(255,255,255,0.15); " +
                        "-fx-border-color: rgba(255,255,255,0.3); -fx-border-width: 1; " +
                        "-fx-background-radius: 6; -fx-border-radius: 6; " +
                        "-fx-cursor: hand; -fx-padding: 4 10;"
        );
        closeBtn.setOnAction(e -> addDialog.close());
        header.getChildren().addAll(dialogTitle, hSpacer, closeBtn);

        // Body
        VBox body = new VBox(14);
        body.setPadding(new Insets(20));
        body.setStyle("-fx-background-color: " + bg() + ";");

        body.getChildren().add(sectionLabel("ACCOUNT DETAILS"));

        // Username
        TextField userField = new TextField();
        userField.setPromptText("Enter username");
        styleInput(userField);
        VBox userBox = new VBox(5, fieldLabel("Username *"), userField);

        // Password
        PasswordField passField = new PasswordField();
        passField.setPromptText("Enter password");
        styleInput(passField);
        VBox passBox = new VBox(5, fieldLabel("Password *"), passField);

        HBox row1 = new HBox(12, userBox, passBox);
        HBox.setHgrow(userBox, Priority.ALWAYS);
        HBox.setHgrow(passBox, Priority.ALWAYS);

        body.getChildren().add(row1);
        body.getChildren().add(separator());
        body.getChildren().add(sectionLabel("ROLE"));

        // Role
        ComboBox<String> roleBox = new ComboBox<>();
        roleBox.getItems().addAll("Staff", "Admin");
        roleBox.setValue("Staff");
        roleBox.setMaxWidth(Double.MAX_VALUE);
        styleInput(roleBox);
        VBox roleVBox = new VBox(5, fieldLabel("Role"), roleBox);

        body.getChildren().add(roleVBox);

        // Error label
        Label formError = new Label();
        formError.setStyle(
                "-fx-font-family: Sans Serif; -fx-font-size: 12px; -fx-text-fill: " + L_ERROR_FG + ";"
        );
        formError.setVisible(false);
        formError.setManaged(false);
        body.getChildren().add(formError);

        // Footer
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(14, 20, 14, 20));
        footer.setStyle(
                "-fx-background-color: " + footerBg() + "; " +
                        "-fx-border-width: 1 0 0 0; -fx-border-color: " + border() + "; " +
                        "-fx-background-radius: 0 0 12 12;"
        );

        Button cancelBtn = buildBtn("Cancel", false, false);
        cancelBtn.setOnAction(e -> addDialog.close());

        Button saveBtn = buildBtn("Save User", true, false);
        saveBtn.setStyle(saveBtn.getStyle().replace(accent(), L_SUCCESS));
        String saveBase = "-fx-font-family: Sans Serif; -fx-font-size: 13px; -fx-font-weight: bold; " +
                "-fx-text-fill: white; -fx-background-color: " + L_SUCCESS + "; " +
                "-fx-background-radius: 7; -fx-cursor: hand; -fx-padding: 8 20;";
        String saveHover = saveBase.replace(L_SUCCESS, L_SUCCESS_HOV);
        saveBtn.setStyle(saveBase);
        saveBtn.setOnMouseEntered(e -> saveBtn.setStyle(saveHover));
        saveBtn.setOnMouseExited(e  -> saveBtn.setStyle(saveBase));

        saveBtn.setOnAction(e -> {
            String u = userField.getText().trim();
            String p = passField.getText().trim();
            String r = roleBox.getValue();

            if (u.isEmpty() || p.isEmpty()) {
                showFormError(formError, "Username and password are required.");
                return;
            }
            if (u.length() < 3) {
                showFormError(formError, "Username must be at least 3 characters.");
                return;
            }
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "INSERT INTO users (username, password, role) VALUES (?, ?, ?)")) {
                ps.setString(1, u);
                ps.setString(2, p);
                ps.setString(3, r);
                ps.executeUpdate();
                addDialog.close();
                loadUsers();
                showStatus("User '" + u + "' added successfully.", false);
            } catch (Exception ex) {
                showFormError(formError,
                        ex.getMessage().contains("Duplicate")
                                ? "Username '" + u + "' already exists."
                                : "Error: " + ex.getMessage());
            }
        });

        footer.getChildren().addAll(cancelBtn, saveBtn);
        root.getChildren().addAll(header, body, footer);

        Scene scene = new Scene(root, 420, 380);
        scene.setFill(Color.TRANSPARENT);
        addDialog.setScene(scene);
        addDialog.centerOnScreen();

        // Entrance animation
        root.setOpacity(0);
        root.setTranslateY(14);
        FadeTransition ft = new FadeTransition(Duration.millis(200), root);
        ft.setFromValue(0); ft.setToValue(1);
        TranslateTransition tt = new TranslateTransition(Duration.millis(200), root);
        tt.setFromY(14); tt.setToY(0);
        ft.play(); tt.play();

        addDialog.show();
    }

    // ── Confirm & delete ──────────────────────────────────────────────────────
    private void confirmAndDelete(UserRow row) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete User");
        confirm.setHeaderText("Delete \"" + row.getUsername() + "\"?");
        confirm.setContentText("This action cannot be undone. The account will be permanently removed.");
        confirm.initOwner(dialog);

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try (Connection conn = DatabaseConnection.getConnection();
                     PreparedStatement ps = conn.prepareStatement(
                             "DELETE FROM users WHERE id = ?")) {
                    ps.setInt(1, row.getId());
                    ps.executeUpdate();
                    loadUsers();
                    showStatus("User '" + row.getUsername() + "' deleted.", false);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    showStatus("Could not delete user: " + ex.getMessage(), true);
                }
            }
        });
    }

    // ── Load users ────────────────────────────────────────────────────────────
    private void loadUsers() {
        userList.clear();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id, username, role, created_at FROM users ORDER BY id ASC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                userList.add(new UserRow(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("role"),
                        rs.getString("created_at")
                ));
            }
            showStatus("Loaded " + userList.size() + " account(s).", false);
        } catch (Exception e) {
            e.printStackTrace();
            showStatus("Failed to load users: " + e.getMessage(), true);
        }
    }

    // ── Status banner ─────────────────────────────────────────────────────────
    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setStyle(
                "-fx-font-family: Sans Serif; -fx-font-size: 12px; " +
                        "-fx-text-fill: " + (isError ? L_ERROR_FG : L_SUCCESS) + ";"
        );
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
    }

    private void showFormError(Label lbl, String msg) {
        lbl.setText(msg);
        lbl.setVisible(true);
        lbl.setManaged(true);
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

    private void styleInput(Control control) {
        control.setStyle(
                "-fx-font-family: Sans Serif; -fx-font-size: 13px; " +
                        "-fx-background-color: " + inputBg() + "; " +
                        "-fx-border-color: " + border() + "; -fx-border-radius: 7; -fx-background-radius: 7; " +
                        "-fx-padding: 7 10; -fx-text-fill: " + text() + ";"
        );
    }

    private Region separator() {
        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setStyle("-fx-background-color: " + border() + ";");
        return sep;
    }

    private Button buildBtn(String text, boolean primary, boolean small) {
        Button btn = new Button(text);
        String base, hover;
        if (primary) {
            base  = "-fx-font-family: Sans Serif; -fx-font-size: 13px; -fx-font-weight: bold; " +
                    "-fx-text-fill: white; -fx-background-color: " + accent() + "; " +
                    "-fx-background-radius: 7; -fx-cursor: hand; -fx-padding: " + (small ? "5 14" : "8 20") + ";";
            hover = base.replace(accent(), darkMode ? "#FF7A3A" : "#A03A0A");
        } else {
            base  = "-fx-font-family: Sans Serif; -fx-font-size: " + (small ? "11" : "13") + "px; " +
                    "-fx-text-fill: " + textMuted() + "; -fx-background-color: transparent; " +
                    "-fx-border-color: " + border() + "; -fx-border-width: 1; " +
                    "-fx-background-radius: 7; -fx-border-radius: 7; -fx-cursor: hand; " +
                    "-fx-padding: " + (small ? "5 12" : "8 18") + ";";
            hover = base.replace("transparent", darkMode ? "#3A2010" : "#F3EBE7");
        }
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e  -> btn.setStyle(base));
        return btn;
    }

    private Button buildDangerBtn(String text) {
        Button btn = new Button(text);
        String base  = "-fx-font-family: Sans Serif; -fx-font-size: 11px; -fx-font-weight: bold; " +
                "-fx-text-fill: white; -fx-background-color: " + L_DANGER + "; " +
                "-fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 5 12;";
        String hover = base.replace(L_DANGER, L_DANGER_HOV);
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e  -> btn.setStyle(base));
        return btn;
    }

    // ── UserRow model ─────────────────────────────────────────────────────────
    public static class UserRow {
        private final int id;
        private final String username;
        private final String role;
        private final String createdAt;

        public UserRow(int id, String username, String role, String createdAt) {
            this.id        = id;
            this.username  = username;
            this.role      = role;
            this.createdAt = createdAt != null ? createdAt : "—";
        }

        public int    getId()        { return id; }
        public String getUsername()  { return username; }
        public String getRole()      { return role; }
        public String getCreatedAt() { return createdAt; }
    }
}