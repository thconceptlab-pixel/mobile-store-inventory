package com.mobilestore.inventory.data.local.dao

import androidx.room.Dao
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.mobilestore.inventory.data.local.entity.SettingEntity

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE `key` = :key")
    suspend fun get(key: String): SettingEntity?

    @Upsert
    suspend fun set(setting: SettingEntity)
}
