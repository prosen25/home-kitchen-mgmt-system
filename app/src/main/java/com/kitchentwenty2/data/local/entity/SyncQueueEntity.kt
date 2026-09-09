package com.kitchentwenty2.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true)
    val syncId: Long = 0,
    val operationType: String, // UPSERT, DELETE
    val collectionName: String,
    val documentId: String?, // optional; if null Firestore can auto-generate on UPSERT
    val payload: String, // JSON payload for the document
    val createdBy: String? = null,
    val createdDateTimeStamp: Long = System.currentTimeMillis(),
    val attempts: Int = 0,
    val lastError: String? = null
)
