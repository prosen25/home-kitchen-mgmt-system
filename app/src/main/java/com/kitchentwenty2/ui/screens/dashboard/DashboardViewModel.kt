package com.kitchentwenty2.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kitchentwenty2.domain.model.DashboardNavigationItem
import com.kitchentwenty2.domain.model.DashboardTab
import com.kitchentwenty2.domain.model.DashboardUiState
import com.kitchentwenty2.domain.model.FinancialSummary
import com.kitchentwenty2.domain.repository.ExpenseRepository
import com.kitchentwenty2.domain.repository.OrderRepository
import com.kitchentwenty2.util.DateTimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val expenseRepository: ExpenseRepository
) : ViewModel() {

    private val _selectedDateMillis = MutableStateFlow(System.currentTimeMillis())
    val selectedDateMillis: StateFlow<Long> = _selectedDateMillis.asStateFlow()

    private val _selectedTab = MutableStateFlow(DashboardTab.ORDERS)
    val selectedTab: StateFlow<DashboardTab> = _selectedTab.asStateFlow()

    private val _selectedNav = MutableStateFlow(DashboardNavigationItem.HOME)
    val selectedNav: StateFlow<DashboardNavigationItem> = _selectedNav.asStateFlow()

    val uiState: StateFlow<DashboardUiState> = _selectedDateMillis.flatMapLatest { dateMillis ->
        combine(
            orderRepository.getOrdersForDate(dateMillis),
            expenseRepository.getExpensesForDate(dateMillis),
            orderRepository.getDailyFinancialSummary(dateMillis),
            _selectedTab,
            _selectedNav
        ) { orders, expenses, financialSummary, tab, nav ->
            DashboardUiState(
                displayDate = DateTimeUtils.getDisplayDateLabel(dateMillis),
                formattedDate = DateTimeUtils.formatDate(dateMillis),
                isToday = DateTimeUtils.isToday(dateMillis),
                isFutureDate = DateTimeUtils.isFuture(dateMillis),
                financialSummary = financialSummary,
                selectedTab = tab,
                orders = orders,
                expenses = expenses,
                selectedNavigationItem = nav
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    fun onPreviousDay() {
        _selectedDateMillis.update { it - 86400000L }
    }

    fun onNextDay() {
        _selectedDateMillis.update { it + 86400000L }
    }

    fun onDateSelected(dateMillis: Long?) {
        dateMillis?.let { millis ->
            _selectedDateMillis.value = millis
        }
    }

    fun selectTab(tab: DashboardTab) {
        _selectedTab.value = tab
    }

    fun selectNavigation(nav: DashboardNavigationItem) {
        _selectedNav.value = nav
    }
}
