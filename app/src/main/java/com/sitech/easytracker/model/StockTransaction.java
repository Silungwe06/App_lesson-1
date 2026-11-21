package com.sitech.easytracker.model;

/** One line of stock history (a stock in or a stock out). */
public class StockTransaction {
    public static final String IN = "IN";
    public static final String OUT = "OUT";

    public long id;
    /** -1 when the product has since been deleted. */
    public long productId = -1;
    public String productName;
    public String sku;
    public String type;
    public int quantity;
    public int previousQty;
    public int newQty;
    public double unitPrice;
    public String notes;
    public String txnDate;

    public boolean isIn() {
        return IN.equals(type);
    }
}
