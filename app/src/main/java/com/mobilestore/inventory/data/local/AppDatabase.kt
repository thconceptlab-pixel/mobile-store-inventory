package com.mobilestore.inventory.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mobilestore.inventory.data.local.dao.*
import com.mobilestore.inventory.data.local.entity.*

/**
 * Single Room database for the whole app. All tables are scoped by storeId
 * so that multi-store data isolation happens at the query layer rather than
 * via separate physical databases — simpler backup/restore, single file.
 *
 * Version history:
 *  1 — initial schema (Phase 1): Stores, Phones, Purchases, Sales,
 *      Transactions, Reversals, Settings.
 *
 * NOTE: exportSchema is true so schema JSON is written to
 * app/schemas/ for migration testing — commit that folder to version control.
 */
@Database(
    entities = [
        StoreEntity::class,
        PhoneEntity::class,
        PurchaseEntity::class,
        SaleEntity::class,
        TransactionEntity::class,
        ReversalEntity::class,
        SettingEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun storeDao(): StoreDao
    abstract fun phoneDao(): PhoneDao
    abstract fun purchaseDao(): PurchaseDao
    abstract fun saleDao(): SaleDao
    abstract fun transactionDao(): TransactionDao
    abstract fun reversalDao(): ReversalDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        const val DATABASE_NAME = "mobile_store_inventory.db"
    }
}
