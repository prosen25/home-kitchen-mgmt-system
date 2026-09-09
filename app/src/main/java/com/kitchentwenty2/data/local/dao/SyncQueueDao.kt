package com.kitchentwenty2.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.kitchentwenty2.data.local.entity.SyncQueueEntity

@Dao
interface SyncQueueDao {
    @Insert
    suspend fun insert(queue: SyncQueueEntity): Long

    @Query("SELECT * FROM sync_queue ORDER BY createdDateTimeStamp ASC")
    suspend fun getAllPending(): List<SyncQueueEntity>

    @Query("SELECT * FROM sync_queue WHERE syncId = :id LIMIT 1")
    suspend fun findById(id: Long): SyncQueueEntity?

    @Query("DELETE FROM sync_queue WHERE syncId = :id")
    suspend fun deleteById(id: Long)

    @Update
    suspend fun update(queue: SyncQueueEntity)
}
