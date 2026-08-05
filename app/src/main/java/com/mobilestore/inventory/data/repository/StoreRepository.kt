package com.mobilestore.inventory.data.repository

import com.mobilestore.inventory.data.local.dao.StoreDao
import com.mobilestore.inventory.data.local.entity.StoreEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StoreRepository @Inject constructor(
    private val storeDao: StoreDao
) {
    fun getAllStores(): Flow<List<StoreEntity>> = storeDao.getAllStores()
    suspend fun getStoreById(storeId: Long) = storeDao.getStoreById(storeId)
    suspend fun createStore(store: StoreEntity): Long = storeDao.insertStore(store)
    suspend fun updateStoreProfile(store: StoreEntity) = storeDao.updateStore(store)
    suspend fun deleteStore(store: StoreEntity) = storeDao.deleteStore(store)
}
