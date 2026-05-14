package com.savvy.stocksavvyyloglog.model;

import com.savvy.stocksavvyyloglog.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Stock.
 * Handles all MySQL CRUD operations for the stocks table.
 * Uses parameterized queries to prevent SQL injection.
 */
public class StockDAO {

    // ── CREATE ────────────────────────────────────────────────────────────────
    public boolean addStock(Stock stock) {
        String sql = "INSERT INTO stocks " +
                "(product_name, category, quantity, unit, cost_per_unit, supplier, date_received, notes, expiry_date, low_stock_threshold) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, stock.getProductName());
            ps.setString(2, stock.getCategory());
            ps.setDouble(3, stock.getQuantity());
            ps.setString(4, stock.getUnit());
            ps.setDouble(5, stock.getCostPerUnit());
            ps.setString(6, stock.getSupplier());
            ps.setString(7, stock.getDateReceived());
            ps.setString(8, stock.getNotes());
            ps.setString(9, stock.getExpiryDate());   // nullable
            ps.setInt(10, stock.getLowStockThreshold());

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            System.err.println("[StockDAO] addStock failed: " + e.getMessage());
            return false;
        }
    }

    // ── READ ──────────────────────────────────────────────────────────────────
    public List<Stock> getAllStocks() {
        List<Stock> list = new ArrayList<>();
        String sql = "SELECT * FROM stocks ORDER BY date_received DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) list.add(mapRow(rs));

        } catch (Exception e) {
            System.err.println("[StockDAO] getAllStocks failed: " + e.getMessage());
        }
        return list;
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────
    public boolean updateStock(Stock stock) {
        String sql = "UPDATE stocks SET product_name=?, category=?, quantity=?, unit=?, " +
                "cost_per_unit=?, supplier=?, date_received=?, notes=?, expiry_date=?, low_stock_threshold=? " +
                "WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, stock.getProductName());
            ps.setString(2, stock.getCategory());
            ps.setDouble(3, stock.getQuantity());
            ps.setString(4, stock.getUnit());
            ps.setDouble(5, stock.getCostPerUnit());
            ps.setString(6, stock.getSupplier());
            ps.setString(7, stock.getDateReceived());
            ps.setString(8, stock.getNotes());
            ps.setString(9, stock.getExpiryDate());   // nullable
            ps.setInt(10, stock.getLowStockThreshold());
            ps.setInt(11, stock.getId());

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            System.err.println("[StockDAO] updateStock failed: " + e.getMessage());
            return false;
        }
    }

    // ── DELETE ────────────────────────────────────────────────────────────────
    public boolean deleteStock(int id) {
        String sql = "DELETE FROM stocks WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            System.err.println("[StockDAO] deleteStock failed: " + e.getMessage());
            return false;
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────────
    private Stock mapRow(ResultSet rs) throws SQLException {
        Stock s = new Stock();
        s.setId(rs.getInt("id"));
        s.setProductName(rs.getString("product_name"));
        s.setCategory(rs.getString("category"));
        s.setQuantity(rs.getDouble("quantity"));
        s.setUnit(rs.getString("unit"));
        s.setCostPerUnit(rs.getDouble("cost_per_unit"));
        s.setSupplier(rs.getString("supplier"));
        s.setDateReceived(rs.getString("date_received"));
        s.setNotes(rs.getString("notes"));
        s.setExpiryDate(rs.getString("expiry_date"));           // NEW
        s.setLowStockThreshold(rs.getInt("low_stock_threshold")); // NEW
        return s;
    }
}