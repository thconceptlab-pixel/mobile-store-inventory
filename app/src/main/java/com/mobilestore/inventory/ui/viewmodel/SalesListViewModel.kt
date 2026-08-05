package com.mobilestore.inventory.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilestore.inventory.data.local.entity.PhoneEntity
import com.mobilestore.inventory.data.repository.PhoneRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * Backs the Sales tab's phone picker: "select a phone from inventory" per
 * the spec, restricted to IN_STOCK items only (a sold phone can't be sold
 * again). Tapping an item navigates to Sale Entry with that phoneId.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SalesListViewModel @Inject constructor(
    private val phoneRepository: PhoneRepository
) : ViewModel() {

    private val storeIdFlow = MutableStateFlow<Long?>(null)
    private val searchQuery = MutableStateFlow("")

    fun setActiveStore(storeId: Long) { storeIdFlow.value = storeId }
    fun updateSearch(query: String) { searchQuery.value = query }

    val sellablePhones: StateFlow<List<PhoneEntity>> = storeIdFlow.filterNotNull()
        .flatMapLatest { phoneRepository.getInStock(it) }
        .combine(searchQuery) { phones, query ->
            if (query.isBlank()) phones
            else phones.filter {
                it.brand.contains(query, ignoreCase = true) ||
                    it.model.contains(query, ignoreCase = true) ||
                    it.imei1.contains(query)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchText: StateFlow<String> = searchQuery
}
