package com.savvy.stocksavvyyloglog.view;

import com.savvy.stocksavvyyloglog.dialog.AddStockDialog;
import com.savvy.stocksavvyyloglog.dialog.StockInDialog;
import com.savvy.stocksavvyyloglog.dialog.StockOutDialog;
import com.savvy.stocksavvyyloglog.util.DatabaseConnection;
import com.savvy.stocksavvyyloglog.util.InventoryExporter;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
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

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class InventoryView {

    // ── Theme tokens ──────────────────────────────────────────────────────────
    private static final String L_BG             = "#FDF5EC";
    private static final String L_CARD_BG        = "#FDFAF0";
    private static final String L_BORDER         = "#E8D8C0";
    private static final String L_ACCENT         = "#C04A10";
    private static final String L_ACCENT2        = "#C8A96E";
    private static final String L_NAV_BG         = "#C04A10";
    private static final String L_NAV_BORDER     = "#C8A96E";
    private static final String L_TEXT           = "#2A1A08";
    private static final String L_TEXT_MUTED     = "#9E8050";
    private static final String L_INPUT_BG       = "#FDF8F4";
    private static final String L_LOW_STOCK_BG   = "#FFF3F0";
    private static final String L_LOW_STOCK_TEXT = "#C04A10";
    // ── Selection colors ──────────────────────────────────────────────────────
    private static final String L_SELECT_BG      = "#F5D5C8";   // warm orange tint
    private static final String L_SELECT_TEXT    = "#5A1A00";   // deep brown text on selection

    private final Stage stage;
    private final BorderPane dashboardRoot;

    private TableView<StockRow> table;
    private ObservableList<StockRow>  allRows;
    private FilteredList<StockRow>    filteredRows;
    private TextField  searchField;
    private ComboBox<String> categoryFilter;
    private Label      statusLabel;
    private InventorySummaryCards summaryCards;

    // ── Model ─────────────────────────────────────────────────────────────────
    public static class StockRow {
        private final int id;
        private final SimpleStringProperty productName;
        private final SimpleStringProperty category;
        private final SimpleStringProperty quantity;
        private final SimpleStringProperty unit;
        private final SimpleStringProperty costPerUnit;
        private final SimpleStringProperty supplier;
        private final SimpleStringProperty dateReceived;
        private final SimpleStringProperty notes;
        private final boolean lowStock;

        public StockRow(int id, String productName, String category,
                        double quantity, String unit, double costPerUnit,
                        String supplier, String dateReceived, String notes,
                        boolean lowStock) {
            this.id           = id;
            this.productName  = new SimpleStringProperty(productName);
            this.category     = new SimpleStringProperty(category != null ? category : "");
            this.quantity     = new SimpleStringProperty(String.format("%.2f", quantity));
            this.unit         = new SimpleStringProperty(unit);
            this.costPerUnit  = new SimpleStringProperty(String.format("₱%.2f", costPerUnit));
            this.supplier     = new SimpleStringProperty(supplier != null ? supplier : "");
            this.dateReceived = new SimpleStringProperty(dateReceived != null ? dateReceived : "");
            this.notes        = new SimpleStringProperty(notes != null ? notes : "");
            this.lowStock     = lowStock;
        }

        public int    getId()           { return id; }
        public String getProductName()  { return productName.get(); }
        public String getCategory()     { return category.get(); }
        public String getQuantity()     { return quantity.get(); }
        public String getUnit()         { return unit.get(); }
        public String getCostPerUnit()  { return costPerUnit.get(); }
        public String getSupplier()     { return supplier.get(); }
        public String getDateReceived() { return dateReceived.get(); }
        public String getNotes()        { return notes.get(); }
        public boolean isLowStock()     { return lowStock; }
    }

    public InventoryView(Stage stage, BorderPane dashboardRoot) {
        this.stage         = stage;
        this.dashboardRoot = dashboardRoot;
    }

    public void show() {
        dashboardRoot.setCenter(buildInventoryPane());
    }

    // ── Main pane ─────────────────────────────────────────────────────────────
    private ScrollPane buildInventoryPane() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(28, 36, 28, 36));
        content.setStyle("-fx-background-color: " + L_BG + ";");

        summaryCards = new InventorySummaryCards();
        HBox cards   = summaryCards.build();

        content.getChildren().addAll(buildHeader(), cards, buildToolbar(), buildTable());

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: " + L_BG + "; -fx-background: " + L_BG + "; -fx-border-width: 0;");
        return scroll;
    }

    // ── Header ────────────────────────────────────────────────────────────────
    private VBox buildHeader() {
        Label title = new Label("📦  Inventory");
        title.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 28px; " +
                "-fx-font-weight: bold; -fx-text-fill: " + L_ACCENT + ";");
        Label subtitle = new Label("Manage your stock levels, costs, and suppliers");
        subtitle.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 12px; -fx-text-fill: " + L_ACCENT2 + ";");
        VBox header = new VBox(4, title, subtitle);
        header.setStyle("-fx-border-width: 0 0 0 4; -fx-border-color: " + L_ACCENT2 + "; -fx-padding: 0 0 0 12;");
        return header;
    }

    // ── Toolbar ───────────────────────────────────────────────────────────────
    private VBox buildToolbar() {
        HBox row1 = new HBox(10);
        row1.setAlignment(Pos.CENTER_LEFT);

        searchField = new TextField();
        searchField.setPromptText("🔍  Search by product, category, supplier…");
        searchField.setPrefWidth(260);
        searchField.setMinWidth(160);
        searchField.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 12px; " +
                "-fx-background-color: " + L_BG + "; -fx-border-color: " + L_BORDER + "; " +
                "-fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6; " +
                "-fx-padding: 8 12; -fx-text-fill: " + L_TEXT + ";");
        searchField.textProperty().addListener((obs, o, n) -> applyFilter());

        categoryFilter = new ComboBox<>();
        categoryFilter.setPromptText("All Categories");
        categoryFilter.setPrefWidth(150);
        categoryFilter.setMinWidth(120);
        categoryFilter.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 12px;");
        categoryFilter.setOnAction(e -> applyFilter());

        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);

        statusLabel = new Label();
        statusLabel.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 11px; -fx-text-fill: " + L_TEXT_MUTED + ";");

        row1.getChildren().addAll(searchField, categoryFilter, spacer1, statusLabel);

        FlowPane row2 = new FlowPane(8, 6);
        row2.setAlignment(Pos.CENTER_LEFT);

        Button addBtn      = actionBtn("➕ Add",       L_ACCENT,  "#E8530A");
        Button editBtn     = actionBtn("✏️ Edit",      "#C8A96E", "#B8996A");
        Button deleteBtn   = actionBtn("🗑 Delete",    "#B85C00", "#A04A00");
        Button stockInBtn  = actionBtn("📥 Stock In",  "#2E7D32", "#1B5E20");
        Button stockOutBtn = actionBtn("📤 Stock Out", "#B85C00", "#8B4500");
        Button historyBtn  = actionBtn("📋 History",   "#5A5A8A", "#4A4A7A");
        Button exportBtn   = actionBtn("💾 Export CSV","#3A6EA5", "#2A5E95");
        Button refreshBtn  = actionBtn("🔄 Refresh",   "#5A5A8A", "#4A4A7A");

        addBtn.setOnAction(e      -> openAddDialog());
        editBtn.setOnAction(e     -> openEditDialog());
        deleteBtn.setOnAction(e   -> deleteSelected());
        stockInBtn.setOnAction(e  -> openStockIn());
        stockOutBtn.setOnAction(e -> openStockOut());
        historyBtn.setOnAction(e  -> new StockHistoryView(stage, dashboardRoot).show());
        exportBtn.setOnAction(e   -> exportCSV());
        refreshBtn.setOnAction(e  -> { loadData(); refreshCategoryFilter(); });

        Separator sep1 = new Separator(javafx.geometry.Orientation.VERTICAL);
        sep1.setPrefHeight(28);
        Separator sep2 = new Separator(javafx.geometry.Orientation.VERTICAL);
        sep2.setPrefHeight(28);

        row2.getChildren().addAll(
                addBtn, editBtn, deleteBtn, sep1,
                stockInBtn, stockOutBtn, sep2,
                historyBtn, exportBtn, refreshBtn
        );

        VBox toolbar = new VBox(10, row1, row2);
        toolbar.setPadding(new Insets(14, 18, 14, 18));
        toolbar.setStyle("-fx-background-color: " + L_CARD_BG + "; " +
                "-fx-border-color: " + L_BORDER + "; -fx-border-width: 1; " +
                "-fx-background-radius: 8; -fx-border-radius: 8;");
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

        TableColumn<StockRow, String> colName     = col("Product Name",  "productName",  180);
        TableColumn<StockRow, String> colCategory = col("Category",      "category",     110);
        TableColumn<StockRow, String> colQty      = col("Quantity",      "quantity",     90);
        TableColumn<StockRow, String> colUnit     = col("Unit",          "unit",         70);
        TableColumn<StockRow, String> colCost     = col("Cost / Unit",   "costPerUnit",  100);
        TableColumn<StockRow, String> colSupplier = col("Supplier",      "supplier",     130);
        TableColumn<StockRow, String> colDate     = col("Latest Received", "dateReceived", 110);
        TableColumn<StockRow, String> colNotes    = col("Notes",         "notes",        160);

        // ── Row factory with selection highlight ──────────────────────────────
        table.setRowFactory(tv -> {
            TableRow<StockRow> row = new TableRow<>() {
                @Override
                protected void updateItem(StockRow item, boolean empty) {
                    super.updateItem(item, empty);
                    applyRowStyle(this, item, empty);
                }
            };
            // Re-apply style when selection state changes
            row.selectedProperty().addListener((obs, wasSelected, isSelected) ->
                    applyRowStyle(row, row.getItem(), row.isEmpty())
            );
            return row;
        });

        table.getColumns().addAll(colName, colCategory, colQty, colUnit, colCost, colSupplier, colDate, colNotes);

        allRows      = FXCollections.observableArrayList();
        filteredRows = new FilteredList<>(allRows, p -> true);
        SortedList<StockRow> sortedRows = new SortedList<>(filteredRows);
        sortedRows.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sortedRows);

        loadData();
        refreshCategoryFilter();

        HBox legend = new HBox(16);
        legend.setAlignment(Pos.CENTER_LEFT);
        legend.setPadding(new Insets(8, 0, 0, 4));
        Label lowLegend = new Label("🟥  Low / out of stock");
        lowLegend.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 11px; -fx-text-fill: " + L_LOW_STOCK_TEXT + ";");
        Label okLegend = new Label("🟩  Normal stock");
        okLegend.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 11px; -fx-text-fill: #4A7C4E;");
        legend.getChildren().addAll(lowLegend, okLegend);

        return new VBox(6, table, legend);
    }

    // ── Row style logic ───────────────────────────────────────────────────────
    private void applyRowStyle(TableRow<StockRow> row, StockRow item, boolean empty) {
        if (empty || item == null) {
            row.setStyle("");
            return;
        }
        if (row.isSelected()) {
            row.setStyle(
                    "-fx-background-color: " + L_SELECT_BG + "; " +
                            "-fx-text-fill: " + L_SELECT_TEXT + ";"
            );
        } else if (item.isLowStock()) {
            row.setStyle("-fx-background-color: " + L_LOW_STOCK_BG + ";");
        } else {
            row.setStyle("-fx-background-color: " +
                    (row.getIndex() % 2 == 0 ? L_CARD_BG : L_BG) + ";");
        }
    }

    private TableColumn<StockRow, String> col(String header, String property, double minWidth) {
        TableColumn<StockRow, String> col = new TableColumn<>(header);
        col.setCellValueFactory(new PropertyValueFactory<>(property));
        col.setMinWidth(minWidth);
        col.setStyle("-fx-font-family: Sans Serif;");
        col.setSortable(true);
        return col;
    }

    // ── Load data ─────────────────────────────────────────────────────────────
    private void loadData() {
        allRows.clear();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT s.id, s.product_name, s.category, s.quantity, s.unit, " +
                             "s.cost_per_unit, s.supplier, s.date_received, s.notes, " +
                             "COALESCE( " +
                             "  (SELECT MAX(DATE(sil.created_at)) FROM stock_in_log sil WHERE sil.product_name = s.product_name), " +
                             "  s.date_received " +
                             ") as latest_received " +
                             "FROM stocks s ORDER BY s.product_name");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                double qty = rs.getDouble("quantity");
                allRows.add(new StockRow(
                        rs.getInt("id"),
                        rs.getString("product_name"),
                        rs.getString("category"),
                        qty,
                        rs.getString("unit"),
                        rs.getDouble("cost_per_unit"),
                        rs.getString("supplier"),
                        rs.getString("latest_received") != null ? rs.getString("latest_received") : "",
                        rs.getString("notes"),
                        qty <= 10
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError("Could not load inventory: " + e.getMessage());
        }
        updateStatus();
        if (summaryCards != null) summaryCards.refresh();
    }

    // ── Filters ───────────────────────────────────────────────────────────────
    private void refreshCategoryFilter() {
        String current = categoryFilter.getValue();
        categoryFilter.getItems().clear();
        categoryFilter.getItems().add("All Categories");
        allRows.stream().map(StockRow::getCategory)
                .filter(c -> c != null && !c.isBlank()).distinct().sorted()
                .forEach(c -> categoryFilter.getItems().add(c));
        categoryFilter.setValue(categoryFilter.getItems().contains(current) ? current : "All Categories");
    }

    private void applyFilter() {
        String text     = searchField.getText() == null ? "" : searchField.getText().toLowerCase().trim();
        String category = categoryFilter.getValue();
        filteredRows.setPredicate(row -> {
            boolean catMatch = category == null || category.equals("All Categories") || category.equals(row.getCategory());
            if (!catMatch) return false;
            if (text.isEmpty()) return true;
            return row.getProductName().toLowerCase().contains(text)
                    || row.getCategory().toLowerCase().contains(text)
                    || row.getSupplier().toLowerCase().contains(text)
                    || row.getNotes().toLowerCase().contains(text);
        });
        updateStatus();
    }

    private void updateStatus() {
        int total = allRows.size(), showing = filteredRows.size();
        long low  = allRows.stream().filter(StockRow::isLowStock).count();
        statusLabel.setText("Showing " + showing + " of " + total + " items" +
                (low > 0 ? "  •  ⚠ " + low + " low stock" : ""));
    }

    // ── Stock In / Out ────────────────────────────────────────────────────────
    private void openStockIn() {
        StockRow sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { showError("Please select a row first."); return; }
        StockInDialog dlg = new StockInDialog(stage, sel);
        dlg.setOnSaved(() -> { loadData(); refreshCategoryFilter(); });
        dlg.show();
    }

    private void openStockOut() {
        StockRow sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { showError("Please select a row first."); return; }
        StockOutDialog dlg = new StockOutDialog(stage, sel);
        dlg.setOnSaved(() -> { loadData(); refreshCategoryFilter(); });
        dlg.show();
    }

    private void exportCSV() {
        ObservableList<StockRow> visible = FXCollections.observableArrayList(filteredRows);
        new InventoryExporter(stage).exportToCSV(visible);
    }

    private void openAddDialog()  { 
        AddStockDialog dlg = new AddStockDialog();
        dlg.setOnSaved(() -> { loadData(); refreshCategoryFilter(); });
        dlg.show(stage);
    }

    private void openEditDialog() {
        StockRow sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { showError("Please select a row to edit."); return; }

        int threshold = 5;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT low_stock_threshold FROM stocks WHERE id = ?")) {
            ps.setInt(1, sel.getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) threshold = rs.getInt(1);
        } catch (Exception e) { e.printStackTrace(); }

        showStockDialog(sel, threshold);
    }

    // ── Styled Add / Edit Dialog ──────────────────────────────────────────────
    private void showStockDialog(StockRow existing, int thresholdVal) {
        Stage dialog = new Stage();
        dialog.initOwner(stage);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.setResizable(false);

        boolean isEdit = existing != null;

        VBox root = new VBox();
        root.setStyle(
                "-fx-background-color: " + L_BG + "; " +
                        "-fx-border-color: " + L_BORDER + "; -fx-border-width: 1; " +
                        "-fx-border-radius: 12; -fx-background-radius: 12;"
        );

        // ── Header ────────────────────────────────────────────────────────────
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 20, 14, 20));
        header.setStyle(
                "-fx-background-color: " + L_NAV_BG + "; " +
                        "-fx-border-width: 0 0 3 0; -fx-border-color: " + L_NAV_BORDER + "; " +
                        "-fx-background-radius: 12 12 0 0;"
        );
        Label headerTitle = new Label(isEdit ? "✏️  Edit Stock Entry" : "📦  Add Stock Entry");
        headerTitle.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 15px; " +
                "-fx-font-weight: bold; -fx-text-fill: #FAF0E6;");
        Region hSpacer = new Region();
        HBox.setHgrow(hSpacer, Priority.ALWAYS);
        Button closeX = new Button("✕");
        closeX.setStyle("-fx-font-size: 13px; -fx-text-fill: #FAF0E6; " +
                "-fx-background-color: rgba(255,255,255,0.15); " +
                "-fx-border-color: rgba(255,255,255,0.3); -fx-border-width: 1; " +
                "-fx-background-radius: 6; -fx-border-radius: 6; " +
                "-fx-cursor: hand; -fx-padding: 4 10;");
        closeX.setOnAction(e -> dialog.close());
        header.getChildren().addAll(headerTitle, hSpacer, closeX);

        // ── Form fields ───────────────────────────────────────────────────────
        TextField fName     = styledField("Product Name *",        isEdit ? existing.getProductName() : "");
        ComboBox<String> cbCategory = new ComboBox<>();
        cbCategory.setEditable(true);
        HBox fCategory = createCustomComboBox(cbCategory, "Category", false);

        TextField fQty      = styledField("Quantity *",            isEdit ? existing.getQuantity()    : "");
        TextField fUnit     = styledField("Unit (e.g. kg, pcs) *", isEdit ? existing.getUnit()        : "");
        TextField fCost     = styledField("Cost per Unit (₱)",     isEdit ? existing.getCostPerUnit().replace("₱","") : "");

        ComboBox<String> cbSupplier = new ComboBox<>();
        cbSupplier.setEditable(true);
        HBox fSupplier = createCustomComboBox(cbSupplier, "Supplier", false);

        TextField fDate     = styledField("YYYY-MM-DD",
                isEdit ? existing.getDateReceived() : LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
        TextField fLowStock = styledField("e.g. 5",                String.valueOf(thresholdVal));
        TextArea fNotes = new TextArea(isEdit ? existing.getNotes() : "");
        fNotes.setPromptText("Optional: batch number, condition, remarks…");
        fNotes.setPrefRowCount(3);
        fNotes.setWrapText(true);
        fNotes.setStyle(inputStyle());

        setupEditAutocomplete(cbCategory, cbSupplier);

        // PREFILL: Set editor text AFTER setup to ensure it's handled by recursion guard
        if (isEdit) {
            cbCategory.getEditor().setText(existing.getCategory());
            cbSupplier.getEditor().setText(existing.getSupplier());
        }

        // Error labels
        Label errName = errorLabel("Please enter a product name.");
        Label errQty  = errorLabel("Enter a valid quantity.");
        Label errCost = errorLabel("Cost must be at least 1.00");
        Label errLow  = errorLabel("Alert cannot exceed quantity.");

        // Real-time validation
        fName.textProperty().addListener((o, ov, nv) -> {
            if (nv.trim().isEmpty()) showFieldError(fName, errName);
            else clearFieldError(fName, errName);
        });
        fQty.textProperty().addListener((o, ov, nv) -> {
            try {
                double qty = Double.parseDouble(nv.trim());
                if (qty <= 0) showFieldError(fQty, errQty);
                else {
                    clearFieldError(fQty, errQty);
                    validateEditLowStock(fQty, fLowStock, errLow);
                }
            } catch (NumberFormatException e) { showFieldError(fQty, errQty); }
        });
        fCost.textProperty().addListener((o, ov, nv) -> validateEditCost(fCost, errCost));
        fLowStock.textProperty().addListener((o, ov, nv) -> validateEditLowStock(fQty, fLowStock, errLow));

        Label errLabel = new Label();
        errLabel.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 12px; -fx-text-fill: #D32F2F;");
        errLabel.setWrapText(true);

        // ── Body layout ───────────────────────────────────────────────────────
        VBox body = new VBox(14);
        body.setPadding(new Insets(20));
        body.setStyle("-fx-background-color: " + L_BG + ";");

        body.getChildren().addAll(
                sectionLabel("PRODUCT DETAILS"),
                dialogRow2(
                    new VBox(5, dialogField("Product Name *", fName), errName),
                    dialogField("Category", fCategory)
                ),
                dialogSep(),
                sectionLabel("STOCK QUANTITY"),
                dialogRow2(
                    new VBox(5, dialogField("Quantity *", fQty), errQty),
                    dialogField("Unit *", fUnit)
                ),
                dialogRow2(
                    new VBox(5, dialogField("Cost per Unit (₱)", fCost), errCost),
                    dialogField("Supplier", fSupplier)
                ),
                dialogSep(),
                sectionLabel("DELIVERY INFO"),
                dialogRow2(
                    dialogField("Date Received (YYYY-MM-DD)", fDate),
                    new VBox(5, dialogField("Low Stock Alert At", fLowStock), errLow)
                ),
                dialogSep(),
                sectionLabel("NOTES"),
                dialogField("Notes (optional)", fNotes),
                errLabel
        );

        ScrollPane bodyScroll = new ScrollPane(body);
        bodyScroll.setFitToWidth(true);
        bodyScroll.setPrefHeight(420);
        bodyScroll.setStyle("-fx-background-color: " + L_BG + "; -fx-background: " + L_BG + "; -fx-border-width: 0;");

        // ── Footer ────────────────────────────────────────────────────────────
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(14, 20, 14, 20));
        footer.setStyle("-fx-background-color: #FDF0E8; " +
                "-fx-border-width: 1 0 0 0; -fx-border-color: " + L_BORDER + "; " +
                "-fx-background-radius: 0 0 12 12;");

        Button cancelBtn = dialogBtn("Cancel", false);
        cancelBtn.setOnAction(e -> dialog.close());

        Button saveBtn = dialogBtn(isEdit ? "✔ Update" : "+ Add Stock", true);
        saveBtn.setOnAction(e -> {
            String name    = fName.getText().trim();
            String qtyStr  = fQty.getText().trim();
            String unit    = fUnit.getText().trim();
            String costStr = fCost.getText().trim();
            String lowStr  = fLowStock.getText().trim();
            String catVal  = cbCategory.getEditor().getText().trim();
            String supVal  = cbSupplier.getEditor().getText().trim();

            validateEditCost(fCost, errCost);
            validateEditLowStock(fQty, fLowStock, errLow);

            if (name.isEmpty() || qtyStr.isEmpty() || unit.isEmpty() || 
                errName.isVisible() || errQty.isVisible() || errCost.isVisible() || errLow.isVisible()) {
                errLabel.setText("⚠  Please fix the errors before saving.");
                return;
            }
            double qty, cost;
            int low;
            qty = Double.parseDouble(qtyStr);
            cost = costStr.isEmpty() ? 0 : Double.parseDouble(costStr);
            try { low = Integer.parseInt(lowStr); } catch (NumberFormatException ex) { low = 5; }

            String dateStr = fDate.getText().trim().isEmpty() ? null : fDate.getText().trim();

            try (Connection conn = DatabaseConnection.getConnection()) {
                if (!isEdit) {
                    PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO stocks (product_name, category, quantity, unit, cost_per_unit, supplier, date_received, notes, low_stock_threshold) " +
                                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
                    ps.setString(1, name);     ps.setString(2, catVal);
                    ps.setDouble(3, qty);      ps.setString(4, unit);
                    ps.setDouble(5, cost);     ps.setString(6, supVal);
                    ps.setString(7, dateStr);  ps.setString(8, fNotes.getText().trim());
                    ps.setInt(9, low);
                    ps.executeUpdate();
                } else {
                    PreparedStatement ps = conn.prepareStatement(
                            "UPDATE stocks SET product_name=?, category=?, quantity=?, unit=?, " +
                                    "cost_per_unit=?, supplier=?, date_received=?, notes=?, low_stock_threshold=? WHERE id=?");
                    ps.setString(1, name);     ps.setString(2, catVal);
                    ps.setDouble(3, qty);      ps.setString(4, unit);
                    ps.setDouble(5, cost);     ps.setString(6, supVal);
                    ps.setString(7, dateStr);  ps.setString(8, fNotes.getText().trim());
                    ps.setInt(9, low);         ps.setInt(10, existing.getId());
                    ps.executeUpdate();
                }
                dialog.close();
                loadData();
                refreshCategoryFilter();
            } catch (Exception ex) {
                ex.printStackTrace();
                errLabel.setText("⚠  Database error: " + ex.getMessage());
            }
        });

        footer.getChildren().addAll(cancelBtn, saveBtn);
        root.getChildren().addAll(header, bodyScroll, footer);

        Scene scene = new Scene(root, 520, 600);
        scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene);
        dialog.centerOnScreen();
        dialog.show();
    }

    private void setupEditAutocomplete(ComboBox<String> cbCat, ComboBox<String> cbSup) {
        java.util.List<String> sups = new java.util.ArrayList<>();
        java.util.List<String> cats = new java.util.ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection()) {
            ResultSet rs = conn.prepareStatement("SELECT DISTINCT supplier FROM stocks WHERE supplier IS NOT NULL AND supplier != ''").executeQuery();
            while (rs.next()) sups.add(rs.getString(1));
            rs = conn.prepareStatement("SELECT DISTINCT category FROM stocks WHERE category IS NOT NULL AND category != ''").executeQuery();
            while (rs.next()) cats.add(rs.getString(1));
        } catch (Exception e) { e.printStackTrace(); }

        setupSingleAutocomplete(cbSup, sups);
        setupSingleAutocomplete(cbCat, cats);
    }

    private void setupSingleAutocomplete(ComboBox<String> cb, java.util.List<String> items) {
        cb.getItems().setAll(items);
        final boolean[] isUpdating = {false};
        cb.getEditor().textProperty().addListener((obs, oldV, newV) -> {
            if (isUpdating[0]) return;
            if (cb.isShowing() && cb.getSelectionModel().getSelectedIndex() != -1) return;

            // FIX: Don't trim immediately to allow spacebar input
            if (newV == null || newV.isEmpty()) {
                isUpdating[0] = true;
                cb.getItems().setAll(items);
                cb.hide();
                isUpdating[0] = false;
            } else {
                String filter = newV.toLowerCase();
                java.util.List<String> filtered = new java.util.ArrayList<>();
                for (String s : items) if (s.toLowerCase().contains(filter)) filtered.add(s);
                
                isUpdating[0] = true;
                cb.getItems().setAll(filtered);
                // FIX: Only show if the field is focused (prevent auto-open on popup load)
                if (filtered.isEmpty()) {
                    cb.hide();
                } else if (cb.getEditor().isFocused()) {
                    cb.show();
                }
                isUpdating[0] = false;
            }
        });
    }

    private HBox createCustomComboBox(ComboBox<String> comboBox, String prompt, boolean showArrow) {
        comboBox.setPromptText(prompt);
        styleComboBox(comboBox);
        comboBox.setMaxWidth(Double.MAX_VALUE);

        StackPane wrapper;
        if (showArrow) {
            Label arrow = new Label("\u25BC");
            arrow.setStyle("-fx-text-fill: " + L_TEXT_MUTED + "; -fx-font-size: 10px; -fx-cursor: hand; -fx-padding: 0 10 0 0;");
            arrow.setOnMouseClicked(e -> {
                if (comboBox.isShowing()) comboBox.hide();
                else comboBox.show();
            });
            wrapper = new StackPane(comboBox, arrow);
            StackPane.setAlignment(arrow, Pos.CENTER_RIGHT);
        } else {
            wrapper = new StackPane(comboBox);
        }

        wrapper.setStyle(
                "-fx-background-color: " + L_INPUT_BG + "; " +
                "-fx-border-color: " + L_BORDER + "; " +
                "-fx-border-radius: 7; -fx-background-radius: 7;"
        );

        HBox container = new HBox(wrapper);
        container.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(wrapper, Priority.ALWAYS);
        return container;
    }

    private void styleComboBox(ComboBox<String> comboBox) {
        comboBox.setStyle(
                "-fx-font-family: Sans Serif; -fx-font-size: 13px; " +
                "-fx-background-color: transparent; " +
                "-fx-border-color: transparent; " +
                "-fx-padding: 0; -fx-text-fill: " + L_TEXT + ";"
        );

        if (comboBox.isEditable()) {
            comboBox.getEditor().setStyle(
                    "-fx-background-color: transparent; " +
                    "-fx-padding: 7 10; -fx-text-fill: " + L_TEXT + ";"
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
                    setTextFill(Color.web(L_TEXT));
                    setStyle("-fx-background-color: " + L_INPUT_BG + ";");
                }
            }
        });
    }

    // ── Dialog style helpers ──────────────────────────────────────────────────
    private TextField styledField(String prompt, String value) {
        TextField tf = new TextField(value);
        tf.setPromptText(prompt);
        tf.setStyle(inputStyle());
        return tf;
    }

    private String inputStyle() {
        return "-fx-font-family: Sans Serif; -fx-font-size: 13px; " +
                "-fx-background-color: " + L_INPUT_BG + "; " +
                "-fx-border-color: " + L_BORDER + "; -fx-border-radius: 7; -fx-background-radius: 7; " +
                "-fx-padding: 7 10; -fx-text-fill: " + L_TEXT + ";";
    }

    private VBox dialogField(String label, javafx.scene.Node input) {
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 11px; " +
                "-fx-font-weight: bold; -fx-text-fill: " + L_TEXT + ";");
        VBox box = new VBox(5, lbl, input);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private HBox dialogRow2(javafx.scene.Node left, javafx.scene.Node right) {
        HBox row = new HBox(12, left, right);
        HBox.setHgrow(left,  Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);
        return row;
    }

    private Label sectionLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 10px; " +
                "-fx-font-weight: bold; -fx-text-fill: " + L_TEXT_MUTED + ";");
        return lbl;
    }

    private Region dialogSep() {
        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setStyle("-fx-background-color: " + L_BORDER + ";");
        return sep;
    }

    private Label errorLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 11px; -fx-text-fill: #D32F2F;");
        lbl.setVisible(false);
        lbl.setManaged(false);
        return lbl;
    }

    private void showFieldError(Control control, Label errLabel) {
        control.setStyle(inputStyle() + " -fx-border-color: #D32F2F;");
        errLabel.setVisible(true);
        errLabel.setManaged(true);
    }

    private void clearFieldError(Control control, Label errLabel) {
        control.setStyle(inputStyle());
        errLabel.setVisible(false);
        errLabel.setManaged(false);
    }

    private void validateEditCost(TextField fCost, Label errCost) {
        try {
            String val = fCost.getText().trim();
            if (val.isEmpty()) { clearFieldError(fCost, errCost); return; }
            double cost = Double.parseDouble(val);
            if (cost < 1) showFieldError(fCost, errCost);
            else clearFieldError(fCost, errCost);
        } catch (NumberFormatException e) {
            errCost.setText("Invalid cost amount.");
            showFieldError(fCost, errCost);
        }
    }

    private void validateEditLowStock(TextField fQty, TextField fLow, Label errLow) {
        try {
            String qStr = fQty.getText().trim();
            String lStr = fLow.getText().trim();
            if (qStr.isEmpty() || lStr.isEmpty()) { clearFieldError(fLow, errLow); return; }
            double qty = Double.parseDouble(qStr);
            double low = Double.parseDouble(lStr);
            if (low > qty) showFieldError(fLow, errLow);
            else clearFieldError(fLow, errLow);
        } catch (NumberFormatException e) {
            errLow.setText("Invalid threshold number.");
            showFieldError(fLow, errLow);
        }
    }

    private Button dialogBtn(String text, boolean primary) {
        Button btn = new Button(text);
        String base, hover;
        if (primary) {
            base  = "-fx-font-family: Sans Serif; -fx-font-size: 13px; -fx-font-weight: bold; " +
                    "-fx-text-fill: white; -fx-background-color: " + L_ACCENT + "; " +
                    "-fx-background-radius: 7; -fx-cursor: hand; -fx-padding: 8 20;";
            hover = base.replace(L_ACCENT, "#A03A0A");
        } else {
            base  = "-fx-font-family: Sans Serif; -fx-font-size: 13px; " +
                    "-fx-text-fill: " + L_TEXT_MUTED + "; -fx-background-color: transparent; " +
                    "-fx-border-color: " + L_BORDER + "; -fx-border-width: 1; " +
                    "-fx-background-radius: 7; -fx-border-radius: 7; -fx-cursor: hand; -fx-padding: 8 18;";
            hover = base.replace("transparent", "#F3EBE7");
        }
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e  -> btn.setStyle(base));
        return btn;
    }

    // ── Delete ────────────────────────────────────────────────────────────────
    private void deleteSelected() {
        StockRow sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { showError("Please select a row to delete."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Stock Entry");
        confirm.setHeaderText("Delete \"" + sel.getProductName() + "\"?");
        confirm.setContentText("This action cannot be undone.");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try (Connection conn = DatabaseConnection.getConnection();
                     PreparedStatement ps = conn.prepareStatement("DELETE FROM stocks WHERE id = ?")) {
                    ps.setInt(1, sel.getId());
                    ps.executeUpdate();
                    loadData();
                    refreshCategoryFilter();
                } catch (Exception e) {
                    e.printStackTrace();
                    showError("Could not delete: " + e.getMessage());
                }
            }
        });
    }

    // ── Toolbar button helpers ────────────────────────────────────────────────
    private Button actionBtn(String text, String bg, String hover) {
        Button btn = new Button(text);
        String base = btnStyle(bg);
        String hov  = btnStyle(hover);
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hov));
        btn.setOnMouseExited(e  -> btn.setStyle(base));
        return btn;
    }

    private String btnStyle(String bgColor) {
        return "-fx-font-family: Sans Serif; -fx-font-size: 12px; -fx-text-fill: white; " +
                "-fx-background-color: " + bgColor + "; -fx-background-radius: 6; " +
                "-fx-cursor: hand; -fx-padding: 7px 14px;";
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}