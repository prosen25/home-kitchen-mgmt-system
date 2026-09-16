package com.kitchentwenty2.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kitchentwenty2.domain.model.FinancialSummary
import com.kitchentwenty2.ui.theme.ExpenseRed
import com.kitchentwenty2.ui.theme.ExpenseRedContainer
import com.kitchentwenty2.ui.theme.ExpenseRedOnContainer
import com.kitchentwenty2.ui.theme.OrderValueBlue
import com.kitchentwenty2.ui.theme.OrderValueBlueContainer
import com.kitchentwenty2.ui.theme.OrderValueBlueOnContainer
import com.kitchentwenty2.ui.theme.LossRed
import com.kitchentwenty2.ui.theme.LossRedContainer
import com.kitchentwenty2.ui.theme.LossRedOnContainer
import com.kitchentwenty2.ui.theme.ProfitGreen
import com.kitchentwenty2.ui.theme.ProfitGreenContainer
import com.kitchentwenty2.ui.theme.ProfitGreenOnContainer
import com.kitchentwenty2.ui.theme.RevenueGreen
import com.kitchentwenty2.ui.theme.RevenueGreenContainer
import com.kitchentwenty2.ui.theme.RevenueGreenOnContainer
import java.util.Locale

@Composable
fun FinancialSummaryCards(
    summary: FinancialSummary,
    modifier: Modifier = Modifier
) {
    val isProfit = summary.netProfit >= 0

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Gross value of valid orders, before collection and expense calculations.
        SummaryBox(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            title = "Total Order Value",
            subtitle = "Gross Bookings",
            amount = formatCurrency(summary.totalOrderValue),
            containerColor = OrderValueBlueContainer,
            contentColor = OrderValueBlueOnContainer,
            accentColor = OrderValueBlue,
            borderColor = OrderValueBlue.copy(alpha = 0.25f)
        )

        // Net Revenue (or "Expected Revenue" if future)
        SummaryBox(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            title = if (summary.isProjected) "Expected Rev." else "Net Revenue",
            subtitle = if (summary.isProjected) "Future Bookings" else "Collected - Refunds",
            amount = formatCurrency(summary.netRevenue),
            containerColor = RevenueGreenContainer,
            contentColor = RevenueGreenOnContainer,
            accentColor = RevenueGreen,
            borderColor = RevenueGreen.copy(alpha = 0.25f)
        )

        // Box 2 (Red): Total Expenses
        SummaryBox(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            title = "Expenses",
            subtitle = "Total Costs",
            amount = formatCurrency(summary.totalExpenses),
            containerColor = ExpenseRedContainer,
            contentColor = ExpenseRedOnContainer,
            accentColor = ExpenseRed,
            borderColor = ExpenseRed.copy(alpha = 0.25f)
        )

        // Box 3 (Bold): Net Profit / Loss
        val profitContainer = if (isProfit) ProfitGreenContainer else LossRedContainer
        val profitContent = if (isProfit) ProfitGreenOnContainer else LossRedOnContainer
        val profitAccent = if (isProfit) ProfitGreen else LossRed

        SummaryBox(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            title = if (isProfit) "Net Profit" else "Net Loss",
            subtitle = "Revenue - Expenses",
            amount = (if (isProfit && summary.netProfit > 0) "+" else "") + formatCurrency(summary.netProfit),
            containerColor = profitContainer,
            contentColor = profitContent,
            accentColor = profitAccent,
            borderColor = profitAccent.copy(alpha = 0.4f),
            isHighlighted = true
        )
    }
}

@Composable
private fun SummaryBox(
    title: String,
    subtitle: String,
    amount: String,
    containerColor: Color,
    contentColor: Color,
    accentColor: Color,
    borderColor: Color,
    isHighlighted: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isHighlighted) 2.dp else 0.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 5.dp, vertical = 8.dp)
            ) {
                Text(
                    text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                maxLines = 2
                )
                Text(
                    text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                    color = contentColor.copy(alpha = 0.7f),
                    maxLines = 1
                )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = amount,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (isHighlighted) FontWeight.ExtraBold else FontWeight.Bold,
                color = contentColor,
                maxLines = 1
            )
        }
    }
}

fun formatCurrency(value: Double): String {
    return String.format(Locale.getDefault(), "₹%,.0f", value)
}
