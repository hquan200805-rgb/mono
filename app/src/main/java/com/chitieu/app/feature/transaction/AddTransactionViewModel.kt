package com.chitieu.app.feature.transaction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chitieu.app.core.database.AccountRepository
import com.chitieu.app.core.database.CategoryRepository
import com.chitieu.app.core.database.TransactionRepository
import com.chitieu.app.core.model.Account
import com.chitieu.app.core.model.Category
import com.chitieu.app.core.model.CategoryType
import com.chitieu.app.core.model.Transaction
import com.chitieu.app.core.model.TransactionSource
import com.chitieu.app.core.model.TransactionType
import com.chitieu.app.core.smart.CategoryClassifier
import com.chitieu.app.core.smart.Confidence
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddTransactionUiState(
    val isEditMode: Boolean = false,
    val editingId: Long? = null,
    val amountText: String = "",
    val type: TransactionType = TransactionType.CHI,
    val selectedCategoryId: Long? = null,
    val selectedAccountId: Long? = null,
    val note: String = "",
    val occurredAt: Long = System.currentTimeMillis(),
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null,
    /** Gợi ý danh mục từ phân loại thông minh */
    val suggestedCategoryId: Long? = null,
    val suggestionLabel: String? = null,
    val userOverrodeCategory: Boolean = false
)

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val categoryClassifier: CategoryClassifier,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val editId: Long? = savedStateHandle.get<Long>("transactionId")
    private var originalCategoryId: Long? = null
    private var suggestJob: Job? = null

    private val _uiState = MutableStateFlow(AddTransactionUiState(isEditMode = editId != null))
    val uiState: StateFlow<AddTransactionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            if (editId != null) {
                val tx = transactionRepository.getById(editId)
                if (tx != null) {
                    originalCategoryId = tx.categoryId
                    _uiState.update {
                        it.copy(
                            isEditMode = true,
                            editingId = tx.id,
                            amountText = tx.amount.toString(),
                            type = tx.type,
                            selectedCategoryId = tx.categoryId,
                            selectedAccountId = tx.accountId,
                            note = tx.note.orEmpty(),
                            occurredAt = tx.occurredAt,
                            userOverrodeCategory = true // không auto ghi đè khi edit
                        )
                    }
                }
            }

            combine(
                categoryRepository.observeAll(),
                accountRepository.observeActiveAccounts()
            ) { categories, accounts ->
                categories to accounts
            }.collect { (categories, accounts) ->
                _uiState.update { state ->
                    val filteredCategories = categories.filter {
                        when (state.type) {
                            TransactionType.CHI -> it.type == CategoryType.CHI
                            TransactionType.THU -> it.type == CategoryType.THU
                            TransactionType.CHUYEN_KHOAN -> true
                        }
                    }
                    state.copy(
                        categories = filteredCategories,
                        accounts = accounts,
                        selectedAccountId = state.selectedAccountId ?: accounts.firstOrNull()?.id,
                        selectedCategoryId = state.selectedCategoryId
                            ?: filteredCategories.firstOrNull()?.id
                    )
                }
            }
        }
    }

    fun onAmountChange(text: String) {
        val filtered = text.filter { it.isDigit() }
        _uiState.update { it.copy(amountText = filtered, errorMessage = null) }
    }

    fun onTypeChange(type: TransactionType) {
        _uiState.update {
            it.copy(
                type = type,
                selectedCategoryId = null,
                userOverrodeCategory = false,
                suggestedCategoryId = null,
                suggestionLabel = null
            )
        }
        scheduleSuggest()
    }

    fun onCategorySelect(id: Long) {
        _uiState.update {
            it.copy(
                selectedCategoryId = id,
                userOverrodeCategory = true
            )
        }
    }

    fun onAccountSelect(id: Long) {
        _uiState.update { it.copy(selectedAccountId = id) }
    }

    fun onNoteChange(note: String) {
        _uiState.update { it.copy(note = note) }
        if (!_uiState.value.userOverrodeCategory) {
            scheduleSuggest()
        }
    }

    private fun scheduleSuggest() {
        suggestJob?.cancel()
        suggestJob = viewModelScope.launch {
            delay(300) // debounce
            val state = _uiState.value
            if (state.userOverrodeCategory) return@launch
            val result = categoryClassifier.suggest(
                note = state.note,
                merchant = null,
                categories = state.categories
            )
            if (result.categoryId != null) {
                val label = when (result.confidence) {
                    Confidence.HISTORY -> "Gợi ý từ lịch sử"
                    Confidence.KEYWORD -> "Gợi ý từ từ khóa"
                    Confidence.NONE -> null
                }
                _uiState.update {
                    it.copy(
                        selectedCategoryId = result.categoryId,
                        suggestedCategoryId = result.categoryId,
                        suggestionLabel = label
                    )
                }
            }
        }
    }

    fun save() {
        val state = _uiState.value
        val amount = state.amountText.toLongOrNull() ?: 0L

        if (amount <= 0) {
            _uiState.update { it.copy(errorMessage = "Vui lòng nhập số tiền hợp lệ") }
            return
        }
        if (state.selectedAccountId == null) {
            _uiState.update { it.copy(errorMessage = "Vui lòng chọn ví") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                val categoryId = state.selectedCategoryId
                if (state.isEditMode && state.editingId != null) {
                    val existing = transactionRepository.getById(state.editingId)
                    transactionRepository.update(
                        Transaction(
                            id = state.editingId,
                            accountId = state.selectedAccountId!!,
                            categoryId = categoryId,
                            amount = amount,
                            type = state.type,
                            note = state.note.ifBlank { null },
                            occurredAt = state.occurredAt,
                            source = existing?.source ?: TransactionSource.THU_CONG,
                            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                    if (categoryId != null) {
                        categoryClassifier.learn(
                            note = state.note,
                            merchant = null,
                            newCategoryId = categoryId,
                            oldCategoryId = originalCategoryId
                        )
                    }
                } else {
                    transactionRepository.insert(
                        Transaction(
                            accountId = state.selectedAccountId!!,
                            categoryId = categoryId,
                            amount = amount,
                            type = state.type,
                            note = state.note.ifBlank { null },
                            occurredAt = state.occurredAt,
                            source = TransactionSource.THU_CONG
                        )
                    )
                    if (categoryId != null && state.note.isNotBlank()) {
                        categoryClassifier.learn(
                            note = state.note,
                            merchant = null,
                            newCategoryId = categoryId,
                            oldCategoryId = null
                        )
                    }
                }
                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, errorMessage = "Lưu thất bại: ${e.message}")
                }
            }
        }
    }

    fun consumeSaveSuccess() {
        _uiState.update { it.copy(saveSuccess = false) }
    }
}
