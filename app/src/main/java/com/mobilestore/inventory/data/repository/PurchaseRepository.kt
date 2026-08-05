package com.mobilestore.inventory.data.repository

import androidx.room.withTransaction
import com.mobilestore.inventory.data.local.AppDatabase
import com.mobilestore.inventory.data.local.dao.PhoneDao
import com.mobilestore.inventory.data.local.dao.PurchaseDao
import com.mobilestore.inventory.data.local.entity.PhoneEntity
import com.mobilestore.inventory.data.local.entity.PurchaseEntity
import com.mobilestore.inventory.data.local.entity.TransactionType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** Input bundle for a new purchase entry — the whole "buy a phone" form. */
data class NewPurchaseInput(
    val storeId: Long,
    val brand: String,
    val model: String,
    val imei1: String,
    val imei2: String?,
    val storage: String,
    val ram: String,
    val color: String,
    val batteryHealth: String,
    val condition: String,
    val ptaStatus: String,
    val accessoriesIncluded: String?,
    val purchasePrice: Double,
    val purchaseDate: Long,
    val supplierName: String,
    val supplierPhone: String?,
    val notes: String?,
    val imagePath: String? = null
)

@Singleton
class PurchaseRepository @Inject constructor(
    private val db: AppDatabase,
    private val phoneDao: PhoneDao,
    private val purchaseDao: PurchaseDao,
    private val transactionRepository: TransactionRepository
) {
    fun getPurchasesForStore(storeId: Long): Flow<List<PurchaseEntity>> =
        purchaseDao.getPurchasesForStore(storeId)

    fun getTotalPurchaseValue(storeId: Long): Flow<Double> = purchaseDao.getTotalPurchaseValue(storeId)

    /** The locked purchase price for a phone — used by Sale Entry to compute live projected profit. */
    suspend fun getPurchaseForPhone(phoneId: Long): PurchaseEntity? = purchaseDao.getPurchaseForPhone(phoneId)

    /**
     * Atomically creates the Phone, its Purchase record, and the ledger
     * Transaction row. Once committed, purchasePrice / purchaseDate / IMEI
     * are immutable — see PurchaseEntity and PurchaseDao for the enforcement.
     */
    suspend fun recordPurchase(input: NewPurchaseInput): Long = db.withTransaction {
        val phone = PhoneEntity(
            storeId = input.storeId,
            brand = input.brand,
            model = input.model,
            imei1 = input.imei1,
            imei2 = input.imei2,
            storage = input.storage,
            ram = input.ram,
            color = input.color,
            batteryHealth = input.batteryHealth,
            condition = input.condition,
            ptaStatus = input.ptaStatus,
            accessoriesIncluded = input.accessoriesIncluded,
            notes = input.notes,
            imagePath = input.imagePath
        )
        val phoneId = phoneDao.insertPhone(phone)

        val purchase = PurchaseEntity(
            storeId = input.storeId,
            phoneId = phoneId,
            purchaseDate = input.purchaseDate,
            purchasePrice = input.purchasePrice,
            supplierName = input.supplierName,
            supplierPhone = input.supplierPhone,
            notes = input.notes
        )
        val purchaseId = purchaseDao.insertPurchase(purchase)

        transactionRepository.recordTransaction(
            storeId = input.storeId,
            type = TransactionType.PURCHASE,
            relatedPhoneId = phoneId,
            amountSigned = -input.purchasePrice,
            relatedPurchaseId = purchaseId,
            notes = "Purchased ${input.brand} ${input.model}"
        )

        phoneId
    }

    /**
     * Reversal flow required by the spec: the original Purchase row and its
     * Transaction row are left completely untouched. This inserts a new
     * REVERSAL_PURCHASE ledger row (amount reversed back in), flags
     * isReversed on the purchase for display purposes only, and returns the
     * phone to available inventory status is NOT changed automatically if it
     * was already sold — that's a separate, explicit business decision the
     * user makes from the History screen (Phase 3).
     */
    suspend fun reversePurchase(purchaseId: Long, originalTransactionId: Long, reason: String) = db.withTransaction {
        purchaseDao.markReversed(purchaseId)
        val original = transactionRepository.getById(originalTransactionId)
            ?: error("Original transaction not found")
        val reversal = transactionRepository.recordTransaction(
            storeId = original.storeId,
            type = TransactionType.REVERSAL_PURCHASE,
            relatedPhoneId = original.relatedPhoneId,
            amountSigned = -original.amount, // cancels out the original signed amount
            relatedPurchaseId = original.relatedPurchaseId,
            notes = "Reversal of ${original.referenceNumber}: $reason"
        )
        transactionRepository.recordReversal(original.storeId, original, reversal, reason)
    }
}
