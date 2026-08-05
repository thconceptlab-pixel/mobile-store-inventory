package com.mobilestore.inventory.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.mobilestore.inventory.data.local.entity.ReversalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReversalDao {
    @Query("SELECT * FROM reversals WHERE storeId = :storeId ORDER BY timestamp DESC")
    fun getReversalsForStore(storeId: Long): Flow<List<ReversalEntity>>

    @Query("SELECT * FROM reversals WHERE originalTransactionId = :transactionId LIMIT 1")
    suspend fun getReversalForTransaction(transactionId: Long): ReversalEntity?

    @Insert
    suspend fun insertReversal(reversal: ReversalEntity): Long
}
