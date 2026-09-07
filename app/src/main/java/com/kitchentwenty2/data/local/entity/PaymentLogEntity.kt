package com.kitchentwenty2.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "payment_logs",
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
data class PaymentLogEntity(
    @PrimaryKey(autoGenerate = true)
    val paymentId: Long = 0,
    val orderId: Long,
    val paymentDate: Long,
    val amount: Double,
    val paymentType: String, // ADVANCE, INTERMEDIATE, SETTLEMENT, REFUND

    // Technical Audit Attributes
    val createdBy: String = "SYSTEM",
    val createdDateTimeStamp: Long = System.currentTimeMillis(),
    val modifiedDateTimeStamp: Long = System.currentTimeMillis()
)
