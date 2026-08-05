package com.mobilestore.inventory.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobilestore.inventory.ui.viewmodel.StoreViewModel

/**
 * First-run screen: create the first shop profile. Shown by NavGraph
 * whenever there's no active store yet. Additional stores ("Store B",
 * "Store C"...) are created the same way from Settings in Phase 3, which
 * will reuse this same form.
 */
@Composable
fun StoreSetupScreen(
    storeViewModel: StoreViewModel = hiltViewModel(),
    onStoreCreated: () -> Unit
) {
    var shopName by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    val canSave = shopName.isNotBlank() && ownerName.isNotBlank() && phoneNumber.isNotBlank() && address.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))
        Icon(Icons.Filled.Storefront, contentDescription = null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text("Set Up Your Shop", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            "This creates your first store profile. You can add more stores later from Settings.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
        )
        Spacer(Modifier.height(28.dp))

        OutlinedTextField(
            value = shopName, onValueChange = { shopName = it },
            label = { Text("Shop Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = ownerName, onValueChange = { ownerName = it },
            label = { Text("Owner Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = phoneNumber, onValueChange = { phoneNumber = it },
            label = { Text("Phone Number") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = address, onValueChange = { address = it },
            label = { Text("Address") }, modifier = Modifier.fillMaxWidth(), minLines = 2
        )
        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                isSaving = true
                storeViewModel.createStore(shopName.trim(), ownerName.trim(), phoneNumber.trim(), address.trim()) {
                    onStoreCreated()
                }
            },
            enabled = canSave && !isSaving,
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            if (isSaving) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            else Text("Create Shop Profile")
        }
        Spacer(Modifier.height(24.dp))
    }
}
