package com.mobilestore.inventory.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/** Bottom navigation destinations per the spec's Navigation section. */
sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object Dashboard : Screen("dashboard", "Dashboard", Icons.Filled.Dashboard)
    data object Inventory : Screen("inventory", "Inventory", Icons.Filled.PhoneAndroid)
    data object Purchases : Screen("purchases", "Purchases", Icons.Filled.ShoppingCart)
    data object Sales : Screen("sales", "Sales", Icons.Filled.PointOfSale)
    data object History : Screen("history", "History", Icons.Filled.Receipt)
    data object Reports : Screen("reports", "Reports", Icons.Filled.BarChart)
    data object Settings : Screen("settings", "Settings", Icons.Filled.Settings)

    companion object {
        val bottomNavItems = listOf(Dashboard, Inventory, Purchases, Sales, History, Reports, Settings)
    }
}

/** Non-bottom-nav routes, added as screens are built out in later phases. */
object Routes {
    const val STORE_SELECT = "store_select"
    const val STORE_CREATE = "store_create"
    const val PURCHASE_ENTRY = "purchase_entry"
    const val SALE_ENTRY = "sale_entry/{phoneId}"
    const val PHONE_DETAIL = "phone_detail/{phoneId}"
    const val TRANSACTION_DETAIL = "transaction_detail/{transactionId}"
    const val SCAN_IMEI = "scan_imei"

    fun saleEntry(phoneId: Long) = "sale_entry/$phoneId"
    fun phoneDetail(phoneId: Long) = "phone_detail/$phoneId"
    fun transactionDetail(transactionId: Long) = "transaction_detail/$transactionId"
}
