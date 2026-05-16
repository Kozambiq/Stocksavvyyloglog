package com.savvy.stocksavvyyloglog.dialog;

import com.savvy.stocksavvyyloglog.model.SaleDAO.SaleRow;
import com.savvy.stocksavvyyloglog.util.DatabaseConnection;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class ReceiptDialog {

    private final Stage stage;
    private final SaleRow row;
    private final String userRole;
    private Runnable onVoid;

    public ReceiptDialog(Stage owner, SaleRow row, String userRole) {
        this.stage = new Stage();
        this.row = row;
        this.userRole = userRole;
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.UNDECORATED);
    }

    public void setOnVoid(Runnable onVoid) {
        this.onVoid = onVoid;
    }

    // ── Theme tokens ──────────────────────────────────────────────────────────
    private static final String CREAM_BG     = "#FDF5EC";
    private static final String ACCENT       = "#C04A10";
    private static final String MUTED_LIGHT  = "#9E8050";
    private static final String TEXT_LIGHT   = "#2A1A08";

    public void show() {
        VBox root = new VBox(0);
        root.setPrefWidth(320);
        root.setStyle("-fx-background-color: " + CREAM_BG + "; -fx-padding: 24;");

        // Receipt Card
        VBox card = new VBox(16);
        card.setStyle("-fx-background-color: white; -fx-padding: 24; -fx-background-radius: 12; " +
                      "-fx-border-color: #E8D8C0; -fx-border-width: 1; -fx-border-radius: 12; " +
                      "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 2);");

        // Header
        Label title = new Label("RECEIPT #" + row.id);
        title.setFont(Font.font("Sans Serif", FontWeight.BOLD, 20));
        title.setTextFill(Color.web(ACCENT));
        
        // Info Section
        VBox info = new VBox(6);
        info.getChildren().addAll(
            infoRow("Date", row.saleDate),
            infoRow("Customer", row.customerName),
            infoRow("Payment", row.paymentMethod),
            infoRow("Status", row.status)
        );

        // Divider
        Region div1 = styledDivider();

        // Order details
        VBox details = new VBox(8);
        details.getChildren().addAll(
            detailRow("Product", row.productName),
            detailRow("Qty", String.format("%.0f", row.quantity)),
            new HBox(5, new Label("Total:"), new Label("₱" + String.format("%.2f", row.totalAmount))) {{
                ((Label)getChildren().get(0)).setStyle("-fx-font-weight: bold;");
                ((Label)getChildren().get(1)).setStyle("-fx-font-weight: bold; -fx-text-fill: " + ACCENT + ";");
            }}
        );

        // Divider
        Region div2 = styledDivider();

        // Actions
        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER);
        
        Button closeBtn = new Button("Close");
        closeBtn.setStyle("-fx-font-family: Sans Serif; -fx-background-color: #E8D8C0; -fx-padding: 8 20; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> stage.close());

        actions.getChildren().add(closeBtn);

        if ("Admin".equalsIgnoreCase(userRole)) {
            if ("Cancelled".equals(row.status)) {
                Button enableBtn = new Button("Enable");
                enableBtn.setStyle("-fx-font-family: Sans Serif; -fx-background-color: #4A7C4E; -fx-text-fill: white; -fx-padding: 8 20; -fx-cursor: hand;");
                enableBtn.setOnAction(e -> updateStatus("Preparing"));
                actions.getChildren().add(enableBtn);
            } else {
                Button voidBtn = new Button("Void Order");
                voidBtn.setStyle("-fx-font-family: Sans Serif; -fx-background-color: #D32F2F; -fx-text-fill: white; -fx-padding: 8 20; -fx-cursor: hand;");
                voidBtn.setOnAction(e -> updateStatus("Cancelled"));
                actions.getChildren().add(voidBtn);
            }
        }

        card.getChildren().addAll(title, info, div1, details, div2, actions);
        root.getChildren().add(card);
        
        stage.setScene(new Scene(root));
        stage.showAndWait();
    }

    private void updateStatus(String status) {
        com.savvy.stocksavvyyloglog.model.SaleDAO dao = new com.savvy.stocksavvyyloglog.model.SaleDAO();
        if (dao.updateStatus(row.id, status)) {
            if (onVoid != null) onVoid.run();
            stage.close();
        } else {
            new Alert(Alert.AlertType.ERROR, "Failed to update order status. Please check production stock levels if re-enabling.").show();
        }
    }

    private HBox infoRow(String label, String value) {
        Label lbl = new Label(label + ": ");
        lbl.setStyle("-fx-font-weight: bold; -fx-text-fill: " + MUTED_LIGHT + ";");
        return new HBox(lbl, new Label(value));
    }

    private HBox detailRow(String label, String value) {
        return new HBox(5, new Label(label + ":"), new Label(value));
    }

    private Region styledDivider() {
        Region div = new Region();
        div.setPrefHeight(1);
        div.setStyle("-fx-background-color: #E8D8C0;");
        return div;
    }
}
