package com.kitchentwenty2.domain.repository

import com.kitchentwenty2.domain.model.MenuItemModel
import kotlinx.coroutines.flow.Flow

interface MenuRepository {
    fun getAllMenuItems(): Flow<List<MenuItemModel>>
    fun searchMenuItems(query: String): Flow<List<MenuItemModel>>
    suspend fun existsByName(name: String, excludeId: Long = 0L): Boolean
    suspend fun addMenuItem(name: String, defaultPrice: Double, category: String = "Main Course", description: String? = null): Long
    suspend fun updateMenuItem(item: MenuItemModel)
    suspend fun deleteMenuItem(itemId: Long)
}
