package com.mobilestore.inventory.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A shop profile. Every other table (Phones, Purchases, Sales, Transactions)
 * carries a storeId foreign key so that switching stores fully isolates data —
 * queries are always scoped by storeId at the DAO level.
 */
@Entity(tableName = "stores")
data class StoreEntity(
    @PrimaryKey(autoGenerate = true) val storeId: Long = 0,
    val shopName: String,
    val ownerName: String,
    val phoneNumber: String,
    val address: String,
    val logoPath: String? = null,
    val currencyCode: String = "PKR",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
