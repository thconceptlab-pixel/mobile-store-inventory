package com.mobilestore.inventory.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilestore.inventory.data.backup.BackupManager
import com.mobilestore.inventory.data.local.entity.StoreEntity
import com.mobilestore.inventory.data.repository.StoreRepository
import com.mobilestore.inventory.data.seed.SeedDataManager
import com.mobilestore.inventory.data.session.AppPreferences
import com.mobilestore.inventory.data.session.AppPreferencesManager
import com.mobilestore.inventory.data.session.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class BackupUiEvent {
    data class ExportSuccess(val uri: Uri) : BackupUiEvent()
    data class ImportSuccess(val message: String) : BackupUiEvent()
    data class Error(val message: String) : BackupUiEvent()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: AppPreferencesManager,
    private val storeRepository: StoreRepository,
    private val backupManager: BackupManager,
    private val seedDataManager: SeedDataManager
) : ViewModel() {

    val preferences: StateFlow<AppPreferences> = preferencesManager.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppPreferences())

    val allStores: StateFlow<List<StoreEntity>> = storeRepository.getAllStores()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferencesManager.setThemeMode(mode) }
    }

    fun setAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.setAppLockEnabled(enabled) }
    }

    fun updateStoreProfile(store: StoreEntity) {
        viewModelScope.launch { storeRepository.updateStoreProfile(store) }
    }

    fun updateCurrency(store: StoreEntity, currencyCode: String) {
        viewModelScope.launch { storeRepository.updateStoreProfile(store.copy(currencyCode = currencyCode)) }
    }

    fun exportBackup(onResult: (BackupUiEvent) -> Unit) {
        viewModelScope.launch {
            try {
                val uri = backupManager.exportBackup()
                onResult(BackupUiEvent.ExportSuccess(uri))
            } catch (e: Exception) {
                onResult(BackupUiEvent.Error(e.message ?: "Export failed"))
            }
        }
    }

    fun importBackup(uri: Uri, onResult: (BackupUiEvent) -> Unit) {
        viewModelScope.launch {
            val success = backupManager.importBackup(uri)
            if (success) onResult(BackupUiEvent.ImportSuccess("Backup restored. Restart the app to see your data."))
            else onResult(BackupUiEvent.Error("Could not read that backup file."))
        }
    }

    fun restartApp() = backupManager.restartApp()

    fun loadSampleData(storeId: Long, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                seedDataManager.seedSampleData(storeId)
                onDone(true, null)
            } catch (e: Exception) {
                onDone(false, e.message ?: "Could not load sample data")
            }
        }
    }
}
