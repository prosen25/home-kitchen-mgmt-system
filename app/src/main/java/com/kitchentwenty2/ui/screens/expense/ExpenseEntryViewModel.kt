package com.kitchentwenty2.ui.screens.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kitchentwenty2.domain.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExpenseEntryViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository
) : ViewModel() {

    fun saveExpense(
        dateMillis: Long,
        category: String,
        amount: Double,
        notes: String?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            if (amount > 0) {
                val id = expenseRepository.addExpense(
                    dateMillis = dateMillis,
                    category = category,
                    amount = amount,
                    notes = notes
                )
                if (id > 0) {
                    onSuccess()
                }
            }
        }
    }

    // New: expose getExpenseById for edit flow
    suspend fun getExpenseById(expenseId: Long) = expenseRepository.getExpenseById(expenseId)

    // New: update existing expense
    fun updateExpense(
        dateMillis: Long,
        category: String,
        amount: Double,
        notes: String?,
        expenseId: Long,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            if (amount >= 0) {
                val ok = expenseRepository.updateExpense(expenseId, dateMillis, category, amount, notes)
                if (ok) onSuccess()
            }
        }
    }
}
