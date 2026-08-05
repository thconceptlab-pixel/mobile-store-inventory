package com.mobilestore.inventory.data.local.relation

import androidx.room.Embedded
import com.mobilestore.inventory.data.local.entity.PhoneEntity

/**
 * Join result used by the Inventory screen, which per the spec must display
 * Purchase Price and Purchase Date alongside each phone — fields that live
 * in the (locked, separate) purchases table, not on PhoneEntity itself.
 */
data class PhoneWithPurchase(
    @Embedded val phone: PhoneEntity,
    val purchasePrice: Double,
    val purchaseDate: Long
)
