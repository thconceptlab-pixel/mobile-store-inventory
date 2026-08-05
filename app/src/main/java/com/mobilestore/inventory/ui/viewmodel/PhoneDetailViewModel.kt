package com.mobilestore.inventory.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilestore.inventory.data.local.entity.PhoneEntity
import com.mobilestore.inventory.data.local.entity.PurchaseEntity
import com.mobilestore.inventory.data.local.entity.SaleEntity
import com.mobilestore.inventory.data.repository.PhoneRepository
import com.mobilestore.inventory.data.repository.PurchaseRepository
import com.mobilestore.inventory.data.repository.SaleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PhoneDetailState(
    val phone: PhoneEntity? = null,
    val purchase: PurchaseEntity? = null,
    val sale: SaleEntity? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class PhoneDetailViewModel @Inject constructor(
    private val phoneRepository: PhoneRepository,
    private val purchaseRepository: PurchaseRepository,
    private val saleRepository: SaleRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PhoneDetailState())
    val state: StateFlow<PhoneDetailState> = _state.asStateFlow()

    fun load(phoneId: Long) {
        viewModelScope.launch {
            val phone = phoneRepository.getPhoneById(phoneId)
            val purchase = purchaseRepository.getPurchaseForPhone(phoneId)
            val sale = if (phone?.status?.name == "SOLD") saleRepository.getSaleForPhoneOrNull(phoneId) else null
            _state.value = PhoneDetailState(phone = phone, purchase = purchase, sale = sale, isLoading = false)
        }
    }
}
