package com.mobilestore.inventory.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilestore.inventory.data.session.AppPreferences
import com.mobilestore.inventory.data.session.AppPreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Small, focused ViewModel so MainActivity doesn't need to pull in every Settings dependency just to read theme/App Lock state. */
@HiltViewModel
class ThemeViewModel @Inject constructor(
    preferencesManager: AppPreferencesManager
) : ViewModel() {
    val preferences: StateFlow<AppPreferences> = preferencesManager.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppPreferences())
}
