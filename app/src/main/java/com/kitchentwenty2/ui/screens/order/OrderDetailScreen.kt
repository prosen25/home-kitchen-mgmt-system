package com.kitchentwenty2.ui.screens.order

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kitchentwenty2.domain.model.OrderDetailUiState
import com.kitchentwenty2.domain.model.OrderStatus
import com.kitchentwenty2.domain.model.PaymentRecord
import com.kitchentwenty2.ui.components.OrderStatusBadge
import com.kitchentwenty2.ui.components.formatCurrency
import com.kitchentwenty2.ui.theme.KitchenTwenty2Theme
import com.kitchentwenty2.ui.theme.LossRed
import com.kitchentwenty2.ui.theme.ProfitGreen
import com.kitchentwenty2.ui.theme.ProfitGreenContainer
import com.kitchentwenty2.ui.theme.RevenueGreen
import com.kitchentwenty2.ui.theme.StatusCancelled
import com.kitchentwenty2.ui.theme.StatusFullyPaid
import com.kitchentwenty2.ui.theme.StatusPartiallyPaid
import com.kitchentwenty2.ui.theme.StatusUnpaid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    orderState: OrderDetailUiState = OrderDetailUiState(),
    onBackClick: () -> Unit = {},
    onCallCustomer: (String) -> Unit = {},
    onOpenNavigation: (String) -> Unit = {},
    onShareToPartner: () -> Unit = {},
    onAddIntermediatePayment: (Double, String) -> Unit = { _, _ -> },
    onSettleSuccess: (Double) -> Unit = {},
    onCancelOrderSuccess: (Double) -> Unit = {},
    onEditOrder: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var status by remember { mutableStateOf(orderState.status) }
    var totalCollected by remember { mutableDoubleStateOf(orderState.totalCollected) }
    var settlementDiscountInput by remember { mutableStateOf("0") }

    val paymentLogs = remember {
        mutableStateListOf<PaymentRecord>().apply {
            addAll(orderState.paymentLogs)
        }
    }

    // Intermediate payment dialog state
    var showPaymentDialog by remember { mutableStateOf(false) }
    var paymentAmountInput by remember { mutableStateOf("") }
    var paymentTypeInput by remember { mutableStateOf("Intermediate Payment") }
    var paymentError by remember { mutableStateOf<String?>(null) }
    var settlementDiscountError by remember { mutableStateOf<String?>(null) }

    // Cancel / Refund prompt dialog state
    var showCancelDialog by remember { mutableStateOf(false) }
    var refundAmountInput by remember { mutableStateOf("") }
    var refundError by remember { mutableStateOf<String?>(null) }

    val settlementDiscount by remember {
        derivedStateOf { settlementDiscountInput.toDoubleOrNull() ?: 0.0 }
    }
    val currentDue by remember {
        derivedStateOf {
            (orderState.totalAmount - totalCollected).coerceAtLeast(0.0)
        }
    }
    val newFinalDue by remember {
        derivedStateOf {
            (currentDue - settlementDiscount).coerceAtLeast(0.0)
        }
    }

    // Wireframe Rule: Settle button enabled only if Order Date <= Today
    val isSettleEnabled = orderState.isTodayOrPast && status != OrderStatus.FULLY_PAID && status != OrderStatus.CANCELLED

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Order Details (#${orderState.orderId})",
                            fontWeight = FontWeight.Bold
                        )
                        OrderStatusBadge(status = status)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (status != OrderStatus.CANCELLED) {
                        IconButton(onClick = { onEditOrder(orderState.orderId) }) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Order")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // -------------------------------------------------------------
            // Section 1: Customer & Delivery Action Card (NEW)
            // -------------------------------------------------------------
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = orderState.customerName,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = orderState.customerPhone,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // [ 📞 Call Customer ]
                            Button(
                                onClick = { onCallCustomer(orderState.customerPhone) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Call Customer",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Address
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = orderState.customerAddress,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(10.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Quick Navigation Actions: [ 🗺️ Open Navigation ] | [ 📤 Share to Partner ]
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = { onOpenNavigation(orderState.googleLocationUrl) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Open Navigation", style = MaterialTheme.typography.labelSmall)
                            }

                            OutlinedButton(
                                onClick = onShareToPartner,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share to Partner", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // Section 2: Order & Payment Progress
            // -------------------------------------------------------------
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Order Date", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(orderState.orderDate, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Financial Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Total Net Amount", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formatCurrency(orderState.totalAmount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Collected So Far", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formatCurrency(totalCollected), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = RevenueGreen)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Remaining Due", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    formatCurrency(currentDue),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (currentDue > 0) StatusUnpaid else StatusFullyPaid
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Progress bar
                        val progress = if (orderState.totalAmount > 0) (totalCollected / orderState.totalAmount).toFloat().coerceIn(0f, 1f) else 0f
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = RevenueGreen,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }

            // -------------------------------------------------------------
            // Section 3: Itemized Breakup & Payment Activity Log
            // -------------------------------------------------------------
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Itemized Breakup",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        orderState.items.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${item.quantity}x ${item.itemName}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = formatCurrency(item.subtotal),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        if (orderState.upfrontDiscount > 0) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Upfront Discount Applied",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = RevenueGreen
                                )
                                Text(
                                    text = "-${formatCurrency(orderState.upfrontDiscount)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = RevenueGreen
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(14.dp))

                        // Payment Log Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Payment Activity Log",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            // [ + Add Intermediate Payment ] Button
                            if (status != OrderStatus.CANCELLED && currentDue > 0) {
                                TextButton(onClick = { showPaymentDialog = true }) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("+ Add Payment", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        paymentLogs.forEach { log ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(log.type, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                        Text(log.date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(
                                        text = "+${formatCurrency(log.amount)}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = RevenueGreen
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // Section 4: Settlement Actions
            // -------------------------------------------------------------
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Settlement Actions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Settlement Discount Field
                        OutlinedTextField(
                            value = settlementDiscountInput,
                            onValueChange = {
                                settlementDiscountInput = it
                                settlementDiscountError = null
                            },
                            label = { Text("Courtesy / Settlement Discount (₹)") },
                            placeholder = { Text("0.00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            enabled = isSettleEnabled,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        settlementDiscountError?.let { error ->
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // New Final Due
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("New Final Due to Settle", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = formatCurrency(newFinalDue),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (newFinalDue > 0) StatusUnpaid else StatusFullyPaid
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Settlement Rule Explanation / Warning
                        if (!orderState.isTodayOrPast) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Settlement is locked for future dates. You can settle on or after the delivery date (${orderState.orderDate}).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // "Mark as Fully Paid & Settle" Button (Green)
                        Button(
                            onClick = {
                                when {
                                    !settlementDiscount.isFinite() || settlementDiscount < 0.0 -> {
                                        settlementDiscountError = "Enter a valid non-negative discount."
                                    }
                                    settlementDiscount > orderState.totalAmount -> {
                                        settlementDiscountError = "Discount cannot exceed the order total."
                                    }
                                    else -> {
                                        totalCollected += newFinalDue
                                        status = OrderStatus.FULLY_PAID
                                        paymentLogs.add(
                                            PaymentRecord(
                                                paymentId = System.currentTimeMillis(),
                                                date = "Today",
                                                amount = newFinalDue,
                                                type = "Final Settlement"
                                            )
                                    )
                                        settlementDiscountError = null
                                        onSettleSuccess(settlementDiscount)
                                    }
                                }
                            },
                            enabled = isSettleEnabled,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ProfitGreen,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (status == OrderStatus.FULLY_PAID) "Order Settled & Fully Paid" else "Mark as Fully Paid & Settle",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // Section 5: Danger Zone (Cancel Order & Full/Partial Refund)
            // -------------------------------------------------------------
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Danger Zone",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Cancel this order and record any full or partial refund to the customer.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = { showCancelDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            enabled = status != OrderStatus.CANCELLED,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (status == OrderStatus.CANCELLED) "Order Already Cancelled" else "Cancel Order & Process Refund",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }

    // Add Intermediate Payment Dialog
    if (showPaymentDialog) {
        AlertDialog(
            onDismissRequest = {
                showPaymentDialog = false
                paymentError = null
            },
            title = { Text("Record Payment", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Current remaining balance: ${formatCurrency(currentDue)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = paymentAmountInput,
                        onValueChange = {
                            paymentAmountInput = it
                            paymentError = null
                        },
                        label = { Text("Payment Amount (₹) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    paymentError?.let { error ->
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    OutlinedTextField(
                        value = paymentTypeInput,
                        onValueChange = { paymentTypeInput = it },
                        label = { Text("Payment Note / Type") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = paymentAmountInput.toDoubleOrNull() ?: 0.0
                        when {
                            !amount.isFinite() || amount <= 0.0 -> {
                                paymentError = "Enter a payment amount greater than zero."
                            }
                            amount > currentDue -> {
                                paymentError = "Payment cannot exceed the outstanding balance."
                            }
                            else -> {
                            totalCollected += amount
                            paymentLogs.add(
                                PaymentRecord(
                                    paymentId = System.currentTimeMillis(),
                                    date = "Today",
                                    amount = amount,
                                    type = paymentTypeInput.ifBlank { "Intermediate Payment" }
                                )
                            )
                            onAddIntermediatePayment(amount, paymentTypeInput.ifBlank { "Intermediate Payment" })
                            if (totalCollected >= orderState.totalAmount) {
                                status = OrderStatus.FULLY_PAID
                            } else {
                                status = OrderStatus.PARTIALLY_PAID
                            }
                            showPaymentDialog = false
                            paymentAmountInput = ""
                            paymentError = null
                            }
                        }
                    }
                ) {
                    Text("Save Payment")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPaymentDialog = false
                    paymentError = null
                }) { Text("Cancel") }
            }
        )
    }

    // Cancel Order & Refund Dialog
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = {
                showCancelDialog = false
                refundError = null
            },
            title = { Text("Cancel Order #${orderState.orderId}", fontWeight = FontWeight.Bold, color = LossRed) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Total collected cash: ${formatCurrency(totalCollected)}. Enter refund amount to return to customer (₹0 to ${formatCurrency(totalCollected)}):",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = refundAmountInput,
                        onValueChange = {
                            refundAmountInput = it
                            refundError = null
                        },
                        label = { Text("Refund Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    refundError?.let { error ->
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val refund = refundAmountInput.toDoubleOrNull() ?: 0.0
                        when {
                            !refund.isFinite() || refund < 0.0 -> {
                                refundError = "Enter a valid non-negative refund amount."
                            }
                            refund > totalCollected -> {
                                refundError = "Refund cannot exceed the amount collected."
                            }
                            else -> {
                                status = OrderStatus.CANCELLED
                                showCancelDialog = false
                                refundError = null
                                onCancelOrderSuccess(refund)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LossRed)
                ) {
                    Text("Confirm Cancellation")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCancelDialog = false
                    refundError = null
                }) { Text("Dismiss") }
            }
        )
    }
}

@Preview(name = "Order Detail Screen", showBackground = true)
@Composable
fun OrderDetailScreenPreview() {
    KitchenTwenty2Theme {
        OrderDetailScreen()
    }
}

