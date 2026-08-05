package com.mobilestore.inventory.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobilestore.inventory.data.local.entity.StoreEntity
import com.mobilestore.inventory.ui.components.EmptyState
import com.mobilestore.inventory.ui.theme.SlateGray
import com.mobilestore.inventory.ui.viewmodel.SalesListViewModel

/**
 * "Sales" bottom-nav tab: select a phone from inventory to sell, per the
 * spec's Sale Entry requirement. Tapping a row opens SaleEntryScreen with
 * that phone pre-loaded.
 */
@Composable
fun SalesListScreen(
    activeStore: StoreEntity,
    viewModel: SalesListViewModel = hiltViewModel(),
    onPhoneSelected: (Long) -> Unit
) {
    LaunchedEffect(activeStore.storeId) { viewModel.setActiveStore(activeStore.storeId) }
    val phones by viewModel.sellablePhones.collectAsState()
    val query by viewModel.searchText.collectAsState()

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(16.dp)) {
            Text("Sell a Phone", style = MaterialTheme.typography.headlineMedium)
            Text("Choose an in-stock item to start a sale.", style = MaterialTheme.typography.bodyMedium, color = SlateGray)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::updateSearch,
                placeholder = { Text("Search by brand, model, IMEI...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (phones.isEmpty()) {
            EmptyState("No in-stock phones available. Record a purchase first.")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(phones, key = { it.phoneId }) { phone ->
                    ElevatedCard(
                        shape = RoundedCornerShape(16.dp),
                        onClick = { onPhoneSelected(phone.phoneId) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.PhoneAndroid, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("${phone.brand} ${phone.model}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text("${phone.storage} • ${phone.color} • IMEI ${phone.imei1}", style = MaterialTheme.typography.bodyMedium, color = SlateGray)
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}
