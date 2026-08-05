package com.mobilestore.inventory.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilestore.inventory.data.local.entity.PaymentMethod
import com.mobilestore.inventory.data.local.entity.PhoneEntity
import com.mobilestore.inventory.data.repository.NewSaleInput
import com.mobilestore.inventory.data.repository.PhoneRepository
import com.mobilestore.inventory.data.repository.PurchaseRepository
import com.mobilestore.inventory.data.repository.SaleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SaleFormState(
    val storeId: Long = 0,
    val phone: PhoneEntity? = null,
    val purchasePrice: Double? = null,
    val saleDate: Long = System.currentTimeMillis(),
    val customerName: String = "",
    val customerPhone: String = "",
    val sellingPrice: String = "",
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val notes: String = "",
    val errors: Map<String, String> = emptyMap(),
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val submitError: String? = null
) {
    /** Live preview only — the real, locked profit is computed and stored server-side at submit time. */
    val projectedProfit: Double?
        get() {
            val price = sellingPrice.toDoubleOrNull() ?: return null
            val cost = purchasePrice ?: return null
            return price - cost
        }
}

@HiltViewModel
class SaleEntryViewModel @Inject constructor(
    private val saleRepository: SaleRepository,
    private val phoneRepository: PhoneRepository,
    private val purchaseRepository: PurchaseRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SaleFormState())
    val state: StateFlow<SaleFormState> = _state.asStateFlow()

    /** Loads the phone being sold plus its locked purchase price (for the live profit preview only). */
    fun loadPhone(storeId: Long, phoneId: Long) {
        viewModelScope.launch {
            val phone = phoneRepository.getPhoneById(phoneId)
            val purchase = purchaseRepository.getPurchaseForPhone(phoneId)
            _state.value = _state.value.copy(
                storeId = storeId,
                phone = phone,
                purchasePrice = purchase?.purchasePrice
            )
        }
    }

    fun onCustomerNameChange(v: String) { _state.value = _state.value.copy(customerName = v) }
    fun onCustomerPhoneChange(v: String) { _state.value = _state.value.copy(customerPhone = v) }
    fun onSellingPriceChange(v: String) { _state.value = _state.value.copy(sellingPrice = v.filter { it.isDigit() || it == '.' }) }
    fun onPaymentMethodChange(v: PaymentMethod) { _state.value = _state.value.copy(paymentMethod = v) }
    fun onNotesChange(v: String) { _state.value = _state.value.copy(notes = v) }
    fun onDateChange(millis: Long) { _state.value = _state.value.copy(saleDate = millis) }

    private fun validate(s: SaleFormState): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        if (s.phone == null) errors["phone"] = "No phone selected"
        if (s.customerName.isBlank()) errors["customerName"] = "Customer name is required"
        val price = s.sellingPrice.toDoubleOrNull()
        if (price == null || price <= 0) errors["sellingPrice"] = "Enter a valid selling price"
        return errors
    }

    fun submit() {
        val s = _state.value
        val errors = validate(s)
        if (errors.isNotEmpty()) {
            _state.value = s.copy(errors = errors)
            return
        }
        _state.value = s.copy(isSaving = true, errors = emptyMap(), submitError = null)
        viewModelScope.launch {
            try {
                saleRepository.recordSale(
                    NewSaleInput(
                        storeId = s.storeId,
                        phoneId = s.phone!!.phoneId,
                        saleDate = s.saleDate,
                        sellingPrice = s.sellingPrice.toDouble(),
                        customerName = s.customerName.trim(),
                        customerPhone = s.customerPhone.ifBlank { null },
                        paymentMethod = s.paymentMethod,
                        notes = s.notes.ifBlank { null }
                    )
                )
                _state.value = s.copy(isSaving = false, saved = true)
            } catch (e: Exception) {
                _state.value = s.copy(isSaving = false, submitError = e.message ?: "Could not complete sale. Please try again.")
            }
        }
    }
}
