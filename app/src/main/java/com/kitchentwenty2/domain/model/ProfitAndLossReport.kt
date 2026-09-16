package com.kitchentwenty2.domain.model

data class ProfitAndLossReport(
    val grossSales: Double = 0.0,
    val totalDiscounts: Double = 0.0,
    val totalRefunds: Double = 0.0,
    val netRevenue: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val netProfitLoss: Double = 0.0
)
