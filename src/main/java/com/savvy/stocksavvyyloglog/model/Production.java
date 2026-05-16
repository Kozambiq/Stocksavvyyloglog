package com.savvy.stocksavvyyloglog.model;

/**
 * Model class representing a Production entry.
 */
public class Production {

    private int    id;
    private String name;
    private double quantity;
    private double price;
    private String unit;
    private String status; // 'No Stock', 'Low Stock', 'In Stock'
    private String createdAt;

    public Production() {}

    public Production(String name, double quantity, double price, String unit, String status) {
        this.name = name;
        this.quantity = quantity;
        this.price = price;
        this.unit = unit;
        this.status = status;
    }

    public int    getId()                 { return id; }
    public void   setId(int id)           { this.id = id; }

    public String getName()               { return name; }
    public void   setName(String v)       { this.name = v; }

    public double getQuantity()           { return quantity; }
    public void   setQuantity(double v)   { this.quantity = v; }

    public double getPrice()              { return price; }
    public void   setPrice(double v)      { this.price = v; }

    public String getUnit()               { return unit; }
    public void   setUnit(String v)       { this.unit = v; }

    public String getStatus()             { return status; }
    public void   setStatus(String v)     { this.status = v; }

    public String getCreatedAt()          { return createdAt; }
    public void   setCreatedAt(String v)  { this.createdAt = v; }
}
