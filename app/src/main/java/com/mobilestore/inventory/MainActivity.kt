package com.mobilestore.inventory

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobilestore.inventory.data.session.ThemeMode
import com.mobilestore.inventory.ui.navigation.MobileStoreNavHost
import com.mobilestore.inventory.ui.screens.AppLockGate
import com.mobilestore.inventory.ui.theme.MobileStoreInventoryTheme
import com.mobilestore.inventory.ui.viewmodel.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single-Activity host for the whole Compose Navigation graph. No login, no
 * network calls anywhere in this class or below it — the app is fully
 * functional the moment it's installed.
 *
 * Extends FragmentActivity (not plain ComponentActivity) because
 * BiometricPrompt — used by App Lock — requires a FragmentActivity host.
 */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val prefs by themeViewModel.preferences.collectAsState()
            var unlocked by remember(prefs.appLockEnabled) { mutableStateOf(!prefs.appLockEnabled) }

            val darkTheme = when (prefs.themeMode) {
                ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            MobileStoreInventoryTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (unlocked) {
                        MobileStoreNavHost()
                    } else {
                        AppLockGate(onUnlocked = { unlocked = true })
                    }
                }
            }
        }
    }
}
