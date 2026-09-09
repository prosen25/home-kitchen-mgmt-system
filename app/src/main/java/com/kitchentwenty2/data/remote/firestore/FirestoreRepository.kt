package com.kitchentwenty2.data.remote.firestore

import kotlinx.coroutines.flow.Flow

/**
 * Firestore repository contracts for real-time and offline syncing.
 * Implementations should map Firestore documents to domain models and vice-versa.
 */
interface FirestoreUserRepository {
    fun listenCurrentUser(uid: String): Flow<FirestoreUser?>
    suspend fun getUser(uid: String): FirestoreUser?
    suspend fun upsertUser(user: FirestoreUser)
}

interface FirestoreCustomerRepository {
    fun listenAllCustomers(): Flow<List<FirestoreCustomer>>
    suspend fun getCustomer(id: String): FirestoreCustomer?
    suspend fun createOrUpdateCustomer(customer: FirestoreCustomer): String
    suspend fun deleteCustomer(id: String)
}

interface FirestoreOrderRepository {
    fun listenOrdersForDate(startOfDayMillis: Long, endOfDayMillis: Long): Flow<List<FirestoreOrder>>
    fun listenOrder(orderId: String): Flow<FirestoreOrder?>
    suspend fun getOrder(orderId: String): FirestoreOrder?
    suspend fun createOrUpdateOrder(order: FirestoreOrder): String
    suspend fun deleteOrder(orderId: String)
}

interface FirestorePaymentRepository {
    fun listenPaymentsForOrder(orderId: String): Flow<List<FirestorePaymentLog>>
    suspend fun addPayment(payment: FirestorePaymentLog): String
}

interface FirestoreExpenseRepository {
    fun listenExpensesForDate(startOfDayMillis: Long, endOfDayMillis: Long): Flow<List<FirestoreExpense>>
    suspend fun createOrUpdateExpense(expense: FirestoreExpense): String
    suspend fun deleteExpense(expenseId: String)
}
