package com.kitchentwenty2.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class OrderRepositoryImplEditStatusTest {

    @Test
    fun `edit keeps payment status aligned with persisted collection totals`() {
        assertEquals("FULLY_PAID", deriveOrderStatusForEdit("PARTIALLY_PAID", 250.0, 220.0))
        assertEquals("PARTIALLY_PAID", deriveOrderStatusForEdit("PARTIALLY_PAID", 120.0, 220.0))
        assertEquals("CANCELLED", deriveOrderStatusForEdit("CANCELLED", 250.0, 220.0))
        assertEquals("UNPAID", deriveOrderStatusForEdit("UNPAID", 0.0, 220.0))
    }
}
