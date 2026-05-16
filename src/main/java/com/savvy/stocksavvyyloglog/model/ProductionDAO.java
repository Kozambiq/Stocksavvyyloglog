package com.savvy.stocksavvyyloglog.model;

import com.savvy.stocksavvyyloglog.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductionDAO {

    public boolean addProduction(Production p) {
        String sql = "INSERT INTO production_batches (batch_number, product_name, quantity_produced, unit, production_date, status, notes) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, p.getBatchNumber());
            ps.setString(2, p.getProductName());
            ps.setDouble(3, p.getQuantityProduced());
            ps.setString(4, p.getUnit());
            ps.setString(5, p.getProductionDate());
            ps.setString(6, p.getStatus() != null ? p.getStatus() : "In Progress");
            ps.setString(7, p.getNotes());

            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("[ProductionDAO] addProduction failed: " + e.getMessage());
            return false;
        }
    }

    public List<Production> getAllProductions() {
        List<Production> list = new ArrayList<>();
        String sql = "SELECT * FROM production_batches ORDER BY production_date DESC, id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Production p = new Production();
                p.setId(rs.getInt("id"));
                p.setBatchNumber(rs.getString("batch_number"));
                p.setProductName(rs.getString("product_name"));
                p.setQuantityProduced(rs.getDouble("quantity_produced"));
                p.setUnit(rs.getString("unit"));
                p.setProductionDate(rs.getString("production_date"));
                p.setStatus(rs.getString("status"));
                p.setNotes(rs.getString("notes"));
                p.setCreatedAt(rs.getString("created_at"));
                list.add(p);
            }
        } catch (Exception e) {
            System.err.println("[ProductionDAO] getAllProductions failed: " + e.getMessage());
        }
        return list;
    }

    public boolean updateProduction(Production p) {
        String sql = "UPDATE production_batches SET batch_number=?, product_name=?, quantity_produced=?, unit=?, production_date=?, status=?, notes=? WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, p.getBatchNumber());
            ps.setString(2, p.getProductName());
            ps.setDouble(3, p.getQuantityProduced());
            ps.setString(4, p.getUnit());
            ps.setString(5, p.getProductionDate());
            ps.setString(6, p.getStatus());
            ps.setString(7, p.getNotes());
            ps.setInt(8, p.getId());

            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("[ProductionDAO] updateProduction failed: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteProduction(int id) {
        String sql = "DELETE FROM production_batches WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("[ProductionDAO] deleteProduction failed: " + e.getMessage());
            return false;
        }
    }

    public int getTotalProductions() {
        String sql = "SELECT COUNT(*) FROM production_batches";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) { System.err.println("[ProductionDAO] getTotalProductions failed: " + e.getMessage()); }
        return 0;
    }

    public int getInProgressCount() {
        String sql = "SELECT COUNT(*) FROM production_batches WHERE status = 'In Progress'";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) { System.err.println("[ProductionDAO] getInProgressCount failed: " + e.getMessage()); }
        return 0;
    }

    public double getTotalQuantityProduced() {
        String sql = "SELECT COALESCE(SUM(quantity_produced), 0) FROM production_batches";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getDouble(1);
        } catch (Exception e) { System.err.println("[ProductionDAO] getTotalQuantityProduced failed: " + e.getMessage()); }
        return 0;
    }
}