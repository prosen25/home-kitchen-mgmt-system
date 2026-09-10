package com.kitchentwenty2.data.repository

import com.kitchentwenty2.data.local.dao.ExpenseDao
import com.kitchentwenty2.data.local.entity.ExpenseEntity
import com.kitchentwenty2.data.remote.firestore.FirestoreExpense
import com.kitchentwenty2.data.remote.firestore.FirestoreExpenseRepository
import com.kitchentwenty2.domain.model.ExpenseSummaryItem
import com.kitchentwenty2.domain.repository.ExpenseRepository
import com.kitchentwenty2.util.AppErrorLogger
import com.kitchentwenty2.util.DateTimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpenseRepositoryImpl @Inject constructor(
    private val expenseDao: ExpenseDao,
    private val firestoreExpenseRepository: FirestoreExpenseRepository,
    private val errorLogger: AppErrorLogger
) : ExpenseRepository {

    override fun getExpensesForDate(dateMillis: Long): Flow<List<ExpenseSummaryItem>> {
        val startOfDay = DateTimeUtils.getStartOfDay(dateMillis)
        val endOfDay = DateTimeUtils.getEndOfDay(dateMillis)
        return expenseDao.getExpensesByDate(startOfDay, endOfDay)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)
    }

    override fun getTotalExpensesForDate(dateMillis: Long): Flow<Double> {
        val startOfDay = DateTimeUtils.getStartOfDay(dateMillis)
        val endOfDay = DateTimeUtils.getEndOfDay(dateMillis)
        return expenseDao.getTotalExpensesByDate(startOfDay, endOfDay)
            .flowOn(Dispatchers.IO)
    }

    override suspend fun addExpense(
        dateMillis: Long,
        category: String,
        amount: Double,
        notes: String?
    ): Long {
        return withContext(Dispatchers.IO) {
            try {
                val entity = ExpenseEntity(
                    expenseDate = dateMillis,
                    category = category,
                    amount = amount,
                    notes = notes,
                    createdDateTimeStamp = System.currentTimeMillis(),
                    modifiedDateTimeStamp = System.currentTimeMillis()
                )
                val expenseId = expenseDao.insertExpense(entity)
                syncExpenseUpsert(entity.copy(expenseId = expenseId))
                expenseId
            } catch (e: Exception) {
                errorLogger.logException(e, "ExpenseRepository.addExpense")
                -1L
            }
        }
    }

    override suspend fun deleteExpense(expenseId: Long) {
        withContext(Dispatchers.IO) {
            try {
                expenseDao.deleteExpenseById(expenseId)
                firestoreExpenseRepository.deleteExpense(expenseId.toString())
            } catch (e: Exception) {
                errorLogger.logException(e, "ExpenseRepository.deleteExpense")
            }
        }
    }

    override suspend fun getExpenseById(expenseId: Long): ExpenseSummaryItem? {
        return withContext(Dispatchers.IO) {
            try {
                expenseDao.getExpenseById(expenseId)?.let { it.toDomain() }
            } catch (e: Exception) {
                errorLogger.logException(e, "ExpenseRepository.getExpenseById")
                null
            }
        }
    }

    override suspend fun updateExpense(expenseId: Long, dateMillis: Long, category: String, amount: Double, notes: String?): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val existing = expenseDao.getExpenseById(expenseId)
                if (existing == null) return@withContext false

                val updated = ExpenseEntity(
                    expenseId = existing.expenseId,
                    expenseDate = dateMillis,
                    category = category,
                    amount = amount,
                    notes = notes,
                    createdBy = existing.createdBy,
                    createdDateTimeStamp = existing.createdDateTimeStamp,
                    modifiedDateTimeStamp = System.currentTimeMillis()
                )
                val res = expenseDao.insertExpense(updated)
                if (res > 0) {
                    syncExpenseUpsert(updated)
                }
                res > 0
            } catch (e: Exception) {
                errorLogger.logException(e, "ExpenseRepository.updateExpense")
                false
            }
        }
    }

    private suspend fun syncExpenseUpsert(expense: ExpenseEntity) {
        firestoreExpenseRepository.createOrUpdateExpense(
            FirestoreExpense(
                id = expense.expenseId.toString(),
                expenseDate = expense.expenseDate,
                category = expense.category,
                amount = expense.amount,
                notes = expense.notes,
                createdBy = expense.createdBy
            )
        )
    }

    private fun ExpenseEntity.toDomain(): ExpenseSummaryItem {
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return ExpenseSummaryItem(
            expenseId = expenseId,
            category = category,
            amount = amount,
            note = notes ?: "",
            expenseDateMillis = expenseDate,
            timeFormatted = timeFormat.format(Date(createdDateTimeStamp))
        )
    }
}
