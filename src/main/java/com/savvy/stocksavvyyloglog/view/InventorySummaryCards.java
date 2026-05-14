package com.savvy.stocksavvyyloglog.view;

import com.savvy.stocksavvyyloglog.util.DatabaseConnection;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

import java.sql.*;

/**
 * InventorySummaryCards
 * ---------------------
 * Displays three summary cards at the top of the Inventory page:
 *   • Total Items
 *   • Total Stock Value  (Σ quantity × cost_per_unit)
 *   • Low-Stock Count
 *
 * Usage:
 *   InventorySummaryCards cards = new InventorySummaryCards();
 *   VBox pane = cards.build();   // add to your layout
 *   cards.refresh();             // call after any data change
 */
public class InventorySummaryCards {

    // ── Theme tokens (mirrors InventoryView) ──────────────────────────────────
    private static final String L_BG           = "#FDF5EC";
    private static final String L_CARD_BG      = "#FDFAF0";
    private static final String L_BORDER       = "#E8D8C0";
    private static final String L_ACCENT       = "#C04A10";
    private static final String L_ACCENT2      = "#C8A96E";
    private static final String L_TEXT         = "#2A1A08";
    private static final String L_TEXT_MUTED   = "#9E8050";
    private static final String L_LOW_STOCK_BG = "#FFF3F0";
    private static final String LOW_RED        = "#C04A10";
    private static final String OK_GREEN       = "#4A7C4E";
    private static final String BLUE           = "#3A6EA5";

    // ── Live label references so refresh() can update them ────────────────────
    private Label totalItemsValue;
    private Label totalValueValue;
    private Label lowStockValue;

    private HBox root;

    // ── Build ─────────────────────────────────────────────────────────────────
    public HBox build() {
        totalItemsValue = valueLabel("—", L_TEXT);
        totalValueValue = valueLabel("—", OK_GREEN);
        lowStockValue   = valueLabel("—", LOW_RED);

        HBox cards = new HBox(16,
                card("📦  Total Items",       totalItemsValue, BLUE,     "#EAF2FF"),
                card("💰  Total Stock Value",  totalValueValue, OK_GREEN, "#EDFAF0"),
                card("⚠  Low Stock Items",    lowStockValue,   LOW_RED,  L_LOW_STOCK_BG)
        );
        cards.setAlignment(Pos.CENTER_LEFT);

        root = cards;
        refresh();   // load real data immediately
        return cards;
    }

    // ── Refresh: re-query DB and update labels ────────────────────────────────
    public void refresh() {
        if (totalItemsValue == null) return;   // not built yet

        try (Connection conn = DatabaseConnection.getConnection();
             Statement  st   = conn.createStatement();
             ResultSet  rs   = st.executeQuery(
                     "SELECT COUNT(*) AS cnt, " +
                             "       SUM(quantity * cost_per_unit) AS total_val, " +
                             "       SUM(CASE WHEN quantity <= 10 THEN 1 ELSE 0 END) AS low_cnt " +
                             "FROM stocks")) {

            if (rs.next()) {
                int    cnt      = rs.getInt("cnt");
                double totalVal = rs.getDouble("total_val");
                int    lowCnt   = rs.getInt("low_cnt");

                totalItemsValue.setText(String.valueOf(cnt));
                totalValueValue.setText(String.format("₱%,.2f", totalVal));
                lowStockValue.setText(String.valueOf(lowCnt));

                // Pulse red if there are low-stock items
                lowStockValue.setStyle(labelValueStyle(lowCnt > 0 ? LOW_RED : OK_GREEN));
            }

        } catch (Exception e) {
            e.printStackTrace();
            totalItemsValue.setText("Err");
            totalValueValue.setText("Err");
            lowStockValue.setText("Err");
        }
    }

    // ── Card builder ──────────────────────────────────────────────────────────
    private VBox card(String title, Label valueLabel, String accentColor, String cardBg) {
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 11px; " +
                "-fx-font-weight: bold; -fx-text-fill: " + L_TEXT_MUTED + ";");

        // Colour bar on left
        Region bar = new Region();
        bar.setPrefWidth(4);
        bar.setMinWidth(4);
        bar.setStyle("-fx-background-color: " + accentColor + "; -fx-background-radius: 4 0 0 4;");

        VBox textBox = new VBox(4, titleLbl, valueLabel);
        textBox.setAlignment(Pos.CENTER_LEFT);
        textBox.setPadding(new Insets(12, 18, 12, 14));
        HBox.setHgrow(textBox, Priority.ALWAYS);

        HBox inner = new HBox(bar, textBox);
        inner.setAlignment(Pos.CENTER_LEFT);
        inner.setStyle("-fx-background-color: " + cardBg + "; " +
                "-fx-border-color: " + L_BORDER + "; -fx-border-width: 1; " +
                "-fx-background-radius: 8; -fx-border-radius: 8;");
        HBox.setHgrow(inner, Priority.ALWAYS);

        VBox wrapper = new VBox(inner);
        HBox.setHgrow(wrapper, Priority.ALWAYS);
        return wrapper;
    }

    // ── Label helpers ─────────────────────────────────────────────────────────
    private Label valueLabel(String text, String color) {
        Label lbl = new Label(text);
        lbl.setStyle(labelValueStyle(color));
        return lbl;
    }

    private String labelValueStyle(String color) {
        return "-fx-font-family: Sans Serif; -fx-font-size: 22px; " +
                "-fx-font-weight: bold; -fx-text-fill: " + color + ";";
    }
}