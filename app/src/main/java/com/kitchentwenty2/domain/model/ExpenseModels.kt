package com.kitchentwenty2.domain.model

data class ExpenseCategoryItem(
    val id: String,
    val name: String,
    val iconName: String
)

val ExpenseCategories = listOf(
    ExpenseCategoryItem("groceries", "Groceries", "ShoppingCart"),
    ExpenseCategoryItem("packaging", "Packaging", "ReceiptLong"),
    ExpenseCategoryItem("gas", "Gas", "LocalGasStation"),
    ExpenseCategoryItem("other", "Other", "Payments")
)

data class ExpenseFormState(
    val date: String = "07/09/2026",
    val category: String = "Groceries",
    val amount: String = "",
    val notes: String = ""
)

