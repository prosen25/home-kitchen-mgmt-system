package com.kitchentwenty2.data.repository

import com.kitchentwenty2.data.local.dao.ExpenseDao
import com.kitchentwenty2.data.local.entity.ExpenseEntity
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
                expenseDao.insertExpense(entity)
            } catch (e: Exception) {
                errorLogger.logException(e, "ExpenseRepository.addExpense")
                -1L
            }
        }
    }

    override suspend fun deleteExpense(expenseId: Long) {
        withContext(Dispatchers.IO) {
            try {
                // Delete expense directly if needed
            } catch (e: Exception) {
                errorLogger.logException(e, "ExpenseRepository.deleteExpense")
            }
        }
    }

    private fun ExpenseEntity.toDomain(): ExpenseSummaryItem {
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return ExpenseSummaryItem(
            expenseId = expenseId,
            category = category,
            amount = amount,
            note = notes ?: "",
            timeFormatted = timeFormat.format(Date(createdDateTimeStamp))
        )
    }
}
