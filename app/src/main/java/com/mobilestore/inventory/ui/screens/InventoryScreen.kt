package com.mobilestore.inventory.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobilestore.inventory.data.local.entity.PhoneStatus
import com.mobilestore.inventory.data.local.entity.StoreEntity
import com.mobilestore.inventory.data.local.relation.PhoneWithPurchase
import com.mobilestore.inventory.ui.components.EmptyState
import com.mobilestore.inventory.ui.theme.SuccessGreen
import com.mobilestore.inventory.ui.theme.SlateGray
import com.mobilestore.inventory.ui.viewmodel.InventoryViewModel
import com.mobilestore.inventory.ui.viewmodel.StatusFilter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun InventoryScreen(
    activeStore: StoreEntity,
    viewModel: InventoryViewModel = hiltViewModel(),
    onPhoneClick: (Long) -> Unit
) {
    LaunchedEffect(activeStore.storeId) { viewModel.setActiveStore(activeStore.storeId) }

    val phones by viewModel.filteredPhones.collectAsState()
    val brands by viewModel.availableBrands.collectAsState()
    val filters by viewModel.currentFilters.collectAsState()
    val currencyFormat = remember(activeStore.currencyCode) { currencyFormatter(activeStore.currencyCode) }
    var showFilterSheet by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = filters.searchQuery,
                onValueChange = viewModel::updateSearch,
                placeholder = { Text("Search brand, model, IMEI...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            FilledTonalIconButton(onClick = { showFilterSheet = true }) {
                Icon(Icons.Filled.FilterList, contentDescription = "Filters")
            }
        }

        StatusFilterTabs(filters.statusFilter, viewModel::updateStatusFilter)

        if (phones.isEmpty()) {
            EmptyState("No phones match your filters yet.")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(phones, key = { it.phone.phoneId }) { item ->
                    InventoryRow(item, currencyFormat, onClick = { onPhoneClick(item.phone.phoneId) })
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }

    if (showFilterSheet) {
        InventoryFilterSheet(
            brands = brands,
            filters = filters,
            onBrandChange = viewModel::updateBrandFilter,
            onPriceRangeChange = viewModel::updatePriceRange,
            onClear = viewModel::clearFilters,
            onDismiss = { showFilterSheet = false }
        )
    }
}

@Composable
private fun StatusFilterTabs(selected: StatusFilter, onSelect: (StatusFilter) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            StatusFilter.ALL to "All",
            StatusFilter.IN_STOCK to "In Stock",
            StatusFilter.SOLD to "Sold"
        ).forEach { (status, label) ->
            FilterChip(
                selected = selected == status,
                onClick = { onSelect(status) },
                label = { Text(label) }
            )
        }
    }
}

@Composable
private fun InventoryRow(item: PhoneWithPurchase, currencyFormat: java.text.NumberFormat, onClick: () -> Unit) {
    val phone = item.phone
    val dateFmt = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    ElevatedCard(shape = RoundedCornerShape(16.dp), onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.PhoneAndroid, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("${phone.brand} ${phone.model}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("${phone.storage} • ${phone.color}", style = MaterialTheme.typography.bodyMedium, color = SlateGray)
                Text("Purchased ${dateFmt.format(Date(item.purchaseDate))}", style = MaterialTheme.typography.labelMedium, color = SlateGray)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(currencyFormat.format(item.purchasePrice), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                StatusPill(phone.status)
            }
        }
    }
}

@Composable
private fun StatusPill(status: PhoneStatus) {
    val (bg, fg, label) = when (status) {
        PhoneStatus.IN_STOCK -> Triple(SuccessGreen.copy(alpha = 0.15f), SuccessGreen, "In Stock")
        PhoneStatus.SOLD -> Triple(SlateGray.copy(alpha = 0.15f), SlateGray, "Sold")
    }
    Box(
        modifier = Modifier
            .padding(top = 4.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = fg)
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun InventoryFilterSheet(
    brands: List<String>,
    filters: com.mobilestore.inventory.ui.viewmodel.InventoryFilterState,
    onBrandChange: (String?) -> Unit,
    onPriceRangeChange: (Double?, Double?) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    var minPrice by remember(filters.minPrice) { mutableStateOf(filters.minPrice?.toString() ?: "") }
    var maxPrice by remember(filters.maxPrice) { mutableStateOf(filters.maxPrice?.toString() ?: "") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(20.dp)) {
            Text("Filter Inventory", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))

            Text("Brand", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            androidx.compose.foundation.layout.FlowRow {
                FilterChip(selected = filters.brandFilter == null, onClick = { onBrandChange(null) }, label = { Text("All") }, modifier = Modifier.padding(end = 6.dp, bottom = 6.dp))
                brands.forEach { brand ->
                    FilterChip(
                        selected = filters.brandFilter == brand,
                        onClick = { onBrandChange(brand) },
                        label = { Text(brand) },
                        modifier = Modifier.padding(end = 6.dp, bottom = 6.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Purchase Price Range", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = minPrice, onValueChange = { minPrice = it.filter { c -> c.isDigit() } },
                    label = { Text("Min") }, modifier = Modifier.weight(1f), singleLine = true
                )
                OutlinedTextField(
                    value = maxPrice, onValueChange = { maxPrice = it.filter { c -> c.isDigit() } },
                    label = { Text("Max") }, modifier = Modifier.weight(1f), singleLine = true
                )
            }

            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { onClear(); onDismiss() }, modifier = Modifier.weight(1f)) { Text("Clear All") }
                Button(
                    onClick = {
                        onPriceRangeChange(minPrice.toDoubleOrNull(), maxPrice.toDoubleOrNull())
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Apply") }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
