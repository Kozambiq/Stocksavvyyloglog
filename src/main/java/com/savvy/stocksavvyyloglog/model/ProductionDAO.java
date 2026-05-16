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

    public boolean addProduction(Production p) {
        String sql = "INSERT INTO productions (name, quantity, price, unit, status) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, p.getName());
            ps.setDouble(2, p.getQuantity());
            ps.setDouble(3, p.getPrice());
            ps.setString(4, p.getUnit());
            ps.setString(5, p.getStatus());

            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("[ProductionDAO] addProduction failed: " + e.getMessage());
            return false;
        }
    }
}
