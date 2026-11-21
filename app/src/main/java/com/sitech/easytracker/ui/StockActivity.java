package com.sitech.easytracker.ui;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.sitech.easytracker.R;
import com.sitech.easytracker.db.DbException;
import com.sitech.easytracker.model.Product;
import com.sitech.easytracker.model.StockTransaction;
import com.sitech.easytracker.util.Fmt;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** One screen for both Stock In and Stock Out (EXTRA_TYPE = "IN" or "OUT"). */
public class StockActivity extends BaseActivity {
    public static final String EXTRA_TYPE = "type";
    public static final String EXTRA_PRODUCT_ID = "product_id";

    private boolean isIn;
    private Product selected;
    private final Map<String, Product> byLabel = new HashMap<>();
    private final Calendar cal = Calendar.getInstance();

    private TextInputLayout tilProduct, tilQty, tilPrice, tilDate;
    private AutoCompleteTextView acProduct;
    private TextInputEditText etQty, etPrice, etDate, etNotes;
    private TextView tvAvailable;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock);

        isIn = !StockTransaction.OUT.equals(getIntent().getStringExtra(EXTRA_TYPE));
        setupToolbar(isIn ? "Stock In" : "Stock Out", true);

        tilProduct = findViewById(R.id.til_product);
        tilQty = findViewById(R.id.til_qty);
        tilPrice = findViewById(R.id.til_price);
        tilDate = findViewById(R.id.til_date);
        acProduct = findViewById(R.id.ac_product);
        etQty = findViewById(R.id.et_qty);
        etPrice = findViewById(R.id.et_price);
        etDate = findViewById(R.id.et_date);
        etNotes = findViewById(R.id.et_notes);
        tvAvailable = findViewById(R.id.tv_available);
        MaterialButton btnSave = findViewById(R.id.btn_save);

        ((TextInputLayout) findViewById(R.id.til_notes)).setHint(isIn ? "Notes (optional)" : "Reason (optional)");
        tilPrice.setVisibility(isIn ? android.view.View.VISIBLE : android.view.View.GONE);
        btnSave.setText(isIn ? "Record stock in" : "Record stock out");
        if (!isIn) {
            btnSave.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.status_out)));
        }

        List<Product> products = db.getProducts(null, null, "ALL");
        if (products.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("No products yet")
                    .setMessage("Add a product first, then you can record stock for it.")
                    .setCancelable(false)
                    .setPositiveButton("Add product", (d, w) -> {
                        startActivity(new Intent(this, ProductFormActivity.class));
                        finish();
                    })
                    .setNegativeButton("Cancel", (d, w) -> finish())
                    .show();
            return;
        }

        List<String> labels = new ArrayList<>();
        for (Product p : products) {
            String label = p.name + " · " + p.sku;
            labels.add(label);
            byLabel.put(label, p);
        }
        acProduct.setAdapter(new ArrayAdapter<>(this, R.layout.spinner_item, labels));
        acProduct.setThreshold(1);
        acProduct.setOnClickListener(v -> acProduct.showDropDown());
        acProduct.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override
            public void afterTextChanged(Editable s) {
                selected = byLabel.get(s.toString());
                tilProduct.setError(null);
                if (selected == null) {
                    tvAvailable.setText("");
                } else {
                    tvAvailable.setText("Currently in stock: " + Fmt.number(selected.quantity));
                }
            }
        });

        long preId = getIntent().getLongExtra(EXTRA_PRODUCT_ID, -1);
        if (preId >= 0) {
            for (Map.Entry<String, Product> e : byLabel.entrySet()) {
                if (e.getValue().id == preId) {
                    acProduct.setText(e.getKey(), false);
                    selected = e.getValue();
                    tvAvailable.setText("Currently in stock: " + Fmt.number(selected.quantity));
                    break;
                }
            }
        }

        updateDateField();
        etDate.setOnClickListener(v -> pickDate());
        btnSave.setOnClickListener(v -> save());
    }

    private void updateDateField() {
        etDate.setText(Fmt.prettyDate(Fmt.dateOf(cal)));
    }

    private void pickDate() {
        DatePickerDialog dlg = new DatePickerDialog(this, (view, y, m, d) -> {
            cal.set(y, m, d);
            updateDateField();
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        dlg.getDatePicker().setMaxDate(System.currentTimeMillis());
        dlg.show();
    }

    private void save() {
        tilProduct.setError(null);
        tilQty.setError(null);
        tilPrice.setError(null);

        if (selected == null) {
            tilProduct.setError("Choose a product from the list");
            return;
        }
        Integer qty = Fmt.parseQty(etQty.getText() == null ? "" : etQty.getText().toString());
        if (qty == null || qty <= 0) {
            tilQty.setError("Enter a whole number greater than zero");
            return;
        }
        if (!isIn && qty > selected.quantity) {
            tilQty.setError("Only " + Fmt.number(selected.quantity) + " in stock");
            return;
        }
        Double priceOverride = null;
        if (isIn) {
            String ptxt = etPrice.getText() == null ? "" : etPrice.getText().toString().trim();
            if (!ptxt.isEmpty()) {
                priceOverride = Fmt.parsePrice(ptxt);
                if (priceOverride == null) {
                    tilPrice.setError("Enter a valid price (0 or more)");
                    return;
                }
            }
        }
        String notes = etNotes.getText() == null ? "" : etNotes.getText().toString().trim();

        try {
            db.recordMovement(selected.id, isIn ? StockTransaction.IN : StockTransaction.OUT,
                    qty, priceOverride, Fmt.dateOf(cal), notes);
            Toast.makeText(this, isIn
                    ? "Added " + Fmt.number(qty) + " to " + selected.name
                    : "Removed " + Fmt.number(qty) + " from " + selected.name,
                    Toast.LENGTH_SHORT).show();
            finish();
        } catch (DbException e) {
            new AlertDialog.Builder(this)
                    .setTitle("Could not save")
                    .setMessage(e.getMessage())
                    .setPositiveButton("OK", null)
                    .show();
        }
    }
}
