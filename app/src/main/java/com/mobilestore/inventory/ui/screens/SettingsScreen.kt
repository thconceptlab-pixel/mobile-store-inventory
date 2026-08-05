package com.mobilestore.inventory.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobilestore.inventory.data.local.entity.StoreEntity
import com.mobilestore.inventory.data.session.ThemeMode
import com.mobilestore.inventory.ui.components.SectionHeader
import com.mobilestore.inventory.ui.viewmodel.BackupUiEvent
import com.mobilestore.inventory.ui.viewmodel.SettingsViewModel
import com.mobilestore.inventory.ui.viewmodel.StoreViewModel

private val currencyOptions = listOf("PKR", "USD", "AED", "SAR", "GBP", "EUR", "INR", "CAD", "AUD")

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    activeStore: StoreEntity,
    storeViewModel: StoreViewModel,
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val prefs by settingsViewModel.preferences.collectAsState()
    val allStores by settingsViewModel.allStores.collectAsState()

    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    var showAddStoreSheet by remember { mutableStateOf(false) }
    var showRestartPrompt by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            settingsViewModel.importBackup(uri) { event ->
                when (event) {
                    is BackupUiEvent.ImportSuccess -> showRestartPrompt = true
                    is BackupUiEvent.Error -> snackbarMessage = event.message
                    else -> {}
                }
            }
        }
    }

    Scaffold { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("Settings", style = MaterialTheme.typography.headlineMedium)
            snackbarMessage?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(16.dp))

            SectionHeader("Appearance")
            SettingsCard {
                ThemeMode.entries.forEach { mode ->
                    RadioRow(
                        label = mode.name.lowercase().replaceFirstChar { it.uppercase() },
                        selected = prefs.themeMode == mode,
                        onClick = { settingsViewModel.setThemeMode(mode) }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionHeader("Store Profile")
            SettingsCard {
                Text(activeStore.shopName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(activeStore.ownerName, style = MaterialTheme.typography.bodyMedium)
                Text(activeStore.phoneNumber, style = MaterialTheme.typography.bodyMedium)
                Text(activeStore.address, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                if (allStores.size > 1) {
                    Text("${allStores.size} stores total", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(4.dp))
                }
                OutlinedButton(onClick = { showAddStoreSheet = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add Another Store")
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionHeader("Currency")
            SettingsCard {
                androidx.compose.foundation.layout.FlowRow {
                    currencyOptions.forEach { code ->
                        FilterChip(
                            selected = activeStore.currencyCode == code,
                            onClick = { settingsViewModel.updateCurrency(activeStore, code) },
                            label = { Text(code) },
                            modifier = Modifier.padding(end = 6.dp, bottom = 6.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionHeader("Backup & Restore")
            SettingsCard {
                Text(
                    "Local only — nothing is uploaded anywhere.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        settingsViewModel.exportBackup { event ->
                            when (event) {
                                is BackupUiEvent.ExportSuccess -> {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/octet-stream"
                                        putExtra(Intent.EXTRA_STREAM, event.uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Save backup to..."))
                                }
                                is BackupUiEvent.Error -> snackbarMessage = event.message
                                else -> {}
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Export Backup")
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("*/*")) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Restore from Backup")
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionHeader("Demo Data")
            SettingsCard {
                Text(
                    "Load a few sample purchases and sales to try out the Dashboard, Inventory, History, and Reports screens. All sample records are clearly labeled and safe to reverse or ignore.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(10.dp))
                var isLoadingSample by remember { mutableStateOf(false) }
                OutlinedButton(
                    onClick = {
                        isLoadingSample = true
                        settingsViewModel.loadSampleData(activeStore.storeId) { success, message ->
                            isLoadingSample = false
                            snackbarMessage = if (success) "Sample data loaded." else message
                        }
                    },
                    enabled = !isLoadingSample,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLoadingSample) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.Storage, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Load Sample Data")
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionHeader("Security")
            SettingsCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("App Lock", style = MaterialTheme.typography.titleMedium)
                        Text("Require your device PIN or fingerprint to open the app", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    Switch(checked = prefs.appLockEnabled, onCheckedChange = settingsViewModel::setAppLockEnabled)
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    if (showAddStoreSheet) {
        AddStoreSheet(
            storeViewModel = storeViewModel,
            onDismiss = { showAddStoreSheet = false }
        )
    }

    if (showRestartPrompt) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Backup Restored") },
            text = { Text("The app needs to restart to load the restored data.") },
            confirmButton = { TextButton(onClick = { settingsViewModel.restartApp() }) { Text("Restart Now") } }
        )
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun RadioRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddStoreSheet(storeViewModel: StoreViewModel, onDismiss: () -> Unit) {
    var shopName by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    val canSave = shopName.isNotBlank() && ownerName.isNotBlank() && phoneNumber.isNotBlank() && address.isNotBlank()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(20.dp)) {
            Text("Add New Store", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(shopName, { shopName = it }, label = { Text("Shop Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(ownerName, { ownerName = it }, label = { Text("Owner Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(phoneNumber, { phoneNumber = it }, label = { Text("Phone Number") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(address, { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    isSaving = true
                    storeViewModel.createStore(shopName.trim(), ownerName.trim(), phoneNumber.trim(), address.trim()) { onDismiss() }
                },
                enabled = canSave && !isSaving,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Create Store & Switch to It") }
            Spacer(Modifier.height(12.dp))
        }
    }
}
