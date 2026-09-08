package com.kitchentwenty2.domain.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val date: String = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()),
    val category: String = "Groceries",
    val amount: String = "",
    val notes: String = ""
)

