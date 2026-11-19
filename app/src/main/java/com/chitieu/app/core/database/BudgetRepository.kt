package com.chitieu.app.core.database

import com.chitieu.app.core.model.Budget
import com.chitieu.app.core.model.Category
import com.chitieu.app.core.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

data class BudgetProgress(
    val budget: Budget,
    val category: Category?,          // null nếu là ngân sách tổng
    val spent: Long,
    val remaining: Long,
    val percent: Float,               // 0f..1f+ (có thể > 1)
    val dailyRemaining: Long,         // còn được tiêu mỗi ngày
    val daysLeftInMonth: Int,
    val isOver: Boolean,
    val isWarning: Boolean            // >= 80%
)

@Singleton
class BudgetRepository @Inject constructor(
    private val budgetDao: BudgetDao,
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao
) {
    fun currentMonth(): String =
        LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))

    fun observeBudgetProgress(month: String = currentMonth()): Flow<List<BudgetProgress>> {
        val zone = ZoneId.systemDefault()
        val yearMonth = java.time.YearMonth.parse(month)
        val start = yearMonth.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = yearMonth.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val today = LocalDate.now()
        val lastDay = yearMonth.atEndOfMonth()
        val daysLeft = if (today.year == yearMonth.year && today.month == yearMonth.month) {
            (lastDay.dayOfMonth - today.dayOfMonth + 1).coerceAtLeast(1)
        } else if (today.isBefore(yearMonth.atDay(1))) {
            yearMonth.lengthOfMonth()
        } else {
            1
        }

        return combine(
            budgetDao.observeByMonth(month),
            transactionDao.observeExpensesInRange(start, end),
            categoryDao.observeAll()
        ) { budgets, expenses, categories ->
            val catMap = categories.associateBy { it.id }
            budgets.map { be ->
                val budget = be.toDomain()
                val spent = if (budget.categoryId == null) {
                    expenses.sumOf { it.amount }
                } else {
                    expenses.filter { it.categoryId == budget.categoryId }.sumOf { it.amount }
                }
                val remaining = (budget.limitAmount - spent).coerceAtLeast(0)
                val percent = if (budget.limitAmount > 0) {
                    spent.toFloat() / budget.limitAmount
                } else 0f
                val daily = remaining / daysLeft
                BudgetProgress(
                    budget = budget,
                    category = budget.categoryId?.let { id -> catMap[id]?.toDomain() },
                    spent = spent,
                    remaining = remaining,
                    percent = percent,
                    dailyRemaining = daily,
                    daysLeftInMonth = daysLeft,
                    isOver = spent > budget.limitAmount,
                    isWarning = percent >= 0.8f && !spent.let { it > budget.limitAmount }
                )
            }.sortedWith(
                compareBy<BudgetProgress> { it.budget.categoryId != null } // tổng trước
                    .thenByDescending { it.percent }
            )
        }
    }

    suspend fun upsert(budget: Budget): Long =
        budgetDao.upsert(budget.toEntity())

    suspend fun delete(id: Long) =
        budgetDao.deleteById(id)

    suspend fun getOverall(month: String = currentMonth()): Budget? =
        budgetDao.getOverall(month)?.toDomain()

    suspend fun getByCategory(month: String, categoryId: Long): Budget? =
        budgetDao.getByCategory(month, categoryId)?.toDomain()
}
