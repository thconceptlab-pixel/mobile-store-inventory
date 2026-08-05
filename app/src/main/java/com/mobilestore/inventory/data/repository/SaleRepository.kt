package com.mobilestore.inventory.data.repository

import androidx.room.withTransaction
import com.mobilestore.inventory.data.local.AppDatabase
import com.mobilestore.inventory.data.local.dao.PhoneDao
import com.mobilestore.inventory.data.local.dao.PurchaseDao
import com.mobilestore.inventory.data.local.dao.SaleDao
import com.mobilestore.inventory.data.local.entity.PaymentMethod
import com.mobilestore.inventory.data.local.entity.PhoneStatus
import com.mobilestore.inventory.data.local.entity.SaleEntity
import com.mobilestore.inventory.data.local.entity.TransactionType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

data class NewSaleInput(
    val storeId: Long,
    val phoneId: Long,
    val saleDate: Long,
    val sellingPrice: Double,
    val customerName: String,
    val customerPhone: String?,
    val paymentMethod: PaymentMethod,
    val notes: String?
)

@Singleton
class SaleRepository @Inject constructor(
    private val db: AppDatabase,
    private val saleDao: SaleDao,
    private val phoneDao: PhoneDao,
    private val purchaseDao: PurchaseDao,
    private val transactionRepository: TransactionRepository
) {
    fun getSalesForStore(storeId: Long): Flow<List<SaleEntity>> = saleDao.getSalesForStore(storeId)
    fun getTotalSalesValue(storeId: Long): Flow<Double> = saleDao.getTotalSalesValue(storeId)
    fun getTotalProfit(storeId: Long): Flow<Double> = saleDao.getTotalProfit(storeId)
    fun getProfitInRange(storeId: Long, start: Long, end: Long): Flow<Double> =
        saleDao.getProfitInRange(storeId, start, end)

    suspend fun getSaleForPhoneOrNull(phoneId: Long): SaleEntity? = saleDao.getSaleForPhone(phoneId)

    /**
     * Atomically: looks up the phone's locked purchase price, computes
     * profit = sellingPrice - purchasePrice, writes the Sale row, flips the
     * phone to SOLD, and writes the ledger Transaction row.
     */
    suspend fun recordSale(input: NewSaleInput): Long = db.withTransaction {
        val purchase = purchaseDao.getPurchaseForPhone(input.phoneId)
            ?: error("No purchase record found for this phone — cannot sell an item with no purchase history")

        val profit = input.sellingPrice - purchase.purchasePrice

        val sale = SaleEntity(
            storeId = input.storeId,
            phoneId = input.phoneId,
            saleDate = input.saleDate,
            sellingPrice = input.sellingPrice,
            profit = profit,
            customerName = input.customerName,
            customerPhone = input.customerPhone,
            paymentMethod = input.paymentMethod,
            notes = input.notes
        )
        val saleId = saleDao.insertSale(sale)

        phoneDao.setStatus(input.phoneId, PhoneStatus.SOLD)

        transactionRepository.recordTransaction(
            storeId = input.storeId,
            type = TransactionType.SALE,
            relatedPhoneId = input.phoneId,
            amountSigned = input.sellingPrice,
            profit = profit,
            relatedSaleId = saleId,
            notes = "Sold to ${input.customerName}"
        )

        saleId
    }

    /**
     * Reversal: original Sale row stays exactly as recorded. Writes a
     * REVERSAL_SALE ledger row that cancels out the amount and profit, marks
     * isReversed for display, and returns the phone to IN_STOCK so it's
     * sellable again.
     */
    suspend fun reverseSale(saleId: Long, phoneId: Long, originalTransactionId: Long, reason: String) = db.withTransaction {
        saleDao.markReversed(saleId)
        phoneDao.setStatus(phoneId, PhoneStatus.IN_STOCK)

        val original = transactionRepository.getById(originalTransactionId)
            ?: error("Original transaction not found")
        val reversal = transactionRepository.recordTransaction(
            storeId = original.storeId,
            type = TransactionType.REVERSAL_SALE,
            relatedPhoneId = original.relatedPhoneId,
            amountSigned = -original.amount,
            profit = original.profit?.let { -it },
            relatedSaleId = original.relatedSaleId,
            notes = "Reversal of ${original.referenceNumber}: $reason"
        )
        transactionRepository.recordReversal(original.storeId, original, reversal, reason)
    }
}
