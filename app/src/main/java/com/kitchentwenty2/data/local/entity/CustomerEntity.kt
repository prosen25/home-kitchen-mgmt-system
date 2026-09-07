package com.kitchentwenty2.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true)
    val customerId: Long = 0,
    val name: String,
    val mobileNumber: String?,
    val address: String?,
    val googleLocationUrl: String?,
    
    // Technical Audit Attributes
    val createdBy: String = "SYSTEM",
    val createdDateTimeStamp: Long = System.currentTimeMillis(),
    val modifiedDateTimeStamp: Long = System.currentTimeMillis()
)
