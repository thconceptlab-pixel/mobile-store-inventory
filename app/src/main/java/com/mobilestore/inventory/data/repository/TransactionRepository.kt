package com.mobilestore.inventory.data.repository

import com.mobilestore.inventory.data.local.dao.ReversalDao
import com.mobilestore.inventory.data.local.dao.TransactionDao
import com.mobilestore.inventory.data.local.entity.ReversalEntity
import com.mobilestore.inventory.data.local.entity.TransactionEntity
import com.mobilestore.inventory.data.local.entity.TransactionType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the append-only ledger: reference-number generation and running
 * balance. Called from PurchaseRepository / SaleRepository inside a DB
 * transaction so a Purchase/Sale row and its Transaction row are always
 * written together atomically.
 */
@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val reversalDao: ReversalDao
) {
    fun getTransactionsForStore(storeId: Long): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsForStore(storeId)

    fun getTransactionsInRange(storeId: Long, start: Long, end: Long): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsInRange(storeId, start, end)

    fun getRecentTransactions(storeId: Long, limit: Int = 10): Flow<List<TransactionEntity>> =
        transactionDao.getRecentTransactions(storeId, limit)

    fun searchByReference(storeId: Long, query: String) = transactionDao.searchByReference(storeId, query)

    fun getReversalsForStore(storeId: Long) = reversalDao.getReversalsForStore(storeId)

    /**
     * Writes a new ledger row. `amountSigned` should already carry the
     * correct sign (purchases are negative/outflow, sales are positive/inflow
     * relative to a simple running cash balance). Must be called from inside
     * a db.withTransaction block by the caller repository.
     */
    suspend fun recordTransaction(
        storeId: Long,
        type: TransactionType,
        relatedPhoneId: Long,
        amountSigned: Double,
        profit: Double? = null,
        relatedPurchaseId: Long? = null,
        relatedSaleId: Long? = null,
        notes: String? = null
    ): TransactionEntity {
        val last = transactionDao.getLastTransaction(storeId)
        val newBalance = (last?.balance ?: 0.0) + amountSigned
        val refNumber = generateReferenceNumber(storeId, last)
        val entity = TransactionEntity(
            storeId = storeId,
            referenceNumber = refNumber,
            type = type,
            relatedPhoneId = relatedPhoneId,
            relatedPurchaseId = relatedPurchaseId,
            relatedSaleId = relatedSaleId,
            amount = amountSigned,
            profit = profit,
            balance = newBalance,
            notes = notes
        )
        val id = transactionDao.insertTransaction(entity)
        return entity.copy(transactionId = id)
    }

    suspend fun recordReversal(
        storeId: Long,
        originalTransaction: TransactionEntity,
        reversalTransaction: TransactionEntity,
        reason: String,
        reversedBy: String? = null
    ) {
        reversalDao.insertReversal(
            ReversalEntity(
                storeId = storeId,
                originalTransactionId = originalTransaction.transactionId,
                reversalTransactionId = reversalTransaction.transactionId,
                reason = reason,
                reversedBy = reversedBy
            )
        )
    }

    suspend fun getById(transactionId: Long) = transactionDao.getById(transactionId)
    suspend fun getReversalFor(transactionId: Long) = reversalDao.getReversalForTransaction(transactionId)

    private fun generateReferenceNumber(storeId: Long, last: TransactionEntity?): String {
        // Simple monotonically-increasing per-store counter encoded in the
        // reference number itself: TXN-<storeId>-<sequence>. Parsed back out
        // of the last row rather than a separate counter table, to keep the
        // scheme resilient to Settings table resets.
        val lastSeq = last?.referenceNumber
            ?.substringAfterLast("-")
            ?.toIntOrNull() ?: 0
        val next = lastSeq + 1
        return "TXN-%d-%06d".format(storeId, next)
    }
}
