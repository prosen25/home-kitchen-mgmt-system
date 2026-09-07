package com.kitchentwenty2.domain.repository

import com.kitchentwenty2.domain.model.FinancialSummary
import com.kitchentwenty2.domain.model.OrderDetailUiState
import com.kitchentwenty2.domain.model.OrderFormState
import com.kitchentwenty2.domain.model.OrderSummaryItem
import kotlinx.coroutines.flow.Flow

interface OrderRepository {
    fun getOrdersForDate(dateMillis: Long): Flow<List<OrderSummaryItem>>
    fun getOrderDetails(orderId: Long): Flow<OrderDetailUiState?>
    suspend fun saveOrder(orderForm: OrderFormState): Long
    suspend fun addIntermediatePayment(orderId: Long, amount: Double, paymentType: String = "Intermediate Payment")
    suspend fun settleOrder(orderId: Long, settlementDiscount: Double)
    suspend fun cancelOrder(orderId: Long, refundAmount: Double)
    fun getDailyFinancialSummary(dateMillis: Long): Flow<FinancialSummary>
}
