package com.kitchentwenty2.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.kitchentwenty2.data.local.entity.CustomerEntity
import com.kitchentwenty2.data.local.entity.OrderEntity
import com.kitchentwenty2.data.local.entity.OrderItemEntity
import com.kitchentwenty2.data.local.entity.PaymentLogEntity

data class OrderWithDetails(
    @Embedded
    val order: OrderEntity,

    @Relation(
        parentColumn = "customerId",
        entityColumn = "customerId"
    )
    val customer: CustomerEntity?,

    @Relation(
        parentColumn = "orderId",
        entityColumn = "orderId"
    )
    val items: List<OrderItemEntity>,

    @Relation(
        parentColumn = "orderId",
        entityColumn = "orderId"
    )
    val paymentLogs: List<PaymentLogEntity>
)
