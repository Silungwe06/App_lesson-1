package com.sitech.easytracker.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.sitech.easytracker.R;
import com.sitech.easytracker.model.DashboardStats;
import com.sitech.easytracker.model.Product;
import com.sitech.easytracker.model.StockTransaction;
import com.sitech.easytracker.util.Fmt;
import com.sitech.easytracker.util.Prefs;

import java.util.List;

/** Dashboard. Everything shown here is read from the database each time the screen appears. */
public class MainActivity extends BaseActivity {
    private TextView tvValue, tvValueNote, tvProducts, tvUnits, tvLow, tvOut;
    private LinearLayout alertsContainer, recentContainer;
    private TextView alertsEmpty, recentEmpty;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvValue = findViewById(R.id.tv_value);
        tvValueNote = findViewById(R.id.tv_value_note);
        tvProducts = findViewById(R.id.tv_products);
        tvUnits = findViewById(R.id.tv_units);
        tvLow = findViewById(R.id.tv_low);
        tvOut = findViewById(R.id.tv_out);
        alertsContainer = findViewById(R.id.alerts_container);
        recentContainer = findViewById(R.id.recent_container);
        alertsEmpty = findViewById(R.id.tv_alerts_empty);
        recentEmpty = findViewById(R.id.tv_recent_empty);

        findViewById(R.id.act_add).setOnClickListener(v ->
                startActivity(new Intent(this, ProductFormActivity.class)));
        findViewById(R.id.act_in).setOnClickListener(v -> openStock(StockTransaction.IN));
        findViewById(R.id.act_out).setOnClickListener(v -> openStock(StockTransaction.OUT));
        findViewById(R.id.act_inventory).setOnClickListener(v -> openInventory("ALL"));
        findViewById(R.id.card_products).setOnClickListener(v -> openInventory("ALL"));
        findViewById(R.id.card_low).setOnClickListener(v -> openInventory(Product.STATUS_LOW));
        findViewById(R.id.card_out).setOnClickListener(v -> openInventory(Product.STATUS_OUT));
        findViewById(R.id.tv_alerts_all).setOnClickListener(v -> openInventory(Product.STATUS_LOW));
        findViewById(R.id.tv_recent_all).setOnClickListener(v ->
                startActivity(new Intent(this, HistoryActivity.class)));

        setupBottomNav(R.id.nav_home);
    }

    @Override
    protected void onResume() {
        super.onResume();
        ((com.google.android.material.bottomnavigation.BottomNavigationView)
                findViewById(R.id.bottom_nav)).setSelectedItemId(R.id.nav_home);
        refresh();
    }

    private void openStock(String type) {
        Intent i = new Intent(this, StockActivity.class);
        i.putExtra(StockActivity.EXTRA_TYPE, type);
        startActivity(i);
    }

    private void openInventory(String status) {
        Intent i = new Intent(this, InventoryActivity.class);
        i.putExtra(InventoryActivity.EXTRA_STATUS, status);
        startActivity(i);
    }

    private void refresh() {
        boolean atSelling = Prefs.valueAtSellingPrice(this);
        DashboardStats s = db.getStats(atSelling);
        tvValue.setText(Fmt.money(this, s.totalValue));
        tvValueNote.setText(atSelling ? "Valued at selling price" : "Valued at purchase price");
        tvProducts.setText(Fmt.number(s.totalProducts));
        tvUnits.setText(Fmt.number(s.totalUnits));
        tvLow.setText(Fmt.number(s.lowStock));
        tvOut.setText(Fmt.number(s.outOfStock));

        // Stock alerts: low stock and out of stock, most urgent first
        alertsContainer.removeAllViews();
        List<Product> alerts = db.getAlertProducts();
        for (int i = 0; i < Math.min(5, alerts.size()); i++) {
            final Product p = alerts.get(i);
            View row = getLayoutInflater().inflate(R.layout.item_product, alertsContainer, false);
            UiHelper.bindProduct(this, row, p);
            row.setOnClickListener(v -> openDetail(p.id));
            alertsContainer.addView(row);
        }
        alertsEmpty.setVisibility(alerts.isEmpty() ? View.VISIBLE : View.GONE);

        // Recent movements
        recentContainer.removeAllViews();
        List<StockTransaction> recent = db.getTransactions(null, -1, null, 6);
        for (final StockTransaction t : recent) {
            View row = getLayoutInflater().inflate(R.layout.item_transaction, recentContainer, false);
            UiHelper.bindTransaction(this, row, t);
            if (t.productId >= 0) row.setOnClickListener(v -> openDetail(t.productId));
            recentContainer.addView(row);
        }
        recentEmpty.setVisibility(recent.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void openDetail(long productId) {
        Intent i = new Intent(this, ProductDetailActivity.class);
        i.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, productId);
        startActivity(i);
    }
}
