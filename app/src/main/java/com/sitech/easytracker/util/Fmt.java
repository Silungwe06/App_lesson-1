package com.sitech.easytracker.util;

import android.content.Context;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** Formatting and input-parsing helpers. */
public class Fmt {
    public static final int MAX_QTY = 1_000_000_000;

    public static String money(Context c, double v) {
        return Prefs.currency(c) + " " + String.format(Locale.US, "%,.2f", v);
    }

    public static String number(long v) {
        return String.format(Locale.US, "%,d", v);
    }

    public static String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    public static String dateOf(Calendar cal) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.getTime());
    }

    public static String stampNow() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
    }

    /** "2026-11-21" + the current time of day. */
    public static String stampForDate(String yyyyMmDd) {
        String time = new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date());
        return yyyyMmDd + " " + time;
    }

    public static String prettyDate(String yyyyMmDd) {
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(yyyyMmDd);
            return new SimpleDateFormat("dd MMM yyyy", Locale.US).format(d);
        } catch (ParseException | NullPointerException e) {
            return yyyyMmDd == null ? "" : yyyyMmDd;
        }
    }

    public static String prettyDateTime(String stamp) {
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).parse(stamp);
            return new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US).format(d);
        } catch (ParseException | NullPointerException e) {
            return stamp == null ? "" : stamp;
        }
    }

    /** Returns null when the text is empty, not a number, negative or absurdly large. */
    public static Double parsePrice(String s) {
        if (s == null) return null;
        s = s.trim().replace(",", "");
        if (s.isEmpty()) return null;
        try {
            double v = Double.parseDouble(s);
            if (Double.isNaN(v) || Double.isInfinite(v) || v < 0 || v > 1e12) return null;
            return v;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Returns null when the text is empty, not a whole number, negative or too large. */
    public static Integer parseQty(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) return null;
        try {
            long v = Long.parseLong(s);
            if (v < 0 || v > MAX_QTY) return null;
            return (int) v;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String csv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
