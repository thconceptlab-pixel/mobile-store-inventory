package com.mobilestore.inventory.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobilestore.inventory.data.local.entity.StoreEntity
import com.mobilestore.inventory.data.local.entity.TransactionEntity
import com.mobilestore.inventory.data.local.entity.TransactionType
import com.mobilestore.inventory.ui.components.DatePickerField
import com.mobilestore.inventory.ui.components.EmptyState
import com.mobilestore.inventory.ui.theme.DangerRed
import com.mobilestore.inventory.ui.theme.SlateGray
import com.mobilestore.inventory.ui.theme.SuccessGreen
import com.mobilestore.inventory.ui.viewmodel.DateRangePreset
import com.mobilestore.inventory.ui.viewmodel.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Banking-style transaction statement: every purchase, sale, and reversal
 * in chronological order with a running balance, per the spec. Tapping an
 * eligible row (an original PURCHASE or SALE not already reversed) offers
 * "Reverse Transaction" — the only way to correct a mistake; see
 * PurchaseRepository/SaleRepository from Phase 1 for why the original row
 * itself is never edited.
 */
@Composable
fun HistoryScreen(
    activeStore: StoreEntity,
    viewModel: HistoryViewModel = hiltViewModel(),
    onOpenPhone: (Long) -> Unit
) {
    LaunchedEffect(activeStore.storeId) { viewModel.setActiveStore(activeStore.storeId) }
    val transactions by viewModel.transactions.collectAsState()
    val filters by viewModel.currentFilters.collectAsState()
    val reversedIds by viewModel.reversedIds.collectAsState()
    val currencyFormat = remember(activeStore.currencyCode) { currencyFormatter(activeStore.currencyCode) }

    LaunchedEffect(transactions) { viewModel.refreshReversalStatus(transactions) }

    var showCustomRangeDialog by remember { mutableStateOf(false) }
    var reversalTarget by remember { mutableStateOf<TransactionEntity?>(null) }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(16.dp)) {
            Text("Transaction History", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = filters.searchQuery,
                onValueChange = viewModel::updateSearch,
                placeholder = { Text("Search reference number...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            DateFilterRow(
                selected = filters.preset,
                onSelect = { preset ->
                    if (preset == DateRangePreset.CUSTOM) showCustomRangeDialog = true
                    else viewModel.updatePreset(preset)
                }
            )
        }

        if (transactions.isEmpty()) {
            EmptyState("No transactions in this range yet.")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(transactions, key = { it.transactionId }) { tx ->
                    val canReverse = (tx.type == TransactionType.PURCHASE || tx.type == TransactionType.SALE) &&
                        tx.transactionId !in reversedIds
                    StatementRow(
                        tx = tx,
                        currencyFormat = currencyFormat,
                        canReverse = canReverse,
                        onOpenPhone = { onOpenPhone(tx.relatedPhoneId) },
                        onReverseClick = { reversalTarget = tx }
                    )
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }

    if (showCustomRangeDialog) {
        CustomRangeDialog(
            onConfirm = { start, end -> viewModel.updateCustomRange(start, end); showCustomRangeDialog = false },
            onDismiss = { showCustomRangeDialog = false }
        )
    }

    reversalTarget?.let { tx ->
        ReversalDialog(
            tx = tx,
            onConfirm = { reason ->
                viewModel.reverseTransaction(tx, reason) { _, _ -> reversalTarget = null }
            },
            onDismiss = { reversalTarget = null }
        )
    }
}

@Composable
private fun DateFilterRow(selected: DateRangePreset, onSelect: (DateRangePreset) -> Unit) {
    val options = listOf(
        DateRangePreset.ALL to "All",
        DateRangePreset.TODAY to "Today",
        DateRangePreset.YESTERDAY to "Yesterday",
        DateRangePreset.THIS_WEEK to "This Week",
        DateRangePreset.THIS_MONTH to "This Month",
        DateRangePreset.LAST_YEAR to "Last 1 Year",
        DateRangePreset.CUSTOM to "Custom Range"
    )
    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(options) { (preset, label) ->
            FilterChip(selected = selected == preset, onClick = { onSelect(preset) }, label = { Text(label) })
        }
    }
}

@Composable
private fun StatementRow(
    tx: TransactionEntity,
    currencyFormat: java.text.NumberFormat,
    canReverse: Boolean,
    onOpenPhone: () -> Unit,
    onReverseClick: () -> Unit
) {
    val dateFmt = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val (label, color) = when (tx.type) {
        TransactionType.PURCHASE -> "Purchase" to MaterialTheme.colorScheme.primary
        TransactionType.SALE -> "Sale" to SuccessGreen
        TransactionType.REVERSAL_PURCHASE -> "Reversal (Purchase)" to DangerRed
        TransactionType.REVERSAL_SALE -> "Reversal (Sale)" to DangerRed
    }

    ElevatedCard(shape = RoundedCornerShape(14.dp), onClick = onOpenPhone, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = color)
                Text(
                    (if (tx.amount >= 0) "+" else "") + currencyFormat.format(tx.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (tx.amount >= 0) SuccessGreen else DangerRed
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(tx.referenceNumber, style = MaterialTheme.typography.labelMedium, color = SlateGray)
            Text(dateFmt.format(Date(tx.timestamp)), style = MaterialTheme.typography.labelMedium, color = SlateGray)
            tx.profit?.let {
                Text("Profit: ${currencyFormat.format(it)}", style = MaterialTheme.typography.labelMedium, color = SlateGray)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Balance: ${currencyFormat.format(tx.balance)}", style = MaterialTheme.typography.labelMedium, color = SlateGray)
                if (canReverse) {
                    TextButton(onClick = onReverseClick) {
                        Icon(Icons.Filled.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Reverse")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReversalDialog(tx: TransactionEntity, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reverse ${tx.referenceNumber}?") },
        text = {
            Column {
                Text("The original record stays exactly as it was. This creates a new reversal entry that cancels out its amount, so you keep a full audit trail.")
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = reason, onValueChange = { reason = it },
                    label = { Text("Reason for reversal") }, minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(reason.ifBlank { "No reason given" }) }) { Text("Confirm Reversal") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomRangeDialog(onConfirm: (Long, Long) -> Unit, onDismiss: () -> Unit) {
    var start by remember { mutableStateOf(System.currentTimeMillis() - 7 * 86_400_000L) }
    var end by remember { mutableStateOf(System.currentTimeMillis()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom Date Range") },
        text = {
            Column {
                DatePickerField("Start Date", start, { start = it })
                Spacer(Modifier.height(12.dp))
                DatePickerField("End Date", end, { end = it })
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(start, end) }) { Text("Apply") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
