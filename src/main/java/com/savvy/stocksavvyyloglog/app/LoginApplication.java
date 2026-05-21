package com.savvy.stocksavvyyloglog.app;

import com.savvy.stocksavvyyloglog.util.DatabaseConnection;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.paint.CycleMethod;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.prefs.Preferences;

public class LoginApplication extends Application {

    // ── Preferences keys ──────────────────────────────────────────────────────
    private static final Preferences PREFS         = Preferences.userNodeForPackage(LoginApplication.class);
    private static final String      PREF_USER     = "saved_username";
    private static final String      PREF_PASS     = "saved_password";
    private static final String      PREF_REMEMBER = "stay_signed_in";

    // ── Theme tokens ───────────────────────────────────────────────────────────
    private static final String ACCENT       = "#C04A10";
    private static final String ACCENT_DARK  = "#A03A0A";
    private static final String CREAM_BG     = "#FDF5EC";
    private static final String CARD_LIGHT   = "#FFFFFF";
    private static final String BORDER_LIGHT = "#E8D8C0";
    private static final String TEXT_LIGHT   = "#2A1A08";
    private static final String MUTED_LIGHT  = "#9E8050";

    // ── Live references ────────────────────────────────────────────────────────
    private VBox          rightPanel;
    private VBox          card;
    private TextField     usernameField;
    private PasswordField passwordField;
    private TextField     passwordVisible;
    private CheckBox      staySignedIn;
    private Label         usernameLabel;
    private Label         passwordLabel;
    private Text          welcome;
    private Text          subText;
    private Text          copyright;
    private Label         errorLabel;
    private Button        signInBtn;

    @Override
    public void start(Stage stage) {
        stage.setTitle("StockSavy Longganisa \u2014 Login");
        stage.setResizable(false);

        HBox root = new HBox();
        root.setPrefSize(920, 580);

        StackPane leftPanel = buildLeftPanel();
        rightPanel          = buildRightPanel(stage);

        HBox.setHgrow(leftPanel,  Priority.ALWAYS);
        HBox.setHgrow(rightPanel, Priority.ALWAYS);
        leftPanel.setMinWidth(400);
        rightPanel.setMinWidth(520);

        root.getChildren().addAll(leftPanel, rightPanel);

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();

        animatePanel(leftPanel,  -30);
        animatePanel(rightPanel,  30);

        // ── Pre-fill fields if Stay Signed In was checked last time ───────────
        boolean remember = PREFS.getBoolean(PREF_REMEMBER, false);
        if (remember) {
            String savedUser = PREFS.get(PREF_USER, "");
            String savedPass = PREFS.get(PREF_PASS, "");

            if (!savedUser.isEmpty() && !savedPass.isEmpty()) {
                usernameField.setText(savedUser);
                passwordField.setText(savedPass);
                passwordVisible.setText(savedPass);
                staySignedIn.setSelected(true);
            }
        }
    }

    // ── Left Panel ────────────────────────────────────────────────────────────
    private StackPane buildLeftPanel() {
        StackPane pane = new StackPane();
        pane.setPrefWidth(400);

        Rectangle bg = new Rectangle();
        bg.widthProperty().bind(pane.widthProperty());
        bg.heightProperty().bind(pane.heightProperty());
        bg.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web("#C04A10")),
                new Stop(0.5, Color.web("#A03A0A")),
                new Stop(1.0, Color.web("#6B2206"))));

        Circle c1 = circle(180, "#FAF0E6", 0.07);
        StackPane.setAlignment(c1, Pos.TOP_RIGHT);
        StackPane.setMargin(c1, new Insets(-60, -60, 0, 0));

        Circle c2 = circle(120, "#FAF0E6", 0.05);
        StackPane.setAlignment(c2, Pos.BOTTOM_LEFT);
        StackPane.setMargin(c2, new Insets(0, 0, -40, -40));

        Circle c3 = circle(60, "#FAF0E6", 0.08);
        StackPane.setAlignment(c3, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(c3, new Insets(0, 40, 80, 0));

        VBox brand = new VBox(16);
        brand.setAlignment(Pos.CENTER_LEFT);
        brand.setPadding(new Insets(0, 48, 0, 52));

        Text appName = new Text("StockSavy");
        appName.setFont(Font.font("Sans Serif", FontWeight.BOLD, 38));
        appName.setFill(Color.web("#FAF0E6"));

        Text subtitle = new Text("LONGGANISA");
        subtitle.setFont(Font.font("Sans Serif", FontWeight.BOLD, 12));
        subtitle.setFill(Color.web("#FAF0E6", 0.65));

        Rectangle divider = new Rectangle(48, 2);
        divider.setFill(Color.web("#C8A96E"));
        divider.setArcWidth(2);
        divider.setArcHeight(2);

        VBox bullets = new VBox(10);
        bullets.getChildren().addAll(
                bullet("Track stock in real-time"),
                bullet("Monitor sales & revenue"),
                bullet("Manage suppliers & deliveries")
        );

        brand.getChildren().addAll(appName, subtitle, divider, bullets);

        Text footer = new Text("Stock Savy Longganisa \u00A9 2026");
        footer.setFont(Font.font("Sans Serif", 11));
        footer.setFill(Color.web("#FAF0E6", 0.40));
        StackPane.setAlignment(footer, Pos.BOTTOM_CENTER);
        StackPane.setMargin(footer, new Insets(0, 0, 20, 0));

        pane.getChildren().addAll(bg, c1, c2, c3, brand, footer);
        StackPane.setAlignment(brand, Pos.CENTER_LEFT);
        return pane;
    }

    private HBox bullet(String text) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        Circle dot = new Circle(4);
        dot.setFill(Color.web("#C8A96E"));
        Text label = new Text(text);
        label.setFont(Font.font("Sans Serif", 13));
        label.setFill(Color.web("#FAF0E6", 0.80));
        row.getChildren().addAll(dot, label);
        return row;
    }

    private Circle circle(double r, String hex, double opacity) {
        Circle c = new Circle(r);
        c.setFill(Color.web(hex, opacity));
        return c;
    }

    // ── Right Panel ───────────────────────────────────────────────────────────
    private VBox buildRightPanel(Stage stage) {
        VBox pane = new VBox(0);
        pane.setPrefWidth(520);
        pane.setStyle("-fx-background-color: " + CREAM_BG + ";");

        // ── Card ──────────────────────────────────────────────────────────────
        card = new VBox(0);
        card.setMaxWidth(360);
        card.setPadding(new Insets(36, 40, 32, 40));
        card.setStyle(
                "-fx-background-color: " + CARD_LIGHT + "; " +
                        "-fx-background-radius: 16; " +
                        "-fx-border-color: " + BORDER_LIGHT + "; " +
                        "-fx-border-width: 1; -fx-border-radius: 16;"
        );

        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.web("#2A1A08", 0.10));
        shadow.setRadius(28);
        shadow.setOffsetY(6);
        card.setEffect(shadow);

        welcome = new Text("Welcome back");
        welcome.setFont(Font.font("Sans Serif", FontWeight.BOLD, 24));
        welcome.setFill(Color.web(TEXT_LIGHT));

        subText = new Text("Sign in to your account");
        subText.setFont(Font.font("Sans Serif", 13));
        subText.setFill(Color.web(MUTED_LIGHT));

        VBox cardHeader = new VBox(4, welcome, subText);
        cardHeader.setPadding(new Insets(0, 0, 22, 0));

        // Username
        usernameLabel = fieldLabel("USERNAME");
        usernameField = new TextField();
        usernameField.setPromptText("Enter username");
        applyFieldStyle(usernameField);
        VBox usernameBox = new VBox(6, usernameLabel, usernameField);
        usernameBox.setPadding(new Insets(0, 0, 14, 0));

        // Password
        passwordLabel = fieldLabel("PASSWORD");
        passwordField = new PasswordField();
        passwordField.setPromptText("Enter password");
        applyFieldStyle(passwordField);

        passwordVisible = new TextField();
        passwordVisible.setPromptText("Enter password");
        applyFieldStyle(passwordVisible);
        passwordVisible.setVisible(false);
        passwordVisible.setManaged(false);

        passwordField.textProperty().addListener((o, ov, nv) -> {
            if (!passwordVisible.isFocused()) passwordVisible.setText(nv);
        });
        passwordVisible.textProperty().addListener((o, ov, nv) -> {
            if (!passwordField.isFocused()) passwordField.setText(nv);
        });

        Button eyeBtn = new Button("\uD83D\uDC41");
        eyeBtn.setStyle("-fx-background-color: transparent; -fx-border-width: 0; " +
                "-fx-cursor: hand; -fx-font-size: 14px; -fx-padding: 0 6 0 4;");
        final boolean[] showing = {false};
        eyeBtn.setOnAction(e -> {
            showing[0] = !showing[0];
            passwordField.setVisible(!showing[0]);
            passwordField.setManaged(!showing[0]);
            passwordVisible.setVisible(showing[0]);
            passwordVisible.setManaged(showing[0]);
        });

        StackPane pwStack = new StackPane(passwordField, passwordVisible, eyeBtn);
        StackPane.setAlignment(eyeBtn, Pos.CENTER_RIGHT);
        StackPane.setMargin(eyeBtn, new Insets(0, 2, 0, 0));

        VBox passwordBox = new VBox(6, passwordLabel, pwStack);
        passwordBox.setPadding(new Insets(0, 0, 14, 0));

        // ── Stay Signed In checkbox ───────────────────────────────────────────
        staySignedIn = new CheckBox("Stay Signed In");
        staySignedIn.setSelected(PREFS.getBoolean(PREF_REMEMBER, false));
        staySignedIn.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 12px; -fx-text-fill: " + MUTED_LIGHT + ";");

        Label savedBadge = new Label("\u2714 Credentials saved");
        savedBadge.setStyle(
                "-fx-font-family: Sans Serif; -fx-font-size: 10px; " +
                        "-fx-text-fill: #4A7C4E; -fx-font-weight: bold;"
        );
        savedBadge.setVisible(PREFS.getBoolean(PREF_REMEMBER, false) && !PREFS.get(PREF_USER, "").isEmpty());

        staySignedIn.selectedProperty().addListener((o, ov, nv) -> {
            if (!nv) {
                PREFS.remove(PREF_USER);
                PREFS.remove(PREF_PASS);
                PREFS.putBoolean(PREF_REMEMBER, false);
                savedBadge.setVisible(false);
            }
        });

        HBox checkRow = new HBox(10, staySignedIn, savedBadge);
        checkRow.setAlignment(Pos.CENTER_LEFT);
        checkRow.setPadding(new Insets(0, 0, 20, 0));

        // Error label
        errorLabel = new Label();
        errorLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #D32F2F; -fx-font-family: Sans Serif;");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        errorLabel.setPadding(new Insets(0, 0, 8, 0));

        // ── Sign In button ────────────────────────────────────────────────────
        signInBtn = new Button("Sign In");
        signInBtn.setMaxWidth(Double.MAX_VALUE);
        String btnBase  = "-fx-font-family: Sans Serif; -fx-font-size: 14px; -fx-font-weight: bold; " +
                "-fx-text-fill: white; -fx-background-color: " + ACCENT + "; " +
                "-fx-background-radius: 9; -fx-cursor: hand; -fx-padding: 12 0;";
        String btnHover = btnBase.replace(ACCENT, ACCENT_DARK);
        signInBtn.setStyle(btnBase);
        signInBtn.setOnMouseEntered(e -> { if (!signInBtn.isDisabled()) signInBtn.setStyle(btnHover); });
        signInBtn.setOnMouseExited (e -> { if (!signInBtn.isDisabled()) signInBtn.setStyle(btnBase);  });

        signInBtn.setOnAction(e -> {
            String user = usernameField.getText().trim();
            String pass = showing[0] ? passwordVisible.getText() : passwordField.getText();

            if (user.isEmpty() || pass.trim().isEmpty()) {
                showError("Please fill in all fields.");
                return;
            }
            hideError();

            if (staySignedIn.isSelected()) {
                PREFS.put(PREF_USER, user);
                PREFS.put(PREF_PASS, pass);
                PREFS.putBoolean(PREF_REMEMBER, true);
                savedBadge.setVisible(true);
            } else {
                PREFS.remove(PREF_USER);
                PREFS.remove(PREF_PASS);
                PREFS.putBoolean(PREF_REMEMBER, false);
                savedBadge.setVisible(false);
            }

            attemptLogin(stage, user, pass);
        });

        card.getChildren().addAll(
                cardHeader,
                usernameBox, passwordBox,
                checkRow, errorLabel, signInBtn
        );

        copyright = new Text("Stock Savy Longganisa \u00A9 2026");
        copyright.setFont(Font.font("Sans Serif", 11));
        copyright.setFill(Color.web(MUTED_LIGHT, 0.6));

        VBox centerContent = new VBox(card, copyright);
        centerContent.setAlignment(Pos.CENTER);
        VBox.setVgrow(centerContent, Priority.ALWAYS);

        pane.getChildren().add(centerContent);
        rightPanel = pane;
        return pane;
    }

    // ── Core login logic ──────────────────────────────────────────────────────
    private void attemptLogin(Stage stage, String user, String pass) {
        signInBtn.setDisable(true);
        signInBtn.setText("Signing in...");

        try {
            System.out.println("Connecting to database...");
            java.sql.Connection conn = DatabaseConnection.getConnection();
            System.out.println("Connected.");

            java.sql.PreparedStatement stmt = conn.prepareStatement(
                    "SELECT * FROM users WHERE username=? AND password=?"
            );
            stmt.setString(1, user);
            stmt.setString(2, pass);

            java.sql.ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String loggedUser = rs.getString("username");
                String loggedRole = rs.getString("role");
                System.out.println("[Auth] Login OK — user: " + loggedUser + " | role: " + loggedRole);
                conn.close();
                openDashboard(stage, loggedUser, loggedRole);
            } else {
                System.out.println("Invalid credentials.");
                PREFS.remove(PREF_USER);
                PREFS.remove(PREF_PASS);
                PREFS.putBoolean(PREF_REMEMBER, false);
                if (staySignedIn != null) staySignedIn.setSelected(false);
                showError("Incorrect username or password.");
                conn.close();
            }

        } catch (Exception ex) {
            System.out.println("Database error: " + ex.getMessage());
            ex.printStackTrace();
            showError("Could not connect to database. Please try again.");
        } finally {
            signInBtn.setDisable(false);
            signInBtn.setText("Sign In");
        }
    }

    // ── Auth helpers ──────────────────────────────────────────────────────────
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private void openDashboard(Stage loginStage, String username, String role) {
        try {
            new DashboardApplication(username, role).show();
            loginStage.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Failed to open dashboard. Contact support.");
        }
    }

    private Label fieldLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 10px; -fx-font-weight: bold; " +
                "-fx-text-fill: " + MUTED_LIGHT + "; -fx-letter-spacing: 0.08em;");
        return lbl;
    }

    private void applyFieldStyle(TextField field) {
        String bg     = "#FDF8F4";
        String border = BORDER_LIGHT;
        String fg     = TEXT_LIGHT;
        String base   = "-fx-font-family: Sans Serif; -fx-font-size: 13px; " +
                "-fx-background-color: " + bg + "; -fx-text-fill: " + fg + "; " +
                "-fx-prompt-text-fill: #C8BAA8; " +
                "-fx-border-color: " + border + "; -fx-border-radius: 8; " +
                "-fx-background-radius: 8; -fx-padding: 10 36 10 12;";
        field.setStyle(base);
        field.setOnMouseEntered(e -> field.setStyle(base.replace(border, "#C8A96E")));
        field.setOnMouseExited (e -> field.setStyle(base));
        field.focusedProperty().addListener((o, ov, nv) ->
                field.setStyle(nv ? base.replace(border, ACCENT) : base));
    }

    private void animatePanel(javafx.scene.Node node, double fromX) {
        FadeTransition ft = new FadeTransition(Duration.millis(600), node);
        ft.setFromValue(0);
        ft.setToValue(1);
        TranslateTransition tt = new TranslateTransition(Duration.millis(500), node);
        tt.setFromX(fromX);
        tt.setToX(0);
        ft.play();
        tt.play();
    }

    public static void main(String[] args) {
        launch(args);
    }
}