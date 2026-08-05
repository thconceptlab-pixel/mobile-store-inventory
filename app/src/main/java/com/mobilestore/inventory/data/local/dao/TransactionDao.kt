package com.mobilestore.inventory.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.mobilestore.inventory.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

/** Append-only ledger DAO — intentionally has no @Update or @Delete. */
@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE storeId = :storeId ORDER BY timestamp DESC")
    fun getTransactionsForStore(storeId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE storeId = :storeId AND timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    fun getTransactionsInRange(storeId: Long, start: Long, end: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE storeId = :storeId ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentTransactions(storeId: Long, limit: Int): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE storeId = :storeId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastTransaction(storeId: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE transactionId = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Query("""
        SELECT * FROM transactions WHERE storeId = :storeId AND (
            referenceNumber LIKE '%' || :query || '%'
        ) ORDER BY timestamp DESC
    """)
    fun searchByReference(storeId: Long, query: String): Flow<List<TransactionEntity>>

    @Insert
    suspend fun insertTransaction(transaction: TransactionEntity): Long
}
