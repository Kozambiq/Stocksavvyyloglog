package com.savvy.stocksavvyyloglog.util;

import com.savvy.stocksavvyyloglog.view.InventoryView;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * InventoryExporter
 * -----------------
 * Handles CSV export of the inventory table data.
 *
 * Usage (from InventoryView):
 *   InventoryExporter exporter = new InventoryExporter(stage);
 *   exporter.exportToCSV(filteredRows);   // pass the currently visible rows
 *
 * The user is prompted to choose a save location via a file picker.
 * The resulting CSV includes a header row and one row per StockRow.
 */
public class InventoryExporter {

    private final Stage ownerStage;

    public InventoryExporter(Stage ownerStage) {
        this.ownerStage = ownerStage;
    }

    /**
     * Opens a Save-file dialog, then writes the given rows to a CSV file.
     *
     * @param rows the list of StockRow objects currently shown in the table
     */
    public void exportToCSV(ObservableList<? extends InventoryView.StockRow> rows) {
        // Default filename includes today's date
        String defaultName = "inventory_"
                + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                + ".csv";

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Inventory CSV");
        chooser.setInitialFileName(defaultName);
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));

        File file = chooser.showSaveDialog(ownerStage);
        if (file == null) return;   // user cancelled

        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            // Header
            pw.println("Product Name,Category,Quantity,Unit,Cost Per Unit,Supplier,Date Received,Notes");

            // Rows
            for (InventoryView.StockRow row : rows) {
                pw.println(
                        csvEscape(row.getProductName())  + "," +
                                csvEscape(row.getCategory())     + "," +
                                csvEscape(row.getQuantity())     + "," +
                                csvEscape(row.getUnit())         + "," +
                                csvEscape(row.getCostPerUnit())  + "," +
                                csvEscape(row.getSupplier())     + "," +
                                csvEscape(row.getDateReceived()) + "," +
                                csvEscape(row.getNotes())
                );
            }

            showInfo("Export Successful",
                    "Exported " + rows.size() + " row(s) to:\n" + file.getAbsolutePath());

        } catch (IOException e) {
            e.printStackTrace();
            showError("Export Failed", "Could not write file:\n" + e.getMessage());
        }
    }

    // ── CSV helpers ───────────────────────────────────────────────────────────

    /**
     * Wraps a value in double-quotes and escapes any existing double-quotes
     * so the output is valid RFC 4180 CSV.
     */
    private String csvEscape(String value) {
        if (value == null) return "\"\"";
        // Escape existing double-quotes by doubling them
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    // ── Alert helpers ─────────────────────────────────────────────────────────

    private void showInfo(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private void showError(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}