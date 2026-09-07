package com.kitchentwenty2.ui.navigation

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kitchentwenty2.domain.model.DashboardNavigationItem
import com.kitchentwenty2.ui.screens.dashboard.DashboardScreen
import com.kitchentwenty2.ui.screens.dashboard.DashboardViewModel
import com.kitchentwenty2.ui.screens.expense.ExpenseEntryScreen
import com.kitchentwenty2.ui.screens.expense.ExpenseEntryViewModel
import com.kitchentwenty2.ui.screens.menu.MenuSetupScreen
import com.kitchentwenty2.ui.screens.menu.MenuSetupViewModel
import com.kitchentwenty2.ui.screens.order.OrderCreateEditScreen
import com.kitchentwenty2.ui.screens.order.OrderCreateEditViewModel
import com.kitchentwenty2.ui.screens.order.OrderDetailScreen
import com.kitchentwenty2.ui.screens.order.OrderDetailViewModel
import com.kitchentwenty2.util.DateTimeUtils

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object CreateOrder : Screen("order/create")
    data object EditOrder : Screen("order/edit/{orderId}") {
        fun createRoute(orderId: Long) = "order/edit/$orderId"
    }
    data object OrderDetail : Screen("order/detail/{orderId}") {
        fun createRoute(orderId: Long) = "order/detail/$orderId"
    }
    data object MenuSetup : Screen("menu/setup")
    data object AddExpense : Screen("expense/add")
}

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current

    val dialPhone: (String) -> Unit = { phone ->
        if (phone.isNotBlank()) {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${phone.replace(" ", "")}")
            }
            context.startActivity(intent)
        }
    }

    val openMaps: (String?) -> Unit = { mapUrl ->
        if (!mapUrl.isNullOrBlank()) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(mapUrl))
            context.startActivity(intent)
        }
    }

    val shareLocation: (String, String) -> Unit = { address, url ->
        val text = "Delivery Location:\nAddress: $address\nGoogle Maps: $url"
        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(intent, "Share Delivery Info"))
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier
    ) {
        // Screen 1: Dashboard / Home Screen
        composable(Screen.Dashboard.route) {
            val viewModel: DashboardViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()

            DashboardScreen(
                uiState = uiState,
                onOrderClick = { orderId ->
                    navController.navigate(Screen.OrderDetail.createRoute(orderId))
                },
                onDialCustomer = dialPhone,
                onOpenMaps = openMaps,
                onShareLocation = { order ->
                    shareLocation(order.deliveryAddress, order.googleLocationUrl ?: "")
                },
                onNewOrderClick = {
                    navController.navigate(Screen.CreateOrder.route)
                },
                onNewExpenseClick = {
                    navController.navigate(Screen.AddExpense.route)
                },
                onNavigate = { destination ->
                    when (destination) {
                        DashboardNavigationItem.HOME -> { /* Already on dashboard */ }
                        DashboardNavigationItem.MASTER_MENU -> navController.navigate(Screen.MenuSetup.route)
                        DashboardNavigationItem.REPORTS -> { /* Reports placeholder */ }
                    }
                },
                onDateSelected = { millis ->
                    viewModel.onDateSelected(millis)
                }
            )
        }

        // Screen 2: Create Order
        composable(Screen.CreateOrder.route) {
            val viewModel: OrderCreateEditViewModel = hiltViewModel()
            OrderCreateEditScreen(
                orderIdToEdit = null,
                onBackClick = { navController.popBackStack() },
                onSaveSuccess = {
                    navController.popBackStack()
                },
                onTestDial = dialPhone,
                onOpenMaps = { url -> openMaps(url) },
                onShareLocation = shareLocation
            )
        }

        // Screen 2: Edit Order
        composable(
            route = Screen.EditOrder.route,
            arguments = listOf(navArgument("orderId") { type = NavType.LongType })
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getLong("orderId") ?: 0L
            val viewModel: OrderCreateEditViewModel = hiltViewModel()
            OrderCreateEditScreen(
                orderIdToEdit = orderId,
                onBackClick = { navController.popBackStack() },
                onSaveSuccess = { navController.popBackStack() },
                onTestDial = dialPhone,
                onOpenMaps = { url -> openMaps(url) },
                onShareLocation = shareLocation
            )
        }

        // Screen 3: Order Details & Settlement
        composable(
            route = Screen.OrderDetail.route,
            arguments = listOf(navArgument("orderId") { type = NavType.LongType })
        ) {
            val viewModel: OrderDetailViewModel = hiltViewModel()
            val orderState by viewModel.orderState.collectAsState()

            if (orderState != null) {
                OrderDetailScreen(
                    orderState = orderState!!,
                    onBackClick = { navController.popBackStack() },
                    onCallCustomer = dialPhone,
                    onOpenNavigation = { url -> openMaps(url) },
                    onShareToPartner = {
                        orderState?.let { s ->
                            shareLocation(s.customerAddress, s.googleLocationUrl)
                        }
                    },
                    onSettleSuccess = {
                        viewModel.settleOrder(orderState?.settlementDiscount ?: 0.0) {
                            navController.popBackStack()
                        }
                    },
                    onCancelOrderSuccess = { refundAmount ->
                        viewModel.cancelOrder(refundAmount) {
                            navController.popBackStack()
                        }
                    }
                )
            }
        }

        // Screen 4: Master Menu Setup
        composable(Screen.MenuSetup.route) {
            val viewModel: MenuSetupViewModel = hiltViewModel()
            MenuSetupScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        // Screen 5: Expense Entry Screen / Modal
        composable(Screen.AddExpense.route) {
            val viewModel: ExpenseEntryViewModel = hiltViewModel()
            ExpenseEntryScreen(
                onDismiss = { navController.popBackStack() },
                onSaveSuccess = { date, category, amount, notes ->
                    viewModel.saveExpense(
                        dateMillis = DateTimeUtils.parseDate(date),
                        category = category,
                        amount = amount,
                        notes = notes
                    ) {
                        navController.popBackStack()
                    }
                }
            )
        }
    }
}
