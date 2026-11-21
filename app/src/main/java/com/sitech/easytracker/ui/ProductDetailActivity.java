package com.sitech.easytracker.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.sitech.easytracker.R;
import com.sitech.easytracker.model.Product;
import com.sitech.easytracker.model.StockTransaction;
import com.sitech.easytracker.util.Fmt;

import java.util.List;

public class ProductDetailActivity extends BaseActivity {
    public static final String EXTRA_PRODUCT_ID = "product_id";

    private long productId;
    private Product product;
    private TextView tvName, tvSku, tvStatus, tvQty, tvHistoryEmpty;
    private LinearLayout detailsContainer, historyContainer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);
        setupToolbar("Product details", true);

        productId = getIntent().getLongExtra(EXTRA_PRODUCT_ID, -1);
        tvName = findViewById(R.id.tv_name);
        tvSku = findViewById(R.id.tv_sku);
        tvStatus = findViewById(R.id.tv_status);
        tvQty = findViewById(R.id.tv_qty);
        tvHistoryEmpty = findViewById(R.id.tv_history_empty);
        detailsContainer = findViewById(R.id.details_container);
        historyContainer = findViewById(R.id.history_container);

        findViewById(R.id.btn_in).setOnClickListener(v -> openStock(StockTransaction.IN));
        findViewById(R.id.btn_out).setOnClickListener(v -> openStock(StockTransaction.OUT));
    }

    @Override
    protected void onResume() {
        super.onResume();
        product = db.getProduct(productId);
        if (product == null) {
            finish();
            return;
        }
        bind();
    }

    private void bind() {
        tvName.setText(product.name);
        tvSku.setText("Code: " + product.sku);
        tvQty.setText(Fmt.number(product.quantity));
        UiHelper.styleStatusChip(this, tvStatus, product.status());

        detailsContainer.removeAllViews();
        addRow("Category", product.categoryName);
        addRow("Purchase price", Fmt.money(this, product.purchasePrice));
        addRow("Selling price", Fmt.money(this, product.sellingPrice));
        addRow("Minimum stock", Fmt.number(product.minStock));
        addRow("Value at cost", Fmt.money(this, product.quantity * product.purchasePrice));
        addRow("Value at selling price", Fmt.money(this, product.quantity * product.sellingPrice));
        addRow("Date added", Fmt.prettyDate(product.dateAdded));
        if (product.description != null && !product.description.isEmpty()) {
            addRow("Description", product.description);
        }

        historyContainer.removeAllViews();
        List<StockTransaction> list = db.getTransactions(null, product.id, null, 20);
        for (StockTransaction t : list) {
            View row = getLayoutInflater().inflate(R.layout.item_transaction, historyContainer, false);
            UiHelper.bindTransaction(this, row, t);
            historyContainer.addView(row);
        }
        tvHistoryEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void addRow(String label, String value) {
        View row = getLayoutInflater().inflate(R.layout.item_detail_row, detailsContainer, false);
        ((TextView) row.findViewById(R.id.tv_label)).setText(label);
        ((TextView) row.findViewById(R.id.tv_value)).setText(value);
        detailsContainer.addView(row);
    }

    private void openStock(String type) {
        Intent i = new Intent(this, StockActivity.class);
        i.putExtra(StockActivity.EXTRA_TYPE, type);
        i.putExtra(StockActivity.EXTRA_PRODUCT_ID, productId);
        startActivity(i);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_edit) {
            Intent i = new Intent(this, ProductFormActivity.class);
            i.putExtra(ProductFormActivity.EXTRA_PRODUCT_ID, productId);
            startActivity(i);
            return true;
        }
        if (id == R.id.action_delete) {
            confirmDelete();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void confirmDelete() {
        if (product == null) return;
        new AlertDialog.Builder(this)
                .setTitle("Delete product?")
                .setMessage("\"" + product.name + "\" will be removed from your inventory. "
                        + "Its stock movement history will be kept. This cannot be undone.")
                .setPositiveButton("Delete", (d, w) -> {
                    db.deleteProduct(productId);
                    Toast.makeText(this, "Product deleted", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
