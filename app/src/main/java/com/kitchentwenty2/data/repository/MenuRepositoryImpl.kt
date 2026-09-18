package com.kitchentwenty2.data.repository

import com.kitchentwenty2.data.local.dao.MenuItemDao
import com.kitchentwenty2.data.local.entity.MenuItemEntity
import com.kitchentwenty2.data.remote.firestore.FirestoreMenuItem
import com.kitchentwenty2.data.remote.firestore.FirestoreMenuRepository
import com.kitchentwenty2.domain.model.MenuItemModel
import com.kitchentwenty2.domain.repository.MenuRepository
import com.kitchentwenty2.util.AppErrorLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MenuRepositoryImpl @Inject constructor(
    private val menuItemDao: MenuItemDao,
    private val firestoreMenuRepository: FirestoreMenuRepository,
    private val errorLogger: AppErrorLogger
) : MenuRepository {

    override fun getAllMenuItems(): Flow<List<MenuItemModel>> {
        return withRemoteMenuItems(menuItemDao.getAllMenuItems())
            .map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)
    }

    override fun searchMenuItems(query: String): Flow<List<MenuItemModel>> {
        return withRemoteMenuItems(menuItemDao.searchMenuItems(query))
            .map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun existsByName(name: String, excludeId: Long): Boolean {
        return withContext(Dispatchers.IO) {
            menuItemDao.existsByName(name.trim(), excludeId)
        }
    }

    override suspend fun addMenuItem(
        name: String,
        defaultPrice: Double,
        category: String,
        description: String?
    ): Long {
        return withContext(Dispatchers.IO) {
            try {
                val entity = MenuItemEntity(
                    name = name.trim(),
                    defaultPrice = defaultPrice,
                    description = description,
                    createdDateTimeStamp = System.currentTimeMillis(),
                    modifiedDateTimeStamp = System.currentTimeMillis()
                )
                val itemId = menuItemDao.insertMenuItem(entity)
                syncMenuItemUpsert(entity.copy(menuItemId = itemId), category)
                itemId
            } catch (e: Exception) {
                errorLogger.logException(e, "MenuRepository.addMenuItem")
                -1L
            }
        }
    }

    override suspend fun updateMenuItem(item: MenuItemModel) {
        withContext(Dispatchers.IO) {
            try {
                val existing = menuItemDao.getMenuItemById(item.menuItemId)
                if (existing != null) {
                    val updated = existing.copy(
                        name = item.name.trim(),
                        defaultPrice = item.defaultPrice,
                        description = item.description,
                        modifiedDateTimeStamp = System.currentTimeMillis()
                    )
                    menuItemDao.updateMenuItem(updated)
                    syncMenuItemUpsert(updated, item.category)
                }
            } catch (e: Exception) {
                errorLogger.logException(e, "MenuRepository.updateMenuItem")
            }
        }
    }

    override suspend fun deleteMenuItem(itemId: Long) {
        withContext(Dispatchers.IO) {
            try {
                menuItemDao.deleteMenuItemById(itemId)
                firestoreMenuRepository.deleteMenuItem(itemId.toString())
            } catch (e: Exception) {
                errorLogger.logException(e, "MenuRepository.deleteMenuItem")
            }
        }
    }

    private suspend fun syncMenuItemUpsert(item: MenuItemEntity, category: String) {
        firestoreMenuRepository.createOrUpdateMenuItem(
            FirestoreMenuItem(
                id = item.menuItemId.toString(),
                name = item.name,
                category = category,
                description = item.description,
                defaultPrice = item.defaultPrice,
                createdBy = item.createdBy
            )
        )
    }

    private fun withRemoteMenuItems(local: Flow<List<MenuItemEntity>>): Flow<List<MenuItemEntity>> = channelFlow {
        launch {
            firestoreMenuRepository.listenAllMenuItems()
                .catch { errorLogger.logException(it, "MenuRepository.listenAllMenuItems") }
                .collect { menuItems ->
                    menuItems.forEach { menuItem ->
                        val itemId = menuItem.id.toLongOrNull()?.takeIf { it > 0 } ?: return@forEach
                        val now = System.currentTimeMillis()
                        menuItemDao.insertMenuItem(
                            MenuItemEntity(
                                menuItemId = itemId,
                                name = menuItem.name,
                                description = menuItem.description,
                                defaultPrice = menuItem.defaultPrice,
                                createdBy = menuItem.createdBy ?: "SYSTEM",
                                createdDateTimeStamp = menuItem.createdAt?.toDate()?.time ?: now,
                                modifiedDateTimeStamp = menuItem.modifiedAt?.toDate()?.time ?: now
                            )
                        )
                    }
                }
        }
        local.collect { send(it) }
    }

    private fun MenuItemEntity.toDomain() = MenuItemModel(
        menuItemId = menuItemId,
        name = name,
        category = "Main Course",
        description = description,
        defaultPrice = defaultPrice
    )
}
