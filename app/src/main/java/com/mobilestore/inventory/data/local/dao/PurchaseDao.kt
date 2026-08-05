package com.mobilestore.inventory.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.mobilestore.inventory.data.local.entity.PurchaseEntity
import kotlinx.coroutines.flow.Flow

/**
 * Deliberately exposes NO @Update / @Delete for financial fields.
 * purchasePrice / purchaseDate are write-once. Only `isReversed` may flip,
 * via markReversed(), which does not touch price or date.
 */
@Dao
interface PurchaseDao {
    @Query("SELECT * FROM purchases WHERE storeId = :storeId ORDER BY purchaseDate DESC")
    fun getPurchasesForStore(storeId: Long): Flow<List<PurchaseEntity>>

    @Query("SELECT * FROM purchases WHERE phoneId = :phoneId LIMIT 1")
    suspend fun getPurchaseForPhone(phoneId: Long): PurchaseEntity?

    @Query("SELECT * FROM purchases WHERE storeId = :storeId AND purchaseDate BETWEEN :start AND :end")
    fun getPurchasesInRange(storeId: Long, start: Long, end: Long): Flow<List<PurchaseEntity>>

    @Query("SELECT COALESCE(SUM(purchasePrice), 0) FROM purchases WHERE storeId = :storeId AND isReversed = 0")
    fun getTotalPurchaseValue(storeId: Long): Flow<Double>

    @Insert
    suspend fun insertPurchase(purchase: PurchaseEntity): Long

    @Query("UPDATE purchases SET isReversed = 1 WHERE purchaseId = :purchaseId")
    suspend fun markReversed(purchaseId: Long)
}
