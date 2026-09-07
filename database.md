This document defines the local Room Database architecture for Kitchen Twenty2. It includes entity definitions, composite relationship wrappers, Data Access Objects (DAOs), and technical audit attributes for system traceability and runtime error tracking.

## 1. Technical Audit Attributes Standard

Every database table implements the following technical audit attributes to support data tracing, operational analysis, and system auditing:

* `createdBy: String` — Unique identifier or User ID of the entity creator (defaults to system user or `"SYSTEM"` if unauthenticated).
* `createdDateTimeStamp: Long` — UTC Unix timestamp (milliseconds) representing when the record was inserted.
* `modifiedDateTimeStamp: Long` — UTC Unix timestamp (milliseconds) representing when the record was last modified.

---

## 2. Entities (Database Tables)

### `CustomerEntity.kt`
Stores unique customer profiles to support autocomplete suggestions, dialer integration, and delivery location sharing.

```kotlin
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
```

### `MenuItemEntity.kt`
Master catalog containing dishes and default pricing.

```package com.kitchentwenty2.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "menu_items")
data class MenuItemEntity(
    @PrimaryKey(autoGenerate = true)
    val menuItemId: Long = 0,
    val name: String,
    val description: String?,
    val defaultPrice: Double,
    
    // Technical Audit Attributes
    val createdBy: String = "SYSTEM",
    val createdDateTimeStamp: Long = System.currentTimeMillis(),
    val modifiedDateTimeStamp: Long = System.currentTimeMillis()
)
```

### `OrderEntity.kt`
Header record for each order, tracking status, dates, customer details, applied discounts, and payment totals.

```package com.kitchentwenty2.data.local.entity

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
    val customerId: Long?, // Nullable if generic or non-registered customer
    
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
```

### `OrderItemEntity.kt`
Individual items linked to a specific order (supports catalog items and custom manual entries).

```package com.kitchentwenty2.data.local.entity

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
```

### `PaymentLogEntity.kt`
Tracks payment activities (advance, partial/intermediate, settlement, or refunds) against an order.

```package com.kitchentwenty2.data.local.entity

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
```

### `ExpenseEntity.kt`
Tracks daily operational costs.

```package com.kitchentwenty2.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val expenseId: Long = 0,
    val expenseDate: Long, // Epoch timestamp at midnight start-of-day
    val category: String, // Groceries, Packaging, Gas, Other
    val amount: Double,
    val notes: String?,

    // Technical Audit Attributes
    val createdBy: String = "SYSTEM",
    val createdDateTimeStamp: Long = System.currentTimeMillis(),
    val modifiedDateTimeStamp: Long = System.currentTimeMillis()
)
```

### `AppErrorLogEntity.kt`
Logs application runtime exceptions, crashes, and unexpected failures for technical analysis.

```package com.kitchentwenty2.data.local.entity

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
```