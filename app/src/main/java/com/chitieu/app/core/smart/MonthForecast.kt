package com.chitieu.app.core.smart

import com.chitieu.app.core.model.Transaction
import com.chitieu.app.core.model.TransactionType
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class ForecastResult(
    val alreadySpent: Long,
    val projectedTotal: Long,
    val daysPassed: Int,
    val daysRemaining: Int,
    val dailyAverage: Long
)

/**
 * dựBáo = đãChi + (đãChi / sốNgàyĐãQua) × sốNgàyCònLại
 */
object MonthForecast {

    fun forecast(
        transactions: List<Transaction>,
        today: LocalDate = LocalDate.now(),
        zone: ZoneId = ZoneId.systemDefault()
    ): ForecastResult {
        val startOfMonth = today.withDayOfMonth(1)
        val startMs = startOfMonth.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMs = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val spent = transactions
            .filter {
                it.type == TransactionType.CHI &&
                    it.occurredAt >= startMs &&
                    it.occurredAt < endMs
            }
            .sumOf { it.amount }

        val daysPassed = ChronoUnit.DAYS.between(startOfMonth, today).toInt() + 1
        val lastDay = today.lengthOfMonth()
        val daysRemaining = (lastDay - today.dayOfMonth).coerceAtLeast(0)
        val dailyAvg = if (daysPassed > 0) spent / daysPassed else 0L
        val projected = spent + dailyAvg * daysRemaining

        return ForecastResult(
            alreadySpent = spent,
            projectedTotal = projected,
            daysPassed = daysPassed,
            daysRemaining = daysRemaining,
            dailyAverage = dailyAvg
        )
    }
}
