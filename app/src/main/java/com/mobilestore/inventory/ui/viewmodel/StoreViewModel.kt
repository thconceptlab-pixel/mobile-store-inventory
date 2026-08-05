package com.mobilestore.inventory.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilestore.inventory.data.local.entity.StoreEntity
import com.mobilestore.inventory.data.repository.StoreRepository
import com.mobilestore.inventory.data.session.StoreSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives store creation/switching. Also exposes [activeStore] as the single
 * source of truth every other screen (Dashboard, Inventory, Purchase Entry,
 * Sale Entry) reads to know which store's data to show — this is how
 * "completely separate data per store" is enforced app-wide.
 */
@HiltViewModel
class StoreViewModel @Inject constructor(
    private val storeRepository: StoreRepository,
    private val session: StoreSessionManager
) : ViewModel() {

    val allStores: StateFlow<List<StoreEntity>> = storeRepository.getAllStores()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val activeStoreId: StateFlow<Long?> = session.activeStoreId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Emits null while we don't yet know / no store exists — screens should show a loading or setup state. */
    val activeStore: StateFlow<StoreEntity?> = combine(allStores, activeStoreId) { stores, activeId ->
        if (stores.isEmpty()) return@combine null
        stores.firstOrNull { it.storeId == activeId } ?: stores.first()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun switchStore(storeId: Long) {
        viewModelScope.launch { session.setActiveStore(storeId) }
    }

    fun createStore(
        shopName: String,
        ownerName: String,
        phoneNumber: String,
        address: String,
        onCreated: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val id = storeRepository.createStore(
                StoreEntity(
                    shopName = shopName,
                    ownerName = ownerName,
                    phoneNumber = phoneNumber,
                    address = address
                )
            )
            session.setActiveStore(id)
            onCreated(id)
        }
    }
}
