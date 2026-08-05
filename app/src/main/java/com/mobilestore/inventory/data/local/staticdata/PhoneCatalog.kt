package com.mobilestore.inventory.data.local.staticdata

/**
 * Preloaded reference lists used by Purchase Entry's dropdowns. Users can
 * always type a custom value in any of these fields — these are suggestions,
 * not hard constraints, per the spec ("allow users to manually add custom
 * brands/models").
 */
object PhoneCatalog {
    val brands = listOf(
        "Samsung", "Apple", "Xiaomi", "Redmi", "Poco", "Realme", "Oppo", "Vivo",
        "OnePlus", "Google Pixel", "Huawei", "Honor", "Motorola", "Nokia",
        "Infinix", "Tecno", "itel", "Sony", "LG", "Asus", "Lenovo",
        "BlackBerry", "Nothing", "ZTE", "Meizu", "HTC", "Micromax", "Lava", "QMobile"
    )

    val storageOptions = listOf("16GB", "32GB", "64GB", "128GB", "256GB", "512GB", "1TB")
    val ramOptions = listOf("1GB", "2GB", "3GB", "4GB", "6GB", "8GB", "12GB", "16GB")
    val batteryHealthOptions = listOf("100%", "95-99%", "90-94%", "85-89%", "80-84%", "Below 80%")
    val conditionOptions = listOf("Brand New", "Like New", "Excellent", "Good", "Fair", "For Parts")
    val ptaStatusOptions = listOf("PTA Approved", "Non-PTA", "Factory Unlocked (No PTA needed)", "Blocked")
    val accessoryOptions = listOf("Charger", "Original Box", "Cable", "Earphones", "Case/Cover")
}
