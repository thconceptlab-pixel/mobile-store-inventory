package com.mobilestore.inventory.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.mobilestore.inventory.data.local.entity.SaleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales WHERE storeId = :storeId ORDER BY saleDate DESC")
    fun getSalesForStore(storeId: Long): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales WHERE phoneId = :phoneId LIMIT 1")
    suspend fun getSaleForPhone(phoneId: Long): SaleEntity?

    @Query("SELECT * FROM sales WHERE storeId = :storeId AND saleDate BETWEEN :start AND :end")
    fun getSalesInRange(storeId: Long, start: Long, end: Long): Flow<List<SaleEntity>>

    @Query("SELECT COALESCE(SUM(sellingPrice), 0) FROM sales WHERE storeId = :storeId AND isReversed = 0")
    fun getTotalSalesValue(storeId: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(profit), 0) FROM sales WHERE storeId = :storeId AND isReversed = 0")
    fun getTotalProfit(storeId: Long): Flow<Double>

    @Query("""
        SELECT COALESCE(SUM(profit), 0) FROM sales
        WHERE storeId = :storeId AND isReversed = 0 AND saleDate BETWEEN :start AND :end
    """)
    fun getProfitInRange(storeId: Long, start: Long, end: Long): Flow<Double>

    @Insert
    suspend fun insertSale(sale: SaleEntity): Long

    @Query("UPDATE sales SET isReversed = 1 WHERE saleId = :saleId")
    suspend fun markReversed(saleId: Long)
}
