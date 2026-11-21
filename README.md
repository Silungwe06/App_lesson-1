# Easy Tracker — Android Stock Management (v1.0 MVP)

Offline stock tracking for small businesses, by **S.I Tech**. Java + native XML + SQLite. No internet needed.
The UI uses the S.I Tech logo and its blue palette (`res/values/colors.xml`).

- **Package:** `com.sitech.easytracker`
- **Minimum Android:** 5.0 (API 21) · **Target/compile:** Android 14 (API 34)
- **Dependencies:** only `androidx.appcompat` and `com.google.android.material`

## What is in version 1.0
Dashboard (live totals, low/out-of-stock alerts, recent movements, quick actions) · Add / edit / delete / view / search products ·
Categories · Stock In · Stock Out (never negative) · Inventory with search + status + category filters ·
Stock movement history (tap a row to inspect it) · Settings (currency, valuation method, CSV export, database backup).

## Project structure
```
app/src/main/java/com/sitech/easytracker/
  db/DatabaseHelper.java     all SQL; every stock change + history row is one transaction
  db/DbException.java        user-safe error messages
  model/                     Product, StockTransaction, DashboardStats
  util/                      Fmt (formatting + input parsing), Prefs (currency, valuation)
  ui/                        one Activity per screen + BaseActivity (toolbar / bottom nav),
                             UiHelper + adapters (shared list rows)
app/src/main/res/            layouts, drawables, logo (drawable-nodpi), launcher icons (mipmap-*), colours, styles
.github/workflows/build-apk.yml   builds the APK on GitHub automatically
```

## Open the project
1. Install **Android Studio** (Hedgehog or newer). 2. *File → Open* → choose the `EasyTracker` folder.
3. Let Gradle sync (needs internet once). Android Studio uses JDK 17 by default.

## Build the APK
**Android Studio:** *Build → Build Bundle(s) / APK(s) → Build APK(s)*. Output: `app/build/outputs/apk/debug/app-debug.apk`.

**Command line:** `./gradlew assembleDebug` (Windows: `gradlew.bat assembleDebug`), needs JDK 17 and the Android SDK.

**GitHub (no Android Studio needed):** push this project to your repo's `main` branch. The workflow
*Build Easy Tracker APK* runs automatically (or run it from the **Actions** tab). When it finishes, open the run and
download the artifact **EasyTracker-debug-apk**.

## Install the APK
Copy `app-debug.apk` to the phone → open it → allow *Install unknown apps* for your file manager/browser when asked → Install.
Or with USB debugging on: `adb install app-debug.apk`.

## Database (SQLite, `easytracker.db`, version 1)
| Table | Columns |
|---|---|
| `categories` | `id` PK, `name` UNIQUE (case-insensitive) |
| `products` | `id` PK, `name`, `sku` UNIQUE (case-insensitive), `category_id` FK→categories (SET NULL), `purchase_price`, `selling_price`, `quantity` (CHECK ≥ 0), `min_stock`, `description`, `date_added` |
| `stock_transactions` | `id` PK, `product_id` FK→products (SET NULL), `product_name`, `sku`, `type` ('IN'/'OUT'), `quantity` (> 0), `previous_qty`, `new_qty`, `unit_price`, `notes`, `txn_date` |

Indexes: product name, product category, transaction product, transaction date. Foreign keys are enabled.

## Design decisions worth knowing
- **Quantity only changes through Stock In / Stock Out** (the edit screen shows it read-only), so history always adds up.
  A starting quantity on a new product is logged as an "Opening stock" Stock In.
- **Stock status:** Out of Stock = 0 · Low Stock = above 0 and ≤ minimum threshold · In Stock = above the threshold.
- **Stock In price override** updates the product's purchase price and is stored on the history row.
- **Deleting a product** keeps its history (name/code are stored on each history row).
- **Valuation** (Settings): total stock value uses purchase price (default) or selling price.
- **CSV export / backup** use Android's file picker, so no storage permission is needed.

## Next milestones (not built yet)
1.1 Reports (daily/monthly, profit, CSV/PDF) · 1.2 Warehouses, suppliers, customers, purchases, sales, barcode scanning ·
1.3 Accounts, permissions, cloud backup/sync, online dashboard, automated alerts.
Database changes go in `DatabaseHelper.onUpgrade` (bump `DB_VERSION`).
