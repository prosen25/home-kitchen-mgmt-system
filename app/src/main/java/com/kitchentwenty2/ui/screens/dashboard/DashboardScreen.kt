package com.kitchentwenty2.ui.screens.dashboard

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kitchentwenty2.domain.model.DashboardNavigationItem
import com.kitchentwenty2.domain.model.DashboardTab
import com.kitchentwenty2.domain.model.DashboardUiState
import com.kitchentwenty2.domain.model.OrderSummaryItem
import com.kitchentwenty2.ui.components.AddNewModalBottomSheet
import com.kitchentwenty2.ui.components.DashboardBottomBar
import com.kitchentwenty2.ui.components.ExpenseCard
import com.kitchentwenty2.ui.components.FinancialSummaryCards
import com.kitchentwenty2.ui.components.OrderCard
import com.kitchentwenty2.ui.components.TopBarDateSelector
import com.kitchentwenty2.ui.theme.KitchenTwenty2Theme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    uiState: DashboardUiState = DashboardUiState(),
    onOrderClick: (Long) -> Unit = {},
    onDialCustomer: (String) -> Unit = {},
    onOpenMaps: (String?) -> Unit = {},
    onShareLocation: (OrderSummaryItem) -> Unit = {},
    onNewOrderClick: () -> Unit = {},
    onNewExpenseClick: () -> Unit = {},
    onNavigate: (DashboardNavigationItem) -> Unit = {},
    onPreviousDayClick: () -> Unit = {},
    onNextDayClick: () -> Unit = {},
    onDateSelected: (Long?) -> Unit = {},
    // New callbacks for expense actions
    onExpenseEdit: (Long) -> Unit = {},
    onExpenseDelete: (Long) -> Unit = {},
    // New callback for order edit
    onOrderEdit: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Local interactive UI states for previewing and testing interactions
    var currentTab by remember { mutableStateOf(uiState.selectedTab) }
    var showAddSheet by remember { mutableStateOf(false) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var selectedNav by remember { mutableStateOf(uiState.selectedNavigationItem) }

    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopBarDateSelector(
                currentDateDisplay = uiState.displayDate,
                onPreviousDayClick = onPreviousDayClick,
                onNextDayClick = onNextDayClick,
                onDateClick = { showDatePickerDialog = true }
            )
        },
        bottomBar = {
            DashboardBottomBar(
                selectedItem = selectedNav,
                onItemSelected = { navItem ->
                    selectedNav = navItem
                    onNavigate(navItem)
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    showAddSheet = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add New"
                    )
                },
                text = {
                    Text(
                        text = "Add New",
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 1. Financial Summary Section (3-Box Layout)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                FinancialSummaryCards(summary = uiState.financialSummary)
            }

            // 2. Primary Tab Row: Orders (Count) | Expenses (Count)
            PrimaryTabRow(
                selectedTabIndex = currentTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Tab 1: Orders
                Tab(
                    selected = currentTab == DashboardTab.ORDERS,
                    onClick = { currentTab = DashboardTab.ORDERS },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(
                                text = "Orders",
                                fontWeight = if (currentTab == DashboardTab.ORDERS) FontWeight.Bold else FontWeight.Normal
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Badge(
                                containerColor = if (currentTab == DashboardTab.ORDERS)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (currentTab == DashboardTab.ORDERS)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            ) {
                                Text(
                                    text = "${uiState.orders.size}",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                )

                // Tab 2: Expenses
                Tab(
                    selected = currentTab == DashboardTab.EXPENSES,
                    onClick = { currentTab = DashboardTab.EXPENSES },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(
                                text = "Expenses",
                                fontWeight = if (currentTab == DashboardTab.EXPENSES) FontWeight.Bold else FontWeight.Normal
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Badge(
                                containerColor = if (currentTab == DashboardTab.EXPENSES)
                                    MaterialTheme.colorScheme.errorContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (currentTab == DashboardTab.EXPENSES)
                                    MaterialTheme.colorScheme.onErrorContainer
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            ) {
                                Text(
                                    text = "${uiState.expenses.size}",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                )
            }

            // 3. Tab Content List Area (Scrollable LazyColumn)
            when (currentTab) {
                DashboardTab.ORDERS -> {
                    if (uiState.orders.isEmpty()) {
                        EmptyStateView(
                            title = "No orders for this date",
                            subtitle = "Tap '+ Add New' to log your first order"
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = uiState.orders,
                                key = { it.orderId }
                            ) { order ->
                                OrderCard(
                                    order = order,
                                    onOrderClick = onOrderClick,
                                    onDialClick = onDialCustomer,
                                    onMapsClick = onOpenMaps,
                                    onShareLocationClick = onShareLocation,
                                    onEdit = { id -> onOrderEdit(id) }
                                )
                            }
                        }
                    }
                }

                DashboardTab.EXPENSES -> {
                    if (uiState.expenses.isEmpty()) {
                        EmptyStateView(
                            title = "No expenses recorded today",
                            subtitle = "Tap '+ Add New' to record ingredients or operational costs"
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(
                                items = uiState.expenses,
                                key = { it.expenseId }
                            ) { expense ->
                                ExpenseCard(expense = expense,
                                    onEdit = { onExpenseEdit(expense.expenseId) },
                                    onDelete = { onExpenseDelete(expense.expenseId) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet for FAB "Add New"
    if (showAddSheet) {
        AddNewModalBottomSheet(
            sheetState = bottomSheetState,
            onDismissRequest = { showAddSheet = false },
            onNewOrderClick = {
                coroutineScope.launch { bottomSheetState.hide() }.invokeOnCompletion {
                    showAddSheet = false
                    onNewOrderClick()
                }
            },
            onNewExpenseClick = {
                coroutineScope.launch { bottomSheetState.hide() }.invokeOnCompletion {
                    showAddSheet = false
                    onNewExpenseClick()
                }
            }
        )
    }

    // Date Picker Dialog triggered by Date Selector click
    if (showDatePickerDialog) {
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDatePickerDialog = false
                        onDateSelected(datePickerState.selectedDateMillis)
                    }
                ) {
                    Text("Select", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun EmptyStateView(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Inbox,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// -------------------------------------------------------------
// Jetpack Compose Previews
// -------------------------------------------------------------

@Preview(name = "Dashboard - Light Mode", showBackground = true)
@Composable
fun DashboardScreenLightPreview() {
    KitchenTwenty2Theme(darkTheme = false) {
        DashboardScreen()
    }
}

@Preview(name = "Dashboard - Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DashboardScreenDarkPreview() {
    KitchenTwenty2Theme(darkTheme = true) {
        DashboardScreen()
    }
}

