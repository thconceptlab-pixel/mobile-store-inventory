package com.mobilestore.inventory.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class PaymentMethod { CASH, BANK_TRANSFER, CARD, EASYPAISA, JAZZCASH, OTHER }

/**
 * Immutable sale record, mirrors the locking rules of PurchaseEntity.
 * `profit` is computed once at insert time (sellingPrice - purchasePrice)
 * and stored, rather than recomputed on the fly, so historical reports never
 * change even if business logic evolves later.
 */
@Entity(
    tableName = "sales",
    foreignKeys = [
        ForeignKey(entity = StoreEntity::class, parentColumns = ["storeId"], childColumns = ["storeId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = PhoneEntity::class, parentColumns = ["phoneId"], childColumns = ["phoneId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("storeId"), Index("phoneId", unique = true), Index("saleDate")]
)
data class SaleEntity(
    @PrimaryKey(autoGenerate = true) val saleId: Long = 0,
    val storeId: Long,
    val phoneId: Long,
    val saleDate: Long,          // LOCKED after insert
    val sellingPrice: Double,    // LOCKED after insert
    val profit: Double,          // LOCKED — computed once at insert
    val customerName: String,
    val customerPhone: String? = null,
    val paymentMethod: PaymentMethod,
    val notes: String? = null,
    val isReversed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
