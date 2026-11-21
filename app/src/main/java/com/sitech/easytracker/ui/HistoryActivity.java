package com.sitech.easytracker.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.sitech.easytracker.R;
import com.sitech.easytracker.model.StockTransaction;
import com.sitech.easytracker.util.Fmt;

import java.util.List;

/** Full stock movement history. Tap a row to inspect it. */
public class HistoryActivity extends BaseActivity {
    private TransactionAdapter adapter;
    private EditText etSearch;
    private TextView tvEmpty, chipAll, chipIn, chipOut;
    private String type = null; // null = all

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        setupToolbar("Stock History", false);
        setupBottomNav(R.id.nav_history);

        etSearch = findViewById(R.id.et_search);
        tvEmpty = findViewById(R.id.tv_empty);
        chipAll = findViewById(R.id.chip_all);
        chipIn = findViewById(R.id.chip_in);
        chipOut = findViewById(R.id.chip_out);

        ListView list = findViewById(R.id.list);
        adapter = new TransactionAdapter(this);
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, position, id) -> {
            StockTransaction t = adapter.getItem(position);
            if (t != null) showDetails(t);
        });

        chipAll.setOnClickListener(v -> setType(null));
        chipIn.setOnClickListener(v -> setType(StockTransaction.IN));
        chipOut.setOnClickListener(v -> setType(StockTransaction.OUT));

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) { refresh(); }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateChips();
        refresh();
    }

    private void setType(String t) {
        type = t;
        updateChips();
        refresh();
    }

    private void updateChips() {
        chipAll.setSelected(type == null);
        chipIn.setSelected(StockTransaction.IN.equals(type));
        chipOut.setSelected(StockTransaction.OUT.equals(type));
    }

    private void refresh() {
        List<StockTransaction> items = db.getTransactions(type, -1, etSearch.getText().toString(), 500);
        adapter.setItems(items);
        tvEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showDetails(final StockTransaction t) {
        StringBuilder sb = new StringBuilder();
        sb.append("Product:  ").append(t.productName).append('\n');
        sb.append("Code:  ").append(t.sku).append('\n');
        sb.append("Type:  ").append(t.isIn() ? "Stock In" : "Stock Out").append('\n');
        sb.append("Quantity:  ").append(Fmt.number(t.quantity)).append('\n');
        sb.append("Previous quantity:  ").append(Fmt.number(t.previousQty)).append('\n');
        sb.append("New quantity:  ").append(Fmt.number(t.newQty)).append('\n');
        sb.append(t.isIn() ? "Purchase price:  " : "Selling price:  ")
                .append(Fmt.money(this, t.unitPrice)).append('\n');
        sb.append("Date:  ").append(Fmt.prettyDateTime(t.txnDate));
        if (t.notes != null && !t.notes.isEmpty()) {
            sb.append('\n').append(t.isIn() ? "Notes:  " : "Reason:  ").append(t.notes);
        }
        AlertDialog.Builder b = new AlertDialog.Builder(this)
                .setTitle("Movement details")
                .setMessage(sb.toString())
                .setPositiveButton("Close", null);
        if (t.productId >= 0) {
            b.setNeutralButton("Open product", (d, w) -> {
                Intent i = new Intent(this, ProductDetailActivity.class);
                i.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, t.productId);
                startActivity(i);
            });
        }
        b.show();
    }
}
