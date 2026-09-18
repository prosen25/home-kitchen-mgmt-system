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
    ExpenseCategoryItem("bigbasket", "Bigbasket", "ShoppingCart"),
    ExpenseCategoryItem("blinkit", "Blinkit", "ShoppingCart"),
    ExpenseCategoryItem("fish", "Fish", "ShoppingCart"),
    ExpenseCategoryItem("flipkart", "Flipkart", "ShoppingCart"),
    ExpenseCategoryItem("gas", "Gas", "LocalGasStation"),
    ExpenseCategoryItem("groceries", "Groceries", "ShoppingCart"),
    ExpenseCategoryItem("helping_hand", "Helping Hand", "Payments"),
    ExpenseCategoryItem("packaging", "Packaging", "ReceiptLong"),
    ExpenseCategoryItem("other", "Other", "Payments"),
    ExpenseCategoryItem("vegetable", "Vegetable", "ShoppingCart"),
    ExpenseCategoryItem("zepto", "Zepto", "ShoppingCart")
)

data class ExpenseFormState(
    val date: String = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()),
    val category: String = "Groceries",
    val amount: String = "",
    val notes: String = ""
)
