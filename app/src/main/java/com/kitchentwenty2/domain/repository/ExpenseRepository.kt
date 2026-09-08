package com.kitchentwenty2.domain.repository

import com.kitchentwenty2.domain.model.ExpenseSummaryItem
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    fun getExpensesForDate(dateMillis: Long): Flow<List<ExpenseSummaryItem>>
    fun getTotalExpensesForDate(dateMillis: Long): Flow<Double>
    suspend fun addExpense(dateMillis: Long, category: String, amount: Double, notes: String?): Long
    suspend fun deleteExpense(expenseId: Long)

    // New: support fetching single expense and updating
    suspend fun getExpenseById(expenseId: Long): ExpenseSummaryItem?
    suspend fun updateExpense(expenseId: Long, dateMillis: Long, category: String, amount: Double, notes: String?): Boolean
}
