package com.savvy.stocksavvyyloglog.view;

import com.savvy.stocksavvyyloglog.controller.NewSaleController;
import com.savvy.stocksavvyyloglog.model.SaleDAO;
import com.savvy.stocksavvyyloglog.model.SaleDAO.SaleRow;
import com.savvy.stocksavvyyloglog.model.SaleDAO.SaleSummary;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class SalesViewEnhanced {

    // ── Theme tokens ──────────────────────────────────────────────────────────
    private static final String L_BG         = "#FDF5EC";
    private static final String L_CARD_BG    = "#FDFAF0";
    private static final String L_CARD_BG2   = "#FBF4E8";
    private static final String L_BORDER     = "#E8D8C0";
    private static final String L_ACCENT     = "#C04A10";
    private static final String L_ACCENT2    = "#C8A96E";
    private static final String L_TEXT       = "#2A1A08";
    private static final String L_TEXT_MUTED = "#9E8050";
    private static final String L_SUCCESS    = "#4A7C4E";
    private static final String L_NAV_BG     = "#C04A10";

    private final Stage      stage;
    private final BorderPane dashboardRoot;
    private final String     currentUser;
    private final String     currentRole;
    private final SaleDAO    dao = new SaleDAO();

    private TableView<SaleRow>       table;
    private ObservableList<SaleRow>  allRows;
    private FilteredList<SaleRow>    filteredRows;
    private TextField                searchField;
    private DatePicker               dpFrom, dpTo;
    private Label                    statusLabel;
    private Label                    revenueLabel, txLabel, avgLabel, topProductLabel;

    // ── NEW: Status filter combo ──────────────────────────────────────────────
    private ComboBox<String>         statusFilter;

    // ── Constructor ───────────────────────────────────────────────────────────
    public SalesViewEnhanced(Stage stage, BorderPane dashboardRoot,
                             String currentUser, String currentRole) {
        this.stage         = stage;
        this.dashboardRoot = dashboardRoot;
        this.currentUser   = currentUser;
        this.currentRole   = currentRole;
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
                buildStatCards(),
                buildToolbar(),
                buildTableSection()
        );

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: " + L_BG + "; -fx-background: " + L_BG
                + "; -fx-border-width: 0;");
        return scroll;
    }

    // ── Header ────────────────────────────────────────────────────────────────
    private VBox buildHeader() {
        Label title = new Label("💰  Sales");
        title.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 28px; " +
                "-fx-font-weight: bold; -fx-text-fill: " + L_ACCENT + ";");

        Label subtitle = new Label("Track revenue, manage transactions and customer sales");
        subtitle.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 12px; " +
                "-fx-text-fill: " + L_ACCENT2 + ";");

        VBox header = new VBox(6, title, subtitle);
        header.setStyle("-fx-border-width: 0 0 0 4; -fx-border-color: " + L_ACCENT2
                + "; -fx-padding: 0 0 0 12;");
        return header;
    }

    // ── Stat cards ────────────────────────────────────────────────────────────
    private HBox buildStatCards() {
        SaleSummary summary = dao.getSummary();

        revenueLabel    = statValueLabel(String.format("₱%.2f", summary.totalRevenue),    L_ACCENT);
        txLabel         = statValueLabel(String.valueOf(summary.totalTransactions),        L_ACCENT2);
        avgLabel        = statValueLabel(String.format("₱%.2f", summary.avgOrderValue),   L_SUCCESS);
        topProductLabel = statValueLabel(summary.topProduct,                               "#5A5A8A");

        HBox cards = new HBox(0);
        cards.setMaxWidth(Double.MAX_VALUE);
        cards.getChildren().addAll(
                statCard("💰", "Revenue (This Month)",  revenueLabel,    L_ACCENT,  true),
                statCard("🧾", "Transactions",           txLabel,         L_ACCENT2, false),
                statCard("📊", "Avg Order Value",        avgLabel,        L_SUCCESS, false),
                statCard("🏆", "Top Product",            topProductLabel, "#5A5A8A", false)
        );
        return cards;
    }

    private VBox statCard(String icon, String title, Label valueLabel,
                          String accentColor, boolean isFirst) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(20, 24, 20, 24));
        card.setMaxWidth(Double.MAX_VALUE);
        card.setMinHeight(110);
        HBox.setHgrow(card, Priority.ALWAYS);

        String borderStyle = isFirst
                ? "-fx-border-width: 0 0 0 5; -fx-border-color: " + accentColor + ";"
                : "-fx-border-width: 0 0 0 1; -fx-border-color: " + L_BORDER + ";";
        String shadow = "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 4, 0, 0, 2);";
        card.setStyle("-fx-background-color: " + L_CARD_BG + "; " + borderStyle + shadow);

        Label iconLbl  = new Label(icon);
        iconLbl.setStyle("-fx-font-size: 20px;");
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 11px; " +
                "-fx-text-fill: " + L_TEXT_MUTED + ";");

        card.getChildren().addAll(iconLbl, titleLbl, valueLabel);

        String hoverShadow = "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 8, 0, 0, 3);";
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: " + L_CARD_BG2 + "; " + borderStyle + hoverShadow));
        card.setOnMouseExited (e -> card.setStyle("-fx-background-color: " + L_CARD_BG  + "; " + borderStyle + shadow));
        return card;
    }

    private Label statValueLabel(String value, String color) {
        Label lbl = new Label(value);
        lbl.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 22px; " +
                "-fx-font-weight: bold; -fx-text-fill: " + color + ";");
        lbl.setWrapText(true);
        return lbl;
    }

    // ── Toolbar ───────────────────────────────────────────────────────────────
    private HBox buildToolbar() {
        HBox toolbar = new HBox(10);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(14, 18, 14, 18));
        toolbar.setStyle("-fx-background-color: " + L_CARD_BG + "; " +
                "-fx-border-color: " + L_BORDER + "; -fx-border-width: 1; " +
                "-fx-background-radius: 8; -fx-border-radius: 8;");

        // Search
        searchField = new TextField();
        searchField.setPromptText("🔍  Search customer, product, payment…");
        searchField.setPrefWidth(260);
        searchField.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 12px; " +
                "-fx-background-color: " + L_BG + "; -fx-border-color: " + L_BORDER + "; " +
                "-fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6; " +
                "-fx-padding: 8 12; -fx-text-fill: " + L_TEXT + ";");
        searchField.textProperty().addListener((obs, o, n) -> applyFilter());

        // Date range
        dpFrom = datePicker(LocalDate.now().withDayOfMonth(1));
        dpTo   = datePicker(LocalDate.now());
        dpFrom.valueProperty().addListener((obs, o, n) -> applyFilter());
        dpTo.valueProperty().addListener((obs, o, n)   -> applyFilter());

        Label fromLbl = toolbarLabel("From:");
        Label toLbl   = toolbarLabel("To:");

        // ── NEW: Status filter dropdown ───────────────────────────────────────
        statusFilter = new ComboBox<>();
        statusFilter.getItems().addAll("All", "Completed");
        statusFilter.setValue("All");
        statusFilter.setPrefWidth(120);
        statusFilter.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 12px;");
        statusFilter.valueProperty().addListener((obs, o, n) -> applyFilter());

        Button filterBtn = actionBtn("🔍  Filter", "#5A5A8A", "#4A4A7A");
        filterBtn.setOnAction(e -> applyFilter());

        Button clearFilterBtn = actionBtn("✕ Clear", "#9E8050", "#7A6040");
        clearFilterBtn.setOnAction(e -> {
            dpFrom.setValue(LocalDate.now().withDayOfMonth(1));
            dpTo.setValue(LocalDate.now());
            searchField.clear();
            statusFilter.setValue("All");
            loadData();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        statusLabel = new Label();
        statusLabel.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 11px; " +
                "-fx-text-fill: " + L_TEXT_MUTED + ";");

        Button newSaleBtn = actionBtn("➕  New Sale", L_ACCENT, "#A03A0A");
        newSaleBtn.setOnAction(e -> openNewSaleDialog());

        Button exportBtn = actionBtn("📤  Export CSV", L_SUCCESS, "#3A6040");
        exportBtn.setOnAction(e -> exportCSV());

        Button refreshBtn = actionBtn("🔄  Refresh", "#5A5A8A", "#4A4A7A");
        refreshBtn.setOnAction(e -> { loadData(); refreshStatCards(); });

        toolbar.getChildren().addAll(
                searchField, fromLbl, dpFrom, toLbl, dpTo,
                statusFilter,                          // ← NEW
                filterBtn, clearFilterBtn,
                spacer, statusLabel, newSaleBtn, exportBtn, refreshBtn
        );
        return toolbar;
    }

    private Label toolbarLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 12px; " +
                "-fx-text-fill: " + L_TEXT_MUTED + ";");
        return lbl;
    }

    private DatePicker datePicker(LocalDate value) {
        DatePicker dp = new DatePicker(value);
        dp.setPrefWidth(130);
        dp.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 12px;");
        return dp;
    }

    // ── Table section ─────────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private VBox buildTableSection() {
        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(500);
        table.setStyle("-fx-background-color: " + L_CARD_BG + "; " +
                "-fx-border-color: " + L_BORDER + "; -fx-border-width: 1; " +
                "-fx-background-radius: 8; -fx-border-radius: 8; " +
                "-fx-font-family: Sans Serif; -fx-font-size: 12px;");

        TableColumn<SaleRow, String> colId       = strCol("ID",             80,  r -> String.valueOf(r.id));
        TableColumn<SaleRow, String> colDate     = strCol("Date",           110, r -> r.saleDate);
        TableColumn<SaleRow, String> colCustomer = strCol("Customer",       150, r -> r.customerName);
        TableColumn<SaleRow, String> colProduct  = strCol("Product",        160, r -> r.productName);
        TableColumn<SaleRow, String> colQty      = strCol("Qty",            70,  r -> String.format("%.0f", r.quantity));
        TableColumn<SaleRow, String> colPrice    = strCol("Unit Price",     110, r -> String.format("₱%.2f", r.unitPrice));
        TableColumn<SaleRow, String> colTotal    = strCol("Total",          120, r -> String.format("₱%.2f", r.totalAmount));
        TableColumn<SaleRow, String> colPayment  = strCol("Payment",        110, r -> r.paymentMethod);
        TableColumn<SaleRow, String> colSoldBy   = strCol("Sold By",        100, r -> r.soldBy);

        // Payment method color badge
        colPayment.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                String color = paymentColor(item);
                setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
            }
        });

        // Total column bold
        colTotal.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle("-fx-text-fill: " + L_ACCENT + "; -fx-font-weight: bold;");
            }
        });

        // ── NEW: Status column ────────────────────────────────────────────────
        TableColumn<SaleRow, String> colStatus = new TableColumn<>("Status");
        colStatus.setMinWidth(110);
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setText(null); setGraphic(null); setStyle(""); return; }
                Label badge = new Label("✅ Completed");
                badge.setStyle(
                        "-fx-background-color: #E8F5E9; " +
                                "-fx-text-fill: #4A7C4E; " +
                                "-fx-font-weight: bold; " +
                                "-fx-font-size: 11px; " +
                                "-fx-padding: 3 8; " +
                                "-fx-background-radius: 10;"
                );
                setGraphic(badge);
                setText(null);
            }
        });

        table.getColumns().addAll(
                colId, colDate, colCustomer, colProduct,
                colQty, colPrice, colTotal, colPayment, colSoldBy, colStatus
        );

        allRows      = FXCollections.observableArrayList();
        filteredRows = new FilteredList<>(allRows, p -> true);
        table.setItems(filteredRows);
        loadData();

        HBox tableFooter = buildTableFooter();

        VBox section = new VBox(0, table, tableFooter);
        section.setStyle("-fx-border-color: " + L_BORDER + "; -fx-border-width: 1; " +
                "-fx-background-radius: 8; -fx-border-radius: 8; " +
                "-fx-background-color: " + L_CARD_BG + ";");
        return section;
    }

    private HBox buildTableFooter() {
        HBox footer = new HBox(20);
        footer.setPadding(new Insets(10, 18, 10, 18));
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setStyle("-fx-background-color: #FDF0E8; " +
                "-fx-border-color: " + L_BORDER + "; -fx-border-width: 1 0 0 0;");

        statusLabel = new Label("Loading…");
        statusLabel.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 11px; " +
                "-fx-text-fill: " + L_TEXT_MUTED + ";");

        footer.getChildren().add(statusLabel);
        return footer;
    }

    // ── Helper: typed column with lambda ─────────────────────────────────────
    private TableColumn<SaleRow, String> strCol(String header, double minW,
                                                java.util.function.Function<SaleRow, String> extractor) {
        TableColumn<SaleRow, String> col = new TableColumn<>(header);
        col.setMinWidth(minW);
        col.setCellValueFactory(cd -> new SimpleStringProperty(extractor.apply(cd.getValue())));
        col.setStyle("-fx-font-family: Sans Serif;");
        return col;
    }

    // ── Load & filter ─────────────────────────────────────────────────────────
    private void loadData() {
        allRows.clear();
        List<SaleRow> rows = dao.getAllSales();
        allRows.addAll(rows);
        applyFilter();
    }

    private void applyFilter() {
        String search = searchField == null ? "" : searchField.getText().toLowerCase().trim();
        LocalDate from = dpFrom != null ? dpFrom.getValue() : null;
        LocalDate to   = dpTo   != null ? dpTo.getValue()   : null;
        // ── NEW: read selected status filter ─────────────────────────────────
        String selectedStatus = statusFilter != null ? statusFilter.getValue() : "All";
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        filteredRows.setPredicate(row -> {
            // Date range filter
            if (from != null && row.saleDate != null) {
                try {
                    LocalDate d = LocalDate.parse(row.saleDate, fmt);
                    if (d.isBefore(from)) return false;
                } catch (Exception ignored) {}
            }
            if (to != null && row.saleDate != null) {
                try {
                    LocalDate d = LocalDate.parse(row.saleDate, fmt);
                    if (d.isAfter(to)) return false;
                } catch (Exception ignored) {}
            }
            // ── NEW: Status filter (all rows are Completed, so "All" = show all) ──
            if (selectedStatus != null && !selectedStatus.equals("All")) {
                // Since all visible rows are Completed (voided = deleted),
                // selecting "Completed" shows all rows — no rows are excluded.
                // This is ready to extend if you add soft-delete later.
            }
            // Text search
            if (!search.isEmpty()) {
                boolean match = (row.customerName  != null && row.customerName.toLowerCase().contains(search))
                        || (row.productName    != null && row.productName.toLowerCase().contains(search))
                        || (row.paymentMethod  != null && row.paymentMethod.toLowerCase().contains(search))
                        || (row.soldBy         != null && row.soldBy.toLowerCase().contains(search))
                        || String.valueOf(row.id).contains(search);
                if (!match) return false;
            }
            return true;
        });

        updateStatus();
    }

    private void updateStatus() {
        long   shown   = filteredRows.size();
        long   total   = allRows.size();
        double revenue = filteredRows.stream().mapToDouble(r -> r.totalAmount).sum();
        if (statusLabel != null)
            statusLabel.setText(String.format(
                    "Showing %d of %d transactions  •  Total: ₱%.2f", shown, total, revenue));
    }

    private void refreshStatCards() {
        SaleSummary s = dao.getSummary();
        if (revenueLabel    != null) revenueLabel.setText(String.format("₱%.2f", s.totalRevenue));
        if (txLabel         != null) txLabel.setText(String.valueOf(s.totalTransactions));
        if (avgLabel        != null) avgLabel.setText(String.format("₱%.2f", s.avgOrderValue));
        if (topProductLabel != null) topProductLabel.setText(s.topProduct);
    }

    // ── New Sale dialog ───────────────────────────────────────────────────────
    private void openNewSaleDialog() {
        try {
            new NewSaleController(stage, currentUser).show();

            javafx.animation.PauseTransition pause =
                    new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1));
            pause.setOnFinished(e -> { loadData(); refreshStatCards(); });
            pause.play();

        } catch (Exception e) {
            showError("Could not open New Sale dialog: " + e.getMessage());
        }
    }

    // ── Void (delete) a sale ──────────────────────────────────────────────────
    private void confirmVoid(SaleRow row) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Void Sale");
        confirm.setHeaderText("Void Sale #" + row.id + "?");
        confirm.setContentText(
                "Customer: " + row.customerName + "\n" +
                        "Product:  " + row.productName  + "\n" +
                        "Total:    ₱" + String.format("%.2f", row.totalAmount) + "\n\n" +
                        "This will permanently delete the sale record.");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                boolean ok = dao.deleteSale(row.id);
                if (ok) {
                    loadData();
                    refreshStatCards();
                    showSuccess("Sale #" + row.id + " has been voided.");
                } else {
                    showError("Failed to void the sale. Please try again.");
                }
            }
        });
    }

    // ── Export CSV ────────────────────────────────────────────────────────────
    private void exportCSV() {
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Export Sales CSV");
        fc.setInitialFileName("sales_" + LocalDate.now() + ".csv");
        fc.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        java.io.File file = fc.showSaveDialog(stage);
        if (file == null) return;

        try (java.io.PrintWriter pw = new java.io.PrintWriter(file)) {
            pw.println("ID,Date,Customer,Product,Quantity,Unit Price,Total,Payment,Sold By,Status");
            for (SaleRow r : filteredRows) {
                pw.printf("%d,%s,\"%s\",\"%s\",%.0f,%.2f,%.2f,%s,%s,Completed%n",
                        r.id, r.saleDate, r.customerName, r.productName,
                        r.quantity, r.unitPrice, r.totalAmount,
                        r.paymentMethod, r.soldBy);
            }
            showSuccess("Exported " + filteredRows.size() + " records to " + file.getName());
        } catch (Exception e) {
            showError("Export failed: " + e.getMessage());
        }
    }

    // ── Payment color ─────────────────────────────────────────────────────────
    private String paymentColor(String method) {
        if (method == null) return L_TEXT;
        switch (method) {
            case "Cash":          return "#4A7C4E";
            case "GCash":        return "#1565C0";
            case "Bank Transfer": return "#5A5A8A";
            case "Credit":       return "#B85C00";
            default:              return L_TEXT;
        }
    }

    // ── Alerts ────────────────────────────────────────────────────────────────
    private void showSuccess(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    // ── Button helper ─────────────────────────────────────────────────────────
    private Button actionBtn(String text, String bg, String hover) {
        Button btn = new Button(text);
        String base  = btnStyle(bg);
        String hov   = btnStyle(hover);
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hov));
        btn.setOnMouseExited (e -> btn.setStyle(base));
        return btn;
    }

    private String btnStyle(String bg) {
        return "-fx-font-family: Sans Serif; -fx-font-size: 12px; -fx-text-fill: white; " +
                "-fx-background-color: " + bg + "; -fx-background-radius: 6; " +
                "-fx-cursor: hand; -fx-padding: 8px 16px;";
    }
}