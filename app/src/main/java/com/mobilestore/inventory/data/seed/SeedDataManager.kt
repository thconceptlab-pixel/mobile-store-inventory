package com.mobilestore.inventory.data.seed

import com.mobilestore.inventory.data.local.entity.PaymentMethod
import com.mobilestore.inventory.data.repository.NewPurchaseInput
import com.mobilestore.inventory.data.repository.NewSaleInput
import com.mobilestore.inventory.data.repository.PurchaseRepository
import com.mobilestore.inventory.data.repository.SaleRepository
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Optional demo-data generator, triggered manually from Settings ("Load
 * Sample Data"). Never runs automatically — a real shop's first launch
 * should start with an empty, honest inventory, not fake stock. Useful for
 * trying out Dashboard/Inventory/History/Reports before entering real
 * purchases.
 */
@Singleton
class SeedDataManager @Inject constructor(
    private val purchaseRepository: PurchaseRepository,
    private val saleRepository: SaleRepository
) {
    private fun daysAgo(days: Int): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -days)
        return cal.timeInMillis
    }

    suspend fun seedSampleData(storeId: Long) {
        val samplePhones = listOf(
            SamplePhone("Samsung", "Galaxy S21", "128GB", "8GB", "Phantom Gray", "Excellent", 68000.0, 21, "Waheed Mobiles", true, 79000.0, 14, "Ahmed Khan"),
            SamplePhone("Apple", "iPhone 12", "64GB", "4GB", "Blue", "Good", 92000.0, 18, "Bilal Traders", true, 105000.0, 10, "Sara Ahmed"),
            SamplePhone("Xiaomi", "Redmi Note 11", "128GB", "6GB", "Graphite Gray", "Like New", 32000.0, 16, "City Mobiles", true, 38000.0, 9, "Usman Tariq"),
            SamplePhone("OnePlus", "Nord CE 2", "128GB", "8GB", "Bahama Blue", "Excellent", 41000.0, 12, "Waheed Mobiles", false, null, null, null),
            SamplePhone("Realme", "8 Pro", "128GB", "8GB", "Infinite Blue", "Good", 29000.0, 9, "City Mobiles", false, null, null, null),
            SamplePhone("Vivo", "Y21", "64GB", "4GB", "Diamond Glow", "Fair", 21000.0, 7, "Bilal Traders", true, 25500.0, 3, "Fatima Noor"),
            SamplePhone("Google Pixel", "6a", "128GB", "6GB", "Charcoal", "Excellent", 58000.0, 5, "City Mobiles", false, null, null, null),
            SamplePhone("Oppo", "Reno 6", "128GB", "8GB", "Aurora", "Like New", 44000.0, 3, "Waheed Mobiles", false, null, null, null)
        )

        samplePhones.forEach { sample ->
            val phoneId = purchaseRepository.recordPurchase(
                NewPurchaseInput(
                    storeId = storeId,
                    brand = sample.brand,
                    model = sample.model,
                    imei1 = randomImei(),
                    imei2 = null,
                    storage = sample.storage,
                    ram = sample.ram,
                    color = sample.color,
                    batteryHealth = "90-94%",
                    condition = sample.condition,
                    ptaStatus = "PTA Approved",
                    accessoriesIncluded = "Charger, Cable",
                    purchasePrice = sample.purchasePrice,
                    purchaseDate = daysAgo(sample.purchasedDaysAgo),
                    supplierName = sample.supplierName,
                    supplierPhone = "0300-1234567",
                    notes = "Sample data — safe to delete"
                )
            )

            if (sample.sold && sample.sellingPrice != null && sample.soldDaysAgo != null && sample.customerName != null) {
                saleRepository.recordSale(
                    NewSaleInput(
                        storeId = storeId,
                        phoneId = phoneId,
                        saleDate = daysAgo(sample.soldDaysAgo),
                        sellingPrice = sample.sellingPrice,
                        customerName = sample.customerName,
                        customerPhone = "0333-9876543",
                        paymentMethod = PaymentMethod.CASH,
                        notes = "Sample data — safe to delete"
                    )
                )
            }
        }
    }

    private fun randomImei(): String {
        // 15 random digits — not a real IMEI, clearly sample data.
        return (1..15).map { (0..9).random() }.joinToString("")
    }

    private data class SamplePhone(
        val brand: String,
        val model: String,
        val storage: String,
        val ram: String,
        val color: String,
        val condition: String,
        val purchasePrice: Double,
        val purchasedDaysAgo: Int,
        val supplierName: String,
        val sold: Boolean,
        val sellingPrice: Double?,
        val soldDaysAgo: Int?,
        val customerName: String?
    )
}
