package com.savvy.stocksavvyyloglog.model;

import java.time.LocalDate;

/**
 * Model class representing a single sale transaction.
 * Mirrors the Stock model pattern.
 */
public class NewSaleModel {

    private int       id;
    private String    customerName;
    private String    product;
    private int       quantity;
    private double    unitPrice;
    private double    discountPercent;
    private String    paymentMethod;
    private LocalDate deliveryDate;
    private String    notes;

    // ── Constructors ──────────────────────────────────────────────────────────

    public NewSaleModel() {
        this.quantity        = 1;
        this.discountPercent = 0.0;
        this.paymentMethod   = "Cash";
        this.deliveryDate    = LocalDate.now();
    }

    public NewSaleModel(String customerName, String product, int quantity,
                        double unitPrice, double discountPercent,
                        String paymentMethod, LocalDate deliveryDate, String notes) {
        this.customerName    = customerName;
        this.product         = product;
        this.quantity        = quantity;
        this.unitPrice       = unitPrice;
        this.discountPercent = discountPercent;
        this.paymentMethod   = paymentMethod;
        this.deliveryDate    = deliveryDate;
        this.notes           = notes;
    }

    // ── Computed ──────────────────────────────────────────────────────────────

    /** Returns the final total after discount. */
    public double getTotal() {
        return unitPrice * quantity * (1.0 - discountPercent / 100.0);
    }

    /** Basic validation — returns error string or empty if valid. */
    public String validate() {
        StringBuilder errors = new StringBuilder();
        if (customerName == null || customerName.trim().isEmpty())
            errors.append("• Customer name is required.\n");
        if (product == null || product.trim().isEmpty())
            errors.append("• Please select a product.\n");
        if (unitPrice <= 0)
            errors.append("• Unit price must be greater than zero.\n");
        return errors.toString();
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public int       getId()                         { return id; }
    public void      setId(int id)                   { this.id = id; }

    public String    getCustomerName()               { return customerName; }
    public void      setCustomerName(String v)       { this.customerName = v; }

    public String    getProduct()                    { return product; }
    public void      setProduct(String v)            { this.product = v; }

    public int       getQuantity()                   { return quantity; }
    public void      setQuantity(int v)              { this.quantity = v; }

    public double    getUnitPrice()                  { return unitPrice; }
    public void      setUnitPrice(double v)          { this.unitPrice = v; }

    public double    getDiscountPercent()            { return discountPercent; }
    public void      setDiscountPercent(double v)    { this.discountPercent = v; }

    public String    getPaymentMethod()              { return paymentMethod; }
    public void      setPaymentMethod(String v)      { this.paymentMethod = v; }

    public LocalDate getDeliveryDate()               { return deliveryDate; }
    public void      setDeliveryDate(LocalDate v)    { this.deliveryDate = v; }

    public String    getNotes()                      { return notes; }
    public void      setNotes(String v)              { this.notes = v; }

    @Override
    public String toString() {
        return String.format(
                "Sale{customer='%s', product='%s', qty=%d, price=₱%.2f, " +
                        "discount=%.1f%%, payment='%s', delivery=%s, total=₱%.2f}",
                customerName, product, quantity, unitPrice,
                discountPercent, paymentMethod, deliveryDate, getTotal()
        );
    }
}