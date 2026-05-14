package com.savvy.stocksavvyyloglog.model;

/**
 * Model class representing a stock entry.
 * Demonstrates encapsulation (OOP pillar).
 */
public class Stock {

    private int    id;
    private String productName;
    private String category;
    private double quantity;
    private String unit;
    private double costPerUnit;
    private String supplier;
    private String dateReceived;
    private String notes;
    private String expiryDate;       // NEW: expiry date (nullable)
    private int    lowStockThreshold; // NEW: minimum quantity before low-stock alert

    // ── Constructors ──────────────────────────────────────────────────────────
    public Stock() {
        this.lowStockThreshold = 5; // sensible default
    }

    public Stock(String productName, String category, double quantity, String unit,
                 double costPerUnit, String supplier, String dateReceived, String notes,
                 String expiryDate, int lowStockThreshold) {
        this.productName       = productName;
        this.category          = category;
        this.quantity          = quantity;
        this.unit              = unit;
        this.costPerUnit       = costPerUnit;
        this.supplier          = supplier;
        this.dateReceived      = dateReceived;
        this.notes             = notes;
        this.expiryDate        = expiryDate;
        this.lowStockThreshold = lowStockThreshold;
    }

    // ── Computed ──────────────────────────────────────────────────────────────
    public double getTotalCost() { return quantity * costPerUnit; }

    public boolean isLowStock() { return quantity <= lowStockThreshold; }

    // ── Getters & Setters ─────────────────────────────────────────────────────
    public int    getId()                           { return id; }
    public void   setId(int id)                     { this.id = id; }

    public String getProductName()                  { return productName; }
    public void   setProductName(String v)          { this.productName = v; }

    public String getCategory()                     { return category; }
    public void   setCategory(String v)             { this.category = v; }

    public double getQuantity()                     { return quantity; }
    public void   setQuantity(double v)             { this.quantity = v; }

    public String getUnit()                         { return unit; }
    public void   setUnit(String v)                 { this.unit = v; }

    public double getCostPerUnit()                  { return costPerUnit; }
    public void   setCostPerUnit(double v)          { this.costPerUnit = v; }

    public String getSupplier()                     { return supplier; }
    public void   setSupplier(String v)             { this.supplier = v; }

    public String getDateReceived()                 { return dateReceived; }
    public void   setDateReceived(String v)         { this.dateReceived = v; }

    public String getNotes()                        { return notes; }
    public void   setNotes(String v)                { this.notes = v; }

    public String getExpiryDate()                   { return expiryDate; }
    public void   setExpiryDate(String v)           { this.expiryDate = v; }

    public int    getLowStockThreshold()            { return lowStockThreshold; }
    public void   setLowStockThreshold(int v)       { this.lowStockThreshold = v; }

    @Override
    public String toString() {
        return String.format("Stock{id=%d, product='%s', qty=%.2f %s, cost=₱%.2f, expiry=%s}",
                id, productName, quantity, unit, costPerUnit, expiryDate);
    }
}