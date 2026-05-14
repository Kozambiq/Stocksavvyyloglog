package com.savvy.stocksavvyyloglog.view;

import com.savvy.stocksavvyyloglog.util.DatabaseConnection;
import com.savvy.stocksavvyyloglog.dialog.AddScheduleDialog;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;

/**
 * CalendarView — standalone Delivery & Production Schedule screen.
 *
 * Features:
 *  • Month navigation (prev / next / today)
 *  • Click any event pill → popup with:
 *      – Mark as Completed  (turns pill gray, strikes through title)
 *      – Remove Schedule    (confirmation dialog → deletes from DB)
 *      – Edit Schedule      (re-opens AddScheduleDialog pre-filled)
 *      – Undo Completed     (reverts status back to pending)
 *
 * DB requirement — make sure your `schedule` table has a `status` column:
 *   ALTER TABLE schedule ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'pending';
 */
public class CalendarView {

    private static final Logger LOGGER = Logger.getLogger(CalendarView.class.getName());

    // ── Colour tokens (mirrored from DashboardApplication) ──────────────────
    private static final String BG        = "#FDF5EC";
    private static final String CARD_BG   = "#FDFAF0";
    private static final String BORDER    = "#E8D8C0";
    private static final String ACCENT    = "#C04A10";
    private static final String ACCENT2   = "#C8A96E";
    private static final String TEXT      = "#2A1A08";
    private static final String TEXT_MUTED= "#9E8050";
    private static final String HDR_BTN   = "#F0E8DC";
    private static final String HDR_BTN_B = "#D8C8A8";

    private final Stage ownerStage;
    private final BorderPane rootPane;
    private final String currentUser;

    private java.time.YearMonth currentMonth = java.time.YearMonth.now();
    private Label monthLabel;
    private VBox calendarGridContainer;

    // ── Constructor ──────────────────────────────────────────────────────────
    public CalendarView(Stage ownerStage, BorderPane rootPane, String currentUser) {
        this.ownerStage  = ownerStage;
        this.rootPane    = rootPane;
        this.currentUser = currentUser;
    }

    // ── Entry point: swap the dashboard centre pane ──────────────────────────
    public void show() {
        ensureStatusColumn();
        rootPane.setCenter(buildView());
    }

    // ── Top-level layout ─────────────────────────────────────────────────────
    private ScrollPane buildView() {
        VBox content = new VBox(16);
        content.setPadding(new Insets(28, 36, 28, 36));
        content.setStyle("-fx-background-color: " + BG + ";");

        // Page header
        HBox pageHeader = new HBox(12);
        pageHeader.setAlignment(Pos.CENTER_LEFT);

        Label pageTitle = new Label("\uD83D\uDCC5  Delivery & Production Schedule");
        pageTitle.setStyle(
                "-fx-font-family: Sans Serif; -fx-font-size: 20px; " +
                        "-fx-font-weight: bold; -fx-text-fill: " + ACCENT + ";"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addBtn = new Button("+ Add Schedule");
        addBtn.setStyle(
                "-fx-font-family: Sans Serif; -fx-font-size: 12px; -fx-text-fill: white; " +
                        "-fx-background-color: " + ACCENT + "; -fx-background-radius: 6; " +
                        "-fx-cursor: hand; -fx-padding: 8px 18px;"
        );
        addBtn.setOnMouseEntered(e -> addBtn.setStyle(
                "-fx-font-family: Sans Serif; -fx-font-size: 12px; -fx-text-fill: white; " +
                        "-fx-background-color: #E8530A; -fx-background-radius: 6; " +
                        "-fx-cursor: hand; -fx-padding: 8px 18px;"
        ));
        addBtn.setOnMouseExited(e -> addBtn.setStyle(
                "-fx-font-family: Sans Serif; -fx-font-size: 12px; -fx-text-fill: white; " +
                        "-fx-background-color: " + ACCENT + "; -fx-background-radius: 6; " +
                        "-fx-cursor: hand; -fx-padding: 8px 18px;"
        ));
        addBtn.setOnAction(e -> {
            new AddScheduleDialog(ownerStage, currentUser).show();
            refreshCalendar();
        });

        pageHeader.getChildren().addAll(pageTitle, spacer, addBtn);

        // Legend
        HBox legend = buildLegend();

        // Calendar card
        VBox calCard = new VBox(12);
        calCard.setPadding(new Insets(20));
        calCard.setStyle(
                "-fx-background-color: " + CARD_BG + "; " +
                        "-fx-border-width: 1; -fx-border-color: " + BORDER + "; " +
                        "-fx-background-radius: 8; -fx-border-radius: 8;"
        );

        calendarGridContainer = new VBox();
        calCard.getChildren().addAll(buildCalendarNav(), buildCalendarGrid());

        content.getChildren().addAll(pageHeader, legend, calCard);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle(
                "-fx-background-color: " + BG + "; -fx-background: " + BG + "; -fx-border-width: 0;"
        );
        return scroll;
    }

    // ── Legend ────────────────────────────────────────────────────────────────
    private HBox buildLegend() {
        HBox legend = new HBox(16);
        legend.setAlignment(Pos.CENTER_LEFT);
        legend.setPadding(new Insets(8, 12, 8, 12));
        legend.setStyle(
                "-fx-background-color: " + CARD_BG + "; -fx-background-radius: 6; " +
                        "-fx-border-color: " + BORDER + "; -fx-border-width: 1; -fx-border-radius: 6;"
        );

        legend.getChildren().addAll(
                legendPill("Delivery",   "#4A7C4E"),
                legendPill("Production", "#C8A96E"),
                legendPill("Holiday",    "#B85C00"),
                legendPill("Other",      "#6B7080"),
                legendPill("Completed",  "#AAAAAA")
        );
        return legend;
    }

    private HBox legendPill(String label, String color) {
        HBox pill = new HBox(6);
        pill.setAlignment(Pos.CENTER_LEFT);
        Label dot = new Label("  ");
        dot.setStyle(
                "-fx-background-color: " + color + "; -fx-background-radius: 3; " +
                        "-fx-min-width: 14; -fx-min-height: 14;"
        );
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 11px; -fx-text-fill: " + TEXT_MUTED + ";");
        pill.getChildren().addAll(dot, lbl);
        return pill;
    }

    // ── Calendar navigation ───────────────────────────────────────────────────
    private HBox buildCalendarNav() {
        HBox nav = new HBox(10);
        nav.setAlignment(Pos.CENTER_LEFT);

        String navBtnStyle =
                "-fx-font-size: 11px; -fx-background-color: " + HDR_BTN + "; " +
                        "-fx-border-color: " + HDR_BTN_B + "; -fx-border-width: 1; " +
                        "-fx-background-radius: 4; -fx-border-radius: 4; -fx-cursor: hand; " +
                        "-fx-padding: 5 10; -fx-text-fill: " + TEXT + ";";

        Button prevBtn  = new Button("◄");
        Button nextBtn  = new Button("►");
        Button todayBtn = new Button("Today");
        prevBtn.setStyle(navBtnStyle);
        nextBtn.setStyle(navBtnStyle);
        todayBtn.setStyle(navBtnStyle);

        monthLabel = new Label(formatMonth());
        monthLabel.setStyle(
                "-fx-font-family: Sans Serif; -fx-font-size: 13px; " +
                        "-fx-font-weight: bold; -fx-text-fill: " + TEXT + ";"
        );

        prevBtn.setOnAction(e  -> { currentMonth = currentMonth.minusMonths(1); refreshCalendar(); });
        nextBtn.setOnAction(e  -> { currentMonth = currentMonth.plusMonths(1);  refreshCalendar(); });
        todayBtn.setOnAction(e -> { currentMonth = java.time.YearMonth.now();   refreshCalendar(); });

        nav.getChildren().addAll(prevBtn, monthLabel, nextBtn, todayBtn);
        return nav;
    }

    private String formatMonth() {
        return currentMonth.getMonth().getDisplayName(
                java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH)
                + " " + currentMonth.getYear();
    }

    private void refreshCalendar() {
        monthLabel.setText(formatMonth());
        calendarGridContainer.getChildren().setAll(buildCalendarGrid());
    }

    // ── Calendar grid ────────────────────────────────────────────────────────
    private VBox buildCalendarGrid() {
        calendarGridContainer = new VBox();

        GridPane grid = new GridPane();
        grid.setMaxWidth(Double.MAX_VALUE);

        // Day-of-week headers
        String[] days = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        for (int i = 0; i < 7; i++) {
            Label header = new Label(days[i]);
            header.setMaxWidth(Double.MAX_VALUE);
            header.setAlignment(Pos.CENTER);
            header.setPadding(new Insets(10));
            header.setStyle(
                    "-fx-font-family: Sans Serif; -fx-font-size: 12px; " +
                            "-fx-font-weight: bold; -fx-text-fill: #FAF0E6; " +
                            "-fx-background-color: " + ACCENT + ";"
            );
            GridPane.setHgrow(header, Priority.ALWAYS);
            grid.add(header, i, 0);
        }

        // Load events
        java.util.Map<Integer, java.util.List<ScheduleEvent>> events = loadEventsForMonth();

        java.time.LocalDate firstDay    = currentMonth.atDay(1);
        int startDow    = firstDay.getDayOfWeek().getValue() % 7;
        int daysInMonth = currentMonth.lengthOfMonth();
        java.time.LocalDate today = java.time.LocalDate.now();

        int day = 1;
        for (int row = 1; row <= 6 && day <= daysInMonth; row++) {
            for (int col = 0; col < 7; col++) {
                if (row == 1 && col < startDow) {
                    Label empty = new Label();
                    empty.setMaxWidth(Double.MAX_VALUE);
                    empty.setMinHeight(90);
                    empty.setStyle(
                            "-fx-border-color: " + BORDER + "; -fx-border-width: 0.5; " +
                                    "-fx-background-color: #F7F2E8;"
                    );
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
                cell.setMinHeight(90);
                cell.setPadding(new Insets(6));
                cell.setStyle(
                        "-fx-border-color: " + BORDER + "; -fx-border-width: 0.5; " +
                                "-fx-background-color: " + (isToday ? "rgba(192,74,16,0.10)" : "#FDFAF0") + ";"
                );

                Label dayNum = new Label(String.valueOf(day));
                dayNum.setStyle(
                        "-fx-font-family: Sans Serif; -fx-font-size: 11px; " +
                                "-fx-text-fill: " + (isToday ? ACCENT : TEXT) + "; " +
                                (isToday ? "-fx-font-weight: bold;" : "")
                );
                cell.getChildren().add(dayNum);

                if (events.containsKey(day)) {
                    for (ScheduleEvent evt : events.get(day)) {
                        Label pill = buildEventPill(evt);
                        cell.getChildren().add(pill);
                    }
                }

                GridPane.setHgrow(cell, Priority.ALWAYS);
                final int finalDay = day;
                grid.add(cell, col, row);
                day++;
            }
        }

        calendarGridContainer.getChildren().add(grid);
        return calendarGridContainer;
    }

    // ── Event pill ────────────────────────────────────────────────────────────
    private Label buildEventPill(ScheduleEvent evt) {
        boolean done = "completed".equalsIgnoreCase(evt.status);
        String pillColor = done ? "#AAAAAA" : getEventColor(evt.type);
        String titleText = done ? "✓ " + evt.title : evt.title;

        Label pill = new Label(titleText);
        pill.setWrapText(true);
        pill.setMaxWidth(Double.MAX_VALUE);
        pill.setStyle(
                "-fx-font-family: Sans Serif; -fx-font-size: 9px; -fx-text-fill: white; " +
                        "-fx-background-color: " + pillColor + "; " +
                        "-fx-background-radius: 3; -fx-padding: 2 5; -fx-cursor: hand;" +
                        (done ? " -fx-strikethrough: true;" : "")
        );

        pill.setOnMouseClicked(e -> showEventPopup(evt));
        return pill;
    }

    // ── Event action popup ────────────────────────────────────────────────────
    private void showEventPopup(ScheduleEvent evt) {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initOwner(ownerStage);
        popup.setTitle("Schedule: " + evt.title);
        popup.setResizable(false);

        VBox popupLayout = new VBox(0);
        popupLayout.setStyle("-fx-background-color: " + BG + ";");

        // Header
        HBox header = new HBox(10);
        header.setPadding(new Insets(14, 18, 14, 18));
        header.setAlignment(Pos.CENTER_LEFT);
        String headerColor = "completed".equalsIgnoreCase(evt.status) ? "#AAAAAA" : getEventColor(evt.type);
        header.setStyle("-fx-background-color: " + headerColor + ";");

        Label titleLbl = new Label(evt.title);
        titleLbl.setStyle(
                "-fx-font-family: Sans Serif; -fx-font-size: 14px; " +
                        "-fx-font-weight: bold; -fx-text-fill: white;"
        );
        Label dateLbl = new Label("  •  " + evt.date.toString());
        dateLbl.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 12px; -fx-text-fill: rgba(255,255,255,0.8);");
        header.getChildren().addAll(titleLbl, dateLbl);

        // Body
        VBox body = new VBox(10);
        body.setPadding(new Insets(18, 20, 18, 20));

        // Type badge
        HBox metaRow = new HBox(8);
        metaRow.setAlignment(Pos.CENTER_LEFT);
        Label typeBadge = new Label(evt.type != null ? evt.type : "Event");
        typeBadge.setStyle(
                "-fx-font-family: Sans Serif; -fx-font-size: 11px; " +
                        "-fx-text-fill: white; -fx-background-color: " + getEventColor(evt.type) + "; " +
                        "-fx-background-radius: 20; -fx-padding: 3 10;"
        );
        Label statusBadge = new Label("completed".equalsIgnoreCase(evt.status) ? "✓ Completed" : "● Pending");
        statusBadge.setStyle(
                "-fx-font-family: Sans Serif; -fx-font-size: 11px; " +
                        "-fx-text-fill: " + ("completed".equalsIgnoreCase(evt.status) ? "#4A7C4E" : "#B85C00") + "; " +
                        "-fx-background-color: " + ("completed".equalsIgnoreCase(evt.status) ? "#E8F5E9" : "#FFF3E0") + "; " +
                        "-fx-background-radius: 20; -fx-padding: 3 10;"
        );
        metaRow.getChildren().addAll(typeBadge, statusBadge);
        body.getChildren().add(metaRow);

        // Separator
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: " + BORDER + ";");
        body.getChildren().add(sep);

        // Action buttons
        VBox actions = new VBox(8);

        boolean isCompleted = "completed".equalsIgnoreCase(evt.status);

        if (!isCompleted) {
            // Mark as completed
            Button completeBtn = buildPopupBtn(
                    "✔  Mark as Completed",
                    "#1B5E20", "#A5D6A7", "#F1F8F1"
            );
            completeBtn.setOnAction(e -> {
                updateScheduleStatus(evt.id, "completed");
                popup.close();
                refreshCalendar();
            });
            actions.getChildren().add(completeBtn);
        } else {
            // Undo completed
            Button undoBtn = buildPopupBtn(
                    "↩  Mark as Pending",
                    TEXT_MUTED, BORDER, CARD_BG
            );
            undoBtn.setOnAction(e -> {
                updateScheduleStatus(evt.id, "pending");
                popup.close();
                refreshCalendar();
            });
            actions.getChildren().add(undoBtn);
        }

        // Edit
        Button editBtn = buildPopupBtn("✎  Edit Schedule", TEXT, BORDER, CARD_BG);
        editBtn.setOnAction(e -> {
            popup.close();
            // Re-open AddScheduleDialog — pass the schedule id so it pre-fills
            // (you'll need to add an id-aware constructor to AddScheduleDialog)
            new AddScheduleDialog(ownerStage, currentUser).show();
            refreshCalendar();
        });

        // Divider label
        Label divLbl = new Label("────────────────────");
        divLbl.setStyle("-fx-text-fill: " + BORDER + "; -fx-font-size: 9px;");

        // Remove
        Button removeBtn = buildPopupBtn("🗑  Remove Schedule", "#B71C1C", "#EF9A9A", "#FFF5F5");
        removeBtn.setOnAction(e -> {
            popup.close();
            showRemoveConfirmation(evt);
        });

        actions.getChildren().addAll(editBtn, divLbl, removeBtn);
        body.getChildren().add(actions);

        popupLayout.getChildren().addAll(header, body);

        Scene scene = new Scene(popupLayout, 300, isCompleted ? 260 : 280);
        popup.setScene(scene);
        popup.showAndWait();
    }

    // ── Remove confirmation dialog ────────────────────────────────────────────
    private void showRemoveConfirmation(ScheduleEvent evt) {
        Stage confirm = new Stage();
        confirm.initModality(Modality.APPLICATION_MODAL);
        confirm.initOwner(ownerStage);
        confirm.setTitle("Remove Schedule");
        confirm.setResizable(false);

        VBox confirmLayout = new VBox(0);
        confirmLayout.setStyle("-fx-background-color: " + BG + ";");

        // Header
        HBox header = new HBox(10);
        header.setPadding(new Insets(14, 18, 14, 18));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: #B71C1C;");
        Label titleLbl = new Label("🗑  Remove Schedule");
        titleLbl.setStyle(
                "-fx-font-family: Sans Serif; -fx-font-size: 14px; " +
                        "-fx-font-weight: bold; -fx-text-fill: white;"
        );
        header.getChildren().add(titleLbl);

        // Body
        VBox body = new VBox(12);
        body.setPadding(new Insets(20, 20, 20, 20));

        Label warningIcon = new Label("⚠");
        warningIcon.setStyle("-fx-font-size: 28px;");
        warningIcon.setAlignment(Pos.CENTER);
        warningIcon.setMaxWidth(Double.MAX_VALUE);

        Label msg = new Label("Are you sure you want to remove:");
        msg.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 13px; -fx-text-fill: " + TEXT + ";");

        Label evtName = new Label("\"" + evt.title + "\"");
        evtName.setStyle(
                "-fx-font-family: Sans Serif; -fx-font-size: 14px; " +
                        "-fx-font-weight: bold; -fx-text-fill: " + ACCENT + ";"
        );

        Label subMsg = new Label("Scheduled for " + evt.date.toString());
        subMsg.setStyle("-fx-font-family: Sans Serif; -fx-font-size: 11px; -fx-text-fill: " + TEXT_MUTED + ";");

        Label warn = new Label("This action cannot be undone.");
        warn.setStyle(
                "-fx-font-family: Sans Serif; -fx-font-size: 11px; " +
                        "-fx-text-fill: #B71C1C; -fx-font-weight: bold;"
        );

        body.getChildren().addAll(warningIcon, msg, evtName, subMsg, warn);

        // Footer
        HBox footer = new HBox(10);
        footer.setPadding(new Insets(12, 20, 14, 20));
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setStyle("-fx-border-color: " + BORDER + "; -fx-border-width: 1 0 0 0;");

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle(
                "-fx-font-family: Sans Serif; -fx-font-size: 12px; -fx-text-fill: " + TEXT + "; " +
                        "-fx-background-color: " + CARD_BG + "; -fx-border-color: " + BORDER + "; " +
                        "-fx-border-width: 1; -fx-background-radius: 6; -fx-border-radius: 6; " +
                        "-fx-cursor: hand; -fx-padding: 7 18;"
        );
        cancelBtn.setOnAction(e -> confirm.close());

        Button confirmRemoveBtn = new Button("Remove");
        confirmRemoveBtn.setStyle(
                "-fx-font-family: Sans Serif; -fx-font-size: 12px; -fx-text-fill: white; " +
                        "-fx-background-color: #B71C1C; -fx-background-radius: 6; " +
                        "-fx-cursor: hand; -fx-padding: 7 18; -fx-font-weight: bold;"
        );
        confirmRemoveBtn.setOnMouseEntered(e -> confirmRemoveBtn.setStyle(
                "-fx-font-family: Sans Serif; -fx-font-size: 12px; -fx-text-fill: white; " +
                        "-fx-background-color: #8B0000; -fx-background-radius: 6; " +
                        "-fx-cursor: hand; -fx-padding: 7 18; -fx-font-weight: bold;"
        ));
        confirmRemoveBtn.setOnMouseExited(e -> confirmRemoveBtn.setStyle(
                "-fx-font-family: Sans Serif; -fx-font-size: 12px; -fx-text-fill: white; " +
                        "-fx-background-color: #B71C1C; -fx-background-radius: 6; " +
                        "-fx-cursor: hand; -fx-padding: 7 18; -fx-font-weight: bold;"
        ));
        confirmRemoveBtn.setOnAction(e -> {
            deleteSchedule(evt.id);
            confirm.close();
            refreshCalendar();
        });

        footer.getChildren().addAll(cancelBtn, confirmRemoveBtn);
        confirmLayout.getChildren().addAll(header, body, footer);

        Scene scene = new Scene(confirmLayout, 340, 300);
        confirm.setScene(scene);
        confirm.showAndWait();
    }

    // ── DB operations ─────────────────────────────────────────────────────────

    /**
     * Ensures the status column exists.
     * Uses INFORMATION_SCHEMA check instead of IF NOT EXISTS
     * (IF NOT EXISTS for ADD COLUMN requires MySQL 8.0.3+).
     */
    private void ensureStatusColumn() {
        String checkSql =
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE TABLE_SCHEMA = DATABASE() " +
                        "  AND TABLE_NAME = 'schedule' " +
                        "  AND COLUMN_NAME = 'status'";
        try (Connection conn = DatabaseConnection.getConnection()) {
            ResultSet rs = conn.prepareStatement(checkSql).executeQuery();
            if (rs.next() && rs.getInt(1) == 0) {
                conn.prepareStatement(
                        "ALTER TABLE schedule ADD COLUMN status VARCHAR(20) DEFAULT 'pending'"
                ).executeUpdate();
                LOGGER.info("Added status column to schedule table.");
            }
        } catch (Exception e) {
            LOGGER.warning("Could not ensure status column: " + e.getMessage());
        }
    }

    private void updateScheduleStatus(int id, String status) {
        String sql = "UPDATE schedule SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (Exception e) {
            LOGGER.severe("Error updating schedule status: " + e.getMessage());
            showError("Could not update schedule status.");
        }
    }

    private void deleteSchedule(int id) {
        String sql = "DELETE FROM schedule WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (Exception e) {
            LOGGER.severe("Error deleting schedule: " + e.getMessage());
            showError("Could not remove schedule.");
        }
    }

    /**
     * Loads all events for the current month, including their DB id and status.
     * Requires the schedule table to have columns: id, event_date, title, event_type, status
     */
    private java.util.Map<Integer, java.util.List<ScheduleEvent>> loadEventsForMonth() {
        java.util.Map<Integer, java.util.List<ScheduleEvent>> map = new java.util.HashMap<>();
        String sql = "SELECT id, DAY(event_date) AS day, event_date, title, event_type, " +
                "COALESCE(status, 'pending') AS status " +
                "FROM schedule " +
                "WHERE YEAR(event_date) = ? AND MONTH(event_date) = ? " +
                "ORDER BY event_date, id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, currentMonth.getYear());
            ps.setInt(2, currentMonth.getMonthValue());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ScheduleEvent evt = new ScheduleEvent(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("event_type"),
                        rs.getDate("event_date").toLocalDate(),
                        rs.getString("status")
                );
                int day = rs.getInt("day");
                map.computeIfAbsent(day, k -> new java.util.ArrayList<>()).add(evt);
            }
        } catch (Exception e) {
            LOGGER.severe("Error loading schedule: " + e.getMessage());
        }
        return map;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String getEventColor(String type) {
        if (type == null) return ACCENT;
        switch (type) {
            case "Delivery":   return "#4A7C4E";
            case "Production": return "#C8A96E";
            case "Holiday":    return "#B85C00";
            default:           return "#6B7080";
        }
    }

    private Button buildPopupBtn(String text, String textColor, String borderColor, String bgColor) {
        Button btn = new Button(text);
        String base =
                "-fx-font-family: Sans Serif; -fx-font-size: 12px; " +
                        "-fx-text-fill: " + textColor + "; " +
                        "-fx-background-color: " + bgColor + "; " +
                        "-fx-border-color: " + borderColor + "; " +
                        "-fx-border-width: 1; -fx-background-radius: 6; -fx-border-radius: 6; " +
                        "-fx-cursor: hand; -fx-padding: 8 14; -fx-alignment: center-left;";
        btn.setStyle(base);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setOnMouseEntered(e -> btn.setStyle(
                base.replace("-fx-background-color: " + bgColor, "-fx-background-color: derive(" + bgColor + ", -5%)")
        ));
        btn.setOnMouseExited(e -> btn.setStyle(base));
        return btn;
    }

    private void showError(String message) {
        javafx.application.Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.initOwner(ownerStage);
            alert.showAndWait();
        });
    }

    // ── Inner model class ─────────────────────────────────────────────────────
    private static class ScheduleEvent {
        final int id;
        final String title;
        final String type;
        final java.time.LocalDate date;
        final String status;

        ScheduleEvent(int id, String title, String type,
                      java.time.LocalDate date, String status) {
            this.id     = id;
            this.title  = title;
            this.type   = type;
            this.date   = date;
            this.status = status;
        }
    }
}