package com.kitchentwenty2.ui.screens.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kitchentwenty2.domain.model.OrderSummaryItem
import com.kitchentwenty2.domain.repository.OrderRepository
import com.kitchentwenty2.util.DateTimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class ReportsHistoryUiState(
    val selectedDateMillis: Long = System.currentTimeMillis(),
    val displayDate: String = DateTimeUtils.getDisplayDateLabel(selectedDateMillis),
    val orders: List<OrderSummaryItem> = emptyList()
) {
    val totalOrders: Int get() = orders.size
    val totalValue: Double get() = orders.sumOf { it.totalAmount }
    val totalCollected: Double get() = orders.sumOf { it.totalPaid }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReportsHistoryViewModel @Inject constructor(
    private val orderRepository: OrderRepository
) : ViewModel() {
    private val selectedDateMillis = MutableStateFlow(System.currentTimeMillis())

    val uiState: StateFlow<ReportsHistoryUiState> = selectedDateMillis
        .flatMapLatest { dateMillis ->
            orderRepository.getOrdersForDate(dateMillis).map { orders ->
                ReportsHistoryUiState(
                    selectedDateMillis = dateMillis,
                    displayDate = DateTimeUtils.getDisplayDateLabel(dateMillis),
                    orders = orders
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ReportsHistoryUiState()
        )

    fun onPreviousDay() {
        selectedDateMillis.update { it - 86_400_000L }
    }

    fun onNextDay() {
        selectedDateMillis.update { it + 86_400_000L }
    }

    fun onDateSelected(dateMillis: Long?) {
        dateMillis?.let { selectedDateMillis.value = it }
    }
}