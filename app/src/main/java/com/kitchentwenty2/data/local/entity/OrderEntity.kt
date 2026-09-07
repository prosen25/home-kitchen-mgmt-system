package com.kitchentwenty2.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "orders",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["customerId"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["customerId"])]
)
data class OrderEntity(
    @PrimaryKey(autoGenerate = true)
    val orderId: Long = 0,
    val customerId: Long? = null, // Nullable if generic or non-registered customer
    
    // Customer Profile Snapshot
    val customerName: String,
    val customerPhone: String?,
    val customerAddress: String?,
    val googleLocationUrl: String?,
    
    val orderDate: Long, // Epoch timestamp at midnight start-of-day
    val upfrontDiscount: Double = 0.0,
    val settlementDiscount: Double = 0.0,
    val advancePaid: Double = 0.0,
    val totalCollected: Double = 0.0, // Sum of Advance + Intermediate + Settlement Payments
    val refundedAmount: Double = 0.0,
    val totalAmount: Double, // Subtotal - Upfront Discount - Settlement Discount
    
    val status: String, // UNPAID, PARTIALLY_PAID, FULLY_PAID, CANCELLED

    // Technical Audit Attributes
    val createdBy: String = "SYSTEM",
    val createdDateTimeStamp: Long = System.currentTimeMillis(),
    val modifiedDateTimeStamp: Long = System.currentTimeMillis()
)
