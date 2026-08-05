package com.mobilestore.inventory.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobilestore.inventory.data.local.entity.PaymentMethod
import com.mobilestore.inventory.data.local.entity.StoreEntity
import com.mobilestore.inventory.ui.components.DatePickerField
import com.mobilestore.inventory.ui.components.SectionHeader
import com.mobilestore.inventory.ui.theme.SuccessGreen
import com.mobilestore.inventory.ui.theme.DangerRed
import com.mobilestore.inventory.ui.viewmodel.SaleEntryViewModel

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun SaleEntryScreen(
    activeStore: StoreEntity,
    phoneId: Long,
    viewModel: SaleEntryViewModel = hiltViewModel(),
    onSaved: () -> Unit,
    onCancel: () -> Unit
) {
    LaunchedEffect(phoneId) { viewModel.loadPhone(activeStore.storeId, phoneId) }
    val s by viewModel.state.collectAsState()
    val currencyFormat = remember(activeStore.currencyCode) { currencyFormatter(activeStore.currencyCode) }

    LaunchedEffect(s.saved) { if (s.saved) onSaved() }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Sell Phone", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))

        s.phone?.let { phone ->
            Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text("${phone.brand} ${phone.model}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("${phone.storage} • ${phone.color} • IMEI ${phone.imei1}", style = MaterialTheme.typography.bodyMedium)
                    s.purchasePrice?.let {
                        Spacer(Modifier.height(6.dp))
                        Text("Purchase cost: ${currencyFormat.format(it)}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        } ?: run {
            Text("Loading phone details...", style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(Modifier.height(16.dp))
        s.submitError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
        }

        DatePickerField("Sale Date", s.saleDate, viewModel::onDateChange)
        Spacer(Modifier.height(12.dp))

        SectionHeader("Customer")
        OutlinedTextField(
            value = s.customerName, onValueChange = viewModel::onCustomerNameChange,
            label = { Text("Customer Name") }, isError = "customerName" in s.errors,
            supportingText = s.errors["customerName"]?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = s.customerPhone, onValueChange = viewModel::onCustomerPhoneChange,
            label = { Text("Customer Phone") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )

        Spacer(Modifier.height(16.dp))
        SectionHeader("Sale Details")
        OutlinedTextField(
            value = s.sellingPrice, onValueChange = viewModel::onSellingPriceChange,
            label = { Text("Selling Price") }, isError = "sellingPrice" in s.errors,
            supportingText = { Text(s.errors["sellingPrice"] ?: "Locked once saved") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )

        androidx.compose.animation.AnimatedVisibility(visible = s.projectedProfit != null) {
            s.projectedProfit?.let { profit ->
                Column {
                    Spacer(Modifier.height(10.dp))
                    val positive = profit >= 0
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background((if (positive) SuccessGreen else DangerRed).copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Projected Profit", style = MaterialTheme.typography.titleMedium)
                        Text(
                            currencyFormat.format(profit),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (positive) SuccessGreen else DangerRed
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text("Payment Method", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        androidx.compose.foundation.layout.FlowRow {
            PaymentMethod.entries.forEach { method ->
                FilterChip(
                    selected = s.paymentMethod == method,
                    onClick = { viewModel.onPaymentMethodChange(method) },
                    label = { Text(method.name.replace("_", " ")) },
                    modifier = Modifier.padding(end = 6.dp, bottom = 6.dp)
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = s.notes, onValueChange = viewModel::onNotesChange,
            label = { Text("Notes") }, modifier = Modifier.fillMaxWidth(), minLines = 2
        )

        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f).height(52.dp)) { Text("Cancel") }
            Button(
                onClick = viewModel::submit,
                enabled = !s.isSaving && s.phone != null,
                modifier = Modifier.weight(1f).height(52.dp)
            ) {
                if (s.isSaving) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text("Complete Sale")
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}
