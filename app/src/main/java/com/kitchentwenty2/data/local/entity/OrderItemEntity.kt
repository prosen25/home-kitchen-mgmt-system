package com.kitchentwenty2.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "order_items",
    foreignKeys = [
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["orderId"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["orderId"])]
)
data class OrderItemEntity(
    @PrimaryKey(autoGenerate = true)
    val orderItemId: Long = 0,
    val orderId: Long,
    val itemName: String,
    val unitPrice: Double,
    val quantity: Int,
    val subtotal: Double, // Calculated as unitPrice * quantity

    // Technical Audit Attributes
    val createdBy: String = "SYSTEM",
    val createdDateTimeStamp: Long = System.currentTimeMillis(),
    val modifiedDateTimeStamp: Long = System.currentTimeMillis()
)
