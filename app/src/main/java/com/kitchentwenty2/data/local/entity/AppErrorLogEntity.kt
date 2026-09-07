package com.kitchentwenty2.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_error_logs")
data class AppErrorLogEntity(
    @PrimaryKey(autoGenerate = true)
    val errorId: Long = 0,
    val errorType: String, // e.g., NullPointerException, SQLiteException, NetworkException
    val message: String?,
    val stackTrace: String?,
    val screenOrFeatureName: String?, // Screen/Component where failure occurred
    val deviceModel: String?,
    val osVersion: String?,

    // Technical Audit Attributes
    val createdBy: String = "SYSTEM",
    val createdDateTimeStamp: Long = System.currentTimeMillis(),
    val modifiedDateTimeStamp: Long = System.currentTimeMillis()
)
