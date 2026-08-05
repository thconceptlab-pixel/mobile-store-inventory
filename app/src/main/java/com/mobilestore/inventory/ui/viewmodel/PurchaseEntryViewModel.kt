package com.mobilestore.inventory.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilestore.inventory.data.repository.NewPurchaseInput
import com.mobilestore.inventory.data.repository.PhoneRepository
import com.mobilestore.inventory.data.repository.PurchaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class PurchaseFormState(
    val storeId: Long = 0,
    val purchaseDate: Long = System.currentTimeMillis(),
    val brand: String = "",
    val model: String = "",
    val imei1: String = "",
    val imei2: String = "",
    val storage: String = "",
    val ram: String = "",
    val color: String = "",
    val batteryHealth: String = "",
    val condition: String = "",
    val ptaStatus: String = "",
    val accessories: Set<String> = emptySet(),
    val purchasePrice: String = "",
    val supplierName: String = "",
    val supplierPhone: String = "",
    val notes: String = "",
    val modelSuggestions: List<String> = emptyList(),
    val errors: Map<String, String> = emptyMap(),
    val isSaving: Boolean = false,
    val savedPhoneId: Long? = null,
    val submitError: String? = null
)

@HiltViewModel
class PurchaseEntryViewModel @Inject constructor(
    private val purchaseRepository: PurchaseRepository,
    private val phoneRepository: PhoneRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PurchaseFormState())
    val state: StateFlow<PurchaseFormState> = _state.asStateFlow()

    fun setActiveStore(storeId: Long) { _state.value = _state.value.copy(storeId = storeId) }

    fun onBrandChange(brand: String) {
        _state.value = _state.value.copy(brand = brand, model = "")
        viewModelScope.launch {
            val suggestions = if (brand.isNotBlank()) phoneRepository.getDistinctModelsForBrand(_state.value.storeId, brand) else emptyList()
            _state.value = _state.value.copy(modelSuggestions = suggestions)
        }
    }

    fun onModelChange(model: String) { _state.value = _state.value.copy(model = model) }
    fun onImei1Change(v: String) { _state.value = _state.value.copy(imei1 = v.filter { it.isDigit() }.take(15)) }
    fun onImei2Change(v: String) { _state.value = _state.value.copy(imei2 = v.filter { it.isDigit() }.take(15)) }
    fun onStorageChange(v: String) { _state.value = _state.value.copy(storage = v) }
    fun onRamChange(v: String) { _state.value = _state.value.copy(ram = v) }
    fun onColorChange(v: String) { _state.value = _state.value.copy(color = v) }
    fun onBatteryHealthChange(v: String) { _state.value = _state.value.copy(batteryHealth = v) }
    fun onConditionChange(v: String) { _state.value = _state.value.copy(condition = v) }
    fun onPtaStatusChange(v: String) { _state.value = _state.value.copy(ptaStatus = v) }
    fun onDateChange(millis: Long) { _state.value = _state.value.copy(purchaseDate = millis) }
    fun onPriceChange(v: String) { _state.value = _state.value.copy(purchasePrice = v.filter { it.isDigit() || it == '.' }) }
    fun onSupplierNameChange(v: String) { _state.value = _state.value.copy(supplierName = v) }
    fun onSupplierPhoneChange(v: String) { _state.value = _state.value.copy(supplierPhone = v) }
    fun onNotesChange(v: String) { _state.value = _state.value.copy(notes = v) }

    fun toggleAccessory(item: String) {
        val current = _state.value.accessories
        _state.value = _state.value.copy(accessories = if (item in current) current - item else current + item)
    }

    private fun validate(s: PurchaseFormState): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        if (s.brand.isBlank()) errors["brand"] = "Brand is required"
        if (s.model.isBlank()) errors["model"] = "Model is required"
        if (s.imei1.length != 15) errors["imei1"] = "IMEI 1 must be 15 digits"
        if (s.imei2.isNotBlank() && s.imei2.length != 15) errors["imei2"] = "IMEI 2 must be 15 digits"
        if (s.storage.isBlank()) errors["storage"] = "Storage is required"
        if (s.ram.isBlank()) errors["ram"] = "RAM is required"
        if (s.color.isBlank()) errors["color"] = "Color is required"
        if (s.condition.isBlank()) errors["condition"] = "Condition is required"
        if (s.ptaStatus.isBlank()) errors["ptaStatus"] = "PTA status is required"
        val price = s.purchasePrice.toDoubleOrNull()
        if (price == null || price <= 0) errors["purchasePrice"] = "Enter a valid purchase price"
        if (s.supplierName.isBlank()) errors["supplierName"] = "Supplier name is required"
        if (s.purchaseDate > System.currentTimeMillis() + 86_400_000L) errors["purchaseDate"] = "Purchase date can't be in the future"
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
                val phoneId = purchaseRepository.recordPurchase(
                    NewPurchaseInput(
                        storeId = s.storeId,
                        brand = s.brand.trim(),
                        model = s.model.trim(),
                        imei1 = s.imei1,
                        imei2 = s.imei2.ifBlank { null },
                        storage = s.storage,
                        ram = s.ram,
                        color = s.color.trim(),
                        batteryHealth = s.batteryHealth.ifBlank { "Not specified" },
                        condition = s.condition,
                        ptaStatus = s.ptaStatus,
                        accessoriesIncluded = s.accessories.joinToString(", ").ifBlank { null },
                        purchasePrice = s.purchasePrice.toDouble(),
                        purchaseDate = s.purchaseDate,
                        supplierName = s.supplierName.trim(),
                        supplierPhone = s.supplierPhone.ifBlank { null },
                        notes = s.notes.ifBlank { null }
                    )
                )
                _state.value = PurchaseFormState(storeId = s.storeId, savedPhoneId = phoneId)
            } catch (e: android.database.sqlite.SQLiteConstraintException) {
                _state.value = s.copy(isSaving = false, submitError = "A phone with this IMEI already exists in inventory.")
            } catch (e: Exception) {
                _state.value = s.copy(isSaving = false, submitError = e.message ?: "Could not save purchase. Please try again.")
            }
        }
    }

    fun consumeSavedEvent() { _state.value = _state.value.copy(savedPhoneId = null) }
}
