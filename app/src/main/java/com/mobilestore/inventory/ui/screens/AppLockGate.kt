package com.mobilestore.inventory.ui.screens

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * App Lock (PIN/Fingerprint) per the spec. Uses BiometricPrompt with
 * DEVICE_CREDENTIAL allowed as a fallback, so it accepts the phone's PIN /
 * pattern / password as well as fingerprint or face, without this app
 * needing to store or manage a PIN of its own.
 *
 * Shown once per cold app start when App Lock is enabled in Settings.
 */
@Composable
fun AppLockGate(onUnlocked: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun promptAuth() {
        val act = activity ?: return
        val biometricManager = BiometricManager.from(act)
        val allowed = BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        val canAuth = biometricManager.canAuthenticate(allowed)
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            // No fingerprint/PIN set up on this device — don't hard-lock the user out of their own shop data.
            errorMessage = "No screen lock is set up on this device. Disable App Lock in Settings, or set a device PIN first."
            return
        }

        val executor = ContextCompat.getMainExecutor(act)
        val prompt = BiometricPrompt(
            act, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onUnlocked()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    errorMessage = errString.toString()
                }
                override fun onAuthenticationFailed() {
                    errorMessage = "Not recognized. Try again."
                }
            }
        )
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Mobile Store Inventory")
            .setSubtitle("Verify it's you to continue")
            .setAllowedAuthenticators(allowed)
            .build()
        prompt.authenticate(promptInfo)
    }

    LaunchedEffect(Unit) { promptAuth() }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text("App Locked", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        errorMessage?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(16.dp))
        }
        Button(onClick = { errorMessage = null; promptAuth() }) { Text("Unlock") }
    }
}
