package com.savvy.stocksavvyyloglog.view;

import com.savvy.stocksavvyyloglog.model.ProductionDAO;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

import java.sql.*;

public class ProductionSummaryCards {

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
    private static final String PURPLE         = "#6B5B95";

    private Label totalProductionsValue;
    private Label inProgressValue;
    private Label totalQuantityValue;

    private HBox root;

    public HBox build() {
        totalProductionsValue = valueLabel("—", BLUE);
        inProgressValue       = valueLabel("—", LOW_RED);
        totalQuantityValue    = valueLabel("—", OK_GREEN);

        HBox cards = new HBox(16,
                card("📋  Total Batches",      totalProductionsValue, BLUE,     "#EAF2FF"),
                card("⚙️  In Progress",       inProgressValue,       LOW_RED,  L_LOW_STOCK_BG),
                card("📦  Total Produced",     totalQuantityValue,    OK_GREEN, "#EDFAF0")
        );
        cards.setAlignment(Pos.CENTER_LEFT);

        root = cards;
        refresh();
        return cards;
    }

    public void refresh() {
        if (totalProductionsValue == null) return;

        ProductionDAO dao = new ProductionDAO();

        int totalBatches = dao.getTotalProductions();
        int inProgress = dao.getInProgressCount();
        double totalQty = dao.getTotalQuantityProduced();

        totalProductionsValue.setText(String.valueOf(totalBatches));
        inProgressValue.setText(String.valueOf(inProgress));
        totalQuantityValue.setText(String.format("%.0f", totalQty));

        inProgressValue.setStyle(labelValueStyle(inProgress > 0 ? LOW_RED : OK_GREEN));
    }

    private VBox card(String title, Label valueLabel, String accentColor, String cardBg) {
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 11px; " +
                "-fx-font-weight: bold; -fx-text-fill: " + L_TEXT_MUTED + ";");

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