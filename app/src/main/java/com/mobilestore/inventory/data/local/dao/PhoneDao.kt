package com.mobilestore.inventory.data.local.dao

import androidx.room.*
import com.mobilestore.inventory.data.local.entity.PhoneEntity
import com.mobilestore.inventory.data.local.entity.PhoneStatus
import com.mobilestore.inventory.data.local.relation.PhoneWithPurchase
import kotlinx.coroutines.flow.Flow

@Dao
interface PhoneDao {
    @Query("SELECT * FROM phones WHERE storeId = :storeId ORDER BY createdAt DESC")
    fun getPhonesForStore(storeId: Long): Flow<List<PhoneEntity>>

    @Query("SELECT * FROM phones WHERE storeId = :storeId AND status = :status ORDER BY createdAt DESC")
    fun getPhonesByStatus(storeId: Long, status: PhoneStatus): Flow<List<PhoneEntity>>

    @Query("SELECT * FROM phones WHERE phoneId = :phoneId")
    suspend fun getPhoneById(phoneId: Long): PhoneEntity?

    @Query("""
        SELECT * FROM phones WHERE storeId = :storeId AND (
            brand LIKE '%' || :query || '%' OR
            model LIKE '%' || :query || '%' OR
            imei1 LIKE '%' || :query || '%' OR
            imei2 LIKE '%' || :query || '%'
        ) ORDER BY createdAt DESC
    """)
    fun searchPhones(storeId: Long, query: String): Flow<List<PhoneEntity>>

    @Query("SELECT COUNT(*) FROM phones WHERE storeId = :storeId AND status = 'IN_STOCK'")
    fun getInStockCount(storeId: Long): Flow<Int>

    @Query("SELECT DISTINCT brand FROM phones WHERE storeId = :storeId ORDER BY brand ASC")
    fun getDistinctBrandsUsed(storeId: Long): Flow<List<String>>

    @Query("SELECT DISTINCT model FROM phones WHERE storeId = :storeId AND brand = :brand ORDER BY model ASC")
    suspend fun getDistinctModelsForBrand(storeId: Long, brand: String): List<String>

    /** Inventory screen source: joins each phone to its (locked) purchase price/date. */
    @Query("""
        SELECT p.*, pu.purchasePrice as purchasePrice, pu.purchaseDate as purchaseDate
        FROM phones p INNER JOIN purchases pu ON p.phoneId = pu.phoneId
        WHERE p.storeId = :storeId
        ORDER BY p.createdAt DESC
    """)
    fun getPhonesWithPurchaseInfo(storeId: Long): Flow<List<PhoneWithPurchase>>

    @Insert
    suspend fun insertPhone(phone: PhoneEntity): Long

    // Descriptive fields (notes, accessories, image, favorite) may be
    // corrected; the entity's IMEI/brand/model stay as originally entered
    // in this Phase — a guarded "pre-sale correction" flow can be added
    // later if needed, distinct from the locked commercial records.
    @Update
    suspend fun updatePhone(phone: PhoneEntity)

    @Query("UPDATE phones SET status = :status WHERE phoneId = :phoneId")
    suspend fun setStatus(phoneId: Long, status: PhoneStatus)
}
