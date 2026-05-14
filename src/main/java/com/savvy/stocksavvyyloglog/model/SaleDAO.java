package com.savvy.stocksavvyyloglog.model;

import com.savvy.stocksavvyyloglog.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * SaleDAO
 * -------
 * Data Access Object for all sales-related database operations.
 * Works with the existing `sales`, `sales_items`, `products`,
 * `customers`, and `stocks` tables defined in the schema.
 *
 * FIXES APPLIED:
 *  1. findProductId() now throws a clear SQLException instead of
 *     silently falling back to id=1, which caused FK constraint failures
 *     when that ID didn't exist in the products table.
 *  2. stock_out INSERT now uses the correct column set that matches
 *     the actual table schema (removed 'stock_id', added 'out_date').
 *  3. findOrCreateCustomer() fallback removed — throws instead of
 *     silently using id=1.
 */
public class SaleDAO {

    // ── Row model returned by queries ─────────────────────────────────────────
    public static class SaleRow {
        public int    id;
        public String saleDate;
        public String customerName;
        public String productName;
        public double quantity;
        public double unitPrice;
        public double subtotal;
        public double totalAmount;
        public String paymentMethod;
        public String status;
        public String notes;
        public String soldBy;

        @Override
        public String toString() {
            return String.format("Sale#%d [%s] %s — ₱%.2f", id, saleDate, customerName, totalAmount);
        }
    }

    // ── Summary stats ─────────────────────────────────────────────────────────
    public static class SaleSummary {
        public double totalRevenue;
        public int    totalTransactions;
        public double avgOrderValue;
        public String topProduct;
        public String topCustomer;
    }

    // ── CREATE: insert a sale + its items + deduct stock ─────────────────────
    public boolean recordSale(String customerName, String productName,
                              int quantity, double unitPrice, double discount,
                              String paymentMethod, String saleDate,
                              String notes, String soldBy) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // 1. Resolve or create customer
            int customerId = findOrCreateCustomer(conn, customerName);

            // 2. Resolve sold_by user id
            int userId = findUserId(conn, soldBy);

            // 3. Resolve product id — throws clearly if not found (FIX #1)
            int productId = findProductId(conn, productName);

            // 4. Compute totals
            double subtotal    = unitPrice * quantity * (1.0 - discount / 100.0);
            double totalAmount = subtotal;

            // 5. Insert into sales FIRST (parent row must exist before sales_items)
            String insertSale =
                    "INSERT INTO sales (customer_id, sale_date, total_amount, payment_method, sold_by) " +
                            "VALUES (?, ?, ?, ?, ?)";
            int saleId;
            try (PreparedStatement ps = conn.prepareStatement(insertSale, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt   (1, customerId);
                ps.setString(2, saleDate);
                ps.setDouble(3, totalAmount);
                ps.setString(4, paymentMethod);
                ps.setInt   (5, userId);
                ps.executeUpdate();
                ResultSet keys = ps.getGeneratedKeys();
                if (!keys.next()) throw new SQLException("Failed to get generated sale ID.");
                saleId = keys.getInt(1);
            }

            // 6. Insert into sales_items (child — references the saleId we just got)
            String insertItem =
                    "INSERT INTO sales_items (sale_id, product_id, quantity, unit_price, subtotal) " +
                            "VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertItem)) {
                ps.setInt   (1, saleId);
                ps.setInt   (2, productId);
                ps.setDouble(3, quantity);
                ps.setDouble(4, unitPrice);
                ps.setDouble(5, subtotal);
                ps.executeUpdate();
            }

            // 7. Deduct from stocks
            String deductStock =
                    "UPDATE stocks SET quantity = quantity - ? WHERE product_name = ? AND quantity >= ?";
            try (PreparedStatement ps = conn.prepareStatement(deductStock)) {
                ps.setDouble(1, quantity);
                ps.setString(2, productName);
                ps.setDouble(3, quantity);
                int rows = ps.executeUpdate();
                if (rows == 0) {
                    // Not enough stock — roll back and inform caller
                    throw new SQLException("Insufficient stock for product: " + productName);
                }
            }

            // 8. Log to stock_out (best-effort — FIX #2: removed 'stock_id' column)
            try {
                String logOut =
                        "INSERT INTO stock_out (product_id, quantity, reason, notes, removed_by, out_date) " +
                                "VALUES (?, ?, 'Sold', ?, ?, CURDATE())";
                try (PreparedStatement ps = conn.prepareStatement(logOut)) {
                    ps.setInt   (1, productId);
                    ps.setDouble(2, quantity);
                    ps.setString(3, notes != null ? notes : "");
                    ps.setInt   (4, userId);
                    ps.executeUpdate();
                }
            } catch (Exception logEx) {
                // stock_out logging is best-effort; log but don't fail the sale
                System.err.println("[SaleDAO] stock_out log skipped: " + logEx.getMessage());
            }

            conn.commit();
            return true;

        } catch (Exception e) {
            System.err.println("[SaleDAO] recordSale failed: " + e.getMessage());
            if (conn != null) {
                try { conn.rollback(); } catch (Exception ignored) {}
            }
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (Exception ignored) {}
            }
        }
    }

    // ── READ: all sales with customer + product info ──────────────────────────
    public List<SaleRow> getAllSales() {
        List<SaleRow> list = new ArrayList<>();
        String sql =
                "SELECT s.id, s.sale_date, " +
                        "       COALESCE(c.name, 'Unknown') AS customer_name, " +
                        "       COALESCE(p.name, 'Unknown') AS product_name, " +
                        "       COALESCE(si.quantity, 0) AS quantity, " +
                        "       COALESCE(si.unit_price, 0) AS unit_price, " +
                        "       COALESCE(si.subtotal, 0) AS subtotal, " +
                        "       s.total_amount, " +
                        "       COALESCE(s.payment_method, 'Cash') AS payment_method, " +
                        "       COALESCE(u.username, '') AS sold_by " +
                        "FROM sales s " +
                        "LEFT JOIN customers c    ON s.customer_id  = c.id " +
                        "LEFT JOIN users u        ON s.sold_by       = u.id " +
                        "LEFT JOIN sales_items si ON si.sale_id      = s.id " +
                        "LEFT JOIN products p     ON si.product_id   = p.id " +
                        "ORDER BY s.sale_date DESC, s.id DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                SaleRow row = new SaleRow();
                row.id            = rs.getInt("id");
                row.saleDate      = rs.getString("sale_date");
                row.customerName  = rs.getString("customer_name");
                row.productName   = rs.getString("product_name");
                row.quantity      = rs.getDouble("quantity");
                row.unitPrice     = rs.getDouble("unit_price");
                row.subtotal      = rs.getDouble("subtotal");
                row.totalAmount   = rs.getDouble("total_amount");
                row.paymentMethod = rs.getString("payment_method");
                row.soldBy        = rs.getString("sold_by");
                list.add(row);
            }
        } catch (Exception e) {
            System.err.println("[SaleDAO] getAllSales failed: " + e.getMessage());
        }
        return list;
    }

    // ── READ: filter by date range ────────────────────────────────────────────
    public List<SaleRow> getSalesByDateRange(String from, String to) {
        List<SaleRow> list = new ArrayList<>();
        String sql =
                "SELECT s.id, s.sale_date, " +
                        "       COALESCE(c.name, 'Unknown') AS customer_name, " +
                        "       COALESCE(p.name, 'Unknown') AS product_name, " +
                        "       COALESCE(si.quantity, 0) AS quantity, " +
                        "       COALESCE(si.unit_price, 0) AS unit_price, " +
                        "       COALESCE(si.subtotal, 0) AS subtotal, " +
                        "       s.total_amount, " +
                        "       COALESCE(s.payment_method, 'Cash') AS payment_method, " +
                        "       COALESCE(u.username, '') AS sold_by " +
                        "FROM sales s " +
                        "LEFT JOIN customers c    ON s.customer_id  = c.id " +
                        "LEFT JOIN users u        ON s.sold_by       = u.id " +
                        "LEFT JOIN sales_items si ON si.sale_id      = s.id " +
                        "LEFT JOIN products p     ON si.product_id   = p.id " +
                        "WHERE s.sale_date BETWEEN ? AND ? " +
                        "ORDER BY s.sale_date DESC, s.id DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, from);
            ps.setString(2, to);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                SaleRow row = new SaleRow();
                row.id            = rs.getInt("id");
                row.saleDate      = rs.getString("sale_date");
                row.customerName  = rs.getString("customer_name");
                row.productName   = rs.getString("product_name");
                row.quantity      = rs.getDouble("quantity");
                row.unitPrice     = rs.getDouble("unit_price");
                row.subtotal      = rs.getDouble("subtotal");
                row.totalAmount   = rs.getDouble("total_amount");
                row.paymentMethod = rs.getString("payment_method");
                row.soldBy        = rs.getString("sold_by");
                list.add(row);
            }
        } catch (Exception e) {
            System.err.println("[SaleDAO] getSalesByDateRange failed: " + e.getMessage());
        }
        return list;
    }

    // ── READ: summary stats ───────────────────────────────────────────────────
    public SaleSummary getSummary() {
        SaleSummary s = new SaleSummary();
        try (Connection conn = DatabaseConnection.getConnection()) {

            String q1 = "SELECT COALESCE(SUM(total_amount),0), COUNT(*) FROM sales " +
                    "WHERE MONTH(sale_date)=MONTH(CURDATE()) AND YEAR(sale_date)=YEAR(CURDATE())";
            try (PreparedStatement ps = conn.prepareStatement(q1);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    s.totalRevenue      = rs.getDouble(1);
                    s.totalTransactions = rs.getInt(2);
                    s.avgOrderValue     = s.totalTransactions > 0
                            ? s.totalRevenue / s.totalTransactions : 0;
                }
            }

            String q2 = "SELECT p.name FROM sales_items si " +
                    "JOIN products p ON si.product_id = p.id " +
                    "GROUP BY si.product_id ORDER BY SUM(si.quantity) DESC LIMIT 1";
            try (PreparedStatement ps = conn.prepareStatement(q2);
                 ResultSet rs = ps.executeQuery()) {
                s.topProduct = rs.next() ? rs.getString(1) : "N/A";
            }

            String q3 = "SELECT c.name FROM sales s " +
                    "JOIN customers c ON s.customer_id = c.id " +
                    "GROUP BY s.customer_id ORDER BY COUNT(*) DESC LIMIT 1";
            try (PreparedStatement ps = conn.prepareStatement(q3);
                 ResultSet rs = ps.executeQuery()) {
                s.topCustomer = rs.next() ? rs.getString(1) : "N/A";
            }

        } catch (Exception e) {
            System.err.println("[SaleDAO] getSummary failed: " + e.getMessage());
        }
        return s;
    }

    // ── READ: product names from stocks ───────────────────────────────────────
    public List<String> getProductNames() {
        List<String> names = new ArrayList<>();
        String sql = "SELECT DISTINCT product_name FROM stocks ORDER BY product_name ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) names.add(rs.getString(1));
        } catch (Exception e) {
            System.err.println("[SaleDAO] getProductNames failed: " + e.getMessage());
        }
        return names;
    }

    // ── READ: customer names ──────────────────────────────────────────────────
    public List<String> getCustomerNames() {
        List<String> names = new ArrayList<>();
        String sql = "SELECT name FROM customers ORDER BY name ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) names.add(rs.getString(1));
        } catch (Exception e) {
            System.err.println("[SaleDAO] getCustomerNames failed: " + e.getMessage());
        }
        return names;
    }

    // ── READ: unit price from stocks ──────────────────────────────────────────
    public double getUnitPrice(String productName) {
        String sql = "SELECT cost_per_unit FROM stocks WHERE product_name = ? LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, productName);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getDouble(1);
        } catch (Exception e) {
            System.err.println("[SaleDAO] getUnitPrice failed: " + e.getMessage());
        }
        return 0.0;
    }

    // ── DELETE: void a sale by id ─────────────────────────────────────────────
    public boolean deleteSale(int saleId) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM sales_items WHERE sale_id = ?")) {
                ps.setInt(1, saleId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM sales WHERE id = ?")) {
                ps.setInt(1, saleId);
                ps.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (Exception e) {
            System.err.println("[SaleDAO] deleteSale failed: " + e.getMessage());
            if (conn != null) try { conn.rollback(); } catch (Exception ignored) {}
            return false;
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (Exception ignored) {}
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private int findOrCreateCustomer(Connection conn, String name) throws SQLException {
        String find = "SELECT id FROM customers WHERE name = ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(find)) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }
        String insert = "INSERT INTO customers (name) VALUES (?)";
        try (PreparedStatement ps = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) return keys.getInt(1);
        }
        throw new SQLException("Could not find or create customer: " + name);
    }

    private int findUserId(Connection conn, String username) throws SQLException {
        String sql = "SELECT id FROM users WHERE username = ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }
        return 1; // fallback to first user (admin)
    }

    /**
     * Looks up product ID from products table first, then stocks table.
     * FIX: throws SQLException instead of silently returning id=1,
     * which caused FK constraint failures on sales_items.
     */
    private int findProductId(Connection conn, String productName) throws SQLException {
        // Try products table first
        String sql1 = "SELECT id FROM products WHERE name = ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql1)) {
            ps.setString(1, productName);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }
        // Try stocks table
        String sql2 = "SELECT id FROM stocks WHERE product_name = ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql2)) {
            ps.setString(1, productName);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }
        // FIX: throw instead of silently returning 1 (which caused FK failures)
        throw new SQLException("Product not found in products or stocks table: " + productName);
    }
}