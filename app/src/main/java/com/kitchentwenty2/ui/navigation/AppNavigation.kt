package com.kitchentwenty2.ui.navigation

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import com.kitchentwenty2.domain.model.DashboardNavigationItem
import com.kitchentwenty2.ui.screens.auth.LoginScreen
import com.kitchentwenty2.ui.screens.auth.LoginViewModel
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
import com.kitchentwenty2.ui.screens.reports.ReportsHistoryScreen
import com.kitchentwenty2.ui.screens.reports.ReportsHistoryViewModel
import com.kitchentwenty2.util.DateTimeUtils

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Dashboard : Screen("dashboard")
    data object CreateOrder : Screen("order/create?defaultDateMillis={defaultDateMillis}") {
        fun createRoute(defaultDateMillis: Long? = null) =
            if (defaultDateMillis != null) "order/create?defaultDateMillis=$defaultDateMillis" else "order/create"
    }
    data object EditOrder : Screen("order/edit/{orderId}") {
        fun createRoute(orderId: Long) = "order/edit/$orderId"
    }
    data object OrderDetail : Screen("order/detail/{orderId}") {
        fun createRoute(orderId: Long) = "order/detail/$orderId"
    }
    data object MenuSetup : Screen("menu/setup")
    data object ReportsHistory : Screen("reports/history")
    data object AddExpense : Screen("expense/add?defaultDateMillis={defaultDateMillis}") {
        fun createRoute(defaultDateMillis: Long? = null) =
            if (defaultDateMillis != null) "expense/add?defaultDateMillis=$defaultDateMillis" else "expense/add"
    }
    data object EditExpense : Screen("expense/edit/{expenseId}") {
        fun createRoute(expenseId: Long) = "expense/edit/$expenseId"
    }
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

    val startDestination = if (FirebaseAuth.getInstance().currentUser != null) {
        Screen.Dashboard.route
    } else {
        Screen.Login.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Login.route) {
            val viewModel: LoginViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()

            LoginScreen(
                uiState = uiState,
                onEmailChanged = viewModel::onEmailChanged,
                onPasswordChanged = viewModel::onPasswordChanged,
                onSignInClick = viewModel::signInWithEmailPassword,
                onGoogleSignInResult = { idToken ->
                    if (idToken.isNotBlank()) {
                        viewModel.signInWithGoogle(idToken)
                    }
                }
            )

            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                LaunchedEffect(currentUser.uid) {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            }
        }

        // Screen 1: Dashboard / Home Screen
        composable(Screen.Dashboard.route) {
            if (FirebaseAuth.getInstance().currentUser == null) {
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                }
                return@composable
            }

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
                    navController.navigate(
                        Screen.CreateOrder.createRoute(viewModel.selectedDateMillis.value)
                    )
                },

                onNewExpenseClick = {
                    navController.navigate(
                        Screen.AddExpense.createRoute(viewModel.selectedDateMillis.value)
                    )
                },

                onNavigate = { destination ->
                    when (destination) {
                        DashboardNavigationItem.HOME -> { /* Already on dashboard */ }
                        DashboardNavigationItem.MASTER_MENU -> navController.navigate(Screen.MenuSetup.route)
                        DashboardNavigationItem.REPORTS -> navController.navigate(Screen.ReportsHistory.route)
                    }
                },
                onPreviousDayClick = viewModel::onPreviousDay,
                onNextDayClick = viewModel::onNextDay,
                onDateSelected = { millis ->
                    viewModel.onDateSelected(millis)
                },
                onExpenseEdit = { expenseId ->
                    navController.navigate(Screen.EditExpense.createRoute(expenseId))
                },
                onExpenseDelete = viewModel::deleteExpense,
                onOrderEdit = { orderId ->
                    navController.navigate(Screen.EditOrder.createRoute(orderId))
                }
            )
        }

        // Screen 2: Create Order
        composable(
            route = Screen.CreateOrder.route,
            arguments = listOf(
                navArgument("defaultDateMillis") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) {
            val viewModel: OrderCreateEditViewModel = hiltViewModel()
            val formState by viewModel.formState.collectAsState()
            val menuItems by viewModel.menuItems.collectAsState()
            val customerSuggestions by viewModel.customerSuggestions.collectAsState()

            OrderCreateEditScreen(
                formState = formState,
                menuList = menuItems,
                customerProfiles = customerSuggestions,
                onFormStateChanged = viewModel::updateFormState,
                onBackClick = { navController.popBackStack() },
                onSaveOrder = { viewModel.saveOrder { navController.popBackStack() } },
                onTestDial = dialPhone,
                onOpenMaps = { url -> openMaps(url) },
                onShareLocation = shareLocation
            )
        }

        // Screen 2: Edit Order
        composable(
            route = Screen.EditOrder.route,
            arguments = listOf(navArgument("orderId") { type = NavType.LongType })
        ) { _ ->
            val viewModel: OrderCreateEditViewModel = hiltViewModel()
            val formState by viewModel.formState.collectAsState()
            val menuItems by viewModel.menuItems.collectAsState()
            val customerSuggestions by viewModel.customerSuggestions.collectAsState()

            OrderCreateEditScreen(
                formState = formState,
                menuList = menuItems,
                customerProfiles = customerSuggestions,
                onFormStateChanged = viewModel::updateFormState,
                onBackClick = { navController.popBackStack() },
                onSaveOrder = { viewModel.saveOrder { navController.popBackStack() } },
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
                    onAddIntermediatePayment = { amount, paymentType ->
                        viewModel.addIntermediatePayment(amount, paymentType)
                    },
                    onSettleSuccess = { settlementDiscount ->
                        viewModel.settleOrder(settlementDiscount) {
                            navController.popBackStack()
                        }
                    },
                    onCancelOrderSuccess = { refundAmount ->
                        viewModel.cancelOrder(refundAmount) {
                            navController.popBackStack()
                        }
                    },
                    onEditOrder = { orderId ->
                        navController.navigate(Screen.EditOrder.createRoute(orderId))
                    }
                )
            }
        }

        // Screen 4: Master Menu Setup
        composable(Screen.MenuSetup.route) {
            val viewModel: MenuSetupViewModel = hiltViewModel()
            val menuItems by viewModel.menuItems.collectAsState()

            MenuSetupScreen(
                menuItems = menuItems,
                onAddMenuItem = viewModel::addMenuItem,
                onUpdateMenuItem = viewModel::updateMenuItem,
                onDeleteMenuItem = viewModel::deleteMenuItem,
                onBackClick = { navController.popBackStack() }
            )
        }

        // Screen 5: Reports / Order History
        composable(Screen.ReportsHistory.route) {
            val viewModel: ReportsHistoryViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()

            ReportsHistoryScreen(
                uiState = uiState,
                onBackClick = { navController.popBackStack() },
                onPreviousDayClick = viewModel::onPreviousDay,
                onNextDayClick = viewModel::onNextDay,
                onDateSelected = viewModel::onDateSelected,
                onOrderClick = { orderId ->
                    navController.navigate(Screen.OrderDetail.createRoute(orderId))
                }
            )
        }

        // Screen 6: Expense Entry Screen / Modal
        composable(
            route = Screen.AddExpense.route,
            arguments = listOf(
                navArgument("defaultDateMillis") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val viewModel: ExpenseEntryViewModel = hiltViewModel()
            val defaultDateMillis = backStackEntry.arguments
                ?.getLong("defaultDateMillis")
                ?.takeIf { it > 0L }

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
                },
                initialDateMillis = defaultDateMillis
            )
        }

        // Screen 7: Edit Expense
        composable(
            route = Screen.EditExpense.route,
            arguments = listOf(navArgument("expenseId") { type = NavType.LongType })
        ) { backStackEntry ->
            val expenseId = backStackEntry.arguments?.getLong("expenseId") ?: 0L
            val viewModel: ExpenseEntryViewModel = hiltViewModel()

            // Load existing expense and pass initial values to the screen
            var initialLoaded by remember { mutableStateOf(false) }
            var initialDateMillis by remember { mutableStateOf<Long?>(null) }
            var initialCategory by remember { mutableStateOf<String?>(null) }
            var initialAmount by remember { mutableStateOf<Double?>(null) }
            var initialNotes by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(expenseId) {
                val ex = viewModel.getExpenseById(expenseId)
                ex?.let {
                    initialDateMillis = it.expenseDateMillis
                    initialCategory = it.category
                    initialAmount = it.amount
                    initialNotes = it.note
                }
                initialLoaded = true
            }

            if (initialLoaded) {
                ExpenseEntryScreen(
                    onDismiss = { navController.popBackStack() },
                    onSaveSuccess = { date, category, amount, notes ->
                        viewModel.updateExpense(
                            dateMillis = DateTimeUtils.parseDate(date),
                            category = category,
                            amount = amount,
                            notes = notes,
                            expenseId = expenseId
                        ) {
                            navController.popBackStack()
                        }
                    },
                    initialExpenseId = expenseId,
                    initialDateMillis = initialDateMillis,
                    initialCategory = initialCategory,
                    initialAmount = initialAmount,
                    initialNotes = initialNotes
                )
            }
        }
    }
}
