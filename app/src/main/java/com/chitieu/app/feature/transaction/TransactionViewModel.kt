package com.chitieu.app.feature.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chitieu.app.core.database.TransactionRepository
import com.chitieu.app.core.model.Transaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

data class TransactionListUiState(
    val groups: List<TransactionDayGroup> = emptyList(),
    val isLoading: Boolean = true
)

data class TransactionDayGroup(
    val dateLabel: String,
    val date: LocalDate,
    val transactions: List<Transaction>,
    val totalExpense: Long,
    val totalIncome: Long
)

/** Sự kiện one-shot: yêu cầu hiện Snackbar Undo */
data class UndoDeleteEvent(val transaction: Transaction)

@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private var recentlyDeleted: Transaction? = null

    private val _undoEvent = MutableSharedFlow<UndoDeleteEvent>(extraBufferCapacity = 1)
    val undoEvent: SharedFlow<UndoDeleteEvent> = _undoEvent.asSharedFlow()

    val uiState: StateFlow<TransactionListUiState> = transactionRepository
        .observeAll()
        .map { list ->
            val zone = ZoneId.systemDefault()
            val grouped = list
                .groupBy {
                    Instant.ofEpochMilli(it.occurredAt).atZone(zone).toLocalDate()
                }
                .toSortedMap(compareByDescending { it })
                .map { (date, txs) ->
                    TransactionDayGroup(
                        dateLabel = formatDateLabel(date),
                        date = date,
                        transactions = txs,
                        totalExpense = txs.filter { it.type.name == "CHI" }.sumOf { it.amount },
                        totalIncome = txs.filter { it.type.name == "THU" }.sumOf { it.amount }
                    )
                }
            TransactionListUiState(groups = grouped, isLoading = false)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TransactionListUiState()
        )

    fun deleteTransaction(tx: Transaction) {
        viewModelScope.launch {
            recentlyDeleted = tx
            transactionRepository.delete(tx.id)
            _undoEvent.emit(UndoDeleteEvent(tx))
        }
    }

    fun undoDelete() {
        val tx = recentlyDeleted ?: return
        viewModelScope.launch {
            // Insert lại với cùng dữ liệu (id mới cũng được, hoặc giữ id cũ)
            transactionRepository.insert(tx.copy(id = 0)) // để Room tự generate id mới
            recentlyDeleted = null
        }
    }

    private fun formatDateLabel(date: LocalDate): String {
        val today = LocalDate.now()
        return when (date) {
            today -> "Hôm nay"
            today.minusDays(1) -> "Hôm qua"
            else -> date.format(
                DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy", Locale("vi", "VN"))
            )
        }
    }
}
