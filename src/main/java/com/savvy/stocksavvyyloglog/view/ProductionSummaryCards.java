package com.savvy.stocksavvyyloglog.view;

import com.savvy.stocksavvyyloglog.util.DatabaseConnection;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

import java.sql.*;

public class ProductionSummaryCards {

    private static final String L_TEXT_MUTED   = "#9E8050";
    private static final String L_BORDER       = "#E8D8C0";
    private static final String L_TEXT         = "#2A1A08";
    private static final String OK_GREEN       = "#4A7C4E";
    private static final String BLUE           = "#3A6EA5";
    private static final String LOW_RED        = "#C04A10";

    private Label totalProductsValue;
    private Label lowStockValue;

    public HBox build() {
        totalProductsValue = valueLabel("—", L_TEXT);
        lowStockValue      = valueLabel("—", LOW_RED);

        HBox cards = new HBox(16,
                card("📦  Total Products",    totalProductsValue, BLUE,     "#EAF2FF"),
                card("⚠  Low Stock Items",    lowStockValue,      LOW_RED,  "#FFF3F0")
        );
        cards.setAlignment(Pos.CENTER_LEFT);
        refresh();
        return cards;
    }

    public void refresh() {
        if (totalProductsValue == null) return;
        try (Connection conn = DatabaseConnection.getConnection();
             Statement  st   = conn.createStatement();
             ResultSet  rs   = st.executeQuery(
                     "SELECT COUNT(*) AS cnt, " +
                             "       SUM(CASE WHEN status = 'Low Stock' THEN 1 ELSE 0 END) AS low_cnt " +
                             "FROM productions")) {
            if (rs.next()) {
                totalProductsValue.setText(String.valueOf(rs.getInt("cnt")));
                lowStockValue.setText(String.valueOf(rs.getInt("low_cnt")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private VBox card(String title, Label valueLabel, String accentColor, String cardBg) {
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + L_TEXT_MUTED + ";");

        Region bar = new Region();
        bar.setPrefWidth(4);
        bar.setMinWidth(4);
        bar.setStyle("-fx-background-color: " + accentColor + "; -fx-background-radius: 4 0 0 4;");

        VBox textBox = new VBox(4, titleLbl, valueLabel);
        textBox.setPadding(new Insets(12, 18, 12, 14));
        HBox inner = new HBox(bar, textBox);
        inner.setStyle("-fx-background-color: " + cardBg + "; -fx-border-color: " + L_BORDER + "; -fx-border-width: 1; -fx-background-radius: 8; -fx-border-radius: 8;");
        return new VBox(inner);
    }

    private Label valueLabel(String text, String color) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        return lbl;
    }
}
