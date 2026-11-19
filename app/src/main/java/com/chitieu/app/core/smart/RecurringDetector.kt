package com.chitieu.app.core.smart

import com.chitieu.app.core.model.RecurringRule
import com.chitieu.app.core.model.Transaction
import com.chitieu.app.core.model.TransactionType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.abs

data class RecurringCandidate(
    val merchantKey: String,
    val categoryId: Long?,
    val expectedAmount: Long,
    val periodDays: Int,
    val nextDueDate: Long,
    val occurrenceCount: Int
)

/**
 * Nhóm theo merchantKey (chuẩn hóa).
 * ≥ 3 lần, khoảng cách gần đều (±3 ngày), số tiền ±10% → đề xuất.
 */
object RecurringDetector {

    private val zone = ZoneId.systemDefault()

    fun detect(transactions: List<Transaction>): List<RecurringCandidate> {
        val expenses = transactions.filter {
            it.type == TransactionType.CHI && !merchantKeyOf(it).isNullOrBlank()
        }
        val groups = expenses.groupBy { merchantKeyOf(it)!! }
        val results = mutableListOf<RecurringCandidate>()

        for ((key, list) in groups) {
            if (list.size < 3) continue
            val sorted = list.sortedBy { it.occurredAt }
            val dates = sorted.map { toLocalDate(it.occurredAt) }
            val gaps = mutableListOf<Int>()
            for (i in 1 until dates.size) {
                gaps.add(ChronoUnit.DAYS.between(dates[i - 1], dates[i]).toInt())
            }
            if (gaps.isEmpty()) continue

            val medianGap = gaps.sorted()[gaps.size / 2]
            if (medianGap < 5 || medianGap > 40) continue // chỉ quan tâm ~tuần → tháng

            val gapOk = gaps.count { abs(it - medianGap) <= 3 } >= gaps.size * 0.6
            if (!gapOk) continue

            val amounts = sorted.map { it.amount }
            val medianAmount = amounts.sorted()[amounts.size / 2]
            val amountOk = amounts.count {
                abs(it - medianAmount).toDouble() / medianAmount <= 0.10
            } >= amounts.size * 0.6
            if (!amountOk || medianAmount <= 0) continue

            val lastDate = dates.last()
            val next = lastDate.plusDays(medianGap.toLong())
            val nextEpoch = next.atStartOfDay(zone).toInstant().toEpochMilli()

            results.add(
                RecurringCandidate(
                    merchantKey = key,
                    categoryId = sorted.last().categoryId,
                    expectedAmount = medianAmount,
                    periodDays = medianGap,
                    nextDueDate = nextEpoch,
                    occurrenceCount = sorted.size
                )
            )
        }
        return results.sortedBy { it.nextDueDate }
    }

    fun merchantKeyOf(tx: Transaction): String? {
        val raw = tx.merchant?.takeIf { it.isNotBlank() }
            ?: tx.note?.takeIf { it.isNotBlank() }
            ?: return null
        return TextNormalizer.removeDiacritics(raw)
            .lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")
            .take(64)
            .ifBlank { null }
    }

    private fun toLocalDate(epochMs: Long): LocalDate =
        Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate()
}
