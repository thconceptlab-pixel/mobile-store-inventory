package com.mobilestore.inventory.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobilestore.inventory.data.local.entity.StoreEntity
import com.mobilestore.inventory.ui.components.EmptyState
import com.mobilestore.inventory.ui.components.RankedBarList
import com.mobilestore.inventory.ui.components.SectionHeader
import com.mobilestore.inventory.ui.components.SimpleBarChart
import com.mobilestore.inventory.ui.components.StatCard
import com.mobilestore.inventory.ui.theme.SuccessGreen
import com.mobilestore.inventory.ui.viewmodel.ReportExportEvent
import com.mobilestore.inventory.ui.viewmodel.ReportPeriod
import com.mobilestore.inventory.ui.viewmodel.ReportsViewModel

@Composable
fun ReportsScreen(
    activeStore: StoreEntity,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    LaunchedEffect(activeStore.storeId) { viewModel.setActiveStore(activeStore.storeId) }
    val ui by viewModel.uiState.collectAsState()
    val period by viewModel.period.collectAsState()
    val currencyFormat = remember(activeStore.currencyCode) { currencyFormatter(activeStore.currencyCode) }
    var showCustomDialog by remember { mutableStateOf(false) }
    var exportError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    fun shareReport(uri: android.net.Uri, mimeType: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share report"))
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Text("Reports", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))
            PeriodSelector(
                selected = period,
                onSelect = { p -> if (p == ReportPeriod.CUSTOM) showCustomDialog = true else viewModel.setPeriod(p) }
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = {
                        viewModel.exportPdf(activeStore.shopName, currencyFormat) { event ->
                            when (event) {
                                is ReportExportEvent.Success -> shareReport(event.uri, "application/pdf")
                                is ReportExportEvent.Error -> exportError = event.message
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Export PDF")
                }
                OutlinedButton(
                    onClick = {
                        viewModel.exportCsv(activeStore.shopName, currencyFormat) { event ->
                            when (event) {
                                is ReportExportEvent.Success -> shareReport(event.uri, "text/csv")
                                is ReportExportEvent.Error -> exportError = event.message
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Export Excel")
                }
            }
            exportError?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(16.dp))
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Purchases", "${ui.purchaseCount} • ${currencyFormat.format(ui.purchaseValue)}", modifier = Modifier.weight(1f))
                StatCard("Sales", "${ui.saleCount} • ${currencyFormat.format(ui.saleValue)}", modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Profit", currencyFormat.format(ui.profit), modifier = Modifier.weight(1f), accentColor = SuccessGreen)
                StatCard("Current Stock", "${ui.currentStockCount} • ${currencyFormat.format(ui.currentStockValue)}", modifier = Modifier.weight(1f))
            }
        }

        item {
            SectionHeader("Daily Profit Trend")
            if (ui.dailyProfitTrend.isEmpty() || ui.dailyProfitTrend.all { it.second == 0.0 }) {
                EmptyState("No sales recorded in this period yet.")
            } else {
                Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Box(Modifier.padding(16.dp)) {
                        SimpleBarChart(data = ui.dailyProfitTrend)
                    }
                }
            }
        }

        item {
            SectionHeader("Top Selling Brands")
            if (ui.topBrands.isEmpty()) {
                EmptyState("No sales in this period yet.")
            } else {
                Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Box(Modifier.padding(16.dp)) {
                        RankedBarList(items = ui.topBrands.map { it.brand to it.unitsSold })
                    }
                }
            }
        }

        item {
            SectionHeader("Top Selling Models")
            if (ui.topModels.isEmpty()) {
                EmptyState("No sales in this period yet.")
            } else {
                Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Box(Modifier.padding(16.dp)) {
                        RankedBarList(items = ui.topModels.map { it.brand to it.unitsSold }, barColor = SuccessGreen)
                    }
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }

    if (showCustomDialog) {
        ReportsCustomRangeDialog(
            onConfirm = { start, end -> viewModel.setCustomRange(start, end); showCustomDialog = false },
            onDismiss = { showCustomDialog = false }
        )
    }
}

@Composable
private fun PeriodSelector(selected: ReportPeriod, onSelect: (ReportPeriod) -> Unit) {
    val options = listOf(
        ReportPeriod.DAILY to "Daily",
        ReportPeriod.WEEKLY to "Weekly",
        ReportPeriod.MONTHLY to "Monthly",
        ReportPeriod.YEARLY to "Yearly",
        ReportPeriod.CUSTOM to "Custom"
    )
    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(options) { (period, label) ->
            FilterChip(selected = selected == period, onClick = { onSelect(period) }, label = { Text(label) })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportsCustomRangeDialog(onConfirm: (Long, Long) -> Unit, onDismiss: () -> Unit) {
    var start by remember { mutableStateOf(System.currentTimeMillis() - 30 * 86_400_000L) }
    var end by remember { mutableStateOf(System.currentTimeMillis()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom Report Range") },
        text = {
            Column {
                com.mobilestore.inventory.ui.components.DatePickerField("Start Date", start, { start = it })
                Spacer(Modifier.height(12.dp))
                com.mobilestore.inventory.ui.components.DatePickerField("End Date", end, { end = it })
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(start, end) }) { Text("Apply") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
