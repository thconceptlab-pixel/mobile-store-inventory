package com.mobilestore.inventory.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobilestore.inventory.data.local.entity.PhoneStatus
import com.mobilestore.inventory.data.local.entity.StoreEntity
import com.mobilestore.inventory.ui.theme.SlateGray
import com.mobilestore.inventory.ui.theme.SuccessGreen
import com.mobilestore.inventory.ui.viewmodel.PhoneDetailViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneDetailScreen(
    activeStore: StoreEntity,
    phoneId: Long,
    viewModel: PhoneDetailViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onSellClick: (Long) -> Unit
) {
    LaunchedEffect(phoneId) { viewModel.load(phoneId) }
    val state by viewModel.state.collectAsState()
    val currencyFormat = remember(activeStore.currencyCode) { currencyFormatter(activeStore.currencyCode) }
    val dateFmt = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Phone Details") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        if (state.isLoading || state.phone == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        val phone = state.phone!!
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("${phone.brand} ${phone.model}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(if (phone.status == PhoneStatus.IN_STOCK) "In Stock" else "Sold",
                color = if (phone.status == PhoneStatus.IN_STOCK) SuccessGreen else SlateGray,
                style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))

            DetailCard(title = "Device") {
                DetailRow("IMEI 1", phone.imei1)
                phone.imei2?.let { DetailRow("IMEI 2", it) }
                DetailRow("Storage", phone.storage)
                DetailRow("RAM", phone.ram)
                DetailRow("Color", phone.color)
                DetailRow("Battery Health", phone.batteryHealth)
                DetailRow("Condition", phone.condition)
                DetailRow("PTA Status", phone.ptaStatus)
                phone.accessoriesIncluded?.let { DetailRow("Accessories", it) }
            }

            state.purchase?.let { purchase ->
                Spacer(Modifier.height(12.dp))
                DetailCard(title = "Purchase Record (Locked)") {
                    DetailRow("Purchase Date", dateFmt.format(Date(purchase.purchaseDate)))
                    DetailRow("Purchase Price", currencyFormat.format(purchase.purchasePrice))
                    DetailRow("Supplier", purchase.supplierName)
                    purchase.supplierPhone?.let { DetailRow("Supplier Phone", it) }
                }
            }

            state.sale?.let { sale ->
                Spacer(Modifier.height(12.dp))
                DetailCard(title = "Sale Record (Locked)") {
                    DetailRow("Sale Date", dateFmt.format(Date(sale.saleDate)))
                    DetailRow("Selling Price", currencyFormat.format(sale.sellingPrice))
                    DetailRow("Profit", currencyFormat.format(sale.profit))
                    DetailRow("Customer", sale.customerName)
                    DetailRow("Payment", sale.paymentMethod.name.replace("_", " "))
                }
            }

            Spacer(Modifier.height(24.dp))
            if (phone.status == PhoneStatus.IN_STOCK) {
                Button(onClick = { onSellClick(phone.phoneId) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text("Sell This Phone")
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun DetailCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = SlateGray, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
