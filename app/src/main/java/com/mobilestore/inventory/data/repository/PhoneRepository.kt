package com.mobilestore.inventory.data.repository

import com.mobilestore.inventory.data.local.dao.PhoneDao
import com.mobilestore.inventory.data.local.entity.PhoneEntity
import com.mobilestore.inventory.data.local.entity.PhoneStatus
import com.mobilestore.inventory.data.local.relation.PhoneWithPurchase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhoneRepository @Inject constructor(
    private val phoneDao: PhoneDao
) {
    fun getPhonesForStore(storeId: Long): Flow<List<PhoneEntity>> = phoneDao.getPhonesForStore(storeId)
    fun getInStock(storeId: Long): Flow<List<PhoneEntity>> = phoneDao.getPhonesByStatus(storeId, PhoneStatus.IN_STOCK)
    fun getSold(storeId: Long): Flow<List<PhoneEntity>> = phoneDao.getPhonesByStatus(storeId, PhoneStatus.SOLD)
    fun search(storeId: Long, query: String): Flow<List<PhoneEntity>> = phoneDao.searchPhones(storeId, query)
    fun inStockCount(storeId: Long): Flow<Int> = phoneDao.getInStockCount(storeId)
    suspend fun getPhoneById(phoneId: Long) = phoneDao.getPhoneById(phoneId)
    suspend fun updatePhoneDetails(phone: PhoneEntity) = phoneDao.updatePhone(phone)
    fun getDistinctBrandsUsed(storeId: Long): Flow<List<String>> = phoneDao.getDistinctBrandsUsed(storeId)
    suspend fun getDistinctModelsForBrand(storeId: Long, brand: String): List<String> =
        phoneDao.getDistinctModelsForBrand(storeId, brand)
    fun getPhonesWithPurchaseInfo(storeId: Long): Flow<List<PhoneWithPurchase>> =
        phoneDao.getPhonesWithPurchaseInfo(storeId)
}
