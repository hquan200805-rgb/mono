package com.chitieu.app.feature.transfer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chitieu.app.core.database.AccountRepository
import com.chitieu.app.core.database.TransactionRepository
import com.chitieu.app.core.model.Account
import com.chitieu.app.core.model.Transaction
import com.chitieu.app.core.model.TransactionSource
import com.chitieu.app.core.model.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class TransferUiState(
    val accounts: List<Account> = emptyList(),
    val fromAccountId: Long? = null,
    val toAccountId: Long? = null,
    val amountText: String = "",
    val note: String = "",
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class TransferViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransferUiState())
    val uiState: StateFlow<TransferUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            accountRepository.observeActiveAccounts().collect { accounts ->
                _uiState.update { state ->
                    state.copy(
                        accounts = accounts,
                        fromAccountId = state.fromAccountId ?: accounts.getOrNull(0)?.id,
                        toAccountId = state.toAccountId ?: accounts.getOrNull(1)?.id
                    )
                }
            }
        }
    }

    fun onFromAccountSelect(id: Long) {
        _uiState.update {
            it.copy(
                fromAccountId = id,
                toAccountId = if (it.toAccountId == id) null else it.toAccountId,
                errorMessage = null
            )
        }
    }

    fun onToAccountSelect(id: Long) {
        _uiState.update {
            it.copy(
                toAccountId = id,
                fromAccountId = if (it.fromAccountId == id) null else it.fromAccountId,
                errorMessage = null
            )
        }
    }

    fun onAmountChange(text: String) {
        _uiState.update { it.copy(amountText = text.filter { c -> c.isDigit() }, errorMessage = null) }
    }

    fun onNoteChange(note: String) {
        _uiState.update { it.copy(note = note) }
    }

    fun transfer() {
        val state = _uiState.value
        val amount = state.amountText.toLongOrNull() ?: 0L
        val fromId = state.fromAccountId
        val toId = state.toAccountId

        when {
            amount <= 0 -> {
                _uiState.update { it.copy(errorMessage = "Vui lòng nhập số tiền hợp lệ") }
                return
            }
            fromId == null -> {
                _uiState.update { it.copy(errorMessage = "Chọn ví nguồn") }
                return
            }
            toId == null -> {
                _uiState.update { it.copy(errorMessage = "Chọn ví đích") }
                return
            }
            fromId == toId -> {
                _uiState.update { it.copy(errorMessage = "Ví nguồn và ví đích phải khác nhau") }
                return
            }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                val transferId = System.currentTimeMillis() // đơn giản, đủ unique cho local
                val now = System.currentTimeMillis()
                val noteText = state.note.ifBlank { "Chuyển khoản" }

                // CHI từ ví nguồn
                transactionRepository.insert(
                    Transaction(
                        accountId = fromId!!,
                        categoryId = null,
                        amount = amount,
                        type = TransactionType.CHI,
                        note = noteText,
                        occurredAt = now,
                        source = TransactionSource.THU_CONG,
                        transferId = transferId
                    )
                )
                // THU vào ví đích
                transactionRepository.insert(
                    Transaction(
                        accountId = toId!!,
                        categoryId = null,
                        amount = amount,
                        type = TransactionType.THU,
                        note = noteText,
                        occurredAt = now,
                        source = TransactionSource.THU_CONG,
                        transferId = transferId
                    )
                )

                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, errorMessage = "Chuyển thất bại: ${e.message}")
                }
            }
        }
    }

    fun consumeSaveSuccess() {
        _uiState.update { it.copy(saveSuccess = false) }
    }
}
