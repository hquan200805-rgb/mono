package com.chitieu.app.feature.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chitieu.app.core.database.AccountRepository
import com.chitieu.app.core.database.AccountWithBalance
import com.chitieu.app.core.database.BudgetProgress
import com.chitieu.app.core.database.BudgetRepository
import com.chitieu.app.core.notification.PendingTransactionRepository
import com.chitieu.app.core.database.TransactionRepository
import com.chitieu.app.core.model.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class OverviewUiState(
    val accounts: List<AccountWithBalance> = emptyList(),
    val monthExpense: Long = 0,
    val monthIncome: Long = 0,
    val overallBudget: BudgetProgress? = null,
    val pendingCount: Int = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class OverviewViewModel @Inject constructor(
    accountRepository: AccountRepository,
    transactionRepository: TransactionRepository,
    budgetRepository: BudgetRepository,
    pendingRepository: PendingTransactionRepository
) : ViewModel() {

    val uiState: StateFlow<OverviewUiState> = combine(
        accountRepository.observeAccountsWithBalance(),
        transactionRepository.observeAll(),
        budgetRepository.observeBudgetProgress(),
        pendingRepository.observePendingCount()
    ) { accounts, transactions, budgets, pendingCount ->
        val zone = ZoneId.systemDefault()
        val now = LocalDate.now()
        val startOfMonth = now.withDayOfMonth(1)
            .atStartOfDay(zone).toInstant().toEpochMilli()
        val startOfNextMonth = now.plusMonths(1).withDayOfMonth(1)
            .atStartOfDay(zone).toInstant().toEpochMilli()

        val monthTxs = transactions.filter {
            it.occurredAt >= startOfMonth && it.occurredAt < startOfNextMonth
        }
        OverviewUiState(
            accounts = accounts,
            monthExpense = monthTxs.filter { it.type == TransactionType.CHI }.sumOf { it.amount },
            monthIncome = monthTxs.filter { it.type == TransactionType.THU }.sumOf { it.amount },
            overallBudget = budgets.firstOrNull { it.budget.categoryId == null },
            pendingCount = pendingCount,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = OverviewUiState()
    )
}
