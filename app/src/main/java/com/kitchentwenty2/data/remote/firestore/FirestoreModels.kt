package com.kitchentwenty2.data.remote.firestore

import com.google.firebase.Timestamp

// Firestore document models — keep fields nullable where Firestore may omit them.

data class FirestoreUser(
    val uid: String = "",
    val email: String? = null,
    val displayName: String? = null,
    val role: String = "CASHIER", // KITCHEN_STAFF | KITCHEN_MANAGER | CASHIER
    val createdAt: Timestamp? = null,
    val modifiedAt: Timestamp? = null
)

data class FirestoreCustomer(
    val id: String = "",
    val name: String = "",
    val mobileNumber: String? = null,
    val address: String? = null,
    val googleLocationUrl: String? = null,
    val createdBy: String? = null,
    val createdAt: Timestamp? = null,
    val modifiedAt: Timestamp? = null
)

data class FirestoreOrderItem(
    val itemId: String = "",
    val itemName: String = "",
    val unitPrice: Double = 0.0,
    val quantity: Int = 1,
    val subtotal: Double = 0.0
)

data class FirestoreOrder(
    val id: String = "",
    val customerId: String? = null,
    val customerSnapshot: FirestoreCustomer? = null,
    val orderDate: Long = 0L,
    val items: List<FirestoreOrderItem> = emptyList(),
    val upfrontDiscount: Double = 0.0,
    val settlementDiscount: Double = 0.0,
    val advancePaid: Double = 0.0,
    val totalCollected: Double = 0.0,
    val refundedAmount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val status: String = "UNPAID",
    val createdBy: String? = null,
    val createdAt: Timestamp? = null,
    val modifiedAt: Timestamp? = null
)

data class FirestorePaymentLog(
    val id: String = "",
    val orderId: String = "",
    val paymentDate: Long = 0L,
    val amount: Double = 0.0,
    val paymentType: String = "INTERMEDIATE",
    val createdBy: String? = null,
    val createdAt: Timestamp? = null
)

data class FirestoreExpense(
    val id: String = "",
    val expenseDate: Long = 0L,
    val category: String = "",
    val amount: Double = 0.0,
    val notes: String? = null,
    val createdBy: String? = null,
    val createdAt: Timestamp? = null,
    val modifiedAt: Timestamp? = null
)
