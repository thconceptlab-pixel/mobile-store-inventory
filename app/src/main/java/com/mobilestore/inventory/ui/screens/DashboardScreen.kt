package com.mobilestore.inventory.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobilestore.inventory.data.local.entity.StoreEntity
import com.mobilestore.inventory.data.local.entity.TransactionEntity
import com.mobilestore.inventory.data.local.entity.TransactionType
import com.mobilestore.inventory.ui.components.EmptyState
import com.mobilestore.inventory.ui.components.SectionHeader
import com.mobilestore.inventory.ui.components.StatCard
import com.mobilestore.inventory.ui.theme.DangerRed
import com.mobilestore.inventory.ui.theme.SuccessGreen
import com.mobilestore.inventory.ui.viewmodel.DashboardViewModel
import com.mobilestore.inventory.ui.viewmodel.StoreViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    activeStore: StoreEntity,
    storeViewModel: StoreViewModel,
    dashboardViewModel: DashboardViewModel = hiltViewModel(),
    onSwitchStoreClick: () -> Unit
) {
    LaunchedEffect(activeStore.storeId) { dashboardViewModel.setActiveStore(activeStore.storeId) }
    val ui by dashboardViewModel.uiState.collectAsState()
    val currencyFormat = remember(activeStore.currencyCode) { currencyFormatter(activeStore.currencyCode) }

    Column(Modifier.fillMaxSize()) {
        DashboardTopBar(activeStore, onSwitchStoreClick)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(
                        title = "In Stock", value = ui.inStockCount.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Total Profit", value = currencyFormat.format(ui.totalProfit),
                        modifier = Modifier.weight(1f), accentColor = SuccessGreen
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(
                        title = "Purchase Value", value = currencyFormat.format(ui.totalPurchaseValue),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Total Sales", value = currencyFormat.format(ui.totalSalesValue),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item { SectionHeader("Today") }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(
                        title = "Today's Purchases", value = currencyFormat.format(ui.todaysPurchaseValue),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Today's Sales", value = currencyFormat.format(ui.todaysSalesValue),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(12.dp))
                StatCard(
                    title = "Today's Profit", value = currencyFormat.format(ui.todaysProfit),
                    modifier = Modifier.fillMaxWidth(), accentColor = SuccessGreen
                )
            }

            item { SectionHeader("Recent Transactions") }
            if (ui.recentTransactions.isEmpty()) {
                item { EmptyState("No transactions yet. Record a purchase to get started.") }
            } else {
                items(ui.recentTransactions, key = { it.transactionId }) { tx ->
                    RecentTransactionRow(tx, currencyFormat)
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun DashboardTopBar(store: StoreEntity, onSwitchStoreClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Dashboard", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(store.shopName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
        FilledTonalIconButton(onClick = onSwitchStoreClick) {
            Icon(Icons.Filled.SwapHoriz, contentDescription = "Switch store")
        }
    }
}

@Composable
private fun RecentTransactionRow(tx: TransactionEntity, currencyFormat: NumberFormat) {
    val (icon, tint) = when (tx.type) {
        TransactionType.PURCHASE -> Icons.Filled.ShoppingCart to MaterialTheme.colorScheme.primary
        TransactionType.SALE -> Icons.Filled.PointOfSale to SuccessGreen
        TransactionType.REVERSAL_PURCHASE, TransactionType.REVERSAL_SALE -> Icons.Filled.Undo to DangerRed
    }
    val dateFmt = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    ElevatedCard(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(tx.type.name.replace("_", " "), style = MaterialTheme.typography.titleMedium)
                Text(tx.referenceNumber, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
                Text(dateFmt.format(Date(tx.timestamp)), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
            }
            Text(
                (if (tx.amount >= 0) "+" else "") + currencyFormat.format(tx.amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (tx.amount >= 0) SuccessGreen else DangerRed
            )
        }
    }
}

fun currencyFormatter(currencyCode: String): NumberFormat {
    return try {
        NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
            currency = java.util.Currency.getInstance(currencyCode)
        }
    } catch (e: Exception) {
        NumberFormat.getNumberInstance(Locale.getDefault())
    }
}
