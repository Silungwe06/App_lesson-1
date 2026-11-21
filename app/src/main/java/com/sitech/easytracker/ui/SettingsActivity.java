package com.sitech.easytracker.ui;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.sitech.easytracker.R;
import com.sitech.easytracker.util.Fmt;
import com.sitech.easytracker.util.Prefs;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

public class SettingsActivity extends BaseActivity {
    private static final int REQ_CSV = 101;
    private static final int REQ_BACKUP = 102;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setupToolbar("Settings", false);
        setupBottomNav(R.id.nav_settings);

        TextView tvVersion = findViewById(R.id.tv_version);
        String version = "1.0.0";
        try {
            version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        tvVersion.setText("Version " + version);

        // Currency
        Spinner sp = findViewById(R.id.sp_currency);
        ArrayAdapter<String> a = new ArrayAdapter<>(this, R.layout.spinner_item, Prefs.CURRENCIES);
        a.setDropDownViewResource(R.layout.spinner_dropdown_item);
        sp.setAdapter(a);
        sp.setSelection(Math.max(0, Arrays.asList(Prefs.CURRENCIES).indexOf(Prefs.currency(this))), false);
        sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String code = Prefs.CURRENCIES[position];
                if (!code.equals(Prefs.currency(SettingsActivity.this))) {
                    Prefs.setCurrency(SettingsActivity.this, code);
                    Toast.makeText(SettingsActivity.this, "Currency set to " + code, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        // Valuation
        RadioGroup rg = findViewById(R.id.rg_valuation);
        rg.check(Prefs.valueAtSellingPrice(this) ? R.id.rb_retail : R.id.rb_cost);
        rg.setOnCheckedChangeListener((group, checkedId) -> {
            Prefs.setValueAtSellingPrice(this, checkedId == R.id.rb_retail);
            Toast.makeText(this, "Stock valuation updated", Toast.LENGTH_SHORT).show();
        });

        // Export / backup (the system file picker chooses where to save; no storage permission needed)
        findViewById(R.id.btn_export).setOnClickListener(v ->
                pickFile(REQ_CSV, "text/csv", "EasyTracker_Inventory_" + Fmt.today() + ".csv"));
        findViewById(R.id.btn_backup).setOnClickListener(v ->
                pickFile(REQ_BACKUP, "application/octet-stream", "EasyTracker_Backup_" + Fmt.today() + ".db"));
    }

    private void pickFile(int requestCode, String mime, String fileName) {
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType(mime);
        i.putExtra(Intent.EXTRA_TITLE, fileName);
        startActivityForResult(i, requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try (OutputStream out = getContentResolver().openOutputStream(uri)) {
            if (out == null) throw new IOException("Cannot open file");
            if (requestCode == REQ_CSV) {
                db.exportInventoryCsv(out, Prefs.valueAtSellingPrice(this));
                Toast.makeText(this, "Inventory exported", Toast.LENGTH_LONG).show();
            } else if (requestCode == REQ_BACKUP) {
                db.backupTo(out);
                Toast.makeText(this, "Database backup saved", Toast.LENGTH_LONG).show();
            }
        } catch (IOException e) {
            Toast.makeText(this, "Could not save the file. Please try again.", Toast.LENGTH_LONG).show();
        }
    }
}
