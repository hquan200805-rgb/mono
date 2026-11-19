package com.chitieu.app.core.smart

import com.chitieu.app.core.model.Transaction
import com.chitieu.app.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnomalyDetectorTest {

    @Test
    fun `median of odd list`() {
        assertEquals(3L, AnomalyDetector.median(listOf(1, 3, 9)))
    }

    @Test
    fun `median of even list`() {
        assertEquals(5L, AnomalyDetector.median(listOf(2, 4, 6, 8)))
    }

    @Test
    fun `needs min samples`() {
        val now = System.currentTimeMillis()
        val txs = (1..5).map {
            Transaction(
                id = it.toLong(),
                accountId = 1,
                categoryId = 1,
                amount = 50_000L,
                type = TransactionType.CHI,
                occurredAt = now - it * 86_400_000L
            )
        }
        assertTrue(AnomalyDetector.detect(txs, minSamples = 10).isEmpty())
    }

    @Test
    fun `flags extreme outlier when enough samples`() {
        val now = System.currentTimeMillis()
        val normal = (1..15).map {
            Transaction(
                id = it.toLong(),
                accountId = 1,
                categoryId = 1,
                amount = 50_000L,
                type = TransactionType.CHI,
                occurredAt = now - it * 86_400_000L
            )
        }
        val outlier = Transaction(
            id = 100,
            accountId = 1,
            categoryId = 1,
            amount = 5_000_000L,
            type = TransactionType.CHI,
            occurredAt = now - 1000
        )
        val flags = AnomalyDetector.detect(normal + outlier, minSamples = 10)
        assertTrue(flags.any { it.transactionId == 100L })
    }
}
