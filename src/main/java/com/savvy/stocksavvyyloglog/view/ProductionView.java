package com.savvy.stocksavvyyloglog.view;

import com.savvy.stocksavvyyloglog.model.Production;
import com.savvy.stocksavvyyloglog.model.ProductionDAO;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

public class ProductionView {

    private static final String L_BG      = "#FDF5EC";
    private static final String L_CARD_BG = "#FDFAF0";
    private static final String L_BORDER  = "#E8D8C0";
    private static final String L_ACCENT  = "#C04A10";

    private final BorderPane dashboardRoot;
    private TableView<ProductionRow> table;
    private ObservableList<ProductionRow> allRows;
    private ProductionDAO dao = new ProductionDAO();

    public static class ProductionRow {
        private final SimpleStringProperty name;
        private final SimpleStringProperty quantity;
        private final SimpleStringProperty price;
        private final SimpleStringProperty unit;
        private final SimpleStringProperty status;

        public ProductionRow(Production p) {
            this.name = new SimpleStringProperty(p.getName());
            this.quantity = new SimpleStringProperty(String.format("%.2f", p.getQuantity()));
            this.price = new SimpleStringProperty(String.format("₱%.2f", p.getPrice()));
            this.unit = new SimpleStringProperty(p.getUnit());
            this.status = new SimpleStringProperty(p.getStatus());
        }

        public String getName() { return name.get(); }
        public String getQuantity() { return quantity.get(); }
        public String getPrice() { return price.get(); }
        public String getUnit() { return unit.get(); }
        public String getStatus() { return status.get(); }
    }

    public ProductionView(BorderPane dashboardRoot) {
        this.dashboardRoot = dashboardRoot;
    }

    private ProductionSummaryCards summaryCards;
    private FilteredList<ProductionRow> filteredRows;

    public void show() {
        dashboardRoot.setCenter(buildPane());
    }

    private ScrollPane buildPane() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(28, 36, 28, 36));
        content.setStyle("-fx-background-color: " + L_BG + ";");

        summaryCards = new ProductionSummaryCards();
        content.getChildren().addAll(buildHeader(), summaryCards.build(), buildToolbar(), buildTable());

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        return scroll;
    }

    private VBox buildHeader() {
        Label title = new Label("🏭  Production Management");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: " + L_ACCENT + ";");
        return new VBox(title);
    }

    private HBox buildToolbar() {
        TextField search = new TextField();
        search.setPromptText("🔍 Search products...");
        search.textProperty().addListener((obs, old, nv) -> applyFilter(nv));
        return new HBox(10, search);
    }

    private TableView<ProductionRow> buildTable() {
        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        table.getColumns().add(col("Produce Name", "name"));
        table.getColumns().add(col("Qty", "quantity"));
        table.getColumns().add(col("Price", "price"));
        table.getColumns().add(col("Unit", "unit"));
        table.getColumns().add(col("Status", "status"));

        allRows = FXCollections.observableArrayList();
        filteredRows = new FilteredList<>(allRows, p -> true);
        loadData();
        table.setItems(filteredRows);
        return table;
    }

    private void applyFilter(String query) {
        filteredRows.setPredicate(row -> {
            if (query == null || query.isEmpty()) return true;
            return row.getName().toLowerCase().contains(query.toLowerCase());
        });
    }


    private TableColumn<ProductionRow, String> col(String title, String prop) {
        TableColumn<ProductionRow, String> col = new TableColumn<>(title);
        col.setCellValueFactory(new PropertyValueFactory<>(prop));
        return col;
    }

    private void loadData() {
        allRows.clear();
        for (Production p : dao.getAllProductions()) {
            allRows.add(new ProductionRow(p));
        }
    }
}
