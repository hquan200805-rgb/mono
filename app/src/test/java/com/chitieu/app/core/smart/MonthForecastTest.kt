package com.chitieu.app.core.smart

import com.chitieu.app.core.model.Transaction
import com.chitieu.app.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class MonthForecastTest {

    @Test
    fun `projects remaining days`() {
        val zone = ZoneId.of("Asia/Ho_Chi_Minh")
        val today = LocalDate.of(2026, 3, 10)
        val start = today.withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()
        // 10 days * 100k = 1M already if one tx of 1M on day 1... simpler: one tx
        val txs = listOf(
            Transaction(
                id = 1,
                accountId = 1,
                amount = 1_000_000L,
                type = TransactionType.CHI,
                occurredAt = start + 1000
            )
        )
        val result = MonthForecast.forecast(txs, today, zone)
        assertEquals(1_000_000L, result.alreadySpent)
        assertEquals(10, result.daysPassed)
        assertTrue(result.projectedTotal >= result.alreadySpent)
        // daily avg = 100000, remaining = 21 in March 2026? March has 31 days, day 10 → 21 remaining
        assertEquals(21, result.daysRemaining)
        assertEquals(1_000_000L + 100_000L * 21, result.projectedTotal)
    }
}
