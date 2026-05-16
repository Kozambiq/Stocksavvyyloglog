package com.savvy.stocksavvyyloglog.model;

import com.savvy.stocksavvyyloglog.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Production.
 */
public class ProductionDAO {

    public List<Production> getAllProductions() {
        List<Production> list = new ArrayList<>();
        String sql = "SELECT * FROM productions ORDER BY created_at DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Production p = new Production();
                p.setId(rs.getInt("id"));
                p.setName(rs.getString("name"));
                p.setQuantity(rs.getDouble("quantity"));
                p.setPrice(rs.getDouble("price"));
                p.setUnit(rs.getString("unit"));
                p.setStatus(rs.getString("status"));
                p.setCreatedAt(rs.getString("created_at"));
                list.add(p);
            }

        } catch (Exception e) {
            System.err.println("[ProductionDAO] getAllProductions failed: " + e.getMessage());
        }
        return list;
    }

    public static class IngredientDeduction {
        public String name;
        public double qty;
        public IngredientDeduction(String name, double qty) { this.name = name; this.qty = qty; }
    }

    public boolean saveProductionWithIngredients(Production p, List<IngredientDeduction> ingredients) {
        String prodSql = "INSERT INTO productions (name, quantity, price, unit, status) VALUES (?, ?, ?, ?, ?)";
        String ingSql  = "INSERT INTO production_ingredients (production_id, stock_id, quantity_used) VALUES (?, ?, ?)";
        
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            int productionId = -1;
            try (PreparedStatement ps = conn.prepareStatement(prodSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, p.getName());
                ps.setDouble(2, p.getQuantity());
                ps.setDouble(3, p.getPrice());
                ps.setString(4, p.getUnit());
                ps.setString(5, p.getStatus());
                ps.executeUpdate();
                
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) productionId = rs.getInt(1);
            }

            if (productionId != -1) {
                StockDAO stockDAO = new StockDAO();
                for (IngredientDeduction ing : ingredients) {
                    // 1. Deduct stock
                    stockDAO.deductStock(conn, ing.name, ing.qty);
                    
                    // 2. Record ingredient
                    int stockId = getStockIdByName(conn, ing.name);
                    try (PreparedStatement ps = conn.prepareStatement(ingSql)) {
                        ps.setInt(1, productionId);
                        ps.setInt(2, stockId);
                        ps.setDouble(3, ing.qty);
                        ps.executeUpdate();
                    }
                }
            }

            conn.commit();
            return true;
        } catch (Exception e) {
            System.err.println("[ProductionDAO] saveProductionWithIngredients failed: " + e.getMessage());
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            return false;
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    public boolean deleteProduction(int id) {
        String sql = "DELETE FROM productions WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("[ProductionDAO] deleteProduction failed: " + e.getMessage());
            return false;
        }
    }

    private int getStockIdByName(Connection conn, String name) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM stocks WHERE product_name = ?")) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt("id") : -1;
        }
    }

}
