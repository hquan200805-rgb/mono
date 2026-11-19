package com.chitieu.app.core.smart

import com.chitieu.app.core.model.Transaction
import com.chitieu.app.core.model.TransactionType
import kotlin.math.abs

data class AnomalyFlag(
    val transactionId: Long,
    val categoryId: Long?,
    val amount: Long,
    val median: Long,
    val mad: Long,
    val threshold: Long
)

/**
 * Phát hiện chi bất thường theo median + MAD (k ≈ 3.5).
 * Cần tối thiểu [minSamples] giao dịch lịch sử trong danh mục.
 */
object AnomalyDetector {

    private const val DEFAULT_K = 3.5
    private const val MIN_SAMPLES = 10
    private const val MIN_ABSOLUTE = 50_000L // không flag khoản quá nhỏ

    fun detect(
        transactions: List<Transaction>,
        lookbackMs: Long = 90L * 24 * 60 * 60 * 1000,
        k: Double = DEFAULT_K,
        minSamples: Int = MIN_SAMPLES
    ): List<AnomalyFlag> {
        val now = System.currentTimeMillis()
        val from = now - lookbackMs
        val expenses = transactions.filter {
            it.type == TransactionType.CHI && it.occurredAt >= from
        }
        if (expenses.isEmpty()) return emptyList()

        // Group by category (null = "Khác")
        val byCat = expenses.groupBy { it.categoryId }
        val flags = mutableListOf<AnomalyFlag>()

        for ((catId, list) in byCat) {
            if (list.size < minSamples) continue
            val amounts = list.map { it.amount }.sorted()
            val median = median(amounts)
            val deviations = amounts.map { abs(it - median) }.sorted()
            val mad = median(deviations).coerceAtLeast(1)
            // threshold = median + k * 1.4826 * MAD (consistency constant for normal dist approx)
            val threshold = (median + k * 1.4826 * mad).toLong()
            val effectiveThreshold = maxOf(threshold, MIN_ABSOLUTE)

            // Chỉ flag các giao dịch gần đây (14 ngày) để tránh spam lịch sử
            val recentCut = now - 14L * 24 * 60 * 60 * 1000
            for (tx in list) {
                if (tx.occurredAt < recentCut) continue
                if (tx.amount > effectiveThreshold) {
                    flags.add(
                        AnomalyFlag(
                            transactionId = tx.id,
                            categoryId = catId,
                            amount = tx.amount,
                            median = median,
                            mad = mad,
                            threshold = effectiveThreshold
                        )
                    )
                }
            }
        }
        return flags.sortedByDescending { it.amount }
    }

    internal fun median(sorted: List<Long>): Long {
        if (sorted.isEmpty()) return 0
        val n = sorted.size
        return if (n % 2 == 1) sorted[n / 2]
        else (sorted[n / 2 - 1] + sorted[n / 2]) / 2
    }
}
