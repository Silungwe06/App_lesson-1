package com.sitech.easytracker.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.sitech.easytracker.R;
import com.sitech.easytracker.model.Product;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Inventory list with search, status filter and category filter. */
public class InventoryActivity extends BaseActivity {
    public static final String EXTRA_STATUS = "status";
    private static final String ALL_CATEGORIES = "All categories";

    private ProductAdapter adapter;
    private EditText etSearch;
    private Spinner spCategory;
    private TextView tvEmpty, tvCount;
    private TextView chipAll, chipIn, chipLow, chipOut;

    private String status = "ALL";
    private String category = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);
        setupToolbar("Inventory", false);
        setupBottomNav(R.id.nav_inventory);

        String s = getIntent().getStringExtra(EXTRA_STATUS);
        if (s != null) status = s;

        etSearch = findViewById(R.id.et_search);
        spCategory = findViewById(R.id.sp_category);
        tvEmpty = findViewById(R.id.tv_empty);
        tvCount = findViewById(R.id.tv_count);
        chipAll = findViewById(R.id.chip_all);
        chipIn = findViewById(R.id.chip_in);
        chipLow = findViewById(R.id.chip_low);
        chipOut = findViewById(R.id.chip_out);

        ListView list = findViewById(R.id.list);
        adapter = new ProductAdapter(this);
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, position, id) -> {
            Product p = adapter.getItem(position);
            if (p == null) return;
            Intent i = new Intent(this, ProductDetailActivity.class);
            i.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, p.id);
            startActivity(i);
        });

        findViewById(R.id.fab_add).setOnClickListener(v ->
                startActivity(new Intent(this, ProductFormActivity.class)));

        chipAll.setOnClickListener(v -> setStatus("ALL"));
        chipIn.setOnClickListener(v -> setStatus(Product.STATUS_IN));
        chipLow.setOnClickListener(v -> setStatus(Product.STATUS_LOW));
        chipOut.setOnClickListener(v -> setStatus(Product.STATUS_OUT));

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) { refresh(); }
        });

        spCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String picked = position == 0 ? null : (String) parent.getItemAtPosition(position);
                if (!Objects.equals(picked, category)) {
                    category = picked;
                    refresh();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCategories();
        updateChips();
        refresh();
    }

    private void loadCategories() {
        List<String> names = new ArrayList<>();
        names.add(ALL_CATEGORIES);
        names.addAll(db.getCategoryNames());
        ArrayAdapter<String> a = new ArrayAdapter<>(this, R.layout.spinner_item, names);
        a.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spCategory.setAdapter(a);
        int idx = category == null ? 0 : names.indexOf(category);
        spCategory.setSelection(Math.max(idx, 0), false);
        if (idx < 0) category = null;
    }

    private void setStatus(String s) {
        status = s;
        updateChips();
        refresh();
    }

    private void updateChips() {
        chipAll.setSelected("ALL".equals(status));
        chipIn.setSelected(Product.STATUS_IN.equals(status));
        chipLow.setSelected(Product.STATUS_LOW.equals(status));
        chipOut.setSelected(Product.STATUS_OUT.equals(status));
    }

    private void refresh() {
        List<Product> items = db.getProducts(etSearch.getText().toString(), category, status);
        adapter.setItems(items);
        tvEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        tvCount.setText(items.size() + (items.size() == 1 ? " product" : " products"));
    }
}
