package com.savvy.stocksavvyyloglog.app;

import com.savvy.stocksavvyyloglog.util.DatabaseConnection;
import com.savvy.stocksavvyyloglog.view.CalendarView;
import com.savvy.stocksavvyyloglog.view.InventoryView;
import com.savvy.stocksavvyyloglog.view.SalesViewEnhanced;
import com.savvy.stocksavvyyloglog.view.SalesOrderView;
import com.savvy.stocksavvyyloglog.view.UserManagementView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DashboardApplication {

    private static final java.util.logging.Logger LOGGER =
            java.util.logging.Logger.getLogger(DashboardApplication.class.getName());

    private Stage dashboardStage;
    private Stage stage;
    private final String currentUser;
    private final String currentRole;
    private BorderPane root;
    private Label clockLabel;

    // ── Theme colour tokens ──────────────────────────────────────────────────
    private static final String L_BG                 = "#FDF5EC";
    private static final String L_CARD_BG            = "#FDFAF0";
    private static final String L_CARD_BG2           = "#FBF4E8";
    private static final String L_BORDER             = "#E8D8C0";
    private static final String L_CARD_CELL          = "#FDF5EC";
    private static final String L_NAV_BG             = "#C04A10";
    private static final String L_NAV_BORDER         = "#C8A96E";
    private static final String L_ACCENT             = "#C04A10";
    private static final String L_ACCENT2            = "#C8A96E";
    private static final String L_TEXT               = "#2A1A08";
    private static final String L_TEXT_MUTED         = "#9E8050";
    private static final String L_FOOTER_TEXT        = "#E0C8A0";
    private static final String L_ALERT_BG           = "#FFF0D0";
    private static final String L_ALERT_TEXT         = "#8A6200";
    private static final String L_HEADER_BTN         = "#F0E8DC";
    private static final String L_HEADER_BTN_BORDER  = "#D8C8A8";

    // ── Colour helpers ───────────────────────────────────────────────────────
    private String bg()          { return L_BG;                }
    private String cardBg()      { return L_CARD_BG;           }
    private String cardBg2()     { return L_CARD_BG2;          }
    private String border()      { return L_BORDER;            }
    private String cardCell()    { return L_CARD_CELL;         }
    private String navBg()       { return L_NAV_BG;            }
    private String navBorder()   { return L_NAV_BORDER;        }
    private String accent()      { return L_ACCENT;            }
    private String accent2()     { return L_ACCENT2;           }
    private String textColor()   { return L_TEXT;              }
    private String textMuted()   { return L_TEXT_MUTED;        }
    private String footerText()  { return L_FOOTER_TEXT;       }
    private String alertBg()     { return L_ALERT_BG;          }
    private String alertText()   { return L_ALERT_TEXT;        }
    private String hdrBtn()      { return L_HEADER_BTN;        }
    private String hdrBtnBdr()   { return L_HEADER_BTN_BORDER; }

    public DashboardApplication(String username, String role) {
        this.currentUser = username;
        this.currentRole = role;
    }

    // ── Entry point ──────────────────────────────────────────────────────────
    public void show() {
        stage = new Stage();
        stage.setTitle("Stock Savy Longganisa - Dashboard");
        stage.setMinWidth(1100);
        stage.setMinHeight(650);
        this.dashboardStage = stage;

        root = new BorderPane();
        rebuildUI();

        Scene scene = new Scene(root, 1366, 768);
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.setMaximized(true);
        stage.show();

        startClock();
    }

    // ── Full build ───────────────────────────────────────────────────────────
    private void rebuildUI() {
        root.setStyle("-fx-background-color: " + bg() + ";");
        root.setTop(createNavbar());
        root.setCenter(createDashboardContent());
        root.setBottom(createFooter());
    }

    // ── Navbar ───────────────────────────────────────────────────────────────
    private HBox createNavbar() {
        HBox navbar = new HBox();
        navbar.setAlignment(Pos.CENTER_LEFT);
        navbar.setSpacing(0);
        navbar.setPrefHeight(65);
        navbar.setStyle(
                "-fx-background-color: " + navBg() + "; " +
                        "-fx-border-width: 0 0 3 0; " +
                        "-fx-border-color: " + navBorder() + ";"
        );

        Button myPageBtn    = createNavButton("MyPage",    true);
        Button inventoryBtn = createNavButton("Inventory", false);
        Button salesBtn     = createNavButton("Sales",     false);

        myPageBtn.setOnAction(e -> {
            setNavActive(myPageBtn, inventoryBtn, salesBtn);
            root.setCenter(createDashboardContent());
        });
        inventoryBtn.setOnAction(e -> {
            setNavActive(inventoryBtn, myPageBtn, salesBtn);
            new InventoryView(stage, root).show();
        });

        salesBtn.setOnAction(e -> {
            setNavActive(salesBtn, myPageBtn, inventoryBtn);
            new SalesViewEnhanced(stage, root, currentUser, currentRole).show();
        });

        HBox leftNav = new HBox(6);
        leftNav.setAlignment(Pos.CENTER_LEFT);
        leftNav.setPadding(new Insets(0, 0, 0, 24));
        leftNav.getChildren().addAll(myPageBtn, inventoryBtn, salesBtn);

        if ("Admin".equals(currentRole)) {
            Button usersBtn = createNavButton("\uD83D\uDEE1  Users", false);
            usersBtn.setOnAction(e -> new UserManagementView(stage, false).show());
            String adminNormal = "-fx-font-family: Sans Serif; -fx-font-size: 13px; " +
                    "-fx-text-fill: " + navBorder() + "; -fx-background-color: transparent; " +
                    "-fx-background-radius: 6px; -fx-border-width: 1; " +
                    "-fx-border-color: " + navBorder() + "; -fx-border-radius: 6px; -fx-cursor: hand;";
            String adminHover = adminNormal.replace("transparent", "rgba(200,169,110,0.15)");
            usersBtn.setStyle(adminNormal);
            usersBtn.setOnMouseEntered(e -> usersBtn.setStyle(adminHover));
            usersBtn.setOnMouseExited(e  -> usersBtn.setStyle(adminNormal));
            leftNav.getChildren().add(usersBtn);
        }

        VBox logoArea = createNavLogoText();

        Region leftSpacer  = new Region();
        Region rightSpacer = new Region();
        HBox.setHgrow(leftSpacer,  Priority.ALWAYS);
        HBox.setHgrow(rightSpacer, Priority.ALWAYS);

        Button signOutBtn = new Button("Sign Out");
        String soBase  = buildSignOutStyle(false);
        String soHover = buildSignOutStyle(true);
        signOutBtn.setStyle(soBase);
        signOutBtn.setOnMouseEntered(e -> signOutBtn.setStyle(soHover));
        signOutBtn.setOnMouseExited(e  -> signOutBtn.setStyle(soBase));
        signOutBtn.setOnAction(e -> signOut());

        // ── Right nav buttons ────────────────────────────────────────────────
        Button salesOrderBtn = createNavButton("Sales Order", false);
        Button receiptsBtn   = createNavButton("Receipts",    false);
        Button calendarBtn   = createNavButton("Calendar",    false);
        Button reportsBtn    = createNavButton("Reports",     false);

        // Wire up Sales Order
        salesOrderBtn.setOnAction(e ->
                new SalesOrderView(stage, root, currentUser, currentRole).show()
        );

        // ── Wire up Calendar → CalendarView ──────────────────────────────────
        calendarBtn.setOnAction(e ->
                new CalendarView(stage, root, currentUser).show()
        );

        HBox rightNav = new HBox(6);
        rightNav.setAlignment(Pos.CENTER_RIGHT);
        rightNav.setPadding(new Insets(0, 24, 0, 0));
        rightNav.getChildren().addAll(
                salesOrderBtn,
                receiptsBtn,
                calendarBtn,
                reportsBtn,
                signOutBtn
        );

        navbar.getChildren().addAll(leftNav, leftSpacer, logoArea, rightSpacer, rightNav);
        return navbar;
    }

    private void setNavActive(Button active, Button... others) {
        String activeStyle = "-fx-font-family: Sans Serif; -fx-font-size: 13px; " +
                "-fx-text-fill: #FDF5EC; -fx-background-color: rgba(255,255,255,0.15); " +
                "-fx-background-radius: 6px; -fx-border-width: 1; " +
                "-fx-border-color: rgba(255,255,255,0.3); -fx-border-radius: 6px; -fx-cursor: hand;";
        String normalStyle = "-fx-font-family: Sans Serif; -fx-font-size: 13px; " +
                "-fx-text-fill: #E0C8A0; -fx-background-color: transparent; " +
                "-fx-background-radius: 6px; -fx-border-width: 0; -fx-cursor: hand;";
        active.setStyle(activeStyle);
        for (Button btn : others) btn.setStyle(normalStyle);
    }

    private String buildSignOutStyle(boolean hover) {
        String bg  = hover ? "#D4470A" : "transparent";
        String bdr = hover ? "#D4470A" : navBorder();
        return "-fx-font-family: Sans Serif; -fx-font-size: 13px; -fx-text-fill: #FAF0E6; " +
                "-fx-background-color: " + bg + "; -fx-border-color: " + bdr + "; " +
                "-fx-border-width: 1px; -fx-border-radius: 6px; -fx-background-radius: 6px; " +
                "-fx-cursor: hand; -fx-padding: 6px 16px;";
    }

    private VBox createNavLogoText() {
        VBox container = new VBox(2);
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(0, 20, 0, 20));

        javafx.scene.text.TextFlow brandLine = new javafx.scene.text.TextFlow();
        brandLine.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        javafx.scene.text.Text stock = new javafx.scene.text.Text("Stock");
        stock.setFont(javafx.scene.text.Font.font("Sans Serif", javafx.scene.text.FontWeight.BOLD, 20));
        stock.setFill(Color.web("#FAF0E6"));

        javafx.scene.text.Text savy = new javafx.scene.text.Text("Savy");
        savy.setFont(javafx.scene.text.Font.font("Sans Serif", javafx.scene.text.FontWeight.BOLD, 20));
        savy.setFill(Color.web("#F5A05A"));

        brandLine.getChildren().addAll(stock, savy);

        HBox brandWrapper = new HBox(brandLine);
        brandWrapper.setAlignment(Pos.CENTER);

        javafx.scene.text.Text subtitle = new javafx.scene.text.Text("L O N G G A N I S A");
        subtitle.setFont(javafx.scene.text.Font.font("Sans Serif", javafx.scene.text.FontWeight.NORMAL, 7));
        subtitle.setFill(Color.web(navBorder()));

        container.getChildren().addAll(brandWrapper, subtitle);
        return container;
    }

    private Button createNavButton(String text, boolean isActive) {
        Button btn = new Button(text);
        btn.setPadding(new Insets(8, 18, 8, 18));
        String activeStyle = "-fx-font-family: Sans Serif; -fx-font-size: 13px; " +
                "-fx-text-fill: #FDF5EC; -fx-background-color: rgba(255,255,255,0.15); " +
                "-fx-background-radius: 6px; -fx-border-width: 1; " +
                "-fx-border-color: rgba(255,255,255,0.3); -fx-border-radius: 6px; -fx-cursor: hand;";
        String normalStyle = "-fx-font-family: Sans Serif; -fx-font-size: 13px; " +
                "-fx-text-fill: #E0C8A0; -fx-background-color: transparent; " +
                "-fx-background-radius: 6px; -fx-border-width: 0; -fx-cursor: hand;";
        String hoverStyle = "-fx-font-family: Sans Serif; -fx-font-size: 13px; " +
                "-fx-text-fill: #FDF5EC; -fx-background-color: rgba(255,255,255,0.1); " +
                "-fx-background-radius: 6px; -fx-border-width: 0; -fx-cursor: hand;";

        btn.setStyle(isActive ? activeStyle : normalStyle);
        if (!isActive) {
            btn.setOnMouseEntered(e -> btn.setStyle(hoverStyle));
            btn.setOnMouseExited(e  -> btn.setStyle(normalStyle));
        }
        return btn;
    }

    private void signOut() {
        try {
            Stage loginStage = new Stage();
            this.dashboardStage = stage;
            LoginApplication login = new LoginApplication();
            login.start(loginStage);
            dashboardStage.close();
        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "Error", e);
        }
    }

    // ── Dashboard content ────────────────────────────────────────────────────
    private ScrollPane createDashboardContent() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(28, 36, 28, 36));
        content.setStyle("-fx-background-color: " + bg() + ";");

        VBox alertBanner = createAlertBanner();
        if (alertBanner != null) content.getChildren().add(alertBanner);

        VBox expiringSoon = createExpiringSoonBanner();
        if (expiringSoon != null) content.getChildren().add(expiringSoon);

        content.getChildren().add(createWelcomeSection());
        content.getChildren().add(createStatCards());

        HBox bottomRow = new HBox(16);
        bottomRow.setMaxWidth(Double.MAX_VALUE);

        VBox calendar = createCalendarSection();
        HBox.setHgrow(calendar, Priority.ALWAYS);

        VBox rightPanel = createRightPanel();
        rightPanel.setPrefWidth(300);
        rightPanel.setMinWidth(280);

        bottomRow.getChildren().addAll(calendar, rightPanel);
        content.getChildren().add(bottomRow);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: " + bg() + "; -fx-background: " + bg() + "; -fx-border-width: 0;");
        return scroll;
    }

    // ── Alert banner (low stock) ─────────────────────────────────────────────
    private VBox createAlertBanner() {
        int lowStockCount = 0;
        try {
            String q = "SELECT COUNT(*) FROM stocks WHERE quantity <= 10";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(q);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) lowStockCount = rs.getInt(1);
            }
        } catch (Exception e) { LOGGER.log(java.util.logging.Level.SEVERE, "Error", e); }

        if (lowStockCount == 0) return null;

        VBox banner = new VBox();
        banner.setPadding(new Insets(12, 20, 12, 20));
        banner.setStyle(
                "-fx-background-color: " + alertBg() + "; " +
                        "-fx-border-color: " + accent2() + "; " +
                        "-fx-border-width: 0 0 0 5; " +
                        "-fx-background-radius: 6; -fx-border-radius: 6;"
        );
        Label alert = new Label("⚠  " + lowStockCount + " product(s) are below minimum stock level — please restock soon.");
        alert.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 13px; -fx-text-fill: " + alertText() + "; -fx-font-weight: bold;");
        banner.getChildren().add(alert);
        return banner;
    }

    // ── Expiring Soon banner ─────────────────────────────────────────────────
    private VBox createExpiringSoonBanner() {
        java.util.List<String[]> items = new java.util.ArrayList<>();
        try {
            try (Connection conn = DatabaseConnection.getConnection()) {
                conn.prepareStatement(
                        "ALTER TABLE stocks ADD COLUMN IF NOT EXISTS expiry_date DATE NULL"
                ).executeUpdate();
            } catch (Exception ignored) {}

            String sql = "SELECT product_name, expiry_date, quantity " +
                    "FROM stocks " +
                    "WHERE expiry_date IS NOT NULL " +
                    "  AND expiry_date BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 7 DAY) " +
                    "ORDER BY expiry_date ASC";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(new String[]{
                            rs.getString("product_name"),
                            rs.getString("expiry_date"),
                            String.valueOf(rs.getInt("quantity"))
                    });
                }
            }
        } catch (Exception e) { LOGGER.log(java.util.logging.Level.SEVERE, "Error", e); }

        if (items.isEmpty()) return null;

        VBox banner = new VBox(6);
        banner.setPadding(new Insets(12, 20, 12, 20));
        banner.setStyle(
                "-fx-background-color: #FFF3CD; " +
                        "-fx-border-color: #FFC107; " +
                        "-fx-border-width: 0 0 0 5; " +
                        "-fx-background-radius: 6; -fx-border-radius: 6;"
        );

        Label header = new Label("⏰  Expiring Soon — " + items.size() + " item(s) expire within 7 days");
        header.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #856404;");
        banner.getChildren().add(header);

        javafx.scene.layout.FlowPane pills = new javafx.scene.layout.FlowPane(8, 6);
        for (String[] item : items) {
            Label pill = new Label(item[0] + "  •  Exp: " + item[1] + "  •  Qty: " + item[2]);
            pill.setStyle(
                    "-fx-font-family: Sans Serif; -fx-font-size: 11px; -fx-text-fill: #856404; " +
                            "-fx-background-color: #FFE69C; -fx-background-radius: 20; -fx-padding: 3 10;"
            );
            pills.getChildren().add(pill);
        }
        banner.getChildren().add(pills);
        return banner;
    }

    private VBox createWelcomeSection() {
        int h = java.time.LocalTime.now().getHour();
        String greeting = h < 12 ? "Good morning" : h < 18 ? "Good afternoon" : "Good evening";

        Label greetLabel = new Label(greeting + ", " + currentUser + "! \uD83D\uDC4B");
        greetLabel.setStyle(
                "-fx-font-family: Sans Serif; -fx-font-size: 28px; " +
                        "-fx-font-weight: bold; -fx-text-fill: " + accent() + ";"
        );

        Label subtitle = new Label("• 100% PURE MEAT  •  " + currentRole + " Account");
        subtitle.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 12px; -fx-text-fill: " + accent2() + ";");

        VBox box = new VBox(4, greetLabel, subtitle);
        box.setStyle("-fx-border-width: 0 0 0 4; -fx-border-color: " + accent2() + "; -fx-padding: 0 0 0 12;");
        return box;
    }

    // ── Stat cards ────────────────────────────────────────────────────────────
    private HBox createStatCards() {
        HBox cards = new HBox(0);
        cards.setMaxWidth(Double.MAX_VALUE);

        String totalSales    = getTotalSalesThisMonth();
        String pendingOrders = getPendingOrdersCount();
        int    lowStockCount = getLowStockCountInt();
        String topProduct    = getTopProduct();

        VBox lowStockCard = createStatCard("⚠", "Low Stock Items",
                String.valueOf(lowStockCount), "#B85C00", false);
        lowStockCard.setCursor(javafx.scene.Cursor.HAND);
        lowStockCard.setOnMouseClicked(e -> showLowStockPopup());

        Label hint = new Label("Click to view items →");
        hint.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 10px; -fx-text-fill: #B85C00; -fx-opacity: 0.7;");
        lowStockCard.getChildren().add(hint);

        cards.getChildren().addAll(
                createStatCard("\uD83D\uDCB0", "Total Sales (May)", totalSales,    accent(),  true),
                createStatCard("\uD83D\uDED2", "Pending Orders",    pendingOrders, accent2(), false),
                lowStockCard,
                createStatCard("\uD83C\uDFC6", "Top Product",       topProduct,    "#4A7C4E", false)
        );
        return cards;
    }

    private VBox createStatCard(String icon, String title, String value,
                                String accentColor, boolean isFirst) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(20, 24, 20, 24));
        card.setMaxWidth(Double.MAX_VALUE);
        card.setMinHeight(120);
        HBox.setHgrow(card, Priority.ALWAYS);

        String borderStyle = isFirst
                ? "-fx-border-width: 0 0 0 5; -fx-border-color: " + accentColor + ";"
                : "-fx-border-width: 0 0 0 1; -fx-border-color: " + border() + ";";

        String shadow = "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 4, 0, 0, 2);";
        card.setStyle("-fx-background-color: " + cardBg() + "; " + borderStyle + shadow);

        Label iconLabel  = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 20px;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 12px; -fx-text-fill: " + textMuted() + ";");

        Label valueLabel = new Label(value);
        valueLabel.setStyle(
                "-fx-font-family: Sans Serif; " +
                        "-fx-font-size: " + (title.equals("Top Product") ? "16" : "26") + "px; " +
                        "-fx-font-weight: bold; -fx-text-fill: " + accentColor + ";"
        );

        card.getChildren().addAll(iconLabel, titleLabel, valueLabel);

        String hoverShadow = "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 8, 0, 0, 3);";
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: " + cardBg2() + "; " + borderStyle + hoverShadow));
        card.setOnMouseExited(e  -> card.setStyle("-fx-background-color: " + cardBg()  + "; " + borderStyle + shadow));

        return card;
    }

    // ── Low Stock popup dialog ────────────────────────────────────────────────
    private void showLowStockPopup() {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initOwner(stage);
        popup.setTitle("Low Stock Items");
        popup.setMinWidth(480);

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: " + bg() + ";");

        HBox header = new HBox();
        header.setPadding(new Insets(16, 20, 16, 20));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: #B85C00;");
        Label title = new Label("⚠  Low Stock Items");
        title.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: white;");
        header.getChildren().add(title);

        VBox body = new VBox(8);
        body.setPadding(new Insets(16, 20, 16, 20));

        try {
            String sql = "SELECT product_name, quantity " +
                    "FROM stocks WHERE quantity <= 10 ORDER BY quantity ASC";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                HBox colHeader = new HBox();
                colHeader.setPadding(new Insets(6, 10, 6, 10));
                colHeader.setStyle("-fx-background-color: #F0E0D0; -fx-background-radius: 4;");
                Label colProduct  = new Label("Product");
                Label colQty      = new Label("Current Qty");
                Label colStatus   = new Label("Status");
                colProduct.setPrefWidth(240);
                colQty.setPrefWidth(110);
                colStatus.setPrefWidth(100);
                for (Label l : new Label[]{colProduct, colQty, colStatus}) {
                    l.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #7A3A00;");
                }
                colHeader.getChildren().addAll(colProduct, colQty, colStatus);
                body.getChildren().add(colHeader);

                boolean hasRows = false;
                while (rs.next()) {
                    hasRows = true;
                    String name = rs.getString("product_name");
                    int    qty  = rs.getInt("quantity");
                    String status      = qty == 0 ? "Out of Stock" : "Low Stock";
                    String statusColor = qty == 0 ? "#C0392B" : "#B85C00";

                    HBox row = new HBox();
                    row.setPadding(new Insets(8, 10, 8, 10));
                    row.setStyle("-fx-background-color: white; -fx-background-radius: 4; " +
                            "-fx-border-color: #F0D8C0; -fx-border-width: 1; -fx-border-radius: 4;");

                    Label lName   = new Label(name);
                    Label lQty    = new Label(String.valueOf(qty));
                    Label lStatus = new Label(status);
                    lName.setPrefWidth(240);
                    lQty.setPrefWidth(110);
                    lStatus.setPrefWidth(100);

                    lName.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 12px; -fx-text-fill: " + textColor() + ";");
                    lQty.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #B85C00;");
                    lStatus.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + statusColor + ";");

                    row.getChildren().addAll(lName, lQty, lStatus);
                    body.getChildren().add(row);
                }

                if (!hasRows) {
                    Label empty = new Label("All items are sufficiently stocked.");
                    empty.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 12px; -fx-text-fill: " + textMuted() + ";");
                    body.getChildren().add(empty);
                }
            }
        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "Error", e);
            body.getChildren().add(new Label("Error loading low stock data."));
        }

        ScrollPane bodyScroll = new ScrollPane(body);
        bodyScroll.setFitToWidth(true);
        bodyScroll.setPrefHeight(320);
        bodyScroll.setStyle("-fx-background-color: " + bg() + "; -fx-background: " + bg() + "; -fx-border-width: 0;");

        Button closeBtn = new Button("Close");
        closeBtn.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 12px; -fx-text-fill: white; " +
                "-fx-background-color: #B85C00; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 8 20;");
        closeBtn.setOnAction(e -> popup.close());
        HBox footer = new HBox(closeBtn);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(12, 20, 12, 20));
        footer.setStyle("-fx-border-color: " + border() + "; -fx-border-width: 1 0 0 0;");

        root.getChildren().addAll(header, bodyScroll, footer);

        Scene popupScene = new Scene(root, 500, 420);
        popup.setScene(popupScene);
        popup.showAndWait();
    }

    // ── Right panel (Recent Activity + Low Stock list) ────────────────────────
    private VBox createRightPanel() {
        VBox panel = new VBox(14);

        VBox activitySection = new VBox(10);
        activitySection.setPadding(new Insets(16));
        activitySection.setStyle(
                "-fx-background-color: " + cardBg() + "; " +
                        "-fx-border-color: " + border() + "; -fx-border-width: 1; " +
                        "-fx-background-radius: 8; -fx-border-radius: 8;"
        );

        Label actTitle = new Label("\uD83D\uDD50  Recent Activity");
        actTitle.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + accent() + ";");

        VBox activityList = new VBox(6);
        try {
            String query =
                    "SELECT activity_type, description, activity_date FROM (" +
                            "  SELECT 'Sale' AS activity_type," +
                            "         CONCAT(p.name, ' x', si.quantity) AS description," +
                            "         s.sale_date AS activity_date" +
                            "  FROM sales_items si" +
                            "  JOIN products p ON si.product_id = p.id" +
                            "  JOIN sales s ON si.sale_id = s.id" +
                            "  UNION ALL" +
                            "  SELECT 'Stock In'," +
                            "         CONCAT(product_name, ' +', quantity)," +
                            "         created_at" +
                            "  FROM stock_in_log" +
                            "  UNION ALL" +
                            "  SELECT 'Order'," +
                            "         CONCAT(supplier_name, ' — ', status)," +
                            "         order_date" +
                            "  FROM purchase_orders" +
                            ") AS combined" +
                            " ORDER BY activity_date DESC LIMIT 8";

            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(query);
                 ResultSet rs = ps.executeQuery()) {

                boolean hasResults = false;
                while (rs.next()) {
                    hasResults = true;
                    String type = rs.getString("activity_type");
                    String desc = rs.getString("description");
                    String date = rs.getString("activity_date");
                    String icon = "Sale".equals(type) ? "\uD83D\uDCB0"
                            : "Stock In".equals(type) ? "\uD83D\uDCE6"
                              : "\uD83D\uDCCB";
                    activityList.getChildren().add(createActivityItem(icon, type, desc, date));
                }
                if (!hasResults) {
                    Label empty = new Label("No recent activity.");
                    empty.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 12px; -fx-text-fill: " + textMuted() + ";");
                    activityList.getChildren().add(empty);
                }
            }
        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "Error", e);
            try {
                String fallback = "SELECT p.name, si.quantity, s.sale_date " +
                        "FROM sales_items si " +
                        "JOIN products p ON si.product_id = p.id " +
                        "JOIN sales s ON si.sale_id = s.id " +
                        "ORDER BY s.sale_date DESC LIMIT 8";
                try (Connection conn = DatabaseConnection.getConnection();
                     PreparedStatement ps = conn.prepareStatement(fallback);
                     ResultSet rs = ps.executeQuery()) {
                    boolean hasResults = false;
                    while (rs.next()) {
                        hasResults = true;
                        activityList.getChildren().add(createActivityItem(
                                "\uD83D\uDCB0", "Sale",
                                rs.getString("name") + " x" + rs.getInt("quantity"),
                                rs.getString("sale_date")));
                    }
                    if (!hasResults) {
                        Label empty = new Label("No recent activity.");
                        empty.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 12px; -fx-text-fill: " + textMuted() + ";");
                        activityList.getChildren().add(empty);
                    }
                }
            } catch (Exception ex) {
                LOGGER.log(java.util.logging.Level.SEVERE, "Error", ex);
                Label err = new Label("Could not load activity.");
                err.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 12px; -fx-text-fill: " + textMuted() + ";");
                activityList.getChildren().add(err);
            }
        }

        activitySection.getChildren().addAll(actTitle, activityList);

        VBox lowStockSection = new VBox(8);
        lowStockSection.setPadding(new Insets(14));
        lowStockSection.setStyle(
                "-fx-background-color: #FFF8F0; " +
                        "-fx-border-color: #F0C080; -fx-border-width: 1; " +
                        "-fx-background-radius: 8; -fx-border-radius: 8;"
        );

        HBox lowStockHeader = new HBox(8);
        lowStockHeader.setAlignment(Pos.CENTER_LEFT);
        Label lowStockTitle = new Label("\uD83D\uDD34  Stock Overview");
        lowStockTitle.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #B85C00;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button viewAllBtn = new Button("View All");
        viewAllBtn.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 10px; -fx-text-fill: #B85C00; " +
                "-fx-background-color: #FFDDB0; -fx-background-radius: 10; -fx-cursor: hand; -fx-padding: 2 8;");
        viewAllBtn.setOnAction(e -> showLowStockPopup());
        lowStockHeader.getChildren().addAll(lowStockTitle, spacer, viewAllBtn);

        VBox lowStockList = new VBox(5);
        try {
            String sql = "SELECT product_name, quantity " +
                    "FROM stocks WHERE quantity <= 10 ORDER BY quantity ASC LIMIT 5";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                boolean hasItems = false;
                while (rs.next()) {
                    hasItems = true;
                    String name = rs.getString("product_name");
                    int    qty  = rs.getInt("quantity");

                    HBox row = new HBox(6);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.setPadding(new Insets(6, 10, 6, 10));
                    row.setStyle("-fx-background-color: white; -fx-background-radius: 4; " +
                            "-fx-border-color: #F0D8C0; -fx-border-width: 1; -fx-border-radius: 4;");

                    Label nameLbl = new Label(name);
                    nameLbl.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 11px; -fx-text-fill: " + textColor() + ";");
                    nameLbl.setMaxWidth(150);
                    nameLbl.setWrapText(false);

                    Region rowSpacer = new Region();
                    HBox.setHgrow(rowSpacer, Priority.ALWAYS);

                    String qtyText  = qty == 0 ? "Out of stock" : qty + " left";
                    String qtyColor = qty == 0 ? "#C0392B" : "#B85C00";
                    Label qtyLbl = new Label(qtyText);
                    qtyLbl.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 11px; " +
                            "-fx-font-weight: bold; -fx-text-fill: " + qtyColor + ";");

                    row.getChildren().addAll(nameLbl, rowSpacer, qtyLbl);
                    lowStockList.getChildren().add(row);
                }

                if (!hasItems) {
                    Label none = new Label("All items are sufficiently stocked ✓");
                    none.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 11px; -fx-text-fill: #4A7C4E;");
                    lowStockList.getChildren().add(none);
                }
            }
        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "Error", e);
            lowStockList.getChildren().add(new Label("Error loading data."));
        }

        lowStockSection.getChildren().addAll(lowStockHeader, lowStockList);
        panel.getChildren().addAll(activitySection, lowStockSection);
        return panel;
    }

    private HBox createActivityItem(String icon, String type, String desc, String date) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(7, 10, 7, 10));
        row.setStyle("-fx-background-color: " + cardCell() + "; -fx-background-radius: 6; -fx-border-radius: 6;");

        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size: 14px;");

        VBox info = new VBox(2);
        Label descLbl = new Label(desc);
        descLbl.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + textColor() + ";");
        descLbl.setWrapText(false);

        String typeColor = "Sale".equals(type) ? "#4A7C4E"
                : "Stock In".equals(type) ? "#C04A10"
                  : "#5A5A8A";
        Label typeDateLbl = new Label("[" + type + "]  •  " + (date != null ? date : ""));
        typeDateLbl.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 10px; -fx-text-fill: " + typeColor + ";");
        info.getChildren().addAll(descLbl, typeDateLbl);

        row.getChildren().addAll(iconLbl, info);
        return row;
    }

    // ── DB helpers ────────────────────────────────────────────────────────────
    private String getTotalSalesThisMonth() {
        String query = "SELECT COALESCE(SUM(total_amount), 0) FROM sales " +
                "WHERE MONTH(sale_date) = MONTH(CURDATE()) AND YEAR(sale_date) = YEAR(CURDATE())";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return String.format("₱%.2f", rs.getDouble(1));
        } catch (Exception e) { LOGGER.log(java.util.logging.Level.SEVERE, "Error", e); }
        return "₱0.00";
    }

    private String getPendingOrdersCount() {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM sales_orders WHERE status = 'Pending'");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return String.valueOf(rs.getInt(1));
        } catch (Exception e) { LOGGER.log(java.util.logging.Level.SEVERE, "Error", e); }
        return "0";
    }

    private int getLowStockCountInt() {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM stocks WHERE quantity <= 10");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) { LOGGER.log(java.util.logging.Level.SEVERE, "Error", e); }
        return 0;
    }

    private String getTopProduct() {
        String query = "SELECT p.name FROM sales_items si " +
                "JOIN products p ON si.product_id = p.id " +
                "GROUP BY si.product_id ORDER BY SUM(si.quantity) DESC LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getString(1);
        } catch (Exception e) { LOGGER.log(java.util.logging.Level.SEVERE, "Error", e); }
        return "N/A";
    }

    // ── Calendar (dashboard mini-preview) ─────────────────────────────────────
    private java.time.YearMonth currentMonth = java.time.YearMonth.now();
    private Label monthLabel;
    private VBox calendarGridContainer = new VBox();

    private VBox createCalendarSection() {
        VBox section = new VBox(12);
        section.setStyle(
                "-fx-background-color: " + cardBg() + "; " +
                        "-fx-border-width: 1; -fx-border-color: " + border() + "; " +
                        "-fx-background-radius: 8; -fx-border-radius: 8;"
        );
        section.setPadding(new Insets(20));

        HBox calHeader = new HBox(10);
        calHeader.setAlignment(Pos.CENTER_LEFT);

        Label calTitle = new Label("\uD83D\uDCC5  Delivery & Production Schedule");
        calTitle.setStyle(
                "-fx-font-family: Sans Serif; -fx-font-size: 14px; " +
                        "-fx-font-weight: bold; -fx-text-fill: " + textColor() + ";"
        );

        Region calSpacer = new Region();
        HBox.setHgrow(calSpacer, Priority.ALWAYS);

        // ── "Open Full Calendar" shortcut button ──────────────────────────────
        Button openCalBtn = new Button("Open Full Calendar →");
        openCalBtn.setStyle(
                "-fx-font-family: Sans Serif; -fx-font-size: 11px; " +
                        "-fx-text-fill: " + accent() + "; -fx-background-color: transparent; " +
                        "-fx-border-color: " + accent() + "; -fx-border-width: 1; " +
                        "-fx-background-radius: 4; -fx-border-radius: 4; -fx-cursor: hand; -fx-padding: 4 10;"
        );
        openCalBtn.setOnMouseEntered(e -> openCalBtn.setStyle(
                "-fx-font-family: Sans Serif; -fx-font-size: 11px; " +
                        "-fx-text-fill: white; -fx-background-color: " + accent() + "; " +
                        "-fx-border-color: " + accent() + "; -fx-border-width: 1; " +
                        "-fx-background-radius: 4; -fx-border-radius: 4; -fx-cursor: hand; -fx-padding: 4 10;"
        ));
        openCalBtn.setOnMouseExited(e -> openCalBtn.setStyle(
                "-fx-font-family: Sans Serif; -fx-font-size: 11px; " +
                        "-fx-text-fill: " + accent() + "; -fx-background-color: transparent; " +
                        "-fx-border-color: " + accent() + "; -fx-border-width: 1; " +
                        "-fx-background-radius: 4; -fx-border-radius: 4; -fx-cursor: hand; -fx-padding: 4 10;"
        ));
        openCalBtn.setOnAction(e -> new CalendarView(stage, root, currentUser).show());

        calHeader.getChildren().addAll(calTitle, calSpacer, openCalBtn);

        calendarGridContainer = new VBox();
        section.getChildren().addAll(calHeader, createCalendarNav(), createCalendarGrid());
        return section;
    }

    private HBox createCalendarNav() {
        HBox nav = new HBox(10);
        nav.setAlignment(Pos.CENTER_LEFT);

        String navBtnStyle = "-fx-font-size: 11px; -fx-background-color: " + hdrBtn() + "; " +
                "-fx-border-color: " + hdrBtnBdr() + "; -fx-border-width: 1; " +
                "-fx-background-radius: 4; -fx-border-radius: 4; -fx-cursor: hand; -fx-padding: 5 10; " +
                "-fx-text-fill: " + textColor() + ";";

        Button prevBtn  = new Button("◄");
        Button nextBtn  = new Button("►");
        Button todayBtn = new Button("Today");
        prevBtn.setStyle(navBtnStyle);
        nextBtn.setStyle(navBtnStyle);
        todayBtn.setStyle(navBtnStyle);

        monthLabel = new Label(currentMonth.getMonth().getDisplayName(
                java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH) + " " + currentMonth.getYear());
        monthLabel.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + textColor() + ";");

        prevBtn.setOnAction(e  -> { currentMonth = currentMonth.minusMonths(1); refreshCalendar(); });
        nextBtn.setOnAction(e  -> { currentMonth = currentMonth.plusMonths(1);  refreshCalendar(); });
        todayBtn.setOnAction(e -> { currentMonth = java.time.YearMonth.now();   refreshCalendar(); });

        nav.getChildren().addAll(prevBtn, monthLabel, nextBtn, todayBtn);
        return nav;
    }

    private void refreshCalendar() {
        monthLabel.setText(currentMonth.getMonth().getDisplayName(
                java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH) + " " + currentMonth.getYear());
        calendarGridContainer.getChildren().setAll(buildCalendarGrid());
    }

    private VBox createCalendarGrid() {
        calendarGridContainer.getChildren().setAll(buildCalendarGrid());
        return calendarGridContainer;
    }

    private GridPane buildCalendarGrid() {
        GridPane grid = new GridPane();
        grid.setMaxWidth(Double.MAX_VALUE);

        String[] days = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        for (int i = 0; i < 7; i++) {
            Label header = new Label(days[i]);
            header.setMaxWidth(Double.MAX_VALUE);
            header.setAlignment(Pos.CENTER);
            header.setPadding(new Insets(10));
            header.setStyle(
                    "-fx-font-family: Sans Serif; -fx-font-size: 12px; " +
                            "-fx-font-weight: bold; -fx-text-fill: #FAF0E6; " +
                            "-fx-background-color: #C04A10;"
            );
            GridPane.setHgrow(header, Priority.ALWAYS);
            grid.add(header, i, 0);
        }

        java.util.Map<Integer, java.util.List<String[]>> events = loadEventsForMonth();

        java.time.LocalDate firstDay    = currentMonth.atDay(1);
        int startDow    = firstDay.getDayOfWeek().getValue() % 7;
        int daysInMonth = currentMonth.lengthOfMonth();
        java.time.LocalDate today = java.time.LocalDate.now();

        String todayCellBg = "rgba(192,74,16,0.10)";
        String cellBg      = "#FDFAF0";
        String emptyBg     = "#F7F2E8";

        int day = 1;
        for (int row = 1; row <= 6 && day <= daysInMonth; row++) {
            for (int col = 0; col < 7; col++) {
                if (row == 1 && col < startDow) {
                    Label empty = new Label();
                    empty.setMaxWidth(Double.MAX_VALUE);
                    empty.setMinHeight(80);
                    empty.setStyle("-fx-border-color: " + border() + "; -fx-border-width: 0.5; -fx-background-color: " + emptyBg + ";");
                    GridPane.setHgrow(empty, Priority.ALWAYS);
                    grid.add(empty, col, row);
                    continue;
                }
                if (day > daysInMonth) break;

                boolean isToday = currentMonth.getYear()  == today.getYear()
                        && currentMonth.getMonth() == today.getMonth()
                        && day == today.getDayOfMonth();

                VBox cell = new VBox(3);
                cell.setMaxWidth(Double.MAX_VALUE);
                cell.setMinHeight(80);
                cell.setPadding(new Insets(6));
                cell.setStyle(
                        "-fx-border-color: " + border() + "; -fx-border-width: 0.5; " +
                                "-fx-background-color: " + (isToday ? todayCellBg : cellBg) + ";"
                );

                StackPane dayHeader = new StackPane();
                Label dayNum = new Label(String.valueOf(day));
                String dayNumStyle = "-fx-font-family: Sans Serif; -fx-font-size: 11px; -fx-text-fill: "
                        + (isToday ? accent() : textColor()) + ";"
                        + (isToday ? " -fx-font-weight: bold;" : "");
                dayNum.setStyle(dayNumStyle);
                dayHeader.setAlignment(Pos.CENTER_LEFT);
                dayHeader.getChildren().add(dayNum);
                cell.getChildren().add(dayHeader);

                if (events.containsKey(day)) {
                    for (String[] evt : events.get(day)) {
                        String pillColor = getEventColor(evt[1]);
                        Label evtLabel = new Label(evt[0]);
                        evtLabel.setWrapText(true);
                        evtLabel.setMaxWidth(Double.MAX_VALUE);
                        evtLabel.setStyle(
                                "-fx-font-family: Sans Serif; -fx-font-size: 9px; -fx-text-fill: white; " +
                                        "-fx-background-color: " + pillColor + "; " +
                                        "-fx-background-radius: 3; -fx-padding: 2 5;"
                        );
                        cell.getChildren().add(evtLabel);
                    }
                }

                GridPane.setHgrow(cell, Priority.ALWAYS);
                grid.add(cell, col, row);
                day++;
            }
        }
        return grid;
    }

    private String getEventColor(String type) {
        if (type == null) return "#C04A10";
        switch (type) {
            case "Delivery":   return "#4A7C4E";
            case "Production": return "#C8A96E";
            case "Holiday":    return "#B85C00";
            default:           return "#6B7080";
        }
    }

    private java.util.Map<Integer, java.util.List<String[]>> loadEventsForMonth() {
        java.util.Map<Integer, java.util.List<String[]>> events = new java.util.HashMap<>();
        try {
            java.sql.Connection conn = DatabaseConnection.getConnection();
            String query = "SELECT DAY(event_date) as day, title, event_type FROM schedule " +
                    "WHERE YEAR(event_date) = ? AND MONTH(event_date) = ?";
            java.sql.PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, currentMonth.getYear());
            stmt.setInt(2, currentMonth.getMonthValue());
            java.sql.ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                int d      = rs.getInt("day");
                String ttl = rs.getString("title");
                String typ = rs.getString("event_type");
                events.computeIfAbsent(d, k -> new java.util.ArrayList<>()).add(new String[]{ttl, typ});
            }
            conn.close();
        } catch (Exception e) { LOGGER.log(java.util.logging.Level.SEVERE, "Error", e); }
        return events;
    }

    // ── Footer ────────────────────────────────────────────────────────────────
    private HBox createFooter() {
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(8, 24, 8, 24));
        footer.setStyle(
                "-fx-background-color: " + navBg() + "; " +
                        "-fx-border-width: 2 0 0 0; -fx-border-color: " + navBorder() + ";"
        );

        Label left = new Label("Stock Savy Longganisa  •  © 2026");
        left.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 11px; -fx-text-fill: " + footerText() + ";");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        clockLabel = new Label();
        clockLabel.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 11px; -fx-text-fill: " + accent2() + "; -fx-font-weight: bold;");

        Label right = new Label("  •  Logged in as: " + currentUser + " (" + currentRole + ")");
        right.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 11px; -fx-text-fill: " + footerText() + ";");

        footer.getChildren().addAll(left, spacer, clockLabel, right);
        return footer;
    }

    // ── Clock ─────────────────────────────────────────────────────────────────
    private void startClock() {
        javafx.animation.Timeline clock = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), e -> {
                    if (clockLabel != null) {
                        clockLabel.setText(LocalDateTime.now().format(
                                DateTimeFormatter.ofPattern("EEEE, MMMM d yyyy  •  hh:mm:ss a")));
                    }
                })
        );
        clock.setCycleCount(javafx.animation.Animation.INDEFINITE);
        clock.play();
    }
}