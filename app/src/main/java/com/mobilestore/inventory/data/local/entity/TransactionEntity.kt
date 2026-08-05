package com.mobilestore.inventory.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TransactionType { PURCHASE, SALE, REVERSAL_PURCHASE, REVERSAL_SALE }

/**
 * The banking-style ledger. Every purchase, sale, and reversal writes exactly
 * one row here, in chronological order, with a running `balance` so the
 * History screen can render a statement-style view. Rows are append-only —
 * reversals are new rows, never edits (see ReversalEntity).
 */
@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(entity = StoreEntity::class, parentColumns = ["storeId"], childColumns = ["storeId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("storeId"), Index("timestamp"), Index("referenceNumber", unique = true), Index("type")]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val transactionId: Long = 0,
    val storeId: Long,
    val referenceNumber: String,       // e.g. TXN-000123
    val type: TransactionType,
    val relatedPhoneId: Long,
    val relatedPurchaseId: Long? = null,
    val relatedSaleId: Long? = null,
    val amount: Double,                // purchase price, selling price, or reversed amount (signed)
    val profit: Double? = null,        // only populated for SALE / REVERSAL_SALE rows
    val balance: Double,               // running balance for the store after this row
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String? = null
)
