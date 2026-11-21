package com.sitech.easytracker.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.sitech.easytracker.model.DashboardStats;
import com.sitech.easytracker.model.Product;
import com.sitech.easytracker.model.StockTransaction;
import com.sitech.easytracker.util.Fmt;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * All SQLite access lives here. Screens never write SQL.
 * Every change to stock and its history happens inside one database transaction.
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    public static final String DB_NAME = "easytracker.db";
    private static final int DB_VERSION = 1;

    private static final String PRODUCT_SELECT =
            "SELECT p.id, p.name, p.sku, p.category_id, c.name AS category_name, "
                    + "p.purchase_price, p.selling_price, p.quantity, p.min_stock, "
                    + "p.description, p.date_added "
                    + "FROM products p LEFT JOIN categories c ON c.id = p.category_id ";

    private static DatabaseHelper instance;
    private final Context appContext;

    public static synchronized DatabaseHelper get(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        this.appContext = context;
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE categories ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT NOT NULL COLLATE NOCASE UNIQUE)");

        db.execSQL("CREATE TABLE products ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT NOT NULL, "
                + "sku TEXT NOT NULL COLLATE NOCASE UNIQUE, "
                + "category_id INTEGER, "
                + "purchase_price REAL NOT NULL DEFAULT 0, "
                + "selling_price REAL NOT NULL DEFAULT 0, "
                + "quantity INTEGER NOT NULL DEFAULT 0 CHECK (quantity >= 0), "
                + "min_stock INTEGER NOT NULL DEFAULT 0 CHECK (min_stock >= 0), "
                + "description TEXT, "
                + "date_added TEXT NOT NULL, "
                + "FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL)");

        db.execSQL("CREATE TABLE stock_transactions ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "product_id INTEGER, "
                + "product_name TEXT NOT NULL, "
                + "sku TEXT NOT NULL, "
                + "type TEXT NOT NULL CHECK (type IN ('IN','OUT')), "
                + "quantity INTEGER NOT NULL CHECK (quantity > 0), "
                + "previous_qty INTEGER NOT NULL, "
                + "new_qty INTEGER NOT NULL, "
                + "unit_price REAL NOT NULL DEFAULT 0, "
                + "notes TEXT, "
                + "txn_date TEXT NOT NULL, "
                + "FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE SET NULL)");

        db.execSQL("CREATE INDEX idx_products_name ON products(name)");
        db.execSQL("CREATE INDEX idx_products_category ON products(category_id)");
        db.execSQL("CREATE INDEX idx_txn_product ON stock_transactions(product_id)");
        db.execSQL("CREATE INDEX idx_txn_date ON stock_transactions(txn_date)");

        String[] defaults = {"General", "Electronics", "Groceries", "Clothing", "Hardware", "Stationery"};
        for (String n : defaults) {
            ContentValues v = new ContentValues();
            v.put("name", n);
            db.insert("categories", null, v);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Version 1 is the first release. Future migrations go here.
    }

    // ------------------------------------------------------------------ categories

    public List<String> getCategoryNames() {
        List<String> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT name FROM categories ORDER BY name COLLATE NOCASE", null);
        try {
            while (c.moveToNext()) list.add(c.getString(0));
        } finally {
            c.close();
        }
        return list;
    }

    private long getOrCreateCategory(SQLiteDatabase db, String name) {
        String n = name == null ? "" : name.trim();
        if (n.isEmpty()) n = "General";
        Cursor c = db.rawQuery("SELECT id FROM categories WHERE name = ? COLLATE NOCASE", new String[]{n});
        try {
            if (c.moveToFirst()) return c.getLong(0);
        } finally {
            c.close();
        }
        ContentValues v = new ContentValues();
        v.put("name", n);
        return db.insertOrThrow("categories", null, v);
    }

    // ------------------------------------------------------------------ products

    private boolean skuExists(SQLiteDatabase db, String sku, long excludeId) {
        Cursor c = db.rawQuery("SELECT 1 FROM products WHERE sku = ? COLLATE NOCASE AND id <> ? LIMIT 1",
                new String[]{sku, String.valueOf(excludeId)});
        try {
            return c.moveToFirst();
        } finally {
            c.close();
        }
    }

    /** Adds a product. A starting quantity above zero is logged as an "Opening stock" Stock In. */
    public long addProduct(Product p) throws DbException {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            if (skuExists(db, p.sku, -1)) {
                throw new DbException("The product code \"" + p.sku + "\" is already used by another product.");
            }
            long catId = getOrCreateCategory(db, p.categoryName);
            ContentValues v = new ContentValues();
            v.put("name", p.name);
            v.put("sku", p.sku);
            v.put("category_id", catId);
            v.put("purchase_price", p.purchasePrice);
            v.put("selling_price", p.sellingPrice);
            v.put("quantity", p.quantity);
            v.put("min_stock", p.minStock);
            v.put("description", p.description);
            v.put("date_added", Fmt.today());
            long id = db.insertOrThrow("products", null, v);
            if (p.quantity > 0) {
                insertTransaction(db, id, p.name, p.sku, StockTransaction.IN, p.quantity,
                        0, p.quantity, p.purchasePrice, "Opening stock", Fmt.stampNow());
            }
            db.setTransactionSuccessful();
            return id;
        } catch (SQLException e) {
            throw new DbException("Could not save the product. Please check the details and try again.");
        } finally {
            db.endTransaction();
        }
    }

    /** Updates product details. Quantity is not touched here; use a stock movement for that. */
    public void updateProduct(Product p) throws DbException {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            if (skuExists(db, p.sku, p.id)) {
                throw new DbException("The product code \"" + p.sku + "\" is already used by another product.");
            }
            long catId = getOrCreateCategory(db, p.categoryName);
            ContentValues v = new ContentValues();
            v.put("name", p.name);
            v.put("sku", p.sku);
            v.put("category_id", catId);
            v.put("purchase_price", p.purchasePrice);
            v.put("selling_price", p.sellingPrice);
            v.put("min_stock", p.minStock);
            v.put("description", p.description);
            int rows = db.update("products", v, "id = ?", new String[]{String.valueOf(p.id)});
            if (rows != 1) throw new DbException("This product no longer exists.");
            // Keep history readable if the name or code changed.
            ContentValues t = new ContentValues();
            t.put("product_name", p.name);
            t.put("sku", p.sku);
            db.update("stock_transactions", t, "product_id = ?", new String[]{String.valueOf(p.id)});
            db.setTransactionSuccessful();
        } catch (SQLException e) {
            throw new DbException("Could not update the product. Please try again.");
        } finally {
            db.endTransaction();
        }
    }

    /** Deletes a product. Its stock history is kept (the link to the product is cleared). */
    public void deleteProduct(long id) {
        getWritableDatabase().delete("products", "id = ?", new String[]{String.valueOf(id)});
    }

    public Product getProduct(long id) {
        Cursor c = getReadableDatabase().rawQuery(PRODUCT_SELECT + "WHERE p.id = ?",
                new String[]{String.valueOf(id)});
        try {
            if (c.moveToFirst()) return readProduct(c);
            return null;
        } finally {
            c.close();
        }
    }

    /**
     * @param query    text matched against name, code and category (may be null)
     * @param category exact category name, or null for all
     * @param status   "ALL", Product.STATUS_IN, STATUS_LOW or STATUS_OUT
     */
    public List<Product> getProducts(String query, String category, String status) {
        StringBuilder sql = new StringBuilder(PRODUCT_SELECT).append("WHERE 1=1 ");
        List<String> args = new ArrayList<>();
        if (query != null && !query.trim().isEmpty()) {
            String like = "%" + query.trim() + "%";
            sql.append("AND (p.name LIKE ? OR p.sku LIKE ? OR c.name LIKE ?) ");
            args.add(like);
            args.add(like);
            args.add(like);
        }
        if (category != null) {
            sql.append("AND c.name = ? COLLATE NOCASE ");
            args.add(category);
        }
        if (Product.STATUS_OUT.equals(status)) {
            sql.append("AND p.quantity <= 0 ");
        } else if (Product.STATUS_LOW.equals(status)) {
            sql.append("AND p.quantity > 0 AND p.quantity <= p.min_stock ");
        } else if (Product.STATUS_IN.equals(status)) {
            sql.append("AND p.quantity > p.min_stock ");
        }
        sql.append("ORDER BY p.name COLLATE NOCASE");
        return queryProducts(sql.toString(), args.toArray(new String[0]));
    }

    /** Products at or below their minimum threshold (low stock and out of stock), most urgent first. */
    public List<Product> getAlertProducts() {
        return queryProducts(PRODUCT_SELECT
                + "WHERE p.quantity <= p.min_stock OR p.quantity <= 0 "
                + "ORDER BY p.quantity ASC, p.name COLLATE NOCASE", null);
    }

    private List<Product> queryProducts(String sql, String[] args) {
        List<Product> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(sql, args);
        try {
            while (c.moveToNext()) list.add(readProduct(c));
        } finally {
            c.close();
        }
        return list;
    }

    private Product readProduct(Cursor c) {
        Product p = new Product();
        p.id = c.getLong(0);
        p.name = c.getString(1);
        p.sku = c.getString(2);
        p.categoryId = c.isNull(3) ? -1 : c.getLong(3);
        p.categoryName = c.isNull(4) ? "Uncategorised" : c.getString(4);
        p.purchasePrice = c.getDouble(5);
        p.sellingPrice = c.getDouble(6);
        p.quantity = c.getInt(7);
        p.minStock = c.getInt(8);
        p.description = c.isNull(9) ? "" : c.getString(9);
        p.dateAdded = c.getString(10);
        return p;
    }

    // ------------------------------------------------------------------ stock movements

    /**
     * Records a Stock In or Stock Out. The quantity update and the history row are saved together
     * or not at all. Stock can never go below zero.
     *
     * @param priceOverride Stock In only: a new purchase price (null keeps the product's price)
     * @param date          yyyy-MM-dd
     */
    public void recordMovement(long productId, String type, int qty, Double priceOverride,
                               String date, String notes) throws DbException {
        if (qty <= 0) throw new DbException("Quantity must be greater than zero.");
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            String name;
            String sku;
            int prev;
            double purchase;
            double selling;
            Cursor c = db.rawQuery("SELECT name, sku, quantity, purchase_price, selling_price "
                    + "FROM products WHERE id = ?", new String[]{String.valueOf(productId)});
            try {
                if (!c.moveToFirst()) throw new DbException("This product no longer exists.");
                name = c.getString(0);
                sku = c.getString(1);
                prev = c.getInt(2);
                purchase = c.getDouble(3);
                selling = c.getDouble(4);
            } finally {
                c.close();
            }

            int newQty;
            double unit;
            ContentValues v = new ContentValues();
            if (StockTransaction.IN.equals(type)) {
                if ((long) prev + qty > Fmt.MAX_QTY) {
                    throw new DbException("That quantity is too large for this product.");
                }
                newQty = prev + qty;
                unit = priceOverride != null ? priceOverride : purchase;
                if (priceOverride != null) v.put("purchase_price", priceOverride);
            } else {
                if (qty > prev) {
                    throw new DbException("Not enough stock. Only " + prev + " unit(s) of "
                            + name + " available.");
                }
                newQty = prev - qty;
                unit = selling;
            }
            v.put("quantity", newQty);
            int rows = db.update("products", v, "id = ? AND quantity = ?",
                    new String[]{String.valueOf(productId), String.valueOf(prev)});
            if (rows != 1) throw new DbException("Stock changed while saving. Please try again.");

            insertTransaction(db, productId, name, sku, type, qty, prev, newQty, unit,
                    notes, Fmt.stampForDate(date));
            db.setTransactionSuccessful();
        } catch (SQLException e) {
            throw new DbException("Could not save the stock movement. Please try again.");
        } finally {
            db.endTransaction();
        }
    }

    private void insertTransaction(SQLiteDatabase db, long productId, String name, String sku,
                                   String type, int qty, int prev, int now, double unit,
                                   String notes, String stamp) {
        ContentValues t = new ContentValues();
        t.put("product_id", productId);
        t.put("product_name", name);
        t.put("sku", sku);
        t.put("type", type);
        t.put("quantity", qty);
        t.put("previous_qty", prev);
        t.put("new_qty", now);
        t.put("unit_price", unit);
        t.put("notes", notes);
        t.put("txn_date", stamp);
        db.insertOrThrow("stock_transactions", null, t);
    }

    /**
     * @param type      "IN", "OUT" or null for both
     * @param productId a product id, or -1 for all products
     * @param query     matched against product name / code (may be null)
     */
    public List<StockTransaction> getTransactions(String type, long productId, String query, int limit) {
        StringBuilder sql = new StringBuilder(
                "SELECT id, product_id, product_name, sku, type, quantity, previous_qty, new_qty, "
                        + "unit_price, notes, txn_date FROM stock_transactions WHERE 1=1 ");
        List<String> args = new ArrayList<>();
        if (type != null) {
            sql.append("AND type = ? ");
            args.add(type);
        }
        if (productId >= 0) {
            sql.append("AND product_id = ? ");
            args.add(String.valueOf(productId));
        }
        if (query != null && !query.trim().isEmpty()) {
            String like = "%" + query.trim() + "%";
            sql.append("AND (product_name LIKE ? OR sku LIKE ?) ");
            args.add(like);
            args.add(like);
        }
        sql.append("ORDER BY txn_date DESC, id DESC LIMIT ").append(limit);

        List<StockTransaction> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(sql.toString(), args.toArray(new String[0]));
        try {
            while (c.moveToNext()) {
                StockTransaction t = new StockTransaction();
                t.id = c.getLong(0);
                t.productId = c.isNull(1) ? -1 : c.getLong(1);
                t.productName = c.getString(2);
                t.sku = c.getString(3);
                t.type = c.getString(4);
                t.quantity = c.getInt(5);
                t.previousQty = c.getInt(6);
                t.newQty = c.getInt(7);
                t.unitPrice = c.getDouble(8);
                t.notes = c.isNull(9) ? "" : c.getString(9);
                t.txnDate = c.getString(10);
                list.add(t);
            }
        } finally {
            c.close();
        }
        return list;
    }

    // ------------------------------------------------------------------ dashboard

    public DashboardStats getStats(boolean valueAtSellingPrice) {
        String priceCol = valueAtSellingPrice ? "selling_price" : "purchase_price";
        DashboardStats s = new DashboardStats();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT COUNT(*), COALESCE(SUM(quantity), 0), COALESCE(SUM(quantity * " + priceCol + "), 0), "
                        + "COALESCE(SUM(CASE WHEN quantity <= 0 THEN 1 ELSE 0 END), 0), "
                        + "COALESCE(SUM(CASE WHEN quantity > 0 AND quantity <= min_stock THEN 1 ELSE 0 END), 0) "
                        + "FROM products", null);
        try {
            if (c.moveToFirst()) {
                s.totalProducts = c.getInt(0);
                s.totalUnits = c.getLong(1);
                s.totalValue = c.getDouble(2);
                s.outOfStock = c.getInt(3);
                s.lowStock = c.getInt(4);
            }
        } finally {
            c.close();
        }
        return s;
    }

    // ------------------------------------------------------------------ export / backup

    /** Writes the whole inventory as CSV (opens in Excel / Google Sheets). */
    public void exportInventoryCsv(OutputStream out, boolean valueAtSellingPrice) throws IOException {
        StringBuilder sb = new StringBuilder("\uFEFF");
        sb.append("ID,Name,SKU,Category,Purchase Price,Selling Price,Quantity,Min Stock,Status,Stock Value,Date Added,Description\n");
        for (Product p : getProducts(null, null, "ALL")) {
            double value = p.quantity * (valueAtSellingPrice ? p.sellingPrice : p.purchasePrice);
            String status = Product.STATUS_OUT.equals(p.status()) ? "Out of Stock"
                    : Product.STATUS_LOW.equals(p.status()) ? "Low Stock" : "In Stock";
            sb.append(p.id).append(',')
                    .append(Fmt.csv(p.name)).append(',')
                    .append(Fmt.csv(p.sku)).append(',')
                    .append(Fmt.csv(p.categoryName)).append(',')
                    .append(p.purchasePrice).append(',')
                    .append(p.sellingPrice).append(',')
                    .append(p.quantity).append(',')
                    .append(p.minStock).append(',')
                    .append(status).append(',')
                    .append(value).append(',')
                    .append(Fmt.csv(p.dateAdded)).append(',')
                    .append(Fmt.csv(p.description)).append('\n');
        }
        out.write(sb.toString().getBytes("UTF-8"));
        out.flush();
    }

    /** Copies the database file to the given stream. */
    public void backupTo(OutputStream out) throws IOException {
        SQLiteDatabase db = getWritableDatabase();
        Cursor c = db.rawQuery("PRAGMA wal_checkpoint(FULL)", null);
        try {
            c.moveToFirst();
        } finally {
            c.close();
        }
        File file = appContext.getDatabasePath(DB_NAME);
        InputStream in = new FileInputStream(file);
        try {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            out.flush();
        } finally {
            in.close();
        }
    }
}
