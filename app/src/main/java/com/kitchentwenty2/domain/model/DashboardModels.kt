package com.kitchentwenty2.domain.model

enum class OrderStatus(val displayName: String) {
    UNPAID("Unpaid"),
    PARTIALLY_PAID("Partially Paid"),
    FULLY_PAID("Fully Paid"),
    CANCELLED("Cancelled")
}

data class FinancialSummary(
    val netRevenue: Double,
    val totalExpenses: Double,
    val netProfit: Double,
    val isProjected: Boolean = false // If viewing future date, shows "Expected Revenue"
)

data class OrderSummaryItem(
    val orderId: Long,
    val customerName: String,
    val customerPhone: String,
    val itemsSummary: String,
    val deliveryAddress: String = "",
    val googleLocationUrl: String? = null,
    val totalAmount: Double,
    val outstandingDue: Double,
    val totalPaid: Double,
    val status: OrderStatus
)

data class ExpenseSummaryItem(
    val expenseId: Long,
    val category: String, // Groceries, Packaging, Gas, Other
    val amount: Double,
    val note: String,
    val expenseDateMillis: Long = System.currentTimeMillis(),
    val timeFormatted: String = ""
)

enum class DashboardTab(val title: String) {
    ORDERS("Orders"),
    EXPENSES("Expenses")
}

enum class DashboardNavigationItem(val title: String) {
    HOME("Home"),
    MASTER_MENU("Master Menu"),
    REPORTS("Reports / History")
}

data class DashboardUiState(
    val displayDate: String = "Today (07/09/2026)",
    val formattedDate: String = "07/09/2026",
    val isToday: Boolean = true,
    val isFutureDate: Boolean = false,
    val financialSummary: FinancialSummary = FinancialSummary(
        netRevenue = 4250.0,
        totalExpenses = 1480.0,
        netProfit = 2770.0,
        isProjected = false
    ),
    val selectedTab: DashboardTab = DashboardTab.ORDERS,
    val orders: List<OrderSummaryItem> = listOf(
        OrderSummaryItem(
            orderId = 101,
            customerName = "Jane Doe",
            customerPhone = "+91 98765 43210",
            itemsSummary = "2x Chicken Biryani, 1x Special Raita",
            deliveryAddress = "Flat 402, Green Valley Apts, Indiranagar",
            googleLocationUrl = "https://maps.google.com/?q=12.9716,77.5946",
            totalAmount = 650.0,
            outstandingDue = 250.0,
            totalPaid = 400.0,
            status = OrderStatus.PARTIALLY_PAID
        ),
        OrderSummaryItem(
            orderId = 102,
            customerName = "Rahul Sharma",
            customerPhone = "+91 91234 56780",
            itemsSummary = "3x Veg Fried Rice, 2x Paneer Butter Masala",
            deliveryAddress = "House #12, 4th Cross, Koramangala",
            googleLocationUrl = "https://maps.google.com/?q=12.9352,77.6245",
            totalAmount = 1100.0,
            outstandingDue = 0.0,
            totalPaid = 1100.0,
            status = OrderStatus.FULLY_PAID
        ),
        OrderSummaryItem(
            orderId = 103,
            customerName = "Ananya Sen",
            customerPhone = "+91 99887 76655",
            itemsSummary = "1x Family Biryani Pot, 4x Gulab Jamun",
            deliveryAddress = "Villa 9, Palm Meadows, Whitefield",
            googleLocationUrl = "https://maps.google.com/?q=12.9698,77.7500",
            totalAmount = 1450.0,
            outstandingDue = 1450.0,
            totalPaid = 0.0,
            status = OrderStatus.UNPAID
        ),
        OrderSummaryItem(
            orderId = 104,
            customerName = "Vikram Patel",
            customerPhone = "+91 94433 22110",
            itemsSummary = "2x Mutton Dum Biryani, 2x Mirchi Ka Salan",
            deliveryAddress = "Tower B, Prestige Lakeview",
            googleLocationUrl = "https://maps.google.com/?q=12.9560,77.7011",
            totalAmount = 1050.0,
            outstandingDue = 0.0,
            totalPaid = 0.0,
            status = OrderStatus.CANCELLED
        )
    ),
    val expenses: List<ExpenseSummaryItem> = listOf(
        ExpenseSummaryItem(
            expenseId = 1,
            category = "Groceries",
            amount = 850.0,
            note = "Basmati rice (10kg) and whole spices",
            expenseDateMillis = System.currentTimeMillis(),
            timeFormatted = "09:30 AM"
        ),
        ExpenseSummaryItem(
            expenseId = 2,
            category = "Packaging",
            amount = 350.0,
            note = "50x 750ml meal containers & carry bags",
            expenseDateMillis = System.currentTimeMillis(),
            timeFormatted = "11:15 AM"
        ),
        ExpenseSummaryItem(
            expenseId = 3,
            category = "Gas",
            amount = 280.0,
            note = "Commercial gas cylinder refill share",
            expenseDateMillis = System.currentTimeMillis(),
            timeFormatted = "02:00 PM"
        )
    ),
    val selectedNavigationItem: DashboardNavigationItem = DashboardNavigationItem.HOME,
    val isAddModalOpen: Boolean = false,
    val showDatePicker: Boolean = false
)

