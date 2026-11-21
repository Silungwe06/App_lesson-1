package com.sitech.easytracker.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.sitech.easytracker.R;
import com.sitech.easytracker.db.DatabaseHelper;

/** Shared toolbar and bottom-navigation behaviour. */
public abstract class BaseActivity extends AppCompatActivity {
    protected DatabaseHelper db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = DatabaseHelper.get(this);
    }

    protected void setupToolbar(String title, boolean showBack) {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(showBack);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    protected void setupBottomNav(final int currentItemId) {
        BottomNavigationView nav = findViewById(R.id.bottom_nav);
        nav.setSelectedItemId(currentItemId);
        nav.setOnItemReselectedListener(item -> { /* already here */ });
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == currentItemId) return true;
            Class<?> target;
            if (id == R.id.nav_home) target = MainActivity.class;
            else if (id == R.id.nav_inventory) target = InventoryActivity.class;
            else if (id == R.id.nav_history) target = HistoryActivity.class;
            else target = SettingsActivity.class;

            Intent intent = new Intent(this, target);
            if (id == R.id.nav_home) {
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            }
            startActivity(intent);
            overridePendingTransition(0, 0);
            if (currentItemId != R.id.nav_home) finish();
            return true;
        });
    }
}
