package com.sitech.easytracker.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.sitech.easytracker.R;
import com.sitech.easytracker.model.Product;
import com.sitech.easytracker.model.StockTransaction;
import com.sitech.easytracker.util.Fmt;

/** Fills the shared list-row layouts so Inventory, Dashboard and Detail look identical. */
public class UiHelper {

    public static String statusLabel(String status) {
        if (Product.STATUS_OUT.equals(status)) return "Out of Stock";
        if (Product.STATUS_LOW.equals(status)) return "Low Stock";
        return "In Stock";
    }

    public static int statusColor(Context c, String status) {
        int res = Product.STATUS_OUT.equals(status) ? R.color.status_out
                : Product.STATUS_LOW.equals(status) ? R.color.status_low : R.color.status_ok;
        return ContextCompat.getColor(c, res);
    }

    public static void styleStatusChip(Context c, TextView chip, String status) {
        chip.setText(statusLabel(status));
        chip.setTextColor(statusColor(c, status));
        int bg = Product.STATUS_OUT.equals(status) ? R.drawable.chip_out
                : Product.STATUS_LOW.equals(status) ? R.drawable.chip_low : R.drawable.chip_ok;
        chip.setBackgroundResource(bg);
    }

    public static void bindProduct(Context c, View row, Product p) {
        ((TextView) row.findViewById(R.id.tv_name)).setText(p.name);
        ((TextView) row.findViewById(R.id.tv_sub)).setText(p.sku + "  ·  " + p.categoryName);
        ((TextView) row.findViewById(R.id.tv_price)).setText("Sells at " + Fmt.money(c, p.sellingPrice));
        ((TextView) row.findViewById(R.id.tv_qty)).setText(Fmt.number(p.quantity));
        styleStatusChip(c, (TextView) row.findViewById(R.id.tv_status), p.status());
        row.findViewById(R.id.v_bar).setBackgroundTintList(
                ColorStateList.valueOf(statusColor(c, p.status())));
    }

    public static void bindTransaction(Context c, View row, StockTransaction t) {
        boolean in = t.isIn();
        int color = ContextCompat.getColor(c, in ? R.color.status_ok : R.color.status_out);
        ImageView icon = row.findViewById(R.id.iv_icon);
        icon.setBackgroundResource(in ? R.drawable.bg_circle_green : R.drawable.bg_circle_red);
        icon.setImageResource(in ? R.drawable.ic_stock_in : R.drawable.ic_stock_out);
        icon.setColorFilter(color);

        ((TextView) row.findViewById(R.id.tv_name)).setText(t.productName);
        ((TextView) row.findViewById(R.id.tv_date)).setText(
                (in ? "Stock In" : "Stock Out") + "  ·  " + Fmt.prettyDateTime(t.txnDate));
        TextView notes = row.findViewById(R.id.tv_notes);
        if (t.notes == null || t.notes.trim().isEmpty()) {
            notes.setVisibility(View.GONE);
        } else {
            notes.setVisibility(View.VISIBLE);
            notes.setText(t.notes);
        }
        TextView qty = row.findViewById(R.id.tv_qty);
        qty.setText((in ? "+" : "-") + Fmt.number(t.quantity));
        qty.setTextColor(color);
        ((TextView) row.findViewById(R.id.tv_levels)).setText(
                Fmt.number(t.previousQty) + " → " + Fmt.number(t.newQty));
    }
}
