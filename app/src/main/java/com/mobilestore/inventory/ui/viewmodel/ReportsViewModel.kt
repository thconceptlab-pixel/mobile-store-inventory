package com.mobilestore.inventory.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilestore.inventory.data.export.ReportExportManager
import com.mobilestore.inventory.data.local.entity.PurchaseEntity
import com.mobilestore.inventory.data.local.entity.SaleEntity
import com.mobilestore.inventory.data.repository.PhoneRepository
import com.mobilestore.inventory.data.repository.PurchaseRepository
import com.mobilestore.inventory.data.repository.SaleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Calendar
import javax.inject.Inject

sealed class ReportExportEvent {
    data class Success(val uri: Uri) : ReportExportEvent()
    data class Error(val message: String) : ReportExportEvent()
}

enum class ReportPeriod { DAILY, WEEKLY, MONTHLY, YEARLY, CUSTOM }

data class BrandTally(val brand: String, val unitsSold: Int, val revenue: Double)

data class ReportsUiState(
    val purchaseCount: Int = 0,
    val purchaseValue: Double = 0.0,
    val saleCount: Int = 0,
    val saleValue: Double = 0.0,
    val profit: Double = 0.0,
    val currentStockCount: Int = 0,
    val currentStockValue: Double = 0.0,
    val topBrands: List<BrandTally> = emptyList(),
    val topModels: List<BrandTally> = emptyList(),
    val dailyProfitTrend: List<Pair<String, Double>> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val purchaseRepository: PurchaseRepository,
    private val saleRepository: SaleRepository,
    private val phoneRepository: PhoneRepository,
    private val exportManager: ReportExportManager
) : ViewModel() {

    private val storeIdFlow = MutableStateFlow<Long?>(null)
    private val periodFlow = MutableStateFlow(ReportPeriod.MONTHLY)
    private val customRangeFlow = MutableStateFlow<Pair<Long, Long>?>(null)

    fun setActiveStore(storeId: Long) { storeIdFlow.value = storeId }
    fun setPeriod(period: ReportPeriod) { periodFlow.value = period }
    fun setCustomRange(start: Long, end: Long) { customRangeFlow.value = start to end; periodFlow.value = ReportPeriod.CUSTOM }

    val period: StateFlow<ReportPeriod> = periodFlow

    val uiState: StateFlow<ReportsUiState> = combine(
        storeIdFlow.filterNotNull(), periodFlow, customRangeFlow
    ) { storeId, period, customRange -> Triple(storeId, period, customRange) }
        .flatMapLatest { (storeId, period, customRange) ->
            val (start, end) = resolveRange(period, customRange)
            combine(
                purchaseRepository.getPurchasesForStore(storeId),
                saleRepository.getSalesForStore(storeId),
                phoneRepository.getInStock(storeId),
                phoneRepository.getPhonesWithPurchaseInfo(storeId)
            ) { allPurchases, allSales, inStock, allWithPurchase ->
                val purchasesInRange = allPurchases.filter { !it.isReversed && it.purchaseDate in start..end }
                val salesInRange = allSales.filter { !it.isReversed && it.saleDate in start..end }

                val stockValue = allWithPurchase.filter { it.phone.status.name == "IN_STOCK" }.sumOf { it.purchasePrice }

                val brandTallies = salesInRange
                    .mapNotNull { sale -> allWithPurchase.find { it.phone.phoneId == sale.phoneId }?.let { it.phone.brand to sale } }
                    .groupBy { it.first }
                    .map { (brand, entries) -> BrandTally(brand, entries.size, entries.sumOf { it.second.sellingPrice }) }
                    .sortedByDescending { it.unitsSold }
                    .take(5)

                val modelTallies = salesInRange
                    .mapNotNull { sale -> allWithPurchase.find { it.phone.phoneId == sale.phoneId }?.let { "${it.phone.brand} ${it.phone.model}" to sale } }
                    .groupBy { it.first }
                    .map { (model, entries) -> BrandTally(model, entries.size, entries.sumOf { it.second.sellingPrice }) }
                    .sortedByDescending { it.unitsSold }
                    .take(5)

                val trend = buildDailyTrend(salesInRange, start, end)

                ReportsUiState(
                    purchaseCount = purchasesInRange.size,
                    purchaseValue = purchasesInRange.sumOf { it.purchasePrice },
                    saleCount = salesInRange.size,
                    saleValue = salesInRange.sumOf { it.sellingPrice },
                    profit = salesInRange.sumOf { it.profit },
                    currentStockCount = inStock.size,
                    currentStockValue = stockValue,
                    topBrands = brandTallies,
                    topModels = modelTallies,
                    dailyProfitTrend = trend
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReportsUiState())

    private fun buildDailyTrend(sales: List<SaleEntity>, start: Long, end: Long): List<Pair<String, Double>> {
        val dayFmt = java.text.SimpleDateFormat("dd MMM", java.util.Locale.getDefault())
        val cappedStart = maxOf(start, end - 13L * 86_400_000L) // last 14 days max, so the chart stays readable
        val byDay = sales.filter { it.saleDate in cappedStart..end }
            .groupBy { dayFmt.format(java.util.Date(it.saleDate)) }
            .mapValues { (_, v) -> v.sumOf { it.profit } }
        val points = mutableListOf<Pair<String, Double>>()
        var cursor = cappedStart
        val cal = Calendar.getInstance()
        while (cursor <= end) {
            cal.timeInMillis = cursor
            val label = dayFmt.format(cal.time)
            points += label to (byDay[label] ?: 0.0)
            cursor += 86_400_000L
        }
        return points
    }

    private fun resolveRange(period: ReportPeriod, customRange: Pair<Long, Long>?): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        fun endOfToday(): Long {
            val c = cal.clone() as Calendar
            c.set(Calendar.HOUR_OF_DAY, 23); c.set(Calendar.MINUTE, 59); c.set(Calendar.SECOND, 59); c.set(Calendar.MILLISECOND, 999)
            return c.timeInMillis
        }
        val end = endOfToday()
        val start = when (period) {
            ReportPeriod.DAILY -> {
                val c = cal.clone() as Calendar
                c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
                c.timeInMillis
            }
            ReportPeriod.WEEKLY -> {
                val c = cal.clone() as Calendar
                c.add(Calendar.DAY_OF_YEAR, -7)
                c.timeInMillis
            }
            ReportPeriod.MONTHLY -> {
                val c = cal.clone() as Calendar
                c.add(Calendar.MONTH, -1)
                c.timeInMillis
            }
            ReportPeriod.YEARLY -> {
                val c = cal.clone() as Calendar
                c.add(Calendar.YEAR, -1)
                c.timeInMillis
            }
            ReportPeriod.CUSTOM -> return customRange ?: (0L to end)
        }
        return start to end
    }

    private fun periodLabel(period: ReportPeriod): String = when (period) {
        ReportPeriod.DAILY -> "Daily"
        ReportPeriod.WEEKLY -> "Weekly"
        ReportPeriod.MONTHLY -> "Monthly"
        ReportPeriod.YEARLY -> "Yearly"
        ReportPeriod.CUSTOM -> "Custom Range"
    }

    fun exportPdf(storeName: String, currencyFormat: NumberFormat, onResult: (ReportExportEvent) -> Unit) {
        viewModelScope.launch {
            try {
                val uri = exportManager.exportPdf(storeName, periodLabel(periodFlow.value), uiState.value, currencyFormat)
                onResult(ReportExportEvent.Success(uri))
            } catch (e: Exception) {
                onResult(ReportExportEvent.Error(e.message ?: "Could not create PDF report"))
            }
        }
    }

    fun exportCsv(storeName: String, currencyFormat: NumberFormat, onResult: (ReportExportEvent) -> Unit) {
        viewModelScope.launch {
            try {
                val uri = exportManager.exportCsv(storeName, periodLabel(periodFlow.value), uiState.value, currencyFormat)
                onResult(ReportExportEvent.Success(uri))
            } catch (e: Exception) {
                onResult(ReportExportEvent.Error(e.message ?: "Could not create CSV report"))
            }
        }
    }
}
