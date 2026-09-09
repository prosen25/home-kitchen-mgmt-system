package com.kitchentwenty2.data.repository

import com.kitchentwenty2.data.local.dao.MenuItemDao
import com.kitchentwenty2.data.local.entity.MenuItemEntity
import com.kitchentwenty2.domain.model.MenuItemModel
import com.kitchentwenty2.domain.repository.MenuRepository
import com.kitchentwenty2.util.AppErrorLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MenuRepositoryImpl @Inject constructor(
    private val menuItemDao: MenuItemDao,
    private val errorLogger: AppErrorLogger
) : MenuRepository {

    override fun getAllMenuItems(): Flow<List<MenuItemModel>> {
        return menuItemDao.getAllMenuItems()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)
    }

    override fun searchMenuItems(query: String): Flow<List<MenuItemModel>> {
        return menuItemDao.searchMenuItems(query)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)
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
                menuItemDao.insertMenuItem(entity)
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
            } catch (e: Exception) {
                errorLogger.logException(e, "MenuRepository.deleteMenuItem")
            }
        }
    }

    private fun MenuItemEntity.toDomain() = MenuItemModel(
        menuItemId = menuItemId,
        name = name,
        category = "Main Course",
        description = description,
        defaultPrice = defaultPrice
    )
}
