package com.savvy.stocksavvyyloglog.model;

import com.savvy.stocksavvyyloglog.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SalesOrderDAO {

    // ── Constructor: ensure table exists on first use ─────────────────────────
    public SalesOrderDAO() {
        ensureTableExists();
    }

    private void ensureTableExists() {
        String ddl =
                "CREATE TABLE IF NOT EXISTS sales_orders (" +
                        "  id            INT AUTO_INCREMENT PRIMARY KEY," +
                        "  customer_id   INT            NOT NULL DEFAULT 1," +
                        "  product_id    INT            NOT NULL DEFAULT 1," +
                        "  quantity      DOUBLE         NOT NULL DEFAULT 0," +
                        "  unit_price    DOUBLE         NOT NULL DEFAULT 0," +
                        "  total_amount  DOUBLE         NOT NULL DEFAULT 0," +
                        "  status        VARCHAR(50)    NOT NULL DEFAULT 'Pending'," +
                        "  order_date    DATE           NOT NULL," +
                        "  delivery_date DATE               NULL," +
                        "  notes         TEXT               NULL," +
                        "  created_by    INT                NULL," +
                        "  created_at    TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP" +
                        ")";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement()) {
            st.executeUpdate(ddl);
        } catch (Exception e) {
            System.err.println("[SalesOrderDAO] ensureTableExists failed: " + e.getMessage());
        }
    }

    // ── Row model ─────────────────────────────────────────────────────────────
    public static class SalesOrderRow {
        public int    id;
        public String customerName;
        public String productName;
        public double quantity;
        public double unitPrice;
        public double totalAmount;
        public String status;
        public String orderDate;
        public String deliveryDate;
        public String notes;
        public String createdBy;
    }

    // ── CREATE ────────────────────────────────────────────────────────────────
    public boolean createOrder(String customerName, String productName,
                               double quantity, double unitPrice,
                               String orderDate, String deliveryDate,
                               String notes, String createdBy) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            int customerId = findOrCreateCustomer(conn, customerName);
            int productId  = findProductId(conn, productName);
            int userId     = findUserId(conn, createdBy);
            double total   = quantity * unitPrice;

            String sql =
                    "INSERT INTO sales_orders " +
                            "(customer_id, product_id, quantity, unit_price, total_amount, " +
                            " status, order_date, delivery_date, notes, created_by) " +
                            "VALUES (?, ?, ?, ?, ?, 'Pending', ?, ?, ?, ?)";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt   (1, customerId);
                ps.setInt   (2, productId);
                ps.setDouble(3, quantity);
                ps.setDouble(4, unitPrice);
                ps.setDouble(5, total);
                ps.setString(6, orderDate);
                ps.setString(7, deliveryDate != null ? deliveryDate : null);
                ps.setString(8, notes != null ? notes : "");
                ps.setInt   (9, userId);
                ps.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (Exception e) {
            System.err.println("[SalesOrderDAO] createOrder failed: " + e.getMessage());
            e.printStackTrace(); // full stack trace for debugging
            if (conn != null) try { conn.rollback(); } catch (Exception ignored) {}
            return false;
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (Exception ignored) {}
        }
    }

    // ── READ: all orders ──────────────────────────────────────────────────────
    public List<SalesOrderRow> getAllOrders() {
        List<SalesOrderRow> list = new ArrayList<>();
        String sql =
                "SELECT so.id, " +
                        "       COALESCE(c.name, 'Unknown') AS customer_name, " +
                        "       COALESCE(p.name, 'Unknown') AS product_name, " +
                        "       so.quantity, so.unit_price, so.total_amount, " +
                        "       so.status, so.order_date, so.delivery_date, " +
                        "       COALESCE(so.notes, '') AS notes, " +
                        "       COALESCE(u.username, '') AS created_by " +
                        "FROM sales_orders so " +
                        "LEFT JOIN customers c ON so.customer_id = c.id " +
                        "LEFT JOIN products  p ON so.product_id  = p.id " +
                        "LEFT JOIN users     u ON so.created_by  = u.id " +
                        "ORDER BY so.order_date DESC, so.id DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                SalesOrderRow row = new SalesOrderRow();
                row.id           = rs.getInt("id");
                row.customerName = rs.getString("customer_name");
                row.productName  = rs.getString("product_name");
                row.quantity     = rs.getDouble("quantity");
                row.unitPrice    = rs.getDouble("unit_price");
                row.totalAmount  = rs.getDouble("total_amount");
                row.status       = rs.getString("status");
                row.orderDate    = rs.getString("order_date");
                row.deliveryDate = rs.getString("delivery_date");
                row.notes        = rs.getString("notes");
                row.createdBy    = rs.getString("created_by");
                list.add(row);
            }
        } catch (Exception e) {
            System.err.println("[SalesOrderDAO] getAllOrders failed: " + e.getMessage());
        }
        return list;
    }

    // ── UPDATE STATUS ─────────────────────────────────────────────────────────
    public boolean updateStatus(int orderId, String newStatus) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            SalesOrderRow order = getOrderById(conn, orderId);
            if (order == null) return false;

            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE sales_orders SET status = ? WHERE id = ?")) {
                ps.setString(1, newStatus);
                ps.setInt   (2, orderId);
                ps.executeUpdate();
            }

            if ("Confirmed".equals(newStatus)) {
                int affected = 0;

                // 1) Try exact match first
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE stocks SET quantity = quantity - ? " +
                                "WHERE product_name = ? AND quantity >= ?")) {
                    ps.setDouble(1, order.quantity);
                    ps.setString(2, order.productName);
                    ps.setDouble(3, order.quantity);
                    affected = ps.executeUpdate();
                }

                // 2) Fall back to LIKE — handles "Longganisa" matching "Longganisa Sweet" etc.
                if (affected == 0) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE stocks SET quantity = quantity - ? " +
                                    "WHERE product_name LIKE ? AND quantity >= ? LIMIT 1")) {
                        ps.setDouble(1, order.quantity);
                        ps.setString(2, "%" + order.productName + "%");
                        ps.setDouble(3, order.quantity);
                        affected = ps.executeUpdate();
                    }
                }

                if (affected == 0) {
                    conn.rollback();
                    return false; // genuinely not enough stock
                }
            }

            conn.commit();
            return true;
        } catch (Exception e) {
            System.err.println("[SalesOrderDAO] updateStatus failed: " + e.getMessage());
            if (conn != null) try { conn.rollback(); } catch (Exception ignored) {}
            return false;
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (Exception ignored) {}
        }
    }

    // ── DELETE ────────────────────────────────────────────────────────────────
    public boolean deleteOrder(int orderId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM sales_orders WHERE id = ?")) {
            ps.setInt(1, orderId);
            ps.executeUpdate();
            return true;
        } catch (Exception e) {
            System.err.println("[SalesOrderDAO] deleteOrder failed: " + e.getMessage());
            return false;
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────
    private SalesOrderRow getOrderById(Connection conn, int id) throws SQLException {
        String sql =
                "SELECT so.id, so.quantity, so.unit_price, so.total_amount, so.status, " +
                        "       COALESCE(p.name, '') AS product_name " +
                        "FROM sales_orders so " +
                        "LEFT JOIN products p ON so.product_id = p.id " +
                        "WHERE so.id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                SalesOrderRow row = new SalesOrderRow();
                row.id          = rs.getInt("id");
                row.quantity    = rs.getDouble("quantity");
                row.unitPrice   = rs.getDouble("unit_price");
                row.totalAmount = rs.getDouble("total_amount");
                row.status      = rs.getString("status");
                row.productName = rs.getString("product_name");
                return row;
            }
        }
        return null;
    }

    private int findOrCreateCustomer(Connection conn, String name) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id FROM customers WHERE name = ? LIMIT 1")) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO customers (name) VALUES (?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) return keys.getInt(1);
        }
        return 1;
    }

    private int findProductId(Connection conn, String productName) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id FROM products WHERE name = ? LIMIT 1")) {
            ps.setString(1, productName);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id FROM stocks WHERE product_name = ? LIMIT 1")) {
            ps.setString(1, productName);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }
        return 1;
    }

    private int findUserId(Connection conn, String username) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id FROM users WHERE username = ? LIMIT 1")) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }
        return 1;
    }

    public List<String> getProductionProductNames() {
        List<String> names = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT name FROM productions ORDER BY name ASC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) names.add(rs.getString(1));
        } catch (Exception e) {
            System.err.println("[SalesOrderDAO] getProductionProductNames failed: " + e.getMessage());
        }
        return names;
    }

    public double getProductionPrice(String productName) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT price FROM productions WHERE name = ? LIMIT 1")) {
            ps.setString(1, productName);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getDouble(1);
        } catch (Exception e) {
            System.err.println("[SalesOrderDAO] getProductionPrice failed: " + e.getMessage());
        }
        return 0.0;
    }

    public List<String> getCustomerNames() {
        List<String> names = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT name FROM customers ORDER BY name ASC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) names.add(rs.getString(1));
        } catch (Exception e) {
            System.err.println("[SalesOrderDAO] getCustomerNames failed: " + e.getMessage());
        }
        return names;
    }
}