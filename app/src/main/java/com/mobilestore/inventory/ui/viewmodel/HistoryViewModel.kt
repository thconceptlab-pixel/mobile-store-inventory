package com.mobilestore.inventory.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilestore.inventory.data.local.entity.TransactionEntity
import com.mobilestore.inventory.data.local.entity.TransactionType
import com.mobilestore.inventory.data.repository.PurchaseRepository
import com.mobilestore.inventory.data.repository.SaleRepository
import com.mobilestore.inventory.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

enum class DateRangePreset { TODAY, YESTERDAY, THIS_WEEK, THIS_MONTH, LAST_YEAR, CUSTOM, ALL }

data class HistoryFilterState(
    val preset: DateRangePreset = DateRangePreset.ALL,
    val customStart: Long? = null,
    val customEnd: Long? = null,
    val searchQuery: String = ""
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val purchaseRepository: PurchaseRepository,
    private val saleRepository: SaleRepository
) : ViewModel() {

    private val storeIdFlow = MutableStateFlow<Long?>(null)
    private val filterState = MutableStateFlow(HistoryFilterState())
    val currentFilters: StateFlow<HistoryFilterState> = filterState

    fun setActiveStore(storeId: Long) { storeIdFlow.value = storeId }
    fun updatePreset(preset: DateRangePreset) { filterState.update { it.copy(preset = preset) } }
    fun updateCustomRange(start: Long, end: Long) { filterState.update { it.copy(preset = DateRangePreset.CUSTOM, customStart = start, customEnd = end) } }
    fun updateSearch(query: String) { filterState.update { it.copy(searchQuery = query) } }

    private val rawTransactions: Flow<List<TransactionEntity>> =
        combine(storeIdFlow.filterNotNull(), filterState) { storeId, filters -> storeId to filters }
            .flatMapLatest { (storeId, filters) ->
                val range = resolveRange(filters)
                if (range == null) transactionRepository.getTransactionsForStore(storeId)
                else transactionRepository.getTransactionsInRange(storeId, range.first, range.second)
            }

    val transactions: StateFlow<List<TransactionEntity>> = combine(rawTransactions, filterState) { list, filters ->
        if (filters.searchQuery.isBlank()) list
        else list.filter { it.referenceNumber.contains(filters.searchQuery, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Which transactions already have a reversal on file — drives whether the "Reverse" action shows.
    private val _reversedIds = MutableStateFlow<Set<Long>>(emptySet())
    val reversedIds: StateFlow<Set<Long>> = _reversedIds

    fun refreshReversalStatus(visibleTransactions: List<TransactionEntity>) {
        viewModelScope.launch {
            val reversed = mutableSetOf<Long>()
            visibleTransactions
                .filter { it.type == TransactionType.PURCHASE || it.type == TransactionType.SALE }
                .forEach { tx ->
                    if (transactionRepository.getReversalFor(tx.transactionId) != null) reversed += tx.transactionId
                }
            _reversedIds.value = reversed
        }
    }

    fun reverseTransaction(tx: TransactionEntity, reason: String, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                when (tx.type) {
                    TransactionType.PURCHASE -> {
                        val purchaseId = tx.relatedPurchaseId ?: error("Missing purchase reference")
                        purchaseRepository.reversePurchase(purchaseId, tx.transactionId, reason)
                    }
                    TransactionType.SALE -> {
                        val saleId = tx.relatedSaleId ?: error("Missing sale reference")
                        saleRepository.reverseSale(saleId, tx.relatedPhoneId, tx.transactionId, reason)
                    }
                    else -> error("Only original purchases or sales can be reversed")
                }
                onDone(true, null)
            } catch (e: Exception) {
                onDone(false, e.message ?: "Could not reverse this transaction")
            }
        }
    }

    private fun resolveRange(filters: HistoryFilterState): Pair<Long, Long>? {
        val cal = Calendar.getInstance()
        fun startOfDay(c: Calendar): Long {
            c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
            return c.timeInMillis
        }
        fun endOfDay(c: Calendar): Long {
            c.set(Calendar.HOUR_OF_DAY, 23); c.set(Calendar.MINUTE, 59); c.set(Calendar.SECOND, 59); c.set(Calendar.MILLISECOND, 999)
            return c.timeInMillis
        }
        return when (filters.preset) {
            DateRangePreset.ALL -> null
            DateRangePreset.TODAY -> {
                val start = startOfDay(cal.clone() as Calendar)
                val end = endOfDay(cal.clone() as Calendar)
                start to end
            }
            DateRangePreset.YESTERDAY -> {
                val c = cal.clone() as Calendar
                c.add(Calendar.DAY_OF_YEAR, -1)
                startOfDay(c.clone() as Calendar) to endOfDay(c.clone() as Calendar)
            }
            DateRangePreset.THIS_WEEK -> {
                val c = cal.clone() as Calendar
                c.set(Calendar.DAY_OF_WEEK, c.firstDayOfWeek)
                val start = startOfDay(c.clone() as Calendar)
                val end = endOfDay(cal.clone() as Calendar)
                start to end
            }
            DateRangePreset.THIS_MONTH -> {
                val c = cal.clone() as Calendar
                c.set(Calendar.DAY_OF_MONTH, 1)
                val start = startOfDay(c.clone() as Calendar)
                val end = endOfDay(cal.clone() as Calendar)
                start to end
            }
            DateRangePreset.LAST_YEAR -> {
                val c = cal.clone() as Calendar
                c.add(Calendar.YEAR, -1)
                startOfDay(c.clone() as Calendar) to endOfDay(cal.clone() as Calendar)
            }
            DateRangePreset.CUSTOM -> {
                val s = filters.customStart ?: return null
                val e = filters.customEnd ?: return null
                s to e
            }
        }
    }
}
