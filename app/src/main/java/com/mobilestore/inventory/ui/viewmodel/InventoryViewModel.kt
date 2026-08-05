package com.mobilestore.inventory.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilestore.inventory.data.local.entity.PhoneStatus
import com.mobilestore.inventory.data.local.relation.PhoneWithPurchase
import com.mobilestore.inventory.data.repository.PhoneRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

enum class StatusFilter { ALL, IN_STOCK, SOLD }

data class InventoryFilterState(
    val searchQuery: String = "",
    val statusFilter: StatusFilter = StatusFilter.ALL,
    val brandFilter: String? = null,
    val minPrice: Double? = null,
    val maxPrice: Double? = null,
    val startDate: Long? = null,
    val endDate: Long? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val phoneRepository: PhoneRepository
) : ViewModel() {

    private val storeIdFlow = MutableStateFlow<Long?>(null)
    private val filterState = MutableStateFlow(InventoryFilterState())

    fun setActiveStore(storeId: Long) { storeIdFlow.value = storeId }
    fun updateSearch(query: String) { filterState.update { it.copy(searchQuery = query) } }
    fun updateStatusFilter(status: StatusFilter) { filterState.update { it.copy(statusFilter = status) } }
    fun updateBrandFilter(brand: String?) { filterState.update { it.copy(brandFilter = brand) } }
    fun updatePriceRange(min: Double?, max: Double?) { filterState.update { it.copy(minPrice = min, maxPrice = max) } }
    fun updateDateRange(start: Long?, end: Long?) { filterState.update { it.copy(startDate = start, endDate = end) } }
    fun clearFilters() { filterState.value = InventoryFilterState(searchQuery = filterState.value.searchQuery) }

    val availableBrands: StateFlow<List<String>> = storeIdFlow.filterNotNull()
        .flatMapLatest { phoneRepository.getDistinctBrandsUsed(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val allPhonesForStore: Flow<List<PhoneWithPurchase>> = storeIdFlow.filterNotNull()
        .flatMapLatest { phoneRepository.getPhonesWithPurchaseInfo(it) }

    val filteredPhones: StateFlow<List<PhoneWithPurchase>> = combine(allPhonesForStore, filterState) { phones, f ->
        phones.filter { item ->
            val phone = item.phone
            val matchesSearch = f.searchQuery.isBlank() ||
                phone.brand.contains(f.searchQuery, ignoreCase = true) ||
                phone.model.contains(f.searchQuery, ignoreCase = true) ||
                phone.imei1.contains(f.searchQuery) ||
                (phone.imei2?.contains(f.searchQuery) == true)
            val matchesStatus = when (f.statusFilter) {
                StatusFilter.ALL -> true
                StatusFilter.IN_STOCK -> phone.status == PhoneStatus.IN_STOCK
                StatusFilter.SOLD -> phone.status == PhoneStatus.SOLD
            }
            val matchesBrand = f.brandFilter == null || phone.brand == f.brandFilter
            val matchesMinPrice = f.minPrice == null || item.purchasePrice >= f.minPrice
            val matchesMaxPrice = f.maxPrice == null || item.purchasePrice <= f.maxPrice
            val matchesDate = (f.startDate == null || item.purchaseDate >= f.startDate) &&
                (f.endDate == null || item.purchaseDate <= f.endDate)
            matchesSearch && matchesStatus && matchesBrand && matchesMinPrice && matchesMaxPrice && matchesDate
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentFilters: StateFlow<InventoryFilterState> = filterState
}
