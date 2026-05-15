package com.savvy.stocksavvyyloglog.view;

import com.savvy.stocksavvyyloglog.util.DatabaseConnection;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.sql.*;

/**
 * StockHistoryView
 * ----------------
 * A full-page audit log that merges `stock_in_log` and `stock_out` records into
 * a single chronological table, newest first.
 *
 * Columns: Date | Type (IN / OUT) | Product | Qty | Unit | Reason/Supplier | Notes
 *
 * Usage:
 *   StockHistoryView view = new StockHistoryView(stage, dashboardRoot);
 *   view.show();
 */
public class StockHistoryView {

    // ── Theme tokens ──────────────────────────────────────────────────────────
    private static final String L_BG         = "#FDF5EC";
    private static final String L_CARD_BG    = "#FDFAF0";
    private static final String L_BORDER     = "#E8D8C0";
    private static final String L_ACCENT     = "#C04A10";
    private static final String L_ACCENT2    = "#C8A96E";
    private static final String L_TEXT       = "#2A1A08";
    private static final String L_TEXT_MUTED = "#9E8050";
    private static final String IN_BG        = "#E8F5E9";
    private static final String OUT_BG       = "#FFF3E0";
    private static final String IN_COLOR     = "#2E7D32";
    private static final String OUT_COLOR    = "#B85C00";

    private final Stage stage;
    private final BorderPane dashboardRoot;

    private TableView<HistoryRow> table;
    private ObservableList<HistoryRow> allRows;
    private FilteredList<HistoryRow>  filteredRows;
    private TextField searchField;
    private Label statusLabel;

    // ── Model ─────────────────────────────────────────────────────────────────
    public static class HistoryRow {
        private final SimpleStringProperty date;
        private final SimpleStringProperty type;
        private final SimpleStringProperty productName;
        private final SimpleStringProperty quantity;
        private final SimpleStringProperty unit;
        private final SimpleStringProperty reference;   // supplier (IN) or reason (OUT)
        private final SimpleStringProperty notes;
        private final boolean isIn;

        public HistoryRow(String date, String type, String productName,
                          double quantity, String unit, String reference,
                          String notes, boolean isIn) {
            this.date        = new SimpleStringProperty(date != null ? date : "");
            this.type        = new SimpleStringProperty(type);
            this.productName = new SimpleStringProperty(productName);
            this.quantity    = new SimpleStringProperty(String.format("%.2f", quantity));
            this.unit        = new SimpleStringProperty(unit != null ? unit : "");
            this.reference   = new SimpleStringProperty(reference != null ? reference : "");
            this.notes       = new SimpleStringProperty(notes != null ? notes : "");
            this.isIn        = isIn;
        }

        public String getDate()        { return date.get(); }
        public String getType()        { return type.get(); }
        public String getProductName() { return productName.get(); }
        public String getQuantity()    { return quantity.get(); }
        public String getUnit()        { return unit.get(); }
        public String getReference()   { return reference.get(); }
        public String getNotes()       { return notes.get(); }
        public boolean isIn()          { return isIn; }
    }

    // ── Constructor ───────────────────────────────────────────────────────────
    public StockHistoryView(Stage stage, BorderPane dashboardRoot) {
        this.stage         = stage;
        this.dashboardRoot = dashboardRoot;
    }

    // ── Entry point ───────────────────────────────────────────────────────────
    public void show() {
        dashboardRoot.setCenter(buildPane());
    }

    // ── Main pane ─────────────────────────────────────────────────────────────
    private ScrollPane buildPane() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(28, 36, 28, 36));
        content.setStyle("-fx-background-color: " + L_BG + ";");

        content.getChildren().addAll(
                buildHeader(),
                buildToolbar(),
                buildTable()
        );

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: " + L_BG + "; -fx-background: " + L_BG + "; -fx-border-width: 0;");
        return scroll;
    }

    // ── Header ────────────────────────────────────────────────────────────────
    private VBox buildHeader() {
        // Back button
        Button backBtn = new Button("← Back to Inventory");
        backBtn.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 12px; " +
                "-fx-text-fill: " + L_ACCENT + "; -fx-background-color: transparent; " +
                "-fx-cursor: hand; -fx-padding: 0;");
        backBtn.setOnMouseEntered(e -> backBtn.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 12px; " +
                "-fx-text-fill: #E8530A; -fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 0;"));
        backBtn.setOnMouseExited(e  -> backBtn.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 12px; " +
                "-fx-text-fill: " + L_ACCENT + "; -fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 0;"));
        backBtn.setOnAction(e -> new InventoryView(stage, dashboardRoot).show());

        Label title = new Label("📋  Stock History");
        title.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 28px; " +
                "-fx-font-weight: bold; -fx-text-fill: " + L_ACCENT + ";");

        Label subtitle = new Label("Full audit log of all stock-in and stock-out events");
        subtitle.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 12px; -fx-text-fill: " + L_ACCENT2 + ";");

        VBox header = new VBox(6, backBtn, title, subtitle);
        header.setStyle("-fx-border-width: 0 0 0 4; -fx-border-color: " + L_ACCENT2 + "; -fx-padding: 0 0 0 12;");
        return header;
    }

    // ── Toolbar ───────────────────────────────────────────────────────────────
    private HBox buildToolbar() {
        HBox toolbar = new HBox(12);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(14, 18, 14, 18));
        toolbar.setStyle("-fx-background-color: " + L_CARD_BG + "; " +
                "-fx-border-color: " + L_BORDER + "; -fx-border-width: 1; " +
                "-fx-background-radius: 8; -fx-border-radius: 8;");

        searchField = new TextField();
        searchField.setPromptText("🔍  Search by product, type, supplier, reason…");
        searchField.setPrefWidth(310);
        searchField.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 12px; " +
                "-fx-background-color: " + L_BG + "; -fx-border-color: " + L_BORDER + "; " +
                "-fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6; " +
                "-fx-padding: 8 12; -fx-text-fill: " + L_TEXT + ";");
        searchField.textProperty().addListener((obs, o, n) -> applyFilter(n));

        // Type filter
        ComboBox<String> typeFilter = new ComboBox<>();
        typeFilter.getItems().addAll("All", "IN", "OUT");
        typeFilter.setValue("All");
        typeFilter.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 12px;");
        typeFilter.setOnAction(e -> applyFilterWithType(searchField.getText(), typeFilter.getValue()));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        statusLabel = new Label();
        statusLabel.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 11px; -fx-text-fill: " + L_TEXT_MUTED + ";");

        Button refreshBtn = actionBtn("🔄  Refresh", "#5A5A8A", "#4A4A7A");
        refreshBtn.setOnAction(e -> {
            loadData();
            applyFilterWithType(searchField.getText(), typeFilter.getValue());
        });

        toolbar.getChildren().addAll(searchField, typeFilter, spacer, statusLabel, refreshBtn);
        return toolbar;
    }

    // ── Table ─────────────────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private VBox buildTable() {
        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(520);
        table.setStyle("-fx-background-color: " + L_CARD_BG + "; " +
                "-fx-border-color: " + L_BORDER + "; -fx-border-width: 1; " +
                "-fx-background-radius: 8; -fx-border-radius: 8; " +
                "-fx-font-family: Sans Serif; -fx-font-size: 12px;");

        TableColumn<HistoryRow, String> colDate    = col("Date",             "date",        110);
        TableColumn<HistoryRow, String> colType    = col("Type",             "type",        70);
        TableColumn<HistoryRow, String> colProduct = col("Product",          "productName", 180);
        TableColumn<HistoryRow, String> colQty     = col("Quantity",         "quantity",    90);
        TableColumn<HistoryRow, String> colUnit    = col("Unit",             "unit",        70);
        TableColumn<HistoryRow, String> colRef     = col("Supplier / Reason","reference",   150);
        TableColumn<HistoryRow, String> colNotes   = col("Notes",            "notes",       180);

        // Colour-code IN vs OUT rows
        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(HistoryRow item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setStyle(""); return; }
                setStyle("-fx-background-color: " + (item.isIn() ? IN_BG : OUT_BG) + ";");
            }
        });

        table.getColumns().addAll(colDate, colType, colProduct, colQty, colUnit, colRef, colNotes);

        allRows      = FXCollections.observableArrayList();
        filteredRows = new FilteredList<>(allRows, p -> true);
        table.setItems(filteredRows);
        loadData();

        // Legend
        HBox legend = new HBox(16);
        legend.setAlignment(Pos.CENTER_LEFT);
        legend.setPadding(new Insets(8, 0, 0, 4));
        Label inLbl  = new Label("🟩  Stock In");
        inLbl.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 11px; -fx-text-fill: " + IN_COLOR + ";");
        Label outLbl = new Label("🟧  Stock Out");
        outLbl.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 11px; -fx-text-fill: " + OUT_COLOR + ";");
        legend.getChildren().addAll(inLbl, outLbl);

        return new VBox(6, table, legend);
    }

    private TableColumn<HistoryRow, String> col(String header, String property, double minWidth) {
        TableColumn<HistoryRow, String> col = new TableColumn<>(header);
        col.setCellValueFactory(new PropertyValueFactory<>(property));
        col.setMinWidth(minWidth);
        col.setStyle("-fx-font-family: Sans Serif;");
        return col;
    }

    // ── Load data ─────────────────────────────────────────────────────────────
    private void loadData() {
        allRows.clear();

        // Updated query to match stock_in_log and stock_out schemas
        String sql =
                "SELECT si.created_at AS evt_date, 'IN' AS type, " +
                        "       si.product_name, si.quantity AS qty, " +
                        "       COALESCE(si.unit, '') AS unit, " +
                        "       si.supplier_name AS reference, si.notes " +
                        "FROM stock_in_log si " +
                        "LEFT JOIN stocks s ON si.product_name = s.product_name " +
                        "UNION ALL " +
                        "SELECT so.date_out AS evt_date, 'OUT' AS type, " +
                        "       so.product_name, so.quantity_out AS qty, " +
                        "       COALESCE(so.unit, '') AS unit, " +
                        "       so.reason AS reference, so.notes " +
                        "FROM stock_out so " +
                        "LEFT JOIN stocks s ON so.stock_id = s.id " +
                        "ORDER BY evt_date DESC, type";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                boolean isIn = "IN".equals(rs.getString("type"));
                allRows.add(new HistoryRow(
                        rs.getString("evt_date"),
                        isIn ? "📥 IN" : "📤 OUT",
                        rs.getString("product_name"),
                        rs.getDouble("qty"),
                        rs.getString("unit"),
                        rs.getString("reference"),
                        rs.getString("notes"),
                        isIn
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError("Could not load stock history: " + e.getMessage());
        }
        updateStatus();
    }

    private void applyFilter(String text) {
        applyFilterWithType(text, "All");
    }

    private void applyFilterWithType(String text, String type) {
        String lower = text == null ? "" : text.toLowerCase().trim();
        filteredRows.setPredicate(row -> {
            boolean matchesType = "All".equals(type)
                    || (type.equals("IN")  && row.isIn())
                    || (type.equals("OUT") && !row.isIn());
            if (!matchesType) return false;
            if (lower.isEmpty()) return true;
            return row.getProductName().toLowerCase().contains(lower)
                    || row.getReference().toLowerCase().contains(lower)
                    || row.getNotes().toLowerCase().contains(lower)
                    || row.getType().toLowerCase().contains(lower);
        });
        updateStatus();
    }

    private void updateStatus() {
        long total = allRows.size();
        long shown = filteredRows.size();
        long ins   = allRows.stream().filter(HistoryRow::isIn).count();
        long outs  = total - ins;
        statusLabel.setText("Showing " + shown + " of " + total + "  •  " + ins + " IN  •  " + outs + " OUT");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private Button actionBtn(String text, String bg, String hover) {
        Button btn = new Button(text);
        btn.setStyle(btnStyle(bg));
        btn.setOnMouseEntered(e -> btn.setStyle(btnStyle(hover)));
        btn.setOnMouseExited(e  -> btn.setStyle(btnStyle(bg)));
        return btn;
    }

    private String btnStyle(String bg) {
        return "-fx-font-family: Sans Serif; -fx-font-size: 12px; -fx-text-fill: white; " +
                "-fx-background-color: " + bg + "; -fx-background-radius: 6; " +
                "-fx-cursor: hand; -fx-padding: 8px 18px;";
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}