package com.kitchentwenty2.ui.screens.order

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.unit.sp
import com.kitchentwenty2.domain.model.CustomerProfile
import com.kitchentwenty2.domain.model.MenuItemModel
import com.kitchentwenty2.domain.model.OrderFormState
import com.kitchentwenty2.domain.model.OrderItemForm
import com.kitchentwenty2.domain.model.SampleCustomers
import com.kitchentwenty2.domain.model.SampleMenuItems
import com.kitchentwenty2.ui.components.formatCurrency
import com.kitchentwenty2.ui.theme.KitchenTwenty2Theme
import com.kitchentwenty2.ui.theme.OrangePrimary
import com.kitchentwenty2.ui.theme.RevenueGreen
import com.kitchentwenty2.ui.theme.StatusUnpaid
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OrderCreateEditScreen(
    orderIdToEdit: Long? = null,
    menuList: List<MenuItemModel> = SampleMenuItems,
    customerProfiles: List<CustomerProfile> = SampleCustomers,
    onBackClick: () -> Unit = {},
    onSaveOrder: (OrderFormState) -> Unit = {},
    onSaveSuccess: () -> Unit = {},
    onTestDial: (String) -> Unit = {},
    onOpenMaps: (String) -> Unit = {},
    onShareLocation: (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val isEditMode = orderIdToEdit != null

    // Form fields state
    var orderDate by remember { mutableStateOf("07/09/2026") }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())

    // Customer info state
    var customerName by remember { mutableStateOf(if (isEditMode) "Jane Doe" else "") }
    var mobileNumber by remember { mutableStateOf(if (isEditMode) "+91 98765 43210" else "") }
    var deliveryAddress by remember {
        mutableStateOf(if (isEditMode) "Flat 402, Green Valley Apts, Indiranagar, Bengaluru" else "")
    }
    var googleLocationUrl by remember {
        mutableStateOf(if (isEditMode) "https://maps.google.com/?q=12.9716,77.5946" else "")
    }

    // Autocomplete dropdown state
    var isAutoCompleteExpanded by remember { mutableStateOf(false) }
    val matchingCustomers = remember(customerName, customerProfiles) {
        if (customerName.isNotBlank()) {
            customerProfiles.filter { it.name.contains(customerName, ignoreCase = true) }
        } else emptyList()
    }

    // Items list state
    val orderItems = remember {
        mutableStateListOf<OrderItemForm>().apply {
            if (isEditMode) {
                add(OrderItemForm(1, "Chicken Dum Biryani", 260.0, 2))
                add(OrderItemForm(2, "Special Raita", 60.0, 1))
            }
        }
    }

    // Dialog state for custom item
    var showCustomItemDialog by remember { mutableStateOf(false) }
    var customItemName by remember { mutableStateOf("") }
    var customItemPrice by remember { mutableStateOf("") }

    // Financial calculations state
    var upfrontDiscountInput by remember { mutableStateOf(if (isEditMode) "50" else "0") }
    var advancePaymentInput by remember { mutableStateOf(if (isEditMode) "200" else "0") }

    val subtotal by remember {
        derivedStateOf { orderItems.sumOf { it.subtotal } }
    }
    val upfrontDiscount by remember {
        derivedStateOf { upfrontDiscountInput.toDoubleOrNull() ?: 0.0 }
    }
    val advancePayment by remember {
        derivedStateOf { advancePaymentInput.toDoubleOrNull() ?: 0.0 }
    }
    val netTotal by remember {
        derivedStateOf { (subtotal - upfrontDiscount).coerceAtLeast(0.0) }
    }
    val remainingDue by remember {
        derivedStateOf { (netTotal - advancePayment).coerceAtLeast(0.0) }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditMode) "Edit Order #$orderIdToEdit" else "Create New Order",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            // Footer Bar: Save Order Button
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Balance Due",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatCurrency(remainingDue),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (remainingDue > 0) StatusUnpaid else RevenueGreen
                        )
                    }

                    Button(
                        onClick = {
                            val form = OrderFormState(
                                isEditMode = isEditMode,
                                orderId = orderIdToEdit ?: 0L,
                                orderDate = orderDate,
                                customerName = customerName,
                                mobileNumber = mobileNumber,
                                address = deliveryAddress,
                                googleLocationUrl = googleLocationUrl,
                                items = orderItems.toList(),
                                upfrontDiscount = upfrontDiscount,
                                advancePayment = advancePayment
                            )
                            onSaveOrder(form)
                            onSaveSuccess()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OrangePrimary
                        ),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isEditMode) "Update Order" else "Save Order",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
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
            // Section 1: Basic Information & Order Date
            // -------------------------------------------------------------
            item {
                SectionCard(title = "1. Order Date") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { showDatePicker = true }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Order Date",
                                tint = OrangePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Selected Date",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = orderDate,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        Text(
                            text = "Change",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = OrangePrimary
                        )
                    }
                }
            }

            // -------------------------------------------------------------
            // Section 2: Customer Contact & Location Details
            // -------------------------------------------------------------
            item {
                SectionCard(title = "2. Customer & Delivery Location") {
                    // Customer Name with Autocomplete
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = customerName,
                            onValueChange = {
                                customerName = it
                                isAutoCompleteExpanded = it.isNotBlank() && matchingCustomers.isNotEmpty()
                            },
                            label = { Text("Customer Name *") },
                            placeholder = { Text("e.g. Jane Doe (Type to auto-suggest)") },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = OrangePrimary)
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        // Autocomplete Dropdown
                        DropdownMenu(
                            expanded = isAutoCompleteExpanded,
                            onDismissRequest = { isAutoCompleteExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            matchingCustomers.forEach { profile ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(profile.name, fontWeight = FontWeight.Bold)
                                            Text(
                                                "${profile.mobileNumber} • ${profile.address.take(30)}...",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        customerName = profile.name
                                        mobileNumber = profile.mobileNumber
                                        deliveryAddress = profile.address
                                        googleLocationUrl = profile.googleLocationUrl ?: ""
                                        isAutoCompleteExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Mobile Number with Test Dial
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = mobileNumber,
                            onValueChange = { mobileNumber = it },
                            label = { Text("Mobile Number") },
                            placeholder = { Text("+91 98765 43210") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = { onTestDial(mobileNumber) },
                            enabled = mobileNumber.isNotBlank(),
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (mobileNumber.isNotBlank()) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Test Dial",
                                tint = if (mobileNumber.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Delivery Address
                    OutlinedTextField(
                        value = deliveryAddress,
                        onValueChange = { deliveryAddress = it },
                        label = { Text("Delivery Address") },
                        placeholder = { Text("House number, street name, landmarks...") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Google Location Tag Link
                    OutlinedTextField(
                        value = googleLocationUrl,
                        onValueChange = { googleLocationUrl = it },
                        label = { Text("Google Maps Link / GPS Coordinates") },
                        placeholder = { Text("Paste link from WhatsApp or Maps") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = OrangePrimary)
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Quick Map Actions
                    if (googleLocationUrl.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AssistChip(
                                onClick = { onOpenMaps(googleLocationUrl) },
                                label = { Text("Open in Maps") },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            )
                            AssistChip(
                                onClick = { onShareLocation(deliveryAddress, googleLocationUrl) },
                                label = { Text("Share via WhatsApp") },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                            )
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // Section 3: Order Items Selection
            // -------------------------------------------------------------
            item {
                SectionCard(title = "3. Order Items") {
                    Text(
                        text = "Quick Select from Menu:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Quick Select Chips
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        menuList.take(6).forEach { menuItem ->
                            AssistChip(
                                onClick = {
                                    val existingIndex = orderItems.indexOfFirst { it.itemName == menuItem.name }
                                    if (existingIndex >= 0) {
                                        val existing = orderItems[existingIndex]
                                        orderItems[existingIndex] = existing.copy(quantity = existing.quantity + 1)
                                    } else {
                                        orderItems.add(OrderItemForm(
                                            id = menuItem.menuItemId,
                                            itemName = menuItem.name,
                                            unitPrice = menuItem.defaultPrice,
                                            quantity = 1
                                        ))
                                    }
                                },
                                label = { Text("+ ${menuItem.name} (${formatCurrency(menuItem.defaultPrice)})", fontSize = 11.sp) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                ),
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Selected Items Table List
                    if (orderItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No items added yet. Tap a menu chip above or add a custom item.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            orderItems.forEachIndexed { index, item ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // Item Name & Unit Price
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.itemName,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "${formatCurrency(item.unitPrice)} each",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        // Quantity Stepper: [ - ] [ Count ] [ + ]
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    if (item.quantity > 1) {
                                                        orderItems[index] = item.copy(quantity = item.quantity - 1)
                                                    } else {
                                                        orderItems.removeAt(index)
                                                    }
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
                                            }

                                            Text(
                                                text = "${item.quantity}",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp)
                                            )

                                            IconButton(
                                                onClick = {
                                                    orderItems[index] = item.copy(quantity = item.quantity + 1)
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp))
                                            }
                                        }

                                        // Subtotal
                                        Text(
                                            text = formatCurrency(item.subtotal),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(start = 12.dp)
                                        )

                                        // Trash Icon
                                        IconButton(
                                            onClick = { orderItems.removeAt(index) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "Remove",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Button: [ + Add Custom / Typed Item ]
                    OutlinedButton(
                        onClick = { showCustomItemDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Custom / Typed Item")
                    }
                }
            }

            // -------------------------------------------------------------
            // Section 4: Financial Calculations & Payment
            // -------------------------------------------------------------
            item {
                SectionCard(title = "4. Financial Calculations & Payment") {
                    // Subtotal
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Subtotal", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatCurrency(subtotal), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Upfront Discount Field
                    OutlinedTextField(
                        value = upfrontDiscountInput,
                        onValueChange = { upfrontDiscountInput = it },
                        label = { Text("Upfront Discount (₹)") },
                        placeholder = { Text("0.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Net Total
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Net Total (after discount)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(formatCurrency(netTotal), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = OrangePrimary)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Advance Payment Field
                    OutlinedTextField(
                        value = advancePaymentInput,
                        onValueChange = { advancePaymentInput = it },
                        label = { Text("Advance Payment Collected (₹)") },
                        placeholder = { Text("0.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Remaining Balance Due
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Remaining Balance Due", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            text = formatCurrency(remainingDue),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (remainingDue > 0) StatusUnpaid else RevenueGreen
                        )
                    }
                }
            }
        }
    }

    // Custom Item Dialog
    if (showCustomItemDialog) {
        AlertDialog(
            onDismissRequest = { showCustomItemDialog = false },
            title = { Text("Add Custom Item", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = customItemName,
                        onValueChange = { customItemName = it },
                        label = { Text("Item / Dish Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = customItemPrice,
                        onValueChange = { customItemPrice = it },
                        label = { Text("Price per unit (₹) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val price = customItemPrice.toDoubleOrNull() ?: 0.0
                        if (customItemName.isNotBlank() && price > 0) {
                            orderItems.add(OrderItemForm(
                                itemName = customItemName,
                                unitPrice = price,
                                quantity = 1
                            ))
                            customItemName = ""
                            customItemPrice = ""
                            showCustomItemDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                ) {
                    Text("Add Item")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomItemDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Date Picker Dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            orderDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(millis))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Preview(name = "Create Order Screen", showBackground = true)
@Composable
fun OrderCreateEditScreenPreview() {
    KitchenTwenty2Theme {
        OrderCreateEditScreen()
    }
}

