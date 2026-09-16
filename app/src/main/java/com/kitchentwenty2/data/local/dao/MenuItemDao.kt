package com.kitchentwenty2.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kitchentwenty2.data.local.entity.MenuItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MenuItemDao {
    @Query("SELECT * FROM menu_items ORDER BY name ASC")
    fun getAllMenuItems(): Flow<List<MenuItemEntity>>

    @Query("SELECT * FROM menu_items WHERE menuItemId = :itemId LIMIT 1")
    suspend fun getMenuItemById(itemId: Long): MenuItemEntity?

    @Query("SELECT COUNT(*) FROM menu_items")
    suspend fun getMenuItemCount(): Int

    @Query("SELECT * FROM menu_items ORDER BY menuItemId")
    suspend fun getAllMenuItemsSnapshot(): List<MenuItemEntity>

    @Query("SELECT * FROM menu_items WHERE name LIKE '%' || :query || '%' ORDER BY name LIMIT 50")
    fun searchMenuItems(query: String): Flow<List<MenuItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMenuItem(item: MenuItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllMenuItems(items: List<MenuItemEntity>)

    @Update
    suspend fun updateMenuItem(item: MenuItemEntity)

    @Delete
    suspend fun deleteMenuItem(item: MenuItemEntity)

    @Query("DELETE FROM menu_items WHERE menuItemId = :itemId")
    suspend fun deleteMenuItemById(itemId: Long)
}
