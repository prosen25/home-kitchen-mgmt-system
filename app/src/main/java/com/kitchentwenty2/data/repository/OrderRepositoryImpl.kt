package com.kitchentwenty2.data.repository

import com.kitchentwenty2.data.local.dao.ExpenseDao
import com.kitchentwenty2.data.local.dao.OrderDao
import com.kitchentwenty2.data.local.entity.OrderEntity
import com.kitchentwenty2.data.local.entity.OrderItemEntity
import com.kitchentwenty2.data.local.entity.PaymentLogEntity
import com.kitchentwenty2.data.local.relation.OrderWithDetails
import com.kitchentwenty2.domain.model.FinancialSummary
import com.kitchentwenty2.domain.model.OrderDetailUiState
import com.kitchentwenty2.domain.model.OrderFormState
import com.kitchentwenty2.domain.model.OrderItemForm
import com.kitchentwenty2.domain.model.OrderStatus
import com.kitchentwenty2.domain.model.OrderSummaryItem
import com.kitchentwenty2.domain.model.PaymentRecord
import com.kitchentwenty2.domain.repository.CustomerRepository
import com.kitchentwenty2.domain.repository.OrderRepository
import com.kitchentwenty2.util.AppErrorLogger
import com.kitchentwenty2.util.DateTimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderRepositoryImpl @Inject constructor(
    private val orderDao: OrderDao,
    private val expenseDao: ExpenseDao,
    private val customerRepository: CustomerRepository,
    private val errorLogger: AppErrorLogger
) : OrderRepository {

    override fun getOrdersForDate(dateMillis: Long): Flow<List<OrderSummaryItem>> {
        val startOfDay = DateTimeUtils.getStartOfDay(dateMillis)
        val endOfDay = DateTimeUtils.getEndOfDay(dateMillis)
        return orderDao.getOrdersByDate(startOfDay, endOfDay)
            .map { list -> list.map { it.toSummaryItem() } }
            .flowOn(Dispatchers.IO)
    }

    override fun getOrderDetails(orderId: Long): Flow<OrderDetailUiState?> {
        return orderDao.getOrderWithDetailsById(orderId)
            .map { it?.toDetailUiState() }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun saveOrder(orderForm: OrderFormState): Long {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Auto-save / update customer profile (User Story 1.2 AC 3)
                val customerId = customerRepository.saveOrUpdateCustomer(
                    name = orderForm.customerName,
                    phone = orderForm.mobileNumber.ifBlank { null },
                    address = orderForm.address.ifBlank { null },
                    locationUrl = orderForm.googleLocationUrl.ifBlank { null }
                )

                val now = System.currentTimeMillis()
                val orderTimestamp = DateTimeUtils.parseDate(orderForm.orderDate)
                val netTotal = orderForm.netTotal
                val advance = orderForm.advancePayment

                val initialStatus = when {
                    advance >= netTotal && netTotal > 0 -> "FULLY_PAID"
                    advance > 0 -> "PARTIALLY_PAID"
                    else -> "UNPAID"
                }

                if (orderForm.isEditMode) {
                    val existingOrder = orderDao.getOrderById(orderForm.orderId)
                    if (existingOrder != null) {
                        val updated = existingOrder.copy(
                            customerName = orderForm.customerName.trim(),
                            customerPhone = orderForm.mobileNumber.ifBlank { null },
                            customerAddress = orderForm.address.ifBlank { null },
                            googleLocationUrl = orderForm.googleLocationUrl.ifBlank { null },
                            orderDate = orderTimestamp,
                            upfrontDiscount = orderForm.upfrontDiscount,
                            totalAmount = netTotal,
                            modifiedDateTimeStamp = now
                        )
                        orderDao.updateOrder(updated)
                        orderDao.deleteOrderItemsByOrderId(orderForm.orderId)
                        val items = orderForm.items.map { it.toEntity(orderForm.orderId) }
                        orderDao.insertOrderItems(items)
                        return@withContext orderForm.orderId
                    }
                }

                // New Order Creation
                val orderEntity = OrderEntity(
                    customerId = if (customerId > 0) customerId else null,
                    customerName = orderForm.customerName.trim(),
                    customerPhone = orderForm.mobileNumber.ifBlank { null },
                    customerAddress = orderForm.address.ifBlank { null },
                    googleLocationUrl = orderForm.googleLocationUrl.ifBlank { null },
                    orderDate = orderTimestamp,
                    upfrontDiscount = orderForm.upfrontDiscount,
                    settlementDiscount = 0.0,
                    advancePaid = advance,
                    totalCollected = advance,
                    refundedAmount = 0.0,
                    totalAmount = netTotal,
                    status = initialStatus,
                    createdDateTimeStamp = now,
                    modifiedDateTimeStamp = now
                )

                val newOrderId = orderDao.insertOrder(orderEntity)

                // Insert items
                val items = orderForm.items.map { it.toEntity(newOrderId) }
                orderDao.insertOrderItems(items)

                // Log Advance Payment if present
                if (advance > 0) {
                    orderDao.insertPaymentLog(
                        PaymentLogEntity(
                            orderId = newOrderId,
                            paymentDate = now,
                            amount = advance,
                            paymentType = "ADVANCE",
                            createdDateTimeStamp = now,
                            modifiedDateTimeStamp = now
                        )
                    )
                }

                newOrderId
            } catch (e: Exception) {
                errorLogger.logException(e, "OrderRepository.saveOrder")
                -1L
            }
        }
    }

    override suspend fun addIntermediatePayment(orderId: Long, amount: Double, paymentType: String) {
        withContext(Dispatchers.IO) {
            try {
                val order = orderDao.getOrderById(orderId) ?: return@withContext
                val now = System.currentTimeMillis()
                val newCollected = order.totalCollected + amount
                val newStatus = if (newCollected >= order.totalAmount) "FULLY_PAID" else "PARTIALLY_PAID"

                orderDao.updateOrder(
                    order.copy(
                        totalCollected = newCollected,
                        status = newStatus,
                        modifiedDateTimeStamp = now
                    )
                )

                orderDao.insertPaymentLog(
                    PaymentLogEntity(
                        orderId = orderId,
                        paymentDate = now,
                        amount = amount,
                        paymentType = paymentType,
                        createdDateTimeStamp = now,
                        modifiedDateTimeStamp = now
                    )
                )
            } catch (e: Exception) {
                errorLogger.logException(e, "OrderRepository.addIntermediatePayment")
            }
        }
    }

    override suspend fun settleOrder(orderId: Long, settlementDiscount: Double) {
        withContext(Dispatchers.IO) {
            try {
                val order = orderDao.getOrderById(orderId) ?: return@withContext
                val now = System.currentTimeMillis()
                val adjustedTotal = (order.totalAmount - settlementDiscount).coerceAtLeast(0.0)
                val finalDue = (adjustedTotal - order.totalCollected).coerceAtLeast(0.0)

                orderDao.updateOrder(
                    order.copy(
                        settlementDiscount = settlementDiscount,
                        totalAmount = adjustedTotal,
                        totalCollected = order.totalCollected + finalDue,
                        status = "FULLY_PAID",
                        modifiedDateTimeStamp = now
                    )
                )

                if (finalDue > 0) {
                    orderDao.insertPaymentLog(
                        PaymentLogEntity(
                            orderId = orderId,
                            paymentDate = now,
                            amount = finalDue,
                            paymentType = "SETTLEMENT",
                            createdDateTimeStamp = now,
                            modifiedDateTimeStamp = now
                        )
                    )
                }
            } catch (e: Exception) {
                errorLogger.logException(e, "OrderRepository.settleOrder")
            }
        }
    }

    override suspend fun cancelOrder(orderId: Long, refundAmount: Double) {
        withContext(Dispatchers.IO) {
            try {
                val order = orderDao.getOrderById(orderId) ?: return@withContext
                val now = System.currentTimeMillis()

                orderDao.updateOrder(
                    order.copy(
                        status = "CANCELLED",
                        refundedAmount = refundAmount,
                        modifiedDateTimeStamp = now
                    )
                )

                if (refundAmount > 0) {
                    orderDao.insertPaymentLog(
                        PaymentLogEntity(
                            orderId = orderId,
                            paymentDate = now,
                            amount = refundAmount,
                            paymentType = "REFUND",
                            createdDateTimeStamp = now,
                            modifiedDateTimeStamp = now
                        )
                    )
                }
            } catch (e: Exception) {
                errorLogger.logException(e, "OrderRepository.cancelOrder")
            }
        }
    }

    override fun getDailyFinancialSummary(dateMillis: Long): Flow<FinancialSummary> {
        val startOfDay = DateTimeUtils.getStartOfDay(dateMillis)
        val endOfDay = DateTimeUtils.getEndOfDay(dateMillis)

        return combine(
            orderDao.getDailyRevenueTotals(startOfDay, endOfDay),
            expenseDao.getTotalExpensesByDate(startOfDay, endOfDay)
        ) { revenueCalc, totalExpenses ->
            val netRevenue = (revenueCalc.totalCollected - revenueCalc.totalRefunded).coerceAtLeast(0.0)
            val netProfit = netRevenue - totalExpenses
            val isProjected = DateTimeUtils.isFuture(dateMillis)

            FinancialSummary(
                netRevenue = netRevenue,
                totalExpenses = totalExpenses,
                netProfit = netProfit,
                isProjected = isProjected
            )
        }.flowOn(Dispatchers.IO)
    }

    private fun OrderWithDetails.toSummaryItem(): OrderSummaryItem {
        val orderStatus = when (order.status) {
            "FULLY_PAID" -> OrderStatus.FULLY_PAID
            "PARTIALLY_PAID" -> OrderStatus.PARTIALLY_PAID
            "CANCELLED" -> OrderStatus.CANCELLED
            else -> OrderStatus.UNPAID
        }

        val itemsSummary = if (items.isNotEmpty()) {
            items.joinToString(", ") { "${it.quantity}x ${it.itemName}" }
        } else {
            "No items recorded"
        }

        val due = if (orderStatus == OrderStatus.CANCELLED) 0.0 else (order.totalAmount - order.totalCollected).coerceAtLeast(0.0)

        return OrderSummaryItem(
            orderId = order.orderId,
            customerName = order.customerName,
            customerPhone = order.customerPhone ?: "",
            itemsSummary = itemsSummary,
            deliveryAddress = order.customerAddress ?: "",
            googleLocationUrl = order.googleLocationUrl,
            totalAmount = order.totalAmount,
            outstandingDue = due,
            totalPaid = order.totalCollected,
            status = orderStatus
        )
    }

    private fun OrderWithDetails.toDetailUiState(): OrderDetailUiState {
        val orderStatus = when (order.status) {
            "FULLY_PAID" -> OrderStatus.FULLY_PAID
            "PARTIALLY_PAID" -> OrderStatus.PARTIALLY_PAID
            "CANCELLED" -> OrderStatus.CANCELLED
            else -> OrderStatus.UNPAID
        }

        val due = (order.totalAmount - order.totalCollected).coerceAtLeast(0.0)

        return OrderDetailUiState(
            orderId = order.orderId,
            status = orderStatus,
            orderDate = DateTimeUtils.formatDate(order.orderDate),
            isTodayOrPast = !DateTimeUtils.isFuture(order.orderDate),
            customerName = order.customerName,
            customerPhone = order.customerPhone ?: "",
            customerAddress = order.customerAddress ?: "",
            googleLocationUrl = order.googleLocationUrl ?: "",
            items = items.map { OrderItemForm(it.orderItemId, it.itemName, it.unitPrice, it.quantity) },
            upfrontDiscount = order.upfrontDiscount,
            totalAmount = order.totalAmount,
            totalCollected = order.totalCollected,
            currentRemainingDue = due,
            paymentLogs = paymentLogs.map { PaymentRecord(it.paymentId, DateTimeUtils.formatDate(it.paymentDate), it.amount, it.paymentType) },
            settlementDiscount = order.settlementDiscount
        )
    }

    private fun OrderItemForm.toEntity(orderId: Long) = OrderItemEntity(
        orderId = orderId,
        itemName = itemName,
        unitPrice = unitPrice,
        quantity = quantity,
        subtotal = subtotal,
        createdDateTimeStamp = System.currentTimeMillis(),
        modifiedDateTimeStamp = System.currentTimeMillis()
    )
}
