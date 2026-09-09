package com.kitchentwenty2.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores lightweight metadata about sync operations to help the SyncManager and diagnostics.
 * Uses a string key as the primary key to support well-known keys (e.g. "orders.lastSynced").
 */
@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey
    val key: String,
    val collectionName: String? = null,
    val lastSyncedAt: Long? = null,
    val pendingCount: Int = 0,
    val lastError: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
