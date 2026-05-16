package com.savvy.stocksavvyyloglog.model;

public class Production {

    private int id;
    private String batchNumber;
    private String productName;
    private double quantityProduced;
    private String unit;
    private String productionDate;
    private String status;
    private String notes;
    private String createdAt;

    public Production() {}

    public Production(int id, String batchNumber, String productName, double quantityProduced,
                      String unit, String productionDate, String status, String notes, String createdAt) {
        this.id = id;
        this.batchNumber = batchNumber;
        this.productName = productName;
        this.quantityProduced = quantityProduced;
        this.unit = unit;
        this.productionDate = productionDate;
        this.status = status;
        this.notes = notes;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getBatchNumber() { return batchNumber; }
    public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public double getQuantityProduced() { return quantityProduced; }
    public void setQuantityProduced(double quantityProduced) { this.quantityProduced = quantityProduced; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getProductionDate() { return productionDate; }
    public void setProductionDate(String productionDate) { this.productionDate = productionDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}