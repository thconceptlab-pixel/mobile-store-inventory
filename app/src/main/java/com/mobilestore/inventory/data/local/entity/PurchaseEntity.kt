package com.mobilestore.inventory.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Immutable purchase record. Once inserted, purchasePrice / purchaseDate must
 * NEVER be updated via DAO (no @Update method is exposed for these fields —
 * see PurchaseDao). If a mistake happens, insert a reversal Transaction that
 * references this record instead of mutating it. isReversed is a display
 * flag only — the original row's financial fields stay untouched forever.
 */
@Entity(
    tableName = "purchases",
    foreignKeys = [
        ForeignKey(entity = StoreEntity::class, parentColumns = ["storeId"], childColumns = ["storeId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = PhoneEntity::class, parentColumns = ["phoneId"], childColumns = ["phoneId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("storeId"), Index("phoneId", unique = true), Index("purchaseDate")]
)
data class PurchaseEntity(
    @PrimaryKey(autoGenerate = true) val purchaseId: Long = 0,
    val storeId: Long,
    val phoneId: Long,
    val purchaseDate: Long,      // LOCKED after insert
    val purchasePrice: Double,   // LOCKED after insert
    val supplierName: String,
    val supplierPhone: String? = null,
    val notes: String? = null,
    val isReversed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
