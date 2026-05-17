package com.savvy.stocksavvyyloglog.view;

import com.savvy.stocksavvyyloglog.model.SaleDAO;
import com.savvy.stocksavvyyloglog.model.StockDAO;
import com.savvy.stocksavvyyloglog.model.Stock;
import com.savvy.stocksavvyyloglog.util.ChatbotService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.List;

public class ReportsView {

    private static final String L_BG         = "#FDF5EC";
    private static final String L_CARD_BG   = "#FDFAF0";
    private static final String L_CARD_BG2  = "#FBF4E8";
    private static final String L_BORDER    = "#E8D8C0";
    private static final String L_ACCENT     = "#C04A10";
    private static final String L_ACCENT2    = "#C8A96E";
    private static final String L_TEXT        = "#2A1A08";
    private static final String L_TEXT_MUTED = "#9E8050";

    private final BorderPane rootPane;
    private final String currentUser;
    private final SaleDAO saleDAO = new SaleDAO();
    private final StockDAO stockDAO = new StockDAO();
    private final ChatbotService chatbotService = new ChatbotService();

    private VBox chatHistoryBox;
    private ScrollPane chatScrollPane;

    public ReportsView(BorderPane rootPane, String currentUser) {
        this.rootPane = rootPane;
        this.currentUser = currentUser;
    }

    public void show() {
        rootPane.setCenter(buildView());
    }

    private ScrollPane buildView() {
        VBox mainContent = new VBox(24);
        mainContent.setPadding(new Insets(28, 36, 28, 36));
        mainContent.setStyle("-fx-background-color: " + L_BG + ";");

        mainContent.getChildren().add(createPageHeader());
        mainContent.getChildren().add(createSummaryCards());
        mainContent.getChildren().add(createChatbotBox());

        ScrollPane scroll = new ScrollPane(mainContent);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: " + L_BG + "; -fx-background: " + L_BG + "; -fx-border-width: 0;");
        return scroll;
    }

    private HBox createPageHeader() {
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("\uD83D\uDCCA  Reports & Analytics");
        title.setStyle(
                "-fx-font-family: Sans Serif; -fx-font-size: 20px; " +
                        "-fx-font-weight: bold; -fx-text-fill: " + L_TEXT + ";"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label userLabel = new Label("\uD83D\uDC64 " + currentUser);
        userLabel.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 12px; -fx-text-fill: " + L_TEXT_MUTED + ";");

        header.getChildren().addAll(title, spacer, userLabel);
        return header;
    }

    private HBox createSummaryCards() {
        SaleDAO.SaleSummary saleSummary = saleDAO.getSummary();
        
        // Calculate raw material cost (sum of cost * quantity for all stocks)
        double totalMaterialCost = stockDAO.getAllStocks().stream()
                .mapToDouble(s -> s.getCostPerUnit() * s.getQuantity())
                .sum();

        // Get top material usage (based on stock in logs or just most frequent stock item)
        // For simplicity, we'll use the item with the highest total value
        String topMaterial = stockDAO.getAllStocks().stream()
                .max((s1, s2) -> Double.compare(s1.getQuantity() * s1.getCostPerUnit(), s2.getQuantity() * s2.getCostPerUnit()))
                .map(Stock::getProductName)
                .orElse("N/A");

        HBox cards = new HBox(0);
        cards.setMaxWidth(Double.MAX_VALUE);

        VBox totalSalesCard = createStatCard(
                "\uD83D\uDCB0",
                "Total Sales",
                String.format("₱%.2f", saleSummary.totalRevenue),
                L_ACCENT,
                true
        );

        VBox rawMaterialCostCard = createStatCard(
                "\uD83D\uDCE6",
                "Raw Material Value",
                String.format("₱%.2f", totalMaterialCost),
                "#B85C00",
                false
        );

        VBox bestSellingCard = createStatCard(
                "\uD83C\uDFC6",
                "Best Selling Produce",
                saleSummary.topProduct,
                "#4A7C4E",
                false
        );

        VBox topMaterialCard = createStatCard(
                "\uD83D\uDCA1",
                "Most Valued Material",
                topMaterial,
                "#5A5A8A",
                false
        );

        cards.getChildren().addAll(
                totalSalesCard,
                rawMaterialCostCard,
                bestSellingCard,
                topMaterialCard
        );

        return cards;
    }

    private VBox createStatCard(String icon, String title, String value, String accentColor, boolean isFirst) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(20, 24, 20, 24));
        card.setMaxWidth(Double.MAX_VALUE);
        card.setMinHeight(130);
        HBox.setHgrow(card, Priority.ALWAYS);

        String borderStyle = isFirst
                ? "-fx-border-width: 0 0 0 5; -fx-border-color: " + accentColor + ";"
                : "-fx-border-width: 0 0 0 1; -fx-border-color: " + L_BORDER + ";";

        String shadow = "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 4, 0, 0, 2);";
        card.setStyle("-fx-background-color: " + L_CARD_BG + "; " + borderStyle + shadow);

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 20px;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 12px; -fx-text-fill: " + L_TEXT_MUTED + ";");

        Label valueLabel = new Label(value);
        valueLabel.setStyle(
                "-fx-font-family: Sans Serif; " +
                        "-fx-font-size: " + (title.equals("Best Selling Produce") || title.equals("Most Valued Material") ? "16" : "26") + "px; " +
                        "-fx-font-weight: bold; -fx-text-fill: " + accentColor + ";"
        );

        card.getChildren().addAll(iconLabel, titleLabel, valueLabel);

        String hoverShadow = "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 8, 0, 0, 3);";
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: " + L_CARD_BG2 + "; " + borderStyle + hoverShadow));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: " + L_CARD_BG + "; " + borderStyle + shadow));

        return card;
    }

    private VBox createChatbotBox() {
        VBox container = new VBox(12);
        container.setStyle(
                "-fx-background-color: " + L_CARD_BG + "; " +
                        "-fx-border-color: " + L_BORDER + "; -fx-border-width: 1; " +
                        "-fx-background-radius: 12; -fx-border-radius: 12;"
        );
        container.setPadding(new Insets(20));
        container.setMaxWidth(Double.MAX_VALUE);
        container.setMinHeight(400);

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label botIcon = new Label("\uD83E\uDD14");
        botIcon.setStyle("-fx-font-size: 20px;");

        Label botTitle = new Label("Business Insights Assistant");
        botTitle.setStyle(
                "-fx-font-family: Sans Serif; -fx-font-size: 14px; " +
                        "-fx-font-weight: bold; -fx-text-fill: " + L_TEXT + ";"
        );

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        Label statusDot = new Label("\u25CF");
        statusDot.setStyle("-fx-font-size: 10px; -fx-text-fill: #4A7C4E;");

        Label statusText = new Label("Online");
        statusText.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 11px; -fx-text-fill: " + L_TEXT_MUTED + ";");

        header.getChildren().addAll(botIcon, botTitle, headerSpacer, statusDot, statusText);

        chatHistoryBox = new VBox(10);
        chatHistoryBox.setStyle("-fx-background-color: white;");
        chatHistoryBox.setPadding(new Insets(10));

        Label welcomeMsg = new Label("Hello! I'm your Business Insights Assistant. Ask me anything about your sales, inventory, or production.");
        welcomeMsg.setWrapText(true);
        welcomeMsg.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 13px; -fx-text-fill: " + L_TEXT + ";");
        addChatMessage("assistant", welcomeMsg.getText());

        chatScrollPane = new ScrollPane(chatHistoryBox);
        chatScrollPane.setFitToWidth(true);
        chatScrollPane.setPrefHeight(300);
        chatScrollPane.setStyle("-fx-background-color: white; -fx-border-color: " + L_BORDER + "; -fx-border-radius: 8; -fx-background-radius: 8;");
        
        HBox inputRow = new HBox(10);
        inputRow.setAlignment(Pos.CENTER_LEFT);

        TextField inputField = new TextField();
        inputField.setPromptText("Type your question here...");
        inputField.setStyle(
                "-fx-font-family: Sans Serif; " +
                        "-fx-font-size: 13px; " +
                        "-fx-text-fill: " + L_TEXT + "; " +
                        "-fx-background-color: white; " +
                        "-fx-border-color: " + L_BORDER + "; " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 6; " +
                        "-fx-background-radius: 6; " +
                        "-fx-padding: 10 14;"
        );
        inputField.setPrefHeight(40);
        HBox.setHgrow(inputField, Priority.ALWAYS);

        Button sendBtn = new Button("Send");
        sendBtn.setStyle(
                "-fx-font-family: Sans Serif; " +
                        "-fx-font-size: 13px; " +
                        "-fx-text-fill: white; " +
                        "-fx-background-color: " + L_ACCENT + "; " +
                        "-fx-background-radius: 6; " +
                        "-fx-cursor: hand; " +
                        "-fx-padding: 10 20;"
        );

        Button clearBtn = new Button("Clear");
        clearBtn.setStyle(
                "-fx-font-family: Sans Serif; " +
                        "-fx-font-size: 12px; " +
                        "-fx-text-fill: " + L_TEXT_MUTED + "; " +
                        "-fx-background-color: transparent; " +
                        "-fx-border-color: " + L_BORDER + "; " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 6; " +
                        "-fx-background-radius: 6; " +
                        "-fx-cursor: hand; " +
                        "-fx-padding: 8 14;"
        );

        // Actions
        sendBtn.setOnAction(e -> handleSendMessage(inputField));
        inputField.setOnAction(e -> handleSendMessage(inputField));
        
        clearBtn.setOnAction(e -> {
            chatHistoryBox.getChildren().clear();
            chatbotService.clearHistory();
            addChatMessage("assistant", "Chat history cleared. How can I help you today?");
        });

        inputRow.getChildren().addAll(inputField, sendBtn, clearBtn);

        container.getChildren().addAll(header, chatScrollPane, inputRow);
        return container;
    }

    private void handleSendMessage(TextField inputField) {
        String msg = inputField.getText().trim();
        if (msg.isEmpty()) return;

        inputField.clear();
        addChatMessage("user", msg);

        // Show typing indicator
        Label typingLabel = new Label("Assistant is thinking...");
        typingLabel.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 12px; -fx-text-fill: " + L_TEXT_MUTED + "; -fx-font-style: italic;");
        chatHistoryBox.getChildren().add(typingLabel);
        scrollToBottom();

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return chatbotService.askChatbot(msg);
            }
        };

        task.setOnSucceeded(e -> {
            chatHistoryBox.getChildren().remove(typingLabel);
            addChatMessage("assistant", task.getValue());
            scrollToBottom();
        });

        task.setOnFailed(e -> {
            chatHistoryBox.getChildren().remove(typingLabel);
            addChatMessage("assistant", "Sorry, I encountered an error: " + task.getException().getMessage());
            scrollToBottom();
            task.getException().printStackTrace();
        });

        new Thread(task).start();
    }

    private void addChatMessage(String role, String content) {
        VBox messageBox = new VBox(5);
        messageBox.setPadding(new Insets(8, 12, 8, 12));
        messageBox.setMaxWidth(Double.MAX_VALUE);

        Label roleLabel = new Label(role.equalsIgnoreCase("user") ? "You" : "Assistant");
        roleLabel.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + L_TEXT_MUTED + ";");

        Label contentLabel = new Label(content);
        contentLabel.setWrapText(true);
        contentLabel.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 13px; -fx-text-fill: " + L_TEXT + ";");

        if (role.equalsIgnoreCase("user")) {
            messageBox.setStyle("-fx-background-color: #F0F0F0; -fx-background-radius: 8;");
            messageBox.setAlignment(Pos.TOP_LEFT);
        } else {
            messageBox.setStyle("-fx-background-color: #E8F0FE; -fx-background-radius: 8;");
            messageBox.setAlignment(Pos.TOP_LEFT);
        }

        messageBox.getChildren().addAll(roleLabel, contentLabel);
        chatHistoryBox.getChildren().add(messageBox);
    }

    private void scrollToBottom() {
        Platform.runLater(() -> chatScrollPane.setVvalue(1.0));
    }
}