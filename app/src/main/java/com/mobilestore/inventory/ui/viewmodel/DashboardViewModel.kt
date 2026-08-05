package com.mobilestore.inventory.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilestore.inventory.data.local.entity.TransactionEntity
import com.mobilestore.inventory.data.repository.PhoneRepository
import com.mobilestore.inventory.data.repository.PurchaseRepository
import com.mobilestore.inventory.data.repository.SaleRepository
import com.mobilestore.inventory.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.util.Calendar
import javax.inject.Inject

data class DashboardUiState(
    val inStockCount: Int = 0,
    val totalPurchaseValue: Double = 0.0,
    val totalSalesValue: Double = 0.0,
    val totalProfit: Double = 0.0,
    val todaysPurchaseValue: Double = 0.0,
    val todaysSalesValue: Double = 0.0,
    val todaysProfit: Double = 0.0,
    val recentTransactions: List<TransactionEntity> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val phoneRepository: PhoneRepository,
    private val purchaseRepository: PurchaseRepository,
    private val saleRepository: SaleRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val storeIdFlow = MutableStateFlow<Long?>(null)

    fun setActiveStore(storeId: Long) {
        storeIdFlow.value = storeId
    }

    val uiState: StateFlow<DashboardUiState> = storeIdFlow.filterNotNull().flatMapLatest { storeId ->
        val (startOfDay, endOfDay) = todayRange()

        // First 4 simple totals combined together...
        val totals = combine(
            phoneRepository.inStockCount(storeId),
            purchaseRepository.getTotalPurchaseValue(storeId),
            saleRepository.getTotalSalesValue(storeId),
            saleRepository.getTotalProfit(storeId)
        ) { inStock, totalPurchase, totalSales, totalProfit ->
            listOf(inStock, totalPurchase, totalSales, totalProfit)
        }

        // ...then combined with the two transaction-list flows (kept separate
        // since kotlinx's typed `combine` only goes up to 5 flows).
        combine(
            totals,
            transactionRepository.getRecentTransactions(storeId, 10),
            transactionRepository.getTransactionsInRange(storeId, startOfDay, endOfDay)
        ) { totalsList, recent, todaysTx ->
            val inStock = totalsList[0] as Int
            val totalPurchase = totalsList[1] as Double
            val totalSales = totalsList[2] as Double
            val totalProfit = totalsList[3] as Double

            val todaysPurchases = todaysTx.filter { it.type.name == "PURCHASE" }.sumOf { -it.amount }
            val todaysSales = todaysTx.filter { it.type.name == "SALE" }.sumOf { it.amount }
            val todaysProfit = todaysTx.filter { it.type.name == "SALE" }.sumOf { it.profit ?: 0.0 }

            DashboardUiState(
                inStockCount = inStock,
                totalPurchaseValue = totalPurchase,
                totalSalesValue = totalSales,
                totalProfit = totalProfit,
                todaysPurchaseValue = todaysPurchases,
                todaysSalesValue = todaysSales,
                todaysProfit = todaysProfit,
                recentTransactions = recent
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    private fun todayRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis
        return start to end
    }
}
