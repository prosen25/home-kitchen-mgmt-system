package com.kitchentwenty2.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.kitchentwenty2.data.local.entity.OrderEntity
import com.kitchentwenty2.data.local.entity.OrderItemEntity
import com.kitchentwenty2.data.local.entity.PaymentLogEntity
import com.kitchentwenty2.data.local.relation.OrderWithDetails
import kotlinx.coroutines.flow.Flow

data class DailyRevenueCalculation(
    val totalCollected: Double,
    val totalRefunded: Double
)

@Dao
interface OrderDao {
    @Transaction
    @Query("SELECT * FROM orders WHERE orderDate >= :startOfDay AND orderDate <= :endOfDay ORDER BY orderId DESC")
    fun getOrdersByDate(startOfDay: Long, endOfDay: Long): Flow<List<OrderWithDetails>>

    @Transaction
    @Query("SELECT * FROM orders WHERE orderId = :orderId LIMIT 1")
    fun getOrderWithDetailsById(orderId: Long): Flow<OrderWithDetails?>

    @Query("SELECT * FROM orders WHERE orderId = :orderId LIMIT 1")
    suspend fun getOrderById(orderId: Long): OrderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity): Long

    @Update
    suspend fun updateOrder(order: OrderEntity)

    @Transaction
    suspend fun updateOrderAndPaymentLog(order: OrderEntity, paymentLog: PaymentLogEntity?) {
        updateOrder(order)
        paymentLog?.let { insertPaymentLog(it) }
    }

    @Transaction
    suspend fun updateOrderWithItems(order: OrderEntity, items: List<OrderItemEntity>) {
        updateOrder(order)
        deleteOrderItemsByOrderId(order.orderId)
        insertOrderItems(items)
    }

    @Transaction
    suspend fun insertOrderWithItemsAndPayment(
        order: OrderEntity,
        items: List<OrderItemEntity>,
        paymentLog: PaymentLogEntity?
    ): Long {
        val orderId = insertOrder(order)
        insertOrderItems(items.map { it.copy(orderId = orderId) })
        paymentLog?.let { insertPaymentLog(it.copy(orderId = orderId)) }
        return orderId
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItems(items: List<OrderItemEntity>)

    @Query("DELETE FROM order_items WHERE orderId = :orderId")
    suspend fun deleteOrderItemsByOrderId(orderId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaymentLog(paymentLog: PaymentLogEntity): Long

    @Query("SELECT * FROM payment_logs WHERE orderId = :orderId ORDER BY paymentDate ASC")
    fun getPaymentLogsForOrder(orderId: Long): Flow<List<PaymentLogEntity>>

    @Query("""
        SELECT 
            COALESCE(SUM(totalCollected), 0.0) as totalCollected,
            COALESCE(SUM(refundedAmount), 0.0) as totalRefunded
        FROM orders 
        WHERE orderDate >= :startOfDay AND orderDate <= :endOfDay
    """)
    fun getDailyRevenueTotals(startOfDay: Long, endOfDay: Long): Flow<DailyRevenueCalculation>

    @Query("SELECT COUNT(*) FROM orders")
    suspend fun getOrderCount(): Int
}
