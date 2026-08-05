package com.mobilestore.inventory.data.session

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.appPrefsDataStore by preferencesDataStore(name = "app_prefs")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val appLockEnabled: Boolean = false
)

/**
 * Device/session-level preferences that apply across all stores: theme and
 * App Lock. Currency is a per-store setting (see StoreEntity.currencyCode)
 * since different stores could reasonably trade in different currencies.
 */
@Singleton
class AppPreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val THEME_MODE = stringPreferencesKey("theme_mode")
    private val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")

    val preferences: Flow<AppPreferences> = context.appPrefsDataStore.data.map { prefs ->
        AppPreferences(
            themeMode = prefs[THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
            appLockEnabled = prefs[APP_LOCK_ENABLED] ?: false
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.appPrefsDataStore.edit { it[THEME_MODE] = mode.name }
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.appPrefsDataStore.edit { it[APP_LOCK_ENABLED] = enabled }
    }
}
