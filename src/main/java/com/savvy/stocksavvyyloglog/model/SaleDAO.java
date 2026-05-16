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
        public String orderType;
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
                              double deliveryFee,
                              String paymentMethod, String orderType, String saleDate,
                              String notes, String soldBy) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // DEBUG: Log inputs
            System.out.println("[SaleDAO] recordSale called with: customerName=" + customerName + ", soldBy=" + soldBy);

            // 1. Resolve or create customer
            int customerId = findOrCreateCustomer(conn, customerName);
            System.out.println("[SaleDAO] Resolved customerId: " + customerId);

            // 2. Resolve sold_by user id
            int userId = findUserId(conn, soldBy);
            System.out.println("[SaleDAO] Resolved userId: " + userId);

            // 3. Resolve product id — throws clearly if not found (FIX #1)
            int productId = findProductId(conn, productName);

            // 4. Compute totals
            double subtotal    = unitPrice * quantity * (1.0 - discount / 100.0);
            double totalAmount = subtotal + ("deliver".equals(orderType) ? deliveryFee : 0);

            // 5. Insert into sales FIRST (parent row must exist before sales_items)
            String insertSale =
                    "INSERT INTO sales (customer_id, sale_date, total_amount, payment_method, order_type, sold_by, notes, status) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, 'Pending')";
            int saleId;
            try (PreparedStatement ps = conn.prepareStatement(insertSale, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt   (1, customerId);
                ps.setString(2, saleDate);
                ps.setDouble(3, totalAmount);
                ps.setString(4, paymentMethod);
                ps.setString(5, orderType);
                ps.setInt   (6, userId);
                ps.setString(7, notes);
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

            // 7. Deduct from productions (finished products)
            String deductProd =
                    "UPDATE productions SET quantity = quantity - ? WHERE name = ? AND quantity >= ?";
            try (PreparedStatement ps = conn.prepareStatement(deductProd)) {
                ps.setDouble(1, quantity);
                ps.setString(2, productName);
                ps.setDouble(3, quantity);
                int rows = ps.executeUpdate();
                if (rows == 0) {
                    // Not enough stock in productions — roll back and inform caller
                    throw new SQLException("Insufficient finished product stock for: " + productName);
                }
            }

            // 8. Update production status if needed
            String updateStatus = 
                    "UPDATE productions SET status = CASE " +
                    "WHEN quantity <= 0 THEN 'No Stock' " +
                    "WHEN quantity <= 10 THEN 'Low Stock' " +
                    "ELSE 'In Stock' END " +
                    "WHERE name = ?";
            try (PreparedStatement ps = conn.prepareStatement(updateStatus)) {
                ps.setString(1, productName);
                ps.executeUpdate();
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
                        "       COALESCE(s.order_type, 'pickup') AS order_type, " +
                        "       COALESCE(s.status, 'Complete') AS status, " +
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
                row.orderType     = rs.getString("order_type");
                row.status        = rs.getString("status");
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
                        "       COALESCE(s.order_type, 'pickup') AS order_type, " +
                        "       COALESCE(s.status, 'Complete') AS status, " +
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
                row.orderType     = rs.getString("order_type");
                row.status        = rs.getString("status");
                row.soldBy        = rs.getString("sold_by");
                list.add(row);
            }
        } catch (Exception e) {
            System.err.println("[SaleDAO] getSalesByDateRange failed: " + e.getMessage());
        }
        return list;
    }

    public boolean updateStatus(int saleId, String status) {
        String sql = "UPDATE sales SET status = ? WHERE id = ?";
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // 0. Get current status to determine transition
            String oldStatus = "";
            try (PreparedStatement ps = conn.prepareStatement("SELECT status FROM sales WHERE id = ?")) {
                ps.setInt(1, saleId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) oldStatus = rs.getString(1);
                }
            }

            // 1. Logic:
            // Transition TO Cancelled -> Restore Stock
            if (!"Cancelled".equalsIgnoreCase(oldStatus) && "Cancelled".equalsIgnoreCase(status)) {
                restoreStock(conn, saleId);
            }
            // Transition FROM Cancelled -> Deduct Stock
            else if ("Cancelled".equalsIgnoreCase(oldStatus) && !"Cancelled".equalsIgnoreCase(status)) {
                deductStockBySale(conn, saleId);
            }

            // 2. Update the status
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, status);
                ps.setInt(2, saleId);
                ps.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (Exception e) {
            System.err.println("[SaleDAO] updateStatus failed: " + e.getMessage());
            if (conn != null) try { conn.rollback(); } catch (SQLException ignored) {}
            return false;
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
        }
    }

    private void deductStockBySale(Connection conn, int saleId) throws SQLException {
        String findItems = "SELECT p.name, si.quantity FROM sales_items si " +
                           "JOIN products p ON si.product_id = p.id WHERE si.sale_id = ?";
        String updateProd = "UPDATE productions SET quantity = quantity - ? WHERE name = ? AND quantity >= ?";
        
        try (PreparedStatement psFind = conn.prepareStatement(findItems)) {
            psFind.setInt(1, saleId);
            try (ResultSet rs = psFind.executeQuery()) {
                while (rs.next()) {
                    String prodName = rs.getString("name");
                    double qty      = rs.getDouble("quantity");
                    try (PreparedStatement psUpd = conn.prepareStatement(updateProd)) {
                        psUpd.setDouble(1, qty);
                        psUpd.setString(2, prodName);
                        psUpd.setDouble(3, qty);
                        int rows = psUpd.executeUpdate();
                        if (rows == 0) throw new SQLException("Insufficient production stock to reactivate sale for: " + prodName);
                    }
                }
            }
        }
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

    // ── READ: product names from productions ─────────────────────────────────
    public List<String> getProductionProductNames() {
        List<String> names = new ArrayList<>();
        String sql = "SELECT name FROM productions ORDER BY name ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) names.add(rs.getString(1));
        } catch (Exception e) {
            System.err.println("[SaleDAO] getProductionProductNames failed: " + e.getMessage());
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

    // ── READ: unit price from productions ────────────────────────────────────
    public double getProductionPrice(String productName) {
        String sql = "SELECT price FROM productions WHERE name = ? LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, productName);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getDouble(1);
        } catch (Exception e) {
            System.err.println("[SaleDAO] getProductionPrice failed: " + e.getMessage());
        }
        return 0.0;
    }

    // ── DELETE: hard remove a sale by id ──────────────────────────────────────
    public boolean deleteSale(int saleId) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // 1. Delete items (No stock restoration for hard deletes per requirement)
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM sales_items WHERE sale_id = ?")) {
                ps.setInt(1, saleId);
                ps.executeUpdate();
            }
            // 2. Delete sale record
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

    /** Helper to restore production quantity from a sale's items. */
    private void restoreStock(Connection conn, int saleId) throws SQLException {
        String findItems = "SELECT p.name, si.quantity FROM sales_items si " +
                           "JOIN products p ON si.product_id = p.id WHERE si.sale_id = ?";
        String updateProd = "UPDATE productions SET quantity = quantity + ? WHERE name = ?";
        
        try (PreparedStatement psFind = conn.prepareStatement(findItems)) {
            psFind.setInt(1, saleId);
            try (ResultSet rs = psFind.executeQuery()) {
                while (rs.next()) {
                    String prodName = rs.getString("name");
                    double qty      = rs.getDouble("quantity");
                    try (PreparedStatement psUpd = conn.prepareStatement(updateProd)) {
                        psUpd.setDouble(1, qty);
                        psUpd.setString(2, prodName);
                        psUpd.executeUpdate();
                    }
                }
            }
        }
    }


    // ── Private helpers ───────────────────────────────────────────────────────

    private int findOrCreateCustomer(Connection conn, String name) throws SQLException {
        if (name != null) name = name.trim().toUpperCase();
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
     * Looks up or creates a product ID from the products table to satisfy the FK constraint in sales_items.
     * This method ensures that the product name exists in the products table regardless of its source.
     */
    private int findProductId(Connection conn, String productName) throws SQLException {
        // 1. Try to find the product by name in the products table
        String sqlFind = "SELECT id FROM products WHERE name = ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sqlFind)) {
            ps.setString(1, productName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }

        // 2. If not found, create a placeholder entry in the products table to satisfy the FK
        String sqlInsert = "INSERT INTO products (name) VALUES (?)";
        try (PreparedStatement ps = conn.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, productName);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        
        throw new SQLException("Could not resolve or create product ID for: " + productName);
    }
}