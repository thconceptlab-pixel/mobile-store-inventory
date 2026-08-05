package com.mobilestore.inventory.data.session

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.sessionDataStore by preferencesDataStore(name = "session_prefs")
private val ACTIVE_STORE_ID = longPreferencesKey("active_store_id")

/**
 * Persists which store is currently active across app restarts, so the user
 * doesn't have to re-select their shop every launch. This is a device/session
 * concern, not shop data, so it lives in DataStore rather than Room (see
 * DatabaseModule notes from Phase 1).
 */
@Singleton
class StoreSessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val activeStoreId: Flow<Long?> = context.sessionDataStore.data.map { prefs ->
        prefs[ACTIVE_STORE_ID]?.takeIf { it != 0L }
    }

    suspend fun setActiveStore(storeId: Long) {
        context.sessionDataStore.edit { it[ACTIVE_STORE_ID] = storeId }
    }
}
