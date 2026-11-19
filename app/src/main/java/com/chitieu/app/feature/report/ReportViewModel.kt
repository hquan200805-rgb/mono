package com.chitieu.app.feature.report

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chitieu.app.core.database.CategoryRepository
import com.chitieu.app.core.database.TransactionRepository
import com.chitieu.app.core.model.Category
import com.chitieu.app.core.model.Transaction
import com.chitieu.app.core.model.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class CategorySlice(
    val categoryId: Long?,
    val name: String,
    val icon: String,
    val colorHex: String,
    val amount: Long,
    val percent: Float          // 0..1 relative to total expense
)

data class DayPoint(
    val day: Int,
    val amount: Long
)

data class MonthComparison(
    val currentExpense: Long,
    val previousExpense: Long,
    val deltaPercent: Float?    // null nếu previous = 0
)

data class ReportUiState(
    val selectedMonth: YearMonth = YearMonth.now(),
    val expense: Long = 0,
    val income: Long = 0,
    val net: Long = 0,
    val categorySlices: List<CategorySlice> = emptyList(),
    val dailyTrend: List<DayPoint> = emptyList(),
    val comparison: MonthComparison? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val zone = ZoneId.systemDefault()
    private val _selectedMonth = MutableStateFlow(YearMonth.now())

    val uiState: StateFlow<ReportUiState> = combine(
        transactionRepository.observeAll(),
        categoryRepository.observeAll(),
        _selectedMonth
    ) { allTx, categories, month ->
        buildState(allTx, categories, month)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReportUiState()
    )

    fun previousMonth() {
        _selectedMonth.update { it.minusMonths(1) }
    }

    fun nextMonth() {
        val now = YearMonth.now()
        _selectedMonth.update { current ->
            if (current < now) current.plusMonths(1) else current
        }
    }

    private fun buildState(
        allTx: List<Transaction>,
        categories: List<Category>,
        month: YearMonth
    ): ReportUiState {
        val catMap = categories.associateBy { it.id }
        val start = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val prevStart = month.minusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val monthTx = allTx.filter { it.occurredAt >= start && it.occurredAt < end }
        val prevTx = allTx.filter { it.occurredAt >= prevStart && it.occurredAt < start }

        val expense = monthTx.filter { it.type == TransactionType.CHI }.sumOf { it.amount }
        val income = monthTx.filter { it.type == TransactionType.THU }.sumOf { it.amount }
        val prevExpense = prevTx.filter { it.type == TransactionType.CHI }.sumOf { it.amount }

        // Category breakdown (CHI only)
        val byCat = monthTx
            .filter { it.type == TransactionType.CHI }
            .groupBy { it.categoryId }
            .map { (catId, list) ->
                val amount = list.sumOf { it.amount }
                val cat = catId?.let { catMap[it] }
                CategorySlice(
                    categoryId = catId,
                    name = cat?.name ?: "Khác",
                    icon = cat?.icon ?: "📦",
                    colorHex = cat?.colorHex ?: "#BDBDBD",
                    amount = amount,
                    percent = if (expense > 0) amount.toFloat() / expense else 0f
                )
            }
            .sortedByDescending { it.amount }

        // Daily trend
        val daysInMonth = month.lengthOfMonth()
        val expenseByDay = monthTx
            .filter { it.type == TransactionType.CHI }
            .groupBy {
                java.time.Instant.ofEpochMilli(it.occurredAt).atZone(zone).dayOfMonth
            }
            .mapValues { (_, list) -> list.sumOf { it.amount } }

        val dailyTrend = (1..daysInMonth).map { day ->
            DayPoint(day = day, amount = expenseByDay[day] ?: 0L)
        }

        val delta = when {
            prevExpense == 0L && expense == 0L -> null
            prevExpense == 0L -> null
            else -> ((expense - prevExpense).toFloat() / prevExpense) * 100f
        }

        return ReportUiState(
            selectedMonth = month,
            expense = expense,
            income = income,
            net = income - expense,
            categorySlices = byCat,
            dailyTrend = dailyTrend,
            comparison = MonthComparison(expense, prevExpense, delta),
            isLoading = false
        )
    }
}

fun YearMonth.toDisplay(): String =
    format(DateTimeFormatter.ofPattern("MM/yyyy"))
