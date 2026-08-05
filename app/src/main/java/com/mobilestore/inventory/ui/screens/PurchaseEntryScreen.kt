package com.mobilestore.inventory.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobilestore.inventory.data.local.entity.StoreEntity
import com.mobilestore.inventory.data.local.staticdata.PhoneCatalog
import com.mobilestore.inventory.ui.components.DatePickerField
import com.mobilestore.inventory.ui.components.DropdownTextField
import com.mobilestore.inventory.ui.components.SectionHeader
import com.mobilestore.inventory.ui.viewmodel.PurchaseEntryViewModel

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun PurchaseEntryScreen(
    activeStore: StoreEntity,
    viewModel: PurchaseEntryViewModel = hiltViewModel(),
    scannedImei: String? = null,
    onScanConsumed: () -> Unit = {},
    onScanRequested: () -> Unit = {},
    onSaved: (Long) -> Unit
) {
    LaunchedEffect(activeStore.storeId) { viewModel.setActiveStore(activeStore.storeId) }
    val s by viewModel.state.collectAsState()

    LaunchedEffect(s.savedPhoneId) {
        s.savedPhoneId?.let {
            onSaved(it)
            viewModel.consumeSavedEvent()
        }
    }

    LaunchedEffect(scannedImei) {
        if (!scannedImei.isNullOrBlank()) {
            viewModel.onImei1Change(scannedImei)
            onScanConsumed()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("New Purchase", style = MaterialTheme.typography.headlineMedium)
        Text("Enter the details exactly as verified — price, IMEI, and date lock once saved.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Spacer(Modifier.height(16.dp))

        s.submitError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 8.dp))
        }

        DatePickerField("Purchase Date", s.purchaseDate, viewModel::onDateChange)
        Spacer(Modifier.height(12.dp))

        SectionHeader("Device Details")
        DropdownTextField("Brand", s.brand, PhoneCatalog.brands, viewModel::onBrandChange, isError = "brand" in s.errors, supportingText = s.errors["brand"])
        Spacer(Modifier.height(12.dp))
        DropdownTextField("Model", s.model, s.modelSuggestions, viewModel::onModelChange, isError = "model" in s.errors, supportingText = s.errors["model"] ?: "Type to enter a new model or pick a previous one")
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = s.imei1, onValueChange = viewModel::onImei1Change,
            label = { Text("IMEI 1") }, isError = "imei1" in s.errors,
            supportingText = { Text(s.errors["imei1"] ?: "15 digits — or scan the barcode sticker") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            trailingIcon = {
                IconButton(onClick = onScanRequested) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = "Scan barcode")
                }
            },
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = s.imei2, onValueChange = viewModel::onImei2Change,
            label = { Text("IMEI 2 (Optional)") }, isError = "imei2" in s.errors,
            supportingText = { Text(s.errors["imei2"] ?: "Dual-SIM phones only") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DropdownTextField("Storage", s.storage, PhoneCatalog.storageOptions, viewModel::onStorageChange, modifier = Modifier.weight(1f), isError = "storage" in s.errors)
            DropdownTextField("RAM", s.ram, PhoneCatalog.ramOptions, viewModel::onRamChange, modifier = Modifier.weight(1f), isError = "ram" in s.errors)
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = s.color, onValueChange = viewModel::onColorChange,
            label = { Text("Color") }, isError = "color" in s.errors,
            supportingText = s.errors["color"]?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        Spacer(Modifier.height(12.dp))
        DropdownTextField("Battery Health", s.batteryHealth, PhoneCatalog.batteryHealthOptions, viewModel::onBatteryHealthChange)
        Spacer(Modifier.height(12.dp))
        DropdownTextField("Condition", s.condition, PhoneCatalog.conditionOptions, viewModel::onConditionChange, isError = "condition" in s.errors, supportingText = s.errors["condition"])
        Spacer(Modifier.height(12.dp))
        DropdownTextField("PTA Status", s.ptaStatus, PhoneCatalog.ptaStatusOptions, viewModel::onPtaStatusChange, isError = "ptaStatus" in s.errors, supportingText = s.errors["ptaStatus"])
        Spacer(Modifier.height(12.dp))

        Text("Accessories Included", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        androidx.compose.foundation.layout.FlowRow {
            PhoneCatalog.accessoryOptions.forEach { acc ->
                FilterChip(
                    selected = acc in s.accessories,
                    onClick = { viewModel.toggleAccessory(acc) },
                    label = { Text(acc) },
                    modifier = Modifier.padding(end = 6.dp, bottom = 6.dp)
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        SectionHeader("Commercial Details")
        OutlinedTextField(
            value = s.purchasePrice, onValueChange = viewModel::onPriceChange,
            label = { Text("Purchase Price") }, isError = "purchasePrice" in s.errors,
            supportingText = { Text(s.errors["purchasePrice"] ?: "Locked once saved") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = s.supplierName, onValueChange = viewModel::onSupplierNameChange,
            label = { Text("Supplier Name") }, isError = "supplierName" in s.errors,
            supportingText = s.errors["supplierName"]?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = s.supplierPhone, onValueChange = viewModel::onSupplierPhoneChange,
            label = { Text("Supplier Phone") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = s.notes, onValueChange = viewModel::onNotesChange,
            label = { Text("Notes") }, modifier = Modifier.fillMaxWidth(), minLines = 2
        )

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = viewModel::submit,
            enabled = !s.isSaving,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            if (s.isSaving) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            else Text("Save Purchase & Add to Inventory")
        }
        Spacer(Modifier.height(32.dp))
    }
}
