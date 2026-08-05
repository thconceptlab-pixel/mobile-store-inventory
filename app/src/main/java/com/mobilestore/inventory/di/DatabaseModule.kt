package com.mobilestore.inventory.di

import android.content.Context
import androidx.room.Room
import com.mobilestore.inventory.data.local.AppDatabase
import com.mobilestore.inventory.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides the single Room database instance and all DAOs. Device-level UI
 * preferences (theme, currency selection, App Lock enabled/PIN hash) are
 * intentionally NOT modeled here — they belong in a Preferences DataStore
 * (added in Phase 3 alongside the Settings screen) since they describe the
 * device/session, not the shop's inventory data.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            // No destructive fallback in production — real migrations will be
            // added as the schema evolves in later phases.
            .build()

    @Provides
    fun provideStoreDao(db: AppDatabase): StoreDao = db.storeDao()

    @Provides
    fun providePhoneDao(db: AppDatabase): PhoneDao = db.phoneDao()

    @Provides
    fun providePurchaseDao(db: AppDatabase): PurchaseDao = db.purchaseDao()

    @Provides
    fun provideSaleDao(db: AppDatabase): SaleDao = db.saleDao()

    @Provides
    fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideReversalDao(db: AppDatabase): ReversalDao = db.reversalDao()

    @Provides
    fun provideSettingsDao(db: AppDatabase): SettingsDao = db.settingsDao()
}
