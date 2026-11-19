package com.chitieu.app.core.smart

import com.chitieu.app.core.model.Transaction
import com.chitieu.app.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class RecurringDetectorTest {

    @Test
    fun `detects monthly-like pattern`() {
        val zone = ZoneId.of("Asia/Ho_Chi_Minh")
        val base = LocalDate.of(2026, 1, 5)
        val txs = (0..3).map { i ->
            val d = base.plusDays(i * 30L)
            Transaction(
                id = i.toLong() + 1,
                accountId = 1,
                categoryId = 1,
                amount = 200_000L,
                type = TransactionType.CHI,
                note = "Netflix",
                merchant = "Netflix",
                occurredAt = d.atStartOfDay(zone).toInstant().toEpochMilli()
            )
        }
        val found = RecurringDetector.detect(txs)
        assertTrue(found.isNotEmpty())
        assertEquals("netflix", found.first().merchantKey)
        assertTrue(found.first().periodDays in 27..33)
    }
}
