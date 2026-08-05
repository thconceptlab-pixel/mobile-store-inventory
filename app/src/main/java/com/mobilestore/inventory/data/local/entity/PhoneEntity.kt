package com.mobilestore.inventory.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class PhoneStatus { IN_STOCK, SOLD }

/**
 * A single physical phone unit tracked through its lifecycle: created by a
 * Purchase entry, optionally closed out by a Sale entry. The IMEI values are
 * treated as locked identifiers once created — see PurchaseEntity for the
 * locked commercial fields (price, date).
 */
@Entity(
    tableName = "phones",
    foreignKeys = [
        ForeignKey(
            entity = StoreEntity::class,
            parentColumns = ["storeId"],
            childColumns = ["storeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("storeId"),
        Index("imei1", unique = true),
        Index("brand"),
        Index("model"),
        Index("status")
    ]
)
data class PhoneEntity(
    @PrimaryKey(autoGenerate = true) val phoneId: Long = 0,
    val storeId: Long,
    val brand: String,
    val model: String,
    val imei1: String,
    val imei2: String? = null,
    val storage: String,
    val ram: String,
    val color: String,
    val batteryHealth: String,
    val condition: String,
    val ptaStatus: String,
    val accessoriesIncluded: String? = null,
    val notes: String? = null,
    val imagePath: String? = null,
    val status: PhoneStatus = PhoneStatus.IN_STOCK,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
