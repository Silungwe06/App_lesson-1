package com.sitech.easytracker.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.sitech.easytracker.R;
import com.sitech.easytracker.db.DbException;
import com.sitech.easytracker.model.Product;
import com.sitech.easytracker.util.Fmt;

/** Add a new product, or edit an existing one (pass EXTRA_PRODUCT_ID). */
public class ProductFormActivity extends BaseActivity {
    public static final String EXTRA_PRODUCT_ID = "product_id";

    private long productId = -1;
    private TextInputLayout tilName, tilSku, tilCategory, tilPurchase, tilSelling, tilQty, tilMin;
    private TextInputEditText etName, etSku, etPurchase, etSelling, etQty, etMin, etDesc;
    private AutoCompleteTextView acCategory;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_form);

        productId = getIntent().getLongExtra(EXTRA_PRODUCT_ID, -1);
        final boolean editing = productId >= 0;
        setupToolbar(editing ? "Edit Product" : "Add Product", true);

        tilName = findViewById(R.id.til_name);
        tilSku = findViewById(R.id.til_sku);
        tilCategory = findViewById(R.id.til_category);
        tilPurchase = findViewById(R.id.til_purchase);
        tilSelling = findViewById(R.id.til_selling);
        tilQty = findViewById(R.id.til_qty);
        tilMin = findViewById(R.id.til_min);
        etName = findViewById(R.id.et_name);
        etSku = findViewById(R.id.et_sku);
        etPurchase = findViewById(R.id.et_purchase);
        etSelling = findViewById(R.id.et_selling);
        etQty = findViewById(R.id.et_qty);
        etMin = findViewById(R.id.et_min);
        etDesc = findViewById(R.id.et_desc);
        acCategory = findViewById(R.id.ac_category);

        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, R.layout.spinner_item,
                db.getCategoryNames());
        acCategory.setAdapter(catAdapter);
        acCategory.setThreshold(1);
        acCategory.setOnClickListener(v -> acCategory.showDropDown());

        if (editing) {
            Product p = db.getProduct(productId);
            if (p == null) {
                Toast.makeText(this, "This product no longer exists.", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            etName.setText(p.name);
            etSku.setText(p.sku);
            acCategory.setText(p.categoryName, false);
            etPurchase.setText(plain(p.purchasePrice));
            etSelling.setText(plain(p.sellingPrice));
            etQty.setText(String.valueOf(p.quantity));
            etQty.setEnabled(false);
            findViewById(R.id.tv_qty_note).setVisibility(View.VISIBLE);
            etMin.setText(String.valueOf(p.minStock));
            etDesc.setText(p.description);
            ((com.google.android.material.button.MaterialButton) findViewById(R.id.btn_save))
                    .setText("Save changes");
        } else {
            etMin.setText("5");
        }

        findViewById(R.id.btn_save).setOnClickListener(v -> save());
    }

    private static String plain(double v) {
        if (v == Math.floor(v)) return String.valueOf((long) v);
        return String.valueOf(v);
    }

    private void save() {
        tilName.setError(null);
        tilSku.setError(null);
        tilCategory.setError(null);
        tilPurchase.setError(null);
        tilSelling.setError(null);
        tilQty.setError(null);
        tilMin.setError(null);

        String name = text(etName);
        String sku = text(etSku);
        String category = acCategory.getText().toString().trim();
        boolean ok = true;

        if (name.isEmpty()) {
            tilName.setError("Enter the product name");
            ok = false;
        }
        if (sku.isEmpty()) {
            tilSku.setError("Enter a unique product code");
            ok = false;
        }
        Double purchase = Fmt.parsePrice(text(etPurchase));
        if (purchase == null) {
            tilPurchase.setError("Enter a valid price (0 or more)");
            ok = false;
        }
        Double selling = Fmt.parsePrice(text(etSelling));
        if (selling == null) {
            tilSelling.setError("Enter a valid price (0 or more)");
            ok = false;
        }
        int qty = 0;
        if (productId < 0) {
            String q = text(etQty);
            if (!q.isEmpty()) {
                Integer parsed = Fmt.parseQty(q);
                if (parsed == null) {
                    tilQty.setError("Whole number, 0 or more");
                    ok = false;
                } else {
                    qty = parsed;
                }
            }
        }
        int min = 0;
        String m = text(etMin);
        if (!m.isEmpty()) {
            Integer parsed = Fmt.parseQty(m);
            if (parsed == null) {
                tilMin.setError("Whole number, 0 or more");
                ok = false;
            } else {
                min = parsed;
            }
        }
        if (!ok) return;

        Product p = new Product();
        p.id = productId;
        p.name = name;
        p.sku = sku;
        p.categoryName = category.isEmpty() ? "General" : category;
        p.purchasePrice = purchase;
        p.sellingPrice = selling;
        p.quantity = qty;
        p.minStock = min;
        p.description = etDesc.getText() == null ? "" : etDesc.getText().toString().trim();

        try {
            if (productId >= 0) {
                db.updateProduct(p);
                Toast.makeText(this, "Product updated", Toast.LENGTH_SHORT).show();
            } else {
                db.addProduct(p);
                Toast.makeText(this, "Product added", Toast.LENGTH_SHORT).show();
            }
            finish();
        } catch (DbException e) {
            if (e.getMessage() != null && e.getMessage().contains("product code")) {
                tilSku.setError(e.getMessage());
            } else {
                new AlertDialog.Builder(this)
                        .setTitle("Could not save")
                        .setMessage(e.getMessage())
                        .setPositiveButton("OK", null)
                        .show();
            }
        }
    }

    private static String text(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }
}
