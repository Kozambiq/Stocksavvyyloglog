package com.savvy.stocksavvyyloglog.view;

import com.savvy.stocksavvyyloglog.dialog.NewSalesOrderDialog;
import com.savvy.stocksavvyyloglog.model.SalesOrderDAO;
import com.savvy.stocksavvyyloglog.model.SalesOrderDAO.SalesOrderRow;
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

/**
 * SalesOrderView
 * --------------
 * Full-page Sales Order tab. Displays:
 *   • Summary stat cards (total orders, pending, confirmed, delivered)
 *   • Toolbar: search, date-range filter, status filter, New Order button
 *   • Table: all sales orders with status badges and action buttons
 *   • Status lifecycle: Pending → Confirmed (deducts stock) → Delivered
 */
public class SalesOrderView {

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

    private final Stage          stage;
    private final BorderPane     dashboardRoot;
    private final String         currentUser;
    private final String         currentRole;
    private final SalesOrderDAO  dao = new SalesOrderDAO();

    private TableView<SalesOrderRow>      table;
    private ObservableList<SalesOrderRow> allRows;
    private FilteredList<SalesOrderRow>   filteredRows;
    private TextField                     searchField;
    private DatePicker                    dpFrom, dpTo;
    private ComboBox<String>              statusFilter;
    private Label                         footerLabel;

    // Stat card labels
    private Label totalLabel, pendingLabel, confirmedLabel, deliveredLabel;

    public SalesOrderView(Stage stage, BorderPane dashboardRoot,
                          String currentUser, String currentRole) {
        this.stage         = stage;
        this.dashboardRoot = dashboardRoot;
        this.currentUser   = currentUser;
        this.currentRole   = currentRole;
    }

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
        Label title = new Label("📋  Sales Orders");
        title.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 28px; " +
                "-fx-font-weight: bold; -fx-text-fill: " + L_ACCENT + ";");

        Label subtitle = new Label("Manage purchase agreements before fulfillment");
        subtitle.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 12px; " +
                "-fx-text-fill: " + L_ACCENT2 + ";");

        VBox header = new VBox(6, title, subtitle);
        header.setStyle("-fx-border-width: 0 0 0 4; -fx-border-color: " + L_ACCENT2
                + "; -fx-padding: 0 0 0 12;");
        return header;
    }

    // ── Stat cards ────────────────────────────────────────────────────────────
    private HBox buildStatCards() {
        totalLabel     = statValueLabel("0", L_ACCENT);
        pendingLabel   = statValueLabel("0", "#B85C00");
        confirmedLabel = statValueLabel("0", "#1565C0");
        deliveredLabel = statValueLabel("0", L_SUCCESS);

        HBox cards = new HBox(0);
        cards.setMaxWidth(Double.MAX_VALUE);
        cards.getChildren().addAll(
                statCard("📋", "Total Orders",  totalLabel,     L_ACCENT,   true),
                statCard("⏳", "Pending",        pendingLabel,   "#B85C00",  false),
                statCard("✅", "Confirmed",      confirmedLabel, "#1565C0",  false),
                statCard("📦", "Delivered",      deliveredLabel, L_SUCCESS,  false)
        );

        refreshStatCards();
        return cards;
    }

    private void refreshStatCards() {
        List<SalesOrderRow> rows = dao.getAllOrders();
        long total     = rows.size();
        long pending   = rows.stream().filter(r -> "Pending".equals(r.status)).count();
        long confirmed = rows.stream().filter(r -> "Confirmed".equals(r.status)).count();
        long delivered = rows.stream().filter(r -> "Delivered".equals(r.status)).count();

        if (totalLabel     != null) totalLabel.setText(String.valueOf(total));
        if (pendingLabel   != null) pendingLabel.setText(String.valueOf(pending));
        if (confirmedLabel != null) confirmedLabel.setText(String.valueOf(confirmed));
        if (deliveredLabel != null) deliveredLabel.setText(String.valueOf(delivered));
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

        Label iconLbl  = new Label(icon);  iconLbl.setStyle("-fx-font-size: 20px;");
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
        searchField.setPromptText("🔍  Search customer, product…");
        searchField.setPrefWidth(240);
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

        // Status filter
        statusFilter = new ComboBox<>();
        statusFilter.getItems().addAll("All", "Pending", "Confirmed", "Delivered");
        statusFilter.setValue("All");
        statusFilter.setPrefWidth(120);
        statusFilter.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 12px;");
        statusFilter.valueProperty().addListener((obs, o, n) -> applyFilter());

        Button filterBtn = actionBtn("🔍  Filter", "#5A5A8A", "#4A4A7A");
        filterBtn.setOnAction(e -> applyFilter());

        Button clearBtn = actionBtn("✕ Clear", "#9E8050", "#7A6040");
        clearBtn.setOnAction(e -> {
            dpFrom.setValue(LocalDate.now().withDayOfMonth(1));
            dpTo.setValue(LocalDate.now());
            searchField.clear();
            statusFilter.setValue("All");
            loadData();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button newOrderBtn = actionBtn("➕  New Order", L_ACCENT, "#A03A0A");
        newOrderBtn.setOnAction(e -> openNewOrderDialog());

        Button refreshBtn = actionBtn("🔄  Refresh", "#5A5A8A", "#4A4A7A");
        refreshBtn.setOnAction(e -> { loadData(); refreshStatCards(); });

        toolbar.getChildren().addAll(
                searchField,
                toolbarLabel("From:"), dpFrom,
                toolbarLabel("To:"), dpTo,
                statusFilter,
                filterBtn, clearBtn,
                spacer, newOrderBtn, refreshBtn
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

    // ── Table ─────────────────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private VBox buildTableSection() {
        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(500);
        table.setStyle("-fx-background-color: " + L_CARD_BG + "; " +
                "-fx-border-color: " + L_BORDER + "; -fx-border-width: 1; " +
                "-fx-background-radius: 8; -fx-border-radius: 8; " +
                "-fx-font-family: Sans Serif; -fx-font-size: 12px;");

        TableColumn<SalesOrderRow, String> colId        = strCol("ID",            70,  r -> String.valueOf(r.id));
        TableColumn<SalesOrderRow, String> colDate      = strCol("Order Date",    110, r -> r.orderDate);
        TableColumn<SalesOrderRow, String> colDelivery  = strCol("Delivery Date", 110, r -> r.deliveryDate != null ? r.deliveryDate : "—");
        TableColumn<SalesOrderRow, String> colCustomer  = strCol("Customer",      150, r -> r.customerName);
        TableColumn<SalesOrderRow, String> colProduct   = strCol("Product",       150, r -> r.productName);
        TableColumn<SalesOrderRow, String> colQty       = strCol("Qty",           70,  r -> String.format("%.0f", r.quantity));
        TableColumn<SalesOrderRow, String> colPrice     = strCol("Unit Price",    100, r -> String.format("₱%.2f", r.unitPrice));
        TableColumn<SalesOrderRow, String> colTotal     = strCol("Total",         110, r -> String.format("₱%.2f", r.totalAmount));

        // Total bold + accent
        colTotal.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle("-fx-text-fill: " + L_ACCENT + "; -fx-font-weight: bold;");
            }
        });

        // Status badge column
        TableColumn<SalesOrderRow, String> colStatus = new TableColumn<>("Status");
        colStatus.setMinWidth(120);
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setText(null); setGraphic(null); return; }
                SalesOrderRow row = getTableView().getItems().get(getIndex());
                Label badge = new Label(statusEmoji(row.status) + " " + row.status);
                badge.setStyle(statusBadgeStyle(row.status));
                setGraphic(badge);
                setText(null);
            }
        });

        // Action column — Confirm / Deliver buttons
        TableColumn<SalesOrderRow, String> colAction = new TableColumn<>("Action");
        colAction.setMinWidth(160);
        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button confirmBtn = new Button("Confirm");
            private final Button deliverBtn = new Button("Deliver");
            private final Button deleteBtn  = new Button("Delete");
            private final HBox   box        = new HBox(6, confirmBtn, deliverBtn, deleteBtn);

            {
                confirmBtn.setStyle(smallBtn("#1565C0"));
                deliverBtn.setStyle(smallBtn(L_SUCCESS));
                deleteBtn.setStyle(smallBtn("#9E8050"));
                box.setAlignment(Pos.CENTER_LEFT);

                confirmBtn.setOnAction(e -> {
                    SalesOrderRow row = getTableView().getItems().get(getIndex());
                    handleStatusChange(row, "Confirmed");
                });
                deliverBtn.setOnAction(e -> {
                    SalesOrderRow row = getTableView().getItems().get(getIndex());
                    handleStatusChange(row, "Delivered");
                });
                deleteBtn.setOnAction(e -> {
                    SalesOrderRow row = getTableView().getItems().get(getIndex());
                    handleDelete(row);
                });
            }

            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                SalesOrderRow row = getTableView().getItems().get(getIndex());
                // Show buttons based on current status
                confirmBtn.setVisible("Pending".equals(row.status));
                confirmBtn.setManaged("Pending".equals(row.status));
                deliverBtn.setVisible("Confirmed".equals(row.status));
                deliverBtn.setManaged("Confirmed".equals(row.status));
                deleteBtn.setVisible("Admin".equals(currentRole) && !"Delivered".equals(row.status));
                deleteBtn.setManaged("Admin".equals(currentRole) && !"Delivered".equals(row.status));
                setGraphic(box);
            }
        });

        table.getColumns().addAll(
                colId, colDate, colDelivery, colCustomer, colProduct,
                colQty, colPrice, colTotal, colStatus, colAction
        );

        allRows      = FXCollections.observableArrayList();
        filteredRows = new FilteredList<>(allRows, p -> true);
        table.setItems(filteredRows);
        loadData();

        // Footer
        footerLabel = new Label("Loading…");
        footerLabel.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 11px; " +
                "-fx-text-fill: " + L_TEXT_MUTED + ";");

        HBox footer = new HBox(footerLabel);
        footer.setPadding(new Insets(10, 18, 10, 18));
        footer.setStyle("-fx-background-color: #FDF0E8; " +
                "-fx-border-color: " + L_BORDER + "; -fx-border-width: 1 0 0 0;");

        VBox section = new VBox(0, table, footer);
        section.setStyle("-fx-border-color: " + L_BORDER + "; -fx-border-width: 1; " +
                "-fx-background-radius: 8; -fx-border-radius: 8; " +
                "-fx-background-color: " + L_CARD_BG + ";");
        return section;
    }

    // ── Load & filter ─────────────────────────────────────────────────────────
    private void loadData() {
        allRows.clear();
        allRows.addAll(dao.getAllOrders());
        applyFilter();
    }

    private void applyFilter() {
        String search   = searchField == null ? "" : searchField.getText().toLowerCase().trim();
        LocalDate from  = dpFrom != null ? dpFrom.getValue() : null;
        LocalDate to    = dpTo   != null ? dpTo.getValue()   : null;
        String status   = statusFilter != null ? statusFilter.getValue() : "All";
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        filteredRows.setPredicate(row -> {
            // Date filter
            if (from != null && row.orderDate != null) {
                try { if (LocalDate.parse(row.orderDate, fmt).isBefore(from)) return false; }
                catch (Exception ignored) {}
            }
            if (to != null && row.orderDate != null) {
                try { if (LocalDate.parse(row.orderDate, fmt).isAfter(to)) return false; }
                catch (Exception ignored) {}
            }
            // Status filter
            if (status != null && !"All".equals(status)) {
                if (!status.equals(row.status)) return false;
            }
            // Text search
            if (!search.isEmpty()) {
                boolean match =
                        (row.customerName != null && row.customerName.toLowerCase().contains(search))
                                || (row.productName  != null && row.productName.toLowerCase().contains(search))
                                || String.valueOf(row.id).contains(search);
                if (!match) return false;
            }
            return true;
        });

        updateFooter();
    }

    private void updateFooter() {
        long shown   = filteredRows.size();
        long total   = allRows.size();
        double value = filteredRows.stream().mapToDouble(r -> r.totalAmount).sum();
        if (footerLabel != null)
            footerLabel.setText(String.format(
                    "Showing %d of %d orders  •  Total Value: ₱%.2f", shown, total, value));
    }

    // ── Status actions ────────────────────────────────────────────────────────
    private void handleStatusChange(SalesOrderRow row, String newStatus) {
        String msg = "Confirmed".equals(newStatus)
                ? "Confirm Order #" + row.id + "?\n\nThis will deduct " +
                String.format("%.0f", row.quantity) + " unit(s) of " + row.productName + " from stock."
                : "Mark Order #" + row.id + " as Delivered?";

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle(newStatus + " Order");
        confirm.setHeaderText(newStatus + " Order #" + row.id);
        confirm.setContentText(msg);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                boolean ok = dao.updateStatus(row.id, newStatus);
                if (ok) {
                    loadData();
                    refreshStatCards();
                    showSuccess("Order #" + row.id + " is now " + newStatus + ".");
                } else {
                    showError("Confirmed".equals(newStatus)
                            ? "Failed to confirm. Check if there is enough stock."
                            : "Failed to update order status.");
                }
            }
        });
    }

    private void handleDelete(SalesOrderRow row) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Order");
        confirm.setHeaderText("Delete Order #" + row.id + "?");
        confirm.setContentText("Customer: " + row.customerName + "\nProduct: " + row.productName +
                "\n\nThis cannot be undone.");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                boolean ok = dao.deleteOrder(row.id);
                if (ok) { loadData(); refreshStatCards(); showSuccess("Order #" + row.id + " deleted."); }
                else    { showError("Failed to delete order."); }
            }
        });
    }

    // ── New order dialog ──────────────────────────────────────────────────────
    private void openNewOrderDialog() {
        try {
            NewSalesOrderDialog dialog = new NewSalesOrderDialog(stage, currentUser);
            dialog.show();
            if (dialog.isSaved()) {
                loadData();
                refreshStatCards();
            }
        } catch (Exception e) {
            showError("Could not open New Order dialog: " + e.getMessage());
        }
    }

    // ── Status styling ────────────────────────────────────────────────────────
    private String statusEmoji(String status) {
        if (status == null) return "❓";
        switch (status) {
            case "Pending":   return "⏳";
            case "Confirmed": return "✅";
            case "Delivered": return "📦";
            default:          return "❓";
        }
    }

    private String statusBadgeStyle(String status) {
        String bg, fg;
        switch (status != null ? status : "") {
            case "Pending":
                bg = "#FFF3E0"; fg = "#B85C00"; break;
            case "Confirmed":
                bg = "#E3F2FD"; fg = "#1565C0"; break;
            case "Delivered":
                bg = "#E8F5E9"; fg = "#4A7C4E"; break;
            default:
                bg = "#F5F5F5"; fg = "#757575";
        }
        return "-fx-background-color: " + bg + "; -fx-text-fill: " + fg + "; " +
                "-fx-font-weight: bold; -fx-font-size: 11px; " +
                "-fx-padding: 3 8; -fx-background-radius: 10;";
    }

    private String smallBtn(String bg) {
        return "-fx-font-family: Sans Serif; -fx-font-size: 10px; -fx-text-fill: white; " +
                "-fx-background-color: " + bg + "; -fx-background-radius: 5; " +
                "-fx-cursor: hand; -fx-padding: 3 8;";
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private TableColumn<SalesOrderRow, String> strCol(String header, double minW,
                                                      java.util.function.Function<SalesOrderRow, String> extractor) {
        TableColumn<SalesOrderRow, String> col = new TableColumn<>(header);
        col.setMinWidth(minW);
        col.setCellValueFactory(cd -> new SimpleStringProperty(extractor.apply(cd.getValue())));
        col.setStyle("-fx-font-family: Sans Serif;");
        return col;
    }

    private Button actionBtn(String text, String bg, String hover) {
        Button btn = new Button(text);
        String base = btnStyle(bg);
        String hov  = btnStyle(hover);
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

    private void showSuccess(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Success"); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Error"); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
}