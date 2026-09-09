package com.kitchentwenty2.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kitchentwenty2.domain.model.DashboardNavigationItem
import com.kitchentwenty2.domain.model.DashboardTab
import com.kitchentwenty2.domain.model.DashboardUiState
import com.kitchentwenty2.domain.model.FinancialSummary
import com.kitchentwenty2.domain.repository.ExpenseRepository
import com.kitchentwenty2.domain.repository.OrderRepository
import com.kitchentwenty2.data.remote.firestore.FirestoreOrderRepository
import com.kitchentwenty2.data.remote.firestore.FirestoreOrder
import com.kitchentwenty2.domain.model.OrderSummaryItem
import com.kitchentwenty2.domain.model.OrderStatus
import com.kitchentwenty2.util.DateTimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
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
    private val expenseRepository: ExpenseRepository,
    private val firestoreOrderRepository: FirestoreOrderRepository
) : ViewModel() {

    private val _selectedDateMillis = MutableStateFlow(System.currentTimeMillis())
    val selectedDateMillis: StateFlow<Long> = _selectedDateMillis.asStateFlow()

    private val _selectedTab = MutableStateFlow(DashboardTab.ORDERS)
    val selectedTab: StateFlow<DashboardTab> = _selectedTab.asStateFlow()

    private val _selectedNav = MutableStateFlow(DashboardNavigationItem.HOME)
    val selectedNav: StateFlow<DashboardNavigationItem> = _selectedNav.asStateFlow()

    private val baseState: Flow<DashboardUiState> = _selectedDateMillis.flatMapLatest { dateMillis ->
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
    }

    val uiState: StateFlow<DashboardUiState> = _selectedDateMillis.flatMapLatest { dateMillis ->
        combine(
            baseState,
            firestoreOrderRepository.listenOrdersForDate(
                DateTimeUtils.getStartOfDay(dateMillis),
                DateTimeUtils.getEndOfDay(dateMillis)
            )
        ) { base, remoteOrders ->
            if (remoteOrders.isNotEmpty()) {
                val mergedOrders = remoteOrders.mapNotNull { ro ->
                    try {
                        val parsedOrderId = ro.id.toLongOrNull() ?: System.currentTimeMillis()
                        val itemsSummary = ro.items.joinToString(", ") { "${it.quantity}x ${it.itemName}" }
                        OrderSummaryItem(
                            orderId = parsedOrderId,
                            customerName = ro.customerSnapshot?.name ?: ro.customerId ?: "",
                            customerPhone = ro.customerSnapshot?.mobileNumber ?: "",
                            itemsSummary = itemsSummary,
                            deliveryAddress = ro.customerSnapshot?.address ?: "",
                            googleLocationUrl = ro.customerSnapshot?.googleLocationUrl,
                            totalAmount = ro.totalAmount,
                            outstandingDue = (ro.totalAmount - ro.totalCollected - ro.refundedAmount).coerceAtLeast(0.0),
                            totalPaid = ro.totalCollected,
                            status = when (ro.status.uppercase()) {
                                "UNPAID" -> OrderStatus.UNPAID
                                "FULLY_PAID" -> OrderStatus.FULLY_PAID
                                "CANCELLED" -> OrderStatus.CANCELLED
                                else -> OrderStatus.PARTIALLY_PAID
                            }
                        )
                    } catch (_: Exception) {
                        null
                    }
                }
                base.copy(orders = mergedOrders)
            } else base
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

    // New: expose delete expense action to UI
    fun deleteExpense(expenseId: Long) {
        viewModelScope.launch {
            try {
                expenseRepository.deleteExpense(expenseId)
            } catch (_: Exception) {
                // ignore, AppErrorLogger handled in repository
            }
        }
    }
}
