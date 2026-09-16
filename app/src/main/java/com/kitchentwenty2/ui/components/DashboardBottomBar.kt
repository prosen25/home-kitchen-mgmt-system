package com.kitchentwenty2.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kitchentwenty2.domain.model.DashboardNavigationItem

@Composable
fun DashboardBottomBar(
    selectedItem: DashboardNavigationItem,
    onItemSelected: (DashboardNavigationItem) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        // Destination 1: Home / Dashboard
        NavigationBarItem(
            selected = selectedItem == DashboardNavigationItem.HOME,
            onClick = { onItemSelected(DashboardNavigationItem.HOME) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Home / Dashboard"
                )
            },
            label = {
                Text(
                    text = "Dashboard",
                    fontWeight = if (selectedItem == DashboardNavigationItem.HOME) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            )
        )

        // Destination 2: Master Menu
        NavigationBarItem(
            selected = selectedItem == DashboardNavigationItem.MASTER_MENU,
            onClick = { onItemSelected(DashboardNavigationItem.MASTER_MENU) },
            icon = {
                Icon(
                    imageVector = Icons.Default.RestaurantMenu,
                    contentDescription = "Master Menu"
                )
            },
            label = {
                Text(
                    text = "Menu Setup",
                    fontWeight = if (selectedItem == DashboardNavigationItem.MASTER_MENU) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            )
        )

        // Destination 3: Customers
        NavigationBarItem(
            selected = selectedItem == DashboardNavigationItem.CUSTOMERS,
            onClick = { onItemSelected(DashboardNavigationItem.CUSTOMERS) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Groups,
                    contentDescription = "Customers"
                )
            },
            label = {
                Text(
                    text = "Customers",
                    fontWeight = if (selectedItem == DashboardNavigationItem.CUSTOMERS) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            )
        )

        // Destination 4: Profit & Loss reports
        NavigationBarItem(
            selected = selectedItem == DashboardNavigationItem.REPORTS,
            onClick = { onItemSelected(DashboardNavigationItem.REPORTS) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Assessment,
                    contentDescription = "Profit and Loss reports"
                )
            },
            label = {
                Text(
                    text = "Reports",
                    fontWeight = if (selectedItem == DashboardNavigationItem.REPORTS) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}
