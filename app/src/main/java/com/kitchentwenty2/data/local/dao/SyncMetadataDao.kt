package com.kitchentwenty2.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kitchentwenty2.data.local.entity.SyncMetadataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncMetadataDao {
    @Query("SELECT * FROM sync_metadata WHERE `key` = :key LIMIT 1")
    suspend fun getByKey(key: String): SyncMetadataEntity?

    @Query("SELECT * FROM sync_metadata")
    fun getAll(): Flow<List<SyncMetadataEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SyncMetadataEntity)

    @Update
    suspend fun update(entity: SyncMetadataEntity)

    @Query("DELETE FROM sync_metadata WHERE `key` = :key")
    suspend fun deleteByKey(key: String)

    @Query("DELETE FROM sync_metadata")
    suspend fun clearAll()
}
