package com.mobilestore.inventory.data.local.dao

import androidx.room.*
import com.mobilestore.inventory.data.local.entity.StoreEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StoreDao {
    @Query("SELECT * FROM stores ORDER BY createdAt ASC")
    fun getAllStores(): Flow<List<StoreEntity>>

    @Query("SELECT * FROM stores WHERE storeId = :storeId")
    suspend fun getStoreById(storeId: Long): StoreEntity?

    @Insert
    suspend fun insertStore(store: StoreEntity): Long

    // Profile details (name/phone/address/logo) ARE editable — only the
    // financial/IMEI/date fields in Purchase & Sale are locked.
    @Update
    suspend fun updateStore(store: StoreEntity)

    @Delete
    suspend fun deleteStore(store: StoreEntity)
}
