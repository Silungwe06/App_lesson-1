package com.sitech.easytracker.model;

/** A stock item. Quantity is only ever changed through stock movements. */
public class Product {
    public static final String STATUS_IN = "IN";
    public static final String STATUS_LOW = "LOW";
    public static final String STATUS_OUT = "OUT";

    public long id;
    public String name;
    public String sku;
    public long categoryId;
    public String categoryName;
    public double purchasePrice;
    public double sellingPrice;
    public int quantity;
    public int minStock;
    public String description;
    public String dateAdded;

    /** IN = In Stock, LOW = at or below the minimum threshold, OUT = nothing left. */
    public String status() {
        if (quantity <= 0) return STATUS_OUT;
        if (quantity <= minStock) return STATUS_LOW;
        return STATUS_IN;
    }
}
