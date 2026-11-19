package com.chitieu.app.core.smart

import com.chitieu.app.core.database.RecurringRuleDao
import com.chitieu.app.core.database.TransactionRepository
import com.chitieu.app.core.database.toDomain
import com.chitieu.app.core.database.toEntity
import com.chitieu.app.core.model.RecurringRule
import com.chitieu.app.core.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

data class InsightSnapshot(
    val anomalies: List<AnomalyFlag>,
    val recurringSuggestions: List<RecurringCandidate>,
    val confirmedRecurring: List<RecurringRule>,
    val upcomingRecurring: List<RecurringRule>,
    val forecast: ForecastResult,
    val anomalyTransactions: Map<Long, Transaction>
)

@Singleton
class InsightEngine @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val recurringRuleDao: RecurringRuleDao
) {
    private val zone = ZoneId.systemDefault()

    fun observeInsights(): Flow<InsightSnapshot> {
        return combine(
            transactionRepository.observeAll(),
            recurringRuleDao.observeAll().map { list -> list.map { it.toDomain() } }
        ) { txs, rules ->
            val anomalies = AnomalyDetector.detect(txs)
            val candidates = RecurringDetector.detect(txs)
                .filter { c -> rules.none { it.merchantKey == c.merchantKey && it.isConfirmed } }
            val confirmed = rules.filter { it.isConfirmed }
            val today = LocalDate.now(zone)
            val upcoming = confirmed.filter { rule ->
                val due = Instant.ofEpochMilli(rule.nextDueDate).atZone(zone).toLocalDate()
                val days = ChronoUnit.DAYS.between(today, due)
                days in 0..3
            }
            val forecast = MonthForecast.forecast(txs, today, zone)
            val txMap = txs.associateBy { it.id }
            val anomalyTx = anomalies.mapNotNull { flag ->
                txMap[flag.transactionId]?.let { flag.transactionId to it }
            }.toMap()

            InsightSnapshot(
                anomalies = anomalies,
                recurringSuggestions = candidates,
                confirmedRecurring = confirmed,
                upcomingRecurring = upcoming,
                forecast = forecast,
                anomalyTransactions = anomalyTx
            )
        }
    }

    suspend fun confirmRecurring(candidate: RecurringCandidate) {
        val existing = recurringRuleDao.getByMerchantKey(candidate.merchantKey)
        val rule = RecurringRule(
            id = existing?.id ?: 0,
            merchantKey = candidate.merchantKey,
            categoryId = candidate.categoryId,
            expectedAmount = candidate.expectedAmount,
            periodDays = candidate.periodDays,
            nextDueDate = candidate.nextDueDate,
            isConfirmed = true
        )
        recurringRuleDao.upsert(rule.toEntity())
    }

    suspend fun deleteRecurring(id: Long) {
        recurringRuleDao.delete(id)
    }
}
