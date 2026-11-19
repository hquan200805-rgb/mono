package com.chitieu.app.feature.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chitieu.app.core.database.BudgetProgress
import com.chitieu.app.core.database.BudgetRepository
import com.chitieu.app.core.database.CategoryRepository
import com.chitieu.app.core.model.Budget
import com.chitieu.app.core.model.Category
import com.chitieu.app.core.model.CategoryType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BudgetUiState(
    val month: String = "",
    val progressList: List<BudgetProgress> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = true,
    // Dialog set/edit
    val showEditor: Boolean = false,
    val editingBudgetId: Long? = null,
    val editorCategoryId: Long? = null,   // null = tổng
    val editorLimitText: String = "",
    val editorError: String? = null,
    val isSaving: Boolean = false
)

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val month = budgetRepository.currentMonth()

    private val _editorState = MutableStateFlow(
        BudgetUiState(month = month)
    )

    val uiState: StateFlow<BudgetUiState> = combine(
        budgetRepository.observeBudgetProgress(month),
        categoryRepository.observeByType(CategoryType.CHI),
        _editorState
    ) { progress, categories, editor ->
        editor.copy(
            month = month,
            progressList = progress,
            categories = categories,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BudgetUiState(month = month, isLoading = true)
    )

    fun openCreateOverall() {
        _editorState.update {
            it.copy(
                showEditor = true,
                editingBudgetId = null,
                editorCategoryId = null,
                editorLimitText = "",
                editorError = null
            )
        }
    }

    fun openCreateForCategory(categoryId: Long) {
        _editorState.update {
            it.copy(
                showEditor = true,
                editingBudgetId = null,
                editorCategoryId = categoryId,
                editorLimitText = "",
                editorError = null
            )
        }
    }

    fun openEdit(progress: BudgetProgress) {
        _editorState.update {
            it.copy(
                showEditor = true,
                editingBudgetId = progress.budget.id,
                editorCategoryId = progress.budget.categoryId,
                editorLimitText = progress.budget.limitAmount.toString(),
                editorError = null
            )
        }
    }

    fun closeEditor() {
        _editorState.update { it.copy(showEditor = false, editorError = null) }
    }

    fun onLimitChange(text: String) {
        _editorState.update {
            it.copy(editorLimitText = text.filter { c -> c.isDigit() }, editorError = null)
        }
    }

    fun onEditorCategorySelect(categoryId: Long?) {
        _editorState.update { it.copy(editorCategoryId = categoryId) }
    }

    fun saveBudget() {
        val state = _editorState.value
        val limit = state.editorLimitText.toLongOrNull() ?: 0L
        if (limit <= 0) {
            _editorState.update { it.copy(editorError = "Hạn mức phải lớn hơn 0") }
            return
        }

        viewModelScope.launch {
            _editorState.update { it.copy(isSaving = true) }
            try {
                budgetRepository.upsert(
                    Budget(
                        id = state.editingBudgetId ?: 0,
                        categoryId = state.editorCategoryId,
                        month = month,
                        limitAmount = limit
                    )
                )
                _editorState.update { it.copy(isSaving = false, showEditor = false) }
            } catch (e: Exception) {
                _editorState.update {
                    it.copy(isSaving = false, editorError = "Lưu thất bại: ${e.message}")
                }
            }
        }
    }

    fun deleteBudget(id: Long) {
        viewModelScope.launch {
            budgetRepository.delete(id)
        }
    }
}
