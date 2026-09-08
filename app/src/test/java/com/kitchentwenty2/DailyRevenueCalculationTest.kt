package com.kitchentwenty2

import com.kitchentwenty2.data.local.dao.DailyRevenueCalculation
import com.kitchentwenty2.data.repository.calculateNetRevenue
import org.junit.Assert.assertEquals
import org.junit.Test

class DailyRevenueCalculationTest {
    @Test
    fun `net revenue subtracts refunds from payments recorded for the day`() {
        assertEquals(
            75.0,
            calculateNetRevenue(
                DailyRevenueCalculation(
                    totalCollected = 125.0,
                    totalRefunded = 50.0
                )
            ),
            0.0
        )
    }

    @Test
    fun `net revenue does not become negative after refunds`() {
        assertEquals(
            0.0,
            calculateNetRevenue(
                DailyRevenueCalculation(
                    totalCollected = 25.0,
                    totalRefunded = 50.0
                )
            ),
            0.0
        )
    }
}