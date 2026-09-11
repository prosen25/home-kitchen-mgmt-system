package com.kitchentwenty2.data.repository

import com.kitchentwenty2.data.local.dao.ExpenseDao
import com.kitchentwenty2.data.local.dao.OrderDao
import com.kitchentwenty2.data.local.entity.OrderEntity
import com.kitchentwenty2.data.local.entity.OrderItemEntity
import com.kitchentwenty2.data.local.entity.PaymentLogEntity
import com.kitchentwenty2.data.local.relation.OrderWithDetails
import com.kitchentwenty2.data.remote.firestore.FirestoreCustomer
import com.kitchentwenty2.data.remote.firestore.FirestoreOrder
import com.kitchentwenty2.data.remote.firestore.FirestoreOrderItem
import com.kitchentwenty2.data.remote.firestore.FirestoreOrderRepository
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

internal fun deriveOrderStatusForEdit(existingStatus: String, totalCollected: Double, netTotal: Double): String {
    if (existingStatus == "CANCELLED") return "CANCELLED"
    return when {
        totalCollected >= netTotal && netTotal > 0 -> "FULLY_PAID"
        totalCollected > 0 -> "PARTIALLY_PAID"
        else -> "UNPAID"
    }
}

internal fun calculateNetRevenue(revenueCalculation: com.kitchentwenty2.data.local.dao.DailyRevenueCalculation): Double {
    return (revenueCalculation.totalCollected - revenueCalculation.totalRefunded).coerceAtLeast(0.0)
}

internal fun resolvePaymentTimestampForOrder(orderDateMillis: Long, fallbackTimestamp: Long = System.currentTimeMillis()): Long {
    val todayStart = DateTimeUtils.getStartOfDay(System.currentTimeMillis())
    val orderStart = DateTimeUtils.getStartOfDay(orderDateMillis)
    return if (orderStart < todayStart) orderStart else fallbackTimestamp
}

@Singleton
class OrderRepositoryImpl @Inject constructor(
    private val orderDao: OrderDao,
    private val expenseDao: ExpenseDao,
    private val customerRepository: CustomerRepository,
    private val firestoreOrderRepository: FirestoreOrderRepository,
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
                val existingCustomer = customerRepository.findCustomerByMobileOrName(
                    mobile = orderForm.mobileNumber,
                    name = orderForm.customerName
                )
                val customerId = existingCustomer?.customerId ?: customerRepository.createCustomer(
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
                        // Calculate the difference between the new advance amount and previous advance amount
                        val advanceDelta = advance - existingOrder.advancePaid
                        val newTotalCollected = (existingOrder.totalCollected + advanceDelta).coerceAtLeast(0.0)
                        
                        val updated = existingOrder.copy(
                            customerId = customerId.takeIf { it > 0 } ?: existingOrder.customerId,
                            customerName = orderForm.customerName.trim(),
                            customerPhone = orderForm.mobileNumber.ifBlank { null },
                            customerAddress = orderForm.address.ifBlank { null },
                            googleLocationUrl = orderForm.googleLocationUrl.ifBlank { null },
                            orderDate = orderTimestamp,
                            upfrontDiscount = orderForm.upfrontDiscount,
                            totalAmount = netTotal,
                            settlementDiscount = existingOrder.settlementDiscount,
                            advancePaid = advance,
                            totalCollected = newTotalCollected,
                            refundedAmount = existingOrder.refundedAmount,
                            status = deriveOrderStatusForEdit(
                                existingStatus = existingOrder.status,
                                totalCollected = newTotalCollected,
                                netTotal = netTotal
                            ),
                            modifiedDateTimeStamp = now
                        )
                        val items = orderForm.items.map { it.toEntity(orderForm.orderId) }
                        orderDao.updateOrderWithItems(updated, items)

                        // Update or insert the corresponding ADVANCE payment log entry
                        if (advance > 0) {
                            val advancePaymentLog = PaymentLogEntity(
                                orderId = orderForm.orderId,
                                paymentDate = resolvePaymentTimestampForOrder(orderTimestamp, now),
                                amount = advanceDelta,
                                paymentType = "ADVANCE",
                                createdDateTimeStamp = now,
                                modifiedDateTimeStamp = now
                            )
                            // Ensure payment log record is inserted/updated in local DB
                            orderDao.insertPaymentLog(advancePaymentLog)
                        }

                        // If order date is today or in the past, adjust ADVANCE and SETTLEMENT payment dates to the order date
                        val orderStart = DateTimeUtils.getStartOfDay(orderTimestamp)
                        val todayStart = DateTimeUtils.getStartOfDay(System.currentTimeMillis())
                        if (orderStart <= todayStart) {
                            try {
                                orderDao.updatePaymentDateForType(orderForm.orderId, "ADVANCE", orderStart)
                                orderDao.updatePaymentDateForType(orderForm.orderId, "INTERMEDIATE", orderStart)
                                orderDao.updatePaymentDateForType(orderForm.orderId, "SETTLEMENT", orderStart)
                            } catch (e: Exception) {
                                errorLogger.logException(e, "OrderRepository.adjustPaymentDatesOnEdit")
                            }
                        }

                        orderDao.getOrderWithDetailsSnapshot(orderForm.orderId)?.let { syncOrderUpsert(it) }

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

                val items = orderForm.items.map { it.toEntity(0L) }
                val advancePaymentLog = if (advance > 0) {
                    PaymentLogEntity(
                        orderId = 0L,
                        paymentDate = resolvePaymentTimestampForOrder(orderTimestamp, now),
                        amount = advance,
                        paymentType = "ADVANCE",
                        createdDateTimeStamp = now,
                        modifiedDateTimeStamp = now
                    )
                } else {
                    null
                }

                val orderId = orderDao.insertOrderWithItemsAndPayment(orderEntity, items, advancePaymentLog)
                orderDao.getOrderWithDetailsSnapshot(orderId)?.let { syncOrderUpsert(it) }
                orderId
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
                val outstandingBalance = (order.totalAmount - order.totalCollected).coerceAtLeast(0.0)
                if (!amount.isFinite() || amount <= 0.0 || amount > outstandingBalance) {
                    return@withContext
                }
                val now = System.currentTimeMillis()
                val newCollected = order.totalCollected + amount
                val newStatus = if (newCollected >= order.totalAmount) "FULLY_PAID" else "PARTIALLY_PAID"
                val paymentTimestamp = resolvePaymentTimestampForOrder(order.orderDate, now)

                orderDao.updateOrderAndPaymentLog(
                    order = order.copy(
                        totalCollected = newCollected,
                        status = newStatus,
                        modifiedDateTimeStamp = now
                    ),
                    paymentLog = PaymentLogEntity(
                        orderId = orderId,
                        paymentDate = paymentTimestamp,
                        amount = amount,
                        paymentType = paymentType,
                        createdDateTimeStamp = now,
                        modifiedDateTimeStamp = now
                    )
                )
                orderDao.getOrderWithDetailsSnapshot(orderId)?.let { syncOrderUpsert(it) }
            } catch (e: Exception) {
                errorLogger.logException(e, "OrderRepository.addIntermediatePayment")
            }
        }
    }

    override suspend fun settleOrder(orderId: Long, settlementDiscount: Double) {
        withContext(Dispatchers.IO) {
            try {
                val order = orderDao.getOrderById(orderId) ?: return@withContext
                if (DateTimeUtils.isFuture(order.orderDate) ||
                    !settlementDiscount.isFinite() ||
                    settlementDiscount < 0.0 ||
                    settlementDiscount > order.totalAmount
                ) {
                    return@withContext
                }
                val now = System.currentTimeMillis()
                val adjustedTotal = (order.totalAmount - settlementDiscount).coerceAtLeast(0.0)
                val finalDue = (adjustedTotal - order.totalCollected).coerceAtLeast(0.0)
                val paymentTimestamp = resolvePaymentTimestampForOrder(order.orderDate, now)

                orderDao.updateOrderAndPaymentLog(
                    order = order.copy(
                        settlementDiscount = settlementDiscount,
                        totalAmount = adjustedTotal,
                        totalCollected = order.totalCollected + finalDue,
                        status = "FULLY_PAID",
                        modifiedDateTimeStamp = now
                    ),
                    paymentLog = if (finalDue > 0) {
                        PaymentLogEntity(
                            orderId = orderId,
                            paymentDate = paymentTimestamp,
                            amount = finalDue,
                            paymentType = "SETTLEMENT",
                            createdDateTimeStamp = now,
                            modifiedDateTimeStamp = now
                        )
                    } else {
                        null
                    }
                )
                orderDao.getOrderWithDetailsSnapshot(orderId)?.let { syncOrderUpsert(it) }
            } catch (e: Exception) {
                errorLogger.logException(e, "OrderRepository.settleOrder")
            }
        }
    }

    override suspend fun cancelOrder(orderId: Long, refundAmount: Double) {
        withContext(Dispatchers.IO) {
            try {
                val order = orderDao.getOrderById(orderId) ?: return@withContext
                if (!refundAmount.isFinite() || refundAmount < 0.0 || refundAmount > order.totalCollected) {
                    return@withContext
                }
                val now = System.currentTimeMillis()

                orderDao.updateOrderAndPaymentLog(
                    order = order.copy(
                        status = "CANCELLED",
                        refundedAmount = refundAmount,
                        modifiedDateTimeStamp = now
                    ),
                    paymentLog = if (refundAmount > 0) {
                        PaymentLogEntity(
                            orderId = orderId,
                            paymentDate = now,
                            amount = refundAmount,
                            paymentType = "REFUND",
                            createdDateTimeStamp = now,
                            modifiedDateTimeStamp = now
                        )
                    } else {
                        null
                    }
                )
                orderDao.getOrderWithDetailsSnapshot(orderId)?.let { syncOrderUpsert(it) }
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
            val netRevenue = calculateNetRevenue(revenueCalc)
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
            items = items.map { OrderItemForm(it.orderItemId, it.itemName, it.unitPrice, it.quantity, it.menuItemId, it.isCustom) },
            upfrontDiscount = order.upfrontDiscount,
            advancePaid = order.advancePaid,
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
        menuItemId = menuItemId,
        isCustom = isCustom,
        createdDateTimeStamp = System.currentTimeMillis(),
        modifiedDateTimeStamp = System.currentTimeMillis()
    )

    private suspend fun syncOrderUpsert(orderWithDetails: OrderWithDetails) {
        firestoreOrderRepository.createOrUpdateOrder(
            FirestoreOrder(
                id = orderWithDetails.order.orderId.toString(),
                customerId = orderWithDetails.order.customerId?.toString(),
                customerSnapshot = FirestoreCustomer(
                    id = orderWithDetails.order.customerId?.toString().orEmpty(),
                    name = orderWithDetails.order.customerName,
                    mobileNumber = orderWithDetails.order.customerPhone,
                    address = orderWithDetails.order.customerAddress,
                    googleLocationUrl = orderWithDetails.order.googleLocationUrl,
                    createdBy = orderWithDetails.customer?.createdBy ?: orderWithDetails.order.createdBy
                ),
                orderDate = orderWithDetails.order.orderDate,
                items = orderWithDetails.items.mapIndexed { index, item ->
                    FirestoreOrderItem(
                        itemId = item.orderItemId.takeIf { it > 0 }?.toString() ?: "${orderWithDetails.order.orderId}-$index",
                        itemName = item.itemName,
                        unitPrice = item.unitPrice,
                        quantity = item.quantity,
                        subtotal = item.subtotal
                    )
                },
                upfrontDiscount = orderWithDetails.order.upfrontDiscount,
                settlementDiscount = orderWithDetails.order.settlementDiscount,
                advancePaid = orderWithDetails.order.advancePaid,
                totalCollected = orderWithDetails.order.totalCollected,
                refundedAmount = orderWithDetails.order.refundedAmount,
                totalAmount = orderWithDetails.order.totalAmount,
                status = orderWithDetails.order.status,
                createdBy = orderWithDetails.order.createdBy
            )
        )
    }
}
