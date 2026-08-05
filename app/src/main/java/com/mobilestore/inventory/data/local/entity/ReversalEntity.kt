package com.mobilestore.inventory.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Records that a transaction was reversed and why. The original
 * TransactionEntity / PurchaseEntity / SaleEntity rows are never touched —
 * this table plus a new REVERSAL_* TransactionEntity row form the audit
 * trail described in the app's security requirements.
 */
@Entity(
    tableName = "reversals",
    foreignKeys = [
        ForeignKey(entity = StoreEntity::class, parentColumns = ["storeId"], childColumns = ["storeId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TransactionEntity::class, parentColumns = ["transactionId"], childColumns = ["originalTransactionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TransactionEntity::class, parentColumns = ["transactionId"], childColumns = ["reversalTransactionId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("storeId"), Index("originalTransactionId"), Index("reversalTransactionId", unique = true)]
)
data class ReversalEntity(
    @PrimaryKey(autoGenerate = true) val reversalId: Long = 0,
    val storeId: Long,
    val originalTransactionId: Long,
    val reversalTransactionId: Long,
    val reason: String,
    val reversedBy: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
