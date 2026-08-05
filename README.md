# Mobile Store Inventory Manager — Complete (Phases 1–4)

Offline-first Android app for used-mobile shops. Kotlin + Jetpack Compose +
Room + Hilt + MVVM. **All 4 phases are now in this project.**

## Before you open it — please read this

I wrote and reviewed every file in this project carefully — checking
imports, DAO/query signatures, Hilt dependency graphs, Compose
experimental-API opt-ins, and brace/paren balance across all 62 Kotlin
files. But **I have not compiled this project**: this sandbox has no
Android SDK, no Gradle, and no network access to Google's/Maven Central's
repositories, so there is no way for me to actually run a build here and
hand you a verified zero-error result. Nobody can honestly promise a
guaranteed-clean build without running the toolchain — what I can promise
is that this was built with real care, not generated and left unchecked.

**If Android Studio reports an error on sync or build, send it to me
verbatim and I'll fix it immediately.** Given the size of this project
(62 files, 7 screens, Room + Hilt + CameraX + ML Kit + biometrics), the most
likely sources of any issue are a version mismatch between a library and
the Compose BOM, or a minor Compose API signature drift — both are quick
fixes once I see the actual error text.

## What's new in Phase 4

- **IMEI / barcode scanner** — a "Scan" button next to the IMEI 1 field in
  Purchase Entry opens a live CameraX preview with ML Kit's on-device
  barcode reader (`ui/screens/BarcodeScannerScreen.kt`). It reads the
  barcode sticker many used phones carry (battery compartment or box,
  typically Code128/QR) and fills the IMEI field automatically. Camera
  permission is requested at scan time, and the manual-entry path always
  stays available if scanning isn't possible.
- **PDF export** — Reports screen now has an "Export PDF" button that
  builds a formatted summary (`data/export/ReportExportManager.kt`) using
  Android's built-in `android.graphics.pdf.PdfDocument` — no extra library.
- **Excel export** — "Export Excel" writes a CSV file, which Excel, Google
  Sheets, and Numbers all open natively. I deliberately didn't pull in a
  binary-.xlsx library (e.g. Apache POI): for a shop's simple tabular
  reports, CSV is the safer choice given I can't test an unfamiliar heavy
  dependency's exact API in this sandbox, and it adds real APK size for
  little practical benefit here.
- **Sample data** — Settings → "Load Sample Data" seeds ~8 realistic
  purchases (some sold, some still in stock) so you can see the Dashboard,
  Inventory, History, and Reports populated before entering real inventory.
  Never runs automatically; every seeded record is labeled "Sample data —
  safe to delete" in its notes.
- **Splash screen** — a proper `androidx.core.splashscreen` launch screen
  in the brand's dark-navy color instead of a blank white flash.
- **Small motion polish** — stat cards animate their size when numbers
  change, and Sale Entry's projected-profit banner fades/expands in rather
  than popping in abruptly.

## What's new in Phase 3

- **History** — the banking-style statement view: every purchase, sale, and
  reversal in order with a running balance, reference-number search, and
  date filters (Today/Yesterday/This Week/This Month/Last 1 Year/Custom
  Range via calendar pickers). Each eligible row has a **Reverse
  Transaction** action that opens a reason dialog — this calls straight into
  the `reversePurchase()`/`reverseSale()` methods built in Phase 1, so the
  original record is never touched, only a new reversal entry is added.
- **Reports** — Daily/Weekly/Monthly/Yearly/Custom period selector; totals
  for purchases, sales, profit, and current stock; a daily profit trend bar
  chart and Top Selling Brands / Top Selling Models rankings. Charts are
  built with Compose `Canvas` directly (`ui/components/Charts.kt`) rather
  than a third-party charting library, to keep the dependency list small and
  the code easy to audit.
- **Settings** — theme (System/Light/Dark, applied instantly app-wide),
  per-store currency selection, store profile display + **Add Another
  Store** (the multi-store switcher from Phase 2's Dashboard now has a real
  way to create additional stores), **Backup & Restore** (export shares a
  `.db` file via any app — Drive, email, a USB file manager — and restore
  picks a file and copies it back over the local database, then prompts an
  app restart so the change takes effect cleanly), and **App Lock** — a
  toggle that gates the whole app behind the phone's own fingerprint/PIN
  (via `BiometricPrompt` with `DEVICE_CREDENTIAL` allowed, so it works even
  on devices without fingerprint hardware, using whatever screen lock is
  already set up — no separate in-app PIN to manage or lose).

## What's new in Phase 2

- **Store bootstrap**: first launch shows a "Set Up Your Shop" screen
  (`StoreSetupScreen`) since Dashboard/Purchase/Sale/Inventory all need an
  active store to scope data to. `StoreViewModel` persists the active store
  ID in DataStore (`StoreSessionManager`) so it survives app restarts, and
  exposes a store-switcher bottom sheet from the Dashboard's top bar —
  covers the spec's "easy switch between Store A, Store B..." requirement.
- **Dashboard** — live stat cards (in stock, purchase value, sales, profit,
  today's purchases/sales/profit) and a recent-transactions feed, all driven
  by combined Room `Flow`s so numbers update in real time as you buy/sell.
- **Purchase Entry** — full form matching every field in the spec (brand
  dropdown preloaded with all 29 listed brands, model autocomplete sourced
  from the store's own purchase history, IMEI validation, storage/RAM/PTA/
  condition dropdowns, accessory chips, calendar date picker). Saves
  atomically through `PurchaseRepository.recordPurchase()` from Phase 1.
- **Inventory** — searchable list (brand/model/IMEI), status tabs (All/In
  Stock/Sold), and a filter sheet for brand + purchase-price range. Each row
  shows purchase price/date via the new `PhoneWithPurchase` join query.
  Tapping a phone opens **Phone Detail**, which shows the locked purchase
  (and sale, if any) record and a "Sell This Phone" button.
- **Sale Entry** — reached either from Phone Detail or the new Sales tab
  (pick any in-stock phone → sale form). Shows a live projected-profit
  preview as you type the selling price, payment-method chips, and saves
  atomically through `SaleRepository.recordSale()`.

## What's in Phase 1

- Gradle project (AGP 8.5.2, Kotlin 1.9.24, KSP, Compose BOM 2024.06)
- Full **Room database schema**: Stores, Phones, Purchases, Sales,
  Transactions (ledger), Reversals, Settings — with indexes and foreign keys
- **Locked-record design already implemented at the data layer**:
  - `PurchaseDao` / `SaleDao` expose no update path for price/date/IMEI
  - `TransactionDao` is append-only (no `@Update`, no `@Delete`)
  - Corrections go through `reversePurchase()` / `reverseSale()` in the
    repositories, which write a new `REVERSAL_*` ledger row and a
    `ReversalEntity` audit record — the original rows are never touched
  - Every purchase/sale writes through `db.withTransaction { }` so the
    Phone + Purchase/Sale + Transaction rows are always atomic
- Repository layer (`StoreRepository`, `PhoneRepository`,
  `PurchaseRepository`, `SaleRepository`, `TransactionRepository`) — this is
  the layer future ViewModels will call
- Hilt DI wired end to end (`MobileStoreApplication`, `DatabaseModule`)
- Dark-blue/white Material 3 theme (`ui/theme`) matching the spec's palette
- Bottom navigation with all 7 destinations (Dashboard, Inventory,
  Purchases, Sales, History, Reports, Settings) — each currently shows a
  placeholder screen so the app **compiles and runs today**, ready to have
  real screens dropped in

## One honesty note on "fully offline"

Google's ML Kit Barcode Scanning API (`com.google.mlkit:barcode-scanning`)
is documented as an on-device API with its detection model included in the
library, unlike some other ML Kit APIs that fetch models via Google Play
Services on first use. I'm confident in this based on how the API is
designed and documented, but I haven't been able to verify it by actually
running the scanner on a device from this sandbox — if you test it and it
behaves differently (e.g. it prompts for a Play Services module download),
let me know and I'll swap in a workaround.

## App Lock notes

- If a device has no fingerprint/PIN/pattern configured at all,
  `BiometricManager.canAuthenticate()` returns not-available and the lock
  screen explains that instead of trapping the user — they can turn App
  Lock back off from Settings once unlocked another way, or set up a device
  screen lock first.
- App Lock re-prompts once per cold process start (app fully closed and
  reopened), not on every screen — this matches how most banking/shop apps
  behave and avoids interrupting normal navigation.

## Backup/Restore notes

- Export flushes Room's WAL journal into the main `.db` file first (so nothing
  buffered is missed), copies it to app-private storage, and hands it to any
  share target the user picks.
- Import copies the chosen file directly over the live database path. Since
  the app already has a database connection open at that point, the restored
  data won't appear until the process restarts — Settings shows a "Restart
  Now" prompt right after a successful import rather than silently leaving
  stale data on screen.

## A note on verification

This project was written and reviewed carefully (imports, DAO/query
correctness, Compose API usage, brace/type checks) but **not compiled** —
this environment doesn't have the Android SDK/Gradle toolchain to build an
.apk. Open it in Android Studio and let Gradle sync; if anything doesn't
compile, send me the error and I'll fix it immediately.

## Opening the project

1. Open this folder in Android Studio (Koala or newer recommended).
2. Let Gradle sync — it will generate the Gradle wrapper jar automatically
   on first sync if prompted ("Gradle wrapper not found").
3. Run on an emulator or device (minSdk 24 / Android 7.0+).

No API keys, no login, no internet permission requested — everything is
local Room storage under the hood, as required by the spec.

## Package layout

```
com.mobilestore.inventory
├── data/local/entity/     Room entities (7 tables)
├── data/local/dao/        DAOs — note which ones have no update/delete
├── data/local/            AppDatabase, TypeConverters
├── data/repository/       Business logic incl. reversal + ledger balance
├── di/                    Hilt modules
├── ui/theme/              Color, Type, Theme (Material 3)
├── ui/navigation/         Screen routes, NavGraph, bottom bar
└── ui/screens/            Placeholder screen (replaced screen-by-screen)
```
