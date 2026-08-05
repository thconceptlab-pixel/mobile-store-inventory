package com.mobilestore.inventory.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Simple per-store key/value settings that need to live in the relational
 * DB (e.g. next reference-number counter, low-stock threshold). Device-level
 * UI prefs (dark mode, App Lock enabled) live in DataStore instead — see
 * di/DatabaseModule.kt notes — since they aren't really "store data".
 */
@Entity(tableName = "settings")
data class SettingEntity(
    @PrimaryKey val key: String,
    val value: String
)
