package com.sitech.easytracker.util;

import android.content.Context;
import android.content.SharedPreferences;

/** Small app settings (currency and stock valuation method). */
public class Prefs {
    private static final String FILE = "easytracker_prefs";
    private static final String KEY_CURRENCY = "currency";
    private static final String KEY_VALUE_AT_SELLING = "valuation_selling";

    public static final String[] CURRENCIES = {
            "ZMW", "USD", "EUR", "GBP", "ZAR", "KES", "TZS", "NGN", "GHS", "MWK"
    };

    private static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static String currency(Context c) {
        return sp(c).getString(KEY_CURRENCY, "ZMW");
    }

    public static void setCurrency(Context c, String code) {
        sp(c).edit().putString(KEY_CURRENCY, code).apply();
    }

    /** false = value stock at purchase (cost) price, true = at selling price. */
    public static boolean valueAtSellingPrice(Context c) {
        return sp(c).getBoolean(KEY_VALUE_AT_SELLING, false);
    }

    public static void setValueAtSellingPrice(Context c, boolean selling) {
        sp(c).edit().putBoolean(KEY_VALUE_AT_SELLING, selling).apply();
    }
}
