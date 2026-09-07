package com.kitchentwenty2.domain.model

data class CustomerProfile(
    val customerId: Long,
    val name: String,
    val mobileNumber: String,
    val address: String,
    val googleLocationUrl: String? = null
)

data class OrderItemForm(
    val id: Long = System.currentTimeMillis(),
    val itemName: String,
    val unitPrice: Double,
    val quantity: Int = 1
) {
    val subtotal: Double get() = unitPrice * quantity
}

data class PaymentRecord(
    val paymentId: Long,
    val date: String,
    val amount: Double,
    val type: String // Advance, Intermediate, Settlement, Refund
)

data class OrderFormState(
    val isEditMode: Boolean = false,
    val orderId: Long = 104,
    val orderDate: String = "07/09/2026",
    val customerName: String = "",
    val mobileNumber: String = "",
    val address: String = "",
    val googleLocationUrl: String = "",
    val items: List<OrderItemForm> = listOf(),
    val upfrontDiscount: Double = 0.0,
    val advancePayment: Double = 0.0
) {
    val subtotal: Double get() = items.sumOf { it.subtotal }
    val netTotal: Double get() = (subtotal - upfrontDiscount).coerceAtLeast(0.0)
    val remainingBalance: Double get() = (netTotal - advancePayment).coerceAtLeast(0.0)
}

data class OrderDetailUiState(
    val orderId: Long = 104,
    val status: OrderStatus = OrderStatus.PARTIALLY_PAID,
    val orderDate: String = "07/09/2026",
    val isTodayOrPast: Boolean = true,
    val customerName: String = "Jane Doe",
    val customerPhone: String = "+91 98765 43210",
    val customerAddress: String = "123 Main Street, Apt 4B, Indiranagar, Bengaluru",
    val googleLocationUrl: String = "https://maps.google.com/?q=12.9716,77.5946",
    val items: List<OrderItemForm> = listOf(
        OrderItemForm(1, "Chicken Biryani", 250.0, 2),
        OrderItemForm(2, "Special Raita", 60.0, 1),
        OrderItemForm(3, "Gulab Jamun (4 pcs)", 90.0, 1)
    ),
    val upfrontDiscount: Double = 50.0,
    val totalAmount: Double = 600.0, // (250*2 + 60 + 90) - 50 = 600
    val totalCollected: Double = 250.0,
    val currentRemainingDue: Double = 350.0,
    val paymentLogs: List<PaymentRecord> = listOf(
        PaymentRecord(1, "05/09/2026", 150.0, "Advance Payment"),
        PaymentRecord(2, "07/09/2026", 100.0, "Intermediate Payment")
    ),
    val settlementDiscount: Double = 0.0
) {
    val finalDueAfterSettlementDiscount: Double
        get() = (currentRemainingDue - settlementDiscount).coerceAtLeast(0.0)
}

// Sample Autocomplete Customers
val SampleCustomers = listOf(
    CustomerProfile(
        customerId = 1,
        name = "Jane Doe",
        mobileNumber = "+91 98765 43210",
        address = "123 Main Street, Apt 4B, Indiranagar, Bengaluru",
        googleLocationUrl = "https://maps.google.com/?q=12.9716,77.5946"
    ),
    CustomerProfile(
        customerId = 2,
        name = "Rahul Sharma",
        mobileNumber = "+91 91234 56780",
        address = "House #12, 4th Cross, Koramangala, Bengaluru",
        googleLocationUrl = "https://maps.google.com/?q=12.9352,77.6245"
    ),
    CustomerProfile(
        customerId = 3,
        name = "Ananya Sen",
        mobileNumber = "+91 99887 76655",
        address = "Villa 9, Palm Meadows, Whitefield, Bengaluru",
        googleLocationUrl = "https://maps.google.com/?q=12.9698,77.7500"
    ),
    CustomerProfile(
        customerId = 4,
        name = "Vikram Patel",
        mobileNumber = "+91 94433 22110",
        address = "Tower B, 11th Floor, Prestige Lakeview, Bengaluru",
        googleLocationUrl = "https://maps.google.com/?q=12.9560,77.7011"
    )
)
