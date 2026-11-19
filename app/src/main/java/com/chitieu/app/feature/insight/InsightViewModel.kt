package com.chitieu.app.feature.insight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chitieu.app.core.smart.InsightEngine
import com.chitieu.app.core.smart.InsightSnapshot
import com.chitieu.app.core.smart.RecurringCandidate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InsightViewModel @Inject constructor(
    private val insightEngine: InsightEngine
) : ViewModel() {

    val snapshot: StateFlow<InsightSnapshot?> = insightEngine
        .observeInsights()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun confirmRecurring(candidate: RecurringCandidate) {
        viewModelScope.launch { insightEngine.confirmRecurring(candidate) }
    }

    fun deleteRecurring(id: Long) {
        viewModelScope.launch { insightEngine.deleteRecurring(id) }
    }
}
