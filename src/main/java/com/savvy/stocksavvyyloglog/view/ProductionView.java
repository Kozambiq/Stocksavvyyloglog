package com.savvy.stocksavvyyloglog.view;

import com.savvy.stocksavvyyloglog.model.Production;
import com.savvy.stocksavvyyloglog.model.ProductionDAO;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

public class ProductionView {

    private static final String L_BG      = "#FDF5EC";
    private static final String L_CARD_BG = "#FDFAF0";
    private static final String L_BORDER  = "#E8D8C0";
    private static final String L_ACCENT  = "#C04A10";
    private static final String L_TEXT    = "#2A1A08";

    private final BorderPane dashboardRoot;
    private TableView<ProductionRow> table;
    private ObservableList<ProductionRow> allRows;
    private ProductionDAO dao = new ProductionDAO();
    private com.savvy.stocksavvyyloglog.model.StockDAO stockDAO = new com.savvy.stocksavvyyloglog.model.StockDAO();

    public static class ProductionRow {
        private final int id;
        private final SimpleStringProperty name;
        private final SimpleStringProperty quantity;
        private final SimpleStringProperty price;
        private final SimpleStringProperty unit;
        private final SimpleStringProperty status;

        public ProductionRow(Production p) {
            this.id = p.getId();
            this.name = new SimpleStringProperty(p.getName());
            this.quantity = new SimpleStringProperty(String.format("%.2f", p.getQuantity()));
            this.price = new SimpleStringProperty(String.format("₱%.2f", p.getPrice()));
            this.unit = new SimpleStringProperty(p.getUnit());
            this.status = new SimpleStringProperty(p.getStatus());
        }

        public int getId() { return id; }
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
        search.setPrefWidth(260);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addBtn = new Button("➕ Add Produce");
        addBtn.setStyle("-fx-font-size: 13px; -fx-background-color: " + L_ACCENT + "; -fx-text-fill: white; -fx-padding: 8 16; -fx-cursor: hand; -fx-background-radius: 6;");
        addBtn.setOnAction(e -> showAddDialog());

        Button deleteBtn = new Button("🗑 Delete");
        deleteBtn.setStyle("-fx-font-size: 13px; -fx-background-color: #D32F2F; -fx-text-fill: white; -fx-padding: 8 16; -fx-cursor: hand; -fx-background-radius: 6;");
        deleteBtn.setOnAction(e -> handleDelete());

        return new HBox(10, search, spacer, addBtn, deleteBtn);
    }

    private void handleDelete() {
        ProductionRow selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            new Alert(Alert.AlertType.WARNING, "Please select a row to delete.").show();
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete " + selected.getName() + "?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(response -> {
            if (confirm.getResult() == ButtonType.YES) {
                if (dao.deleteProduction(selected.getId())) {
                    loadData();
                    summaryCards.refresh();
                } else {
                    new Alert(Alert.AlertType.ERROR, "Failed to delete from database.").show();
                }
            }
        });
    }


    private TableView<ProductionRow> buildTable() {
        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(600);
        
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

    private void showAddDialog() {
        javafx.stage.Stage dialog = new javafx.stage.Stage();
        dialog.initOwner(dashboardRoot.getScene().getWindow());
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.initStyle(javafx.stage.StageStyle.UNDECORATED);

        VBox root = new VBox();
        root.setStyle("-fx-background-color: " + L_BG + "; -fx-border-color: " + L_BORDER + "; -fx-border-width: 1; -fx-border-radius: 12; -fx-background-radius: 12;");

        // Header
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 20, 14, 20));
        header.setStyle("-fx-background-color: " + L_ACCENT + "; -fx-background-radius: 12 12 0 0;");
        Label title = new Label("🏭  Add Produce");
        title.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: white;");
        header.getChildren().add(title);

        // Body
        VBox body = new VBox(14);
        body.setPadding(new Insets(20));
        
        Label dialogTitle = new Label("Add New Produce");
        dialogTitle.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + L_TEXT + ";");
        
        TextField fName = styledField("Produce Name *");
        TextField fQty = styledField("Quantity *");
        TextField fPrice = styledField("Price (₱) *");
        TextField fUnit = styledField("Unit (e.g. kg, pcs) *");
        Label errName = errorLabel("Required"), errQty = errorLabel("Must be > 0"), errPrice = errorLabel("Must be > 0"), errUnit = errorLabel("No special chars");

        ComboBox<String> fSource = new ComboBox<>();
        fSource.setPromptText("Select Ingredient");
        fSource.setPrefWidth(200);
        loadInventoryItems(fSource);
        TextField fDeductQty = styledField("Amount to Deduct");
        fDeductQty.setPrefWidth(100);
        
        VBox ingredientsList = new VBox(5);
        java.util.List<IngredientEntry> selectedIngredients = new java.util.ArrayList<>();
        
        Label errIng = errorLabel("Invalid amount or stock");

        Button addIngredientBtn = new Button("OK");
        addIngredientBtn.setStyle("-fx-background-color: " + L_ACCENT + "; -fx-text-fill: white;");
        addIngredientBtn.setOnAction(e -> {
            String source = fSource.getValue();
            try {
                double deduct = Double.parseDouble(fDeductQty.getText());
                if (source == null || deduct <= 0 || !stockDAO.hasEnoughStock(source, deduct)) {
                    errIng.setVisible(true); return;
                }
                errIng.setVisible(false);
                IngredientEntry entry = new IngredientEntry(source, deduct);
                selectedIngredients.add(entry);
                HBox row = new HBox(10);
                Label ingLbl = new Label(source + " (" + deduct + ")");
                ingLbl.setStyle("-fx-text-fill: " + L_TEXT + ";");
                row.getChildren().addAll(ingLbl, createRemoveBtn(row, entry, selectedIngredients, ingredientsList));
                ingredientsList.getChildren().add(row);
                fDeductQty.clear(); // Clear deduct field after adding
            } catch (Exception ex) { errIng.setVisible(true); }
        });

        body.getChildren().addAll(
                dialogTitle,
                dialogField("Produce Name *", fName), errName,
                dialogField("Quantity *", fQty), errQty,
                dialogField("Price (₱) *", fPrice), errPrice,
                dialogField("Unit *", fUnit), errUnit,
                new Separator(),
                new Label("Ingredients:") {{ setStyle("-fx-text-fill: " + L_TEXT + "; -fx-font-weight: bold;"); }},
                new HBox(5, fSource, fDeductQty, addIngredientBtn), errIng,
                ingredientsList
        );
        
        ScrollPane bodyScroll = new ScrollPane(body);
        bodyScroll.setFitToWidth(true);
        bodyScroll.setPrefHeight(400);
        bodyScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        // Footer
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(14, 20, 14, 20));
        footer.setStyle("-fx-background-color: #FDF0E8; -fx-border-width: 1 0 0 0; -fx-border-color: " + L_BORDER + "; -fx-background-radius: 0 0 12 12;");

        Button saveBtn = dialogBtn("Save", true);
        saveBtn.setOnAction(e -> {
            boolean valid = true;
            if (fName.getText().isEmpty()) { errName.setVisible(true); valid = false; } else errName.setVisible(false);
            try { if (Double.parseDouble(fQty.getText()) <= 0) throw new Exception(); errQty.setVisible(false); } catch (Exception ex) { errQty.setVisible(true); valid = false; }
            try { if (Double.parseDouble(fPrice.getText()) <= 0) throw new Exception(); errPrice.setVisible(false); } catch (Exception ex) { errPrice.setVisible(true); valid = false; }
            if (!fUnit.getText().matches("^[a-zA-Z0-9]+$")) { errUnit.setVisible(true); valid = false; } else errUnit.setVisible(false);

            if (!valid) return;

            try {
                String name = fName.getText().trim().toUpperCase();
                String unit = fUnit.getText().trim().toUpperCase();

                com.savvy.stocksavvyyloglog.model.Production p = new com.savvy.stocksavvyyloglog.model.Production(
                        name, Double.parseDouble(fQty.getText()), Double.parseDouble(fPrice.getText()), unit, "In Stock"
                );
                
                java.util.List<ProductionDAO.IngredientDeduction> deductions = new java.util.ArrayList<>();
                for (IngredientEntry ing : selectedIngredients) {
                    deductions.add(new ProductionDAO.IngredientDeduction(ing.name, ing.qty));
                }

                if (dao.saveProductionWithIngredients(p, deductions)) {
                    loadData();
                    summaryCards.refresh();
                    dialog.close();
                }
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "Save Failed").show();
            }
        });

        Button cancelBtn = dialogBtn("Cancel", false);
        cancelBtn.setOnAction(e -> dialog.close());

        footer.getChildren().addAll(cancelBtn, saveBtn);
        root.getChildren().addAll(header, bodyScroll, footer);

        Scene scene = new Scene(root, 400, 500);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        dialog.setScene(scene);
        dialog.show();
    }

    private Button createRemoveBtn(HBox row, IngredientEntry entry, java.util.List<IngredientEntry> list, VBox container) {
        Button btn = new Button("X");
        btn.setStyle("-fx-text-fill: red; -fx-background-color: transparent;");
        btn.setOnAction(e -> {
            container.getChildren().remove(row);
            list.remove(entry);
        });
        return btn;
    }

    private static class IngredientEntry {
        String name; double qty;
        IngredientEntry(String name, double qty) { this.name = name; this.qty = qty; }
    }

    private Label errorLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: red; -fx-font-size: 10px;");
        lbl.setVisible(false);
        return lbl;
    }

    private TextField styledField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 13px; -fx-background-color: #FDF8F4; -fx-border-color: " + L_BORDER + "; -fx-border-radius: 7; -fx-padding: 7 10;");
        return tf;
    }

    private VBox dialogField(String label, javafx.scene.Node input) {
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + L_TEXT + ";");
        return new VBox(5, lbl, input);
    }

    private Button dialogBtn(String text, boolean primary) {
        Button btn = new Button(text);
        btn.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 13px; " + 
                     (primary ? "-fx-background-color: " + L_ACCENT + "; -fx-text-fill: white;" : "-fx-background-color: transparent; -fx-border-color: " + L_BORDER + "; -fx-text-fill: " + L_TEXT + ";") + 
                     " -fx-background-radius: 7; -fx-padding: 8 20; -fx-cursor: hand;");
        return btn;
    }


    private void applyFilter(String query) {
        filteredRows.setPredicate(row -> {
            if (query == null || query.isEmpty()) return true;
            return row.getName().toLowerCase().contains(query.toLowerCase());
        });
    }

    private void loadInventoryItems(ComboBox<String> cb) {
        for (com.savvy.stocksavvyyloglog.model.Stock s : stockDAO.getAllStocks()) {
            cb.getItems().add(s.getProductName());
        }
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
