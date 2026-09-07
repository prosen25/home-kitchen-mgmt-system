package com.kitchentwenty2.ui.screens.order

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kitchentwenty2.domain.model.OrderDetailUiState
import com.kitchentwenty2.domain.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrderDetailViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val orderId: Long = savedStateHandle.get<Long>("orderId") ?: 104L

    val orderState: StateFlow<OrderDetailUiState?> = orderRepository.getOrderDetails(orderId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun addIntermediatePayment(amount: Double, paymentType: String = "Intermediate Payment") {
        viewModelScope.launch {
            if (amount > 0) {
                orderRepository.addIntermediatePayment(orderId, amount, paymentType)
            }
        }
    }

    fun settleOrder(settlementDiscount: Double, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            orderRepository.settleOrder(orderId, settlementDiscount)
            onSuccess()
        }
    }

    fun cancelOrder(refundAmount: Double, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            orderRepository.cancelOrder(orderId, refundAmount)
            onSuccess()
        }
    }
}
