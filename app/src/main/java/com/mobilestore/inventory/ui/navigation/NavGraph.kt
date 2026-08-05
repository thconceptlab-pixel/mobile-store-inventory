package com.mobilestore.inventory.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mobilestore.inventory.ui.screens.*
import com.mobilestore.inventory.ui.viewmodel.StoreViewModel

/**
 * Root of the app. Gates all content behind an active store: if none exists
 * yet, shows StoreSetupScreen; once a store is active, shows the full
 * bottom-nav experience. Every store-scoped screen below reads `activeStore`
 * so switching stores (Phase 3 Settings) instantly re-scopes all data.
 */
@Composable
fun MobileStoreNavHost() {
    val storeViewModel: StoreViewModel = hiltViewModel()
    val activeStore by storeViewModel.activeStore.collectAsState()
    val allStores by storeViewModel.allStores.collectAsState()

    when {
        allStores.isEmpty() -> StoreSetupScreen(storeViewModel = storeViewModel, onStoreCreated = {})
        activeStore == null -> Box(Modifier.fillMaxSize()) // brief moment while session resolves
        else -> MainAppScaffold(activeStore = activeStore!!, storeViewModel = storeViewModel)
    }
}

@Composable
private fun MainAppScaffold(
    activeStore: com.mobilestore.inventory.data.local.entity.StoreEntity,
    storeViewModel: StoreViewModel
) {
    val navController = rememberNavController()
    var showStoreSwitcher by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val allStores by storeViewModel.allStores.collectAsState()

    Scaffold(
        bottomBar = { AppBottomBar(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    activeStore = activeStore,
                    storeViewModel = storeViewModel,
                    onSwitchStoreClick = { showStoreSwitcher = true }
                )
            }
            composable(Screen.Inventory.route) {
                InventoryScreen(
                    activeStore = activeStore,
                    onPhoneClick = { phoneId -> navController.navigate(Routes.phoneDetail(phoneId)) }
                )
            }
            composable(Screen.Purchases.route) { backStackEntry ->
                val scannedImei by backStackEntry.savedStateHandle
                    .getStateFlow<String?>("scanned_imei", null)
                    .collectAsState()
                PurchaseEntryScreen(
                    activeStore = activeStore,
                    scannedImei = scannedImei,
                    onScanConsumed = { backStackEntry.savedStateHandle["scanned_imei"] = null },
                    onScanRequested = { navController.navigate(Routes.SCAN_IMEI) },
                    onSaved = { phoneId -> navController.navigate(Routes.phoneDetail(phoneId)) }
                )
            }
            composable(Routes.SCAN_IMEI) {
                BarcodeScannerScreen(
                    onBarcodeDetected = { code ->
                        navController.previousBackStackEntry?.savedStateHandle?.set("scanned_imei", code)
                        navController.popBackStack()
                    },
                    onCancel = { navController.popBackStack() }
                )
            }
            composable(Screen.Sales.route) {
                SalesListScreen(
                    activeStore = activeStore,
                    onPhoneSelected = { phoneId -> navController.navigate(Routes.saleEntry(phoneId)) }
                )
            }
            composable(Screen.History.route) {
                HistoryScreen(
                    activeStore = activeStore,
                    onOpenPhone = { phoneId -> navController.navigate(Routes.phoneDetail(phoneId)) }
                )
            }
            composable(Screen.Reports.route) {
                ReportsScreen(activeStore = activeStore)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(activeStore = activeStore, storeViewModel = storeViewModel)
            }

            composable(
                route = Routes.PHONE_DETAIL,
                arguments = listOf(navArgument("phoneId") { type = NavType.LongType })
            ) { backStackEntry ->
                val phoneId = backStackEntry.arguments?.getLong("phoneId") ?: return@composable
                PhoneDetailScreen(
                    activeStore = activeStore,
                    phoneId = phoneId,
                    onBack = { navController.popBackStack() },
                    onSellClick = { id -> navController.navigate(Routes.saleEntry(id)) }
                )
            }

            composable(
                route = Routes.SALE_ENTRY,
                arguments = listOf(navArgument("phoneId") { type = NavType.LongType })
            ) { backStackEntry ->
                val phoneId = backStackEntry.arguments?.getLong("phoneId") ?: return@composable
                SaleEntryScreen(
                    activeStore = activeStore,
                    phoneId = phoneId,
                    onSaved = {
                        navController.navigate(Screen.Inventory.route) {
                            popUpTo(Screen.Dashboard.route)
                        }
                    },
                    onCancel = { navController.popBackStack() }
                )
            }
        }
    }

    if (showStoreSwitcher) {
        StoreSwitcherSheet(
            stores = allStores,
            activeStoreId = activeStore.storeId,
            onSelect = { storeViewModel.switchStore(it); showStoreSwitcher = false },
            onAddNew = { /* Full multi-store creation UI lands in Phase 3 Settings */ showStoreSwitcher = false },
            onDismiss = { showStoreSwitcher = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StoreSwitcherSheet(
    stores: List<com.mobilestore.inventory.data.local.entity.StoreEntity>,
    activeStoreId: Long,
    onSelect: (Long) -> Unit,
    onAddNew: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        androidx.compose.foundation.layout.Column(Modifier.padding(20.dp)) {
            Text("Switch Store", style = MaterialTheme.typography.titleLarge)
            androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 12.dp))
            stores.forEach { store ->
                ListItem(
                    headlineContent = { Text(store.shopName) },
                    supportingContent = { Text(store.ownerName) },
                    trailingContent = { if (store.storeId == activeStoreId) Text("Active") },
                    modifier = Modifier
                        .clickable { onSelect(store.storeId) }
                        .padding(vertical = 2.dp)
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun AppBottomBar(navController: androidx.navigation.NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar {
        Screen.bottomNavItems.forEach { screen ->
            val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(screen.icon, contentDescription = screen.label) },
                label = { Text(screen.label) }
            )
        }
    }
}
