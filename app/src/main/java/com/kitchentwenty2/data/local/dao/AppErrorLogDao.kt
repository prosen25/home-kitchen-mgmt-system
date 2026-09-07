package com.kitchentwenty2.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kitchentwenty2.data.local.entity.AppErrorLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppErrorLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AppErrorLogEntity): Long

    @Query("SELECT * FROM app_error_logs ORDER BY createdDateTimeStamp DESC")
    fun getAllLogs(): Flow<List<AppErrorLogEntity>>
}
