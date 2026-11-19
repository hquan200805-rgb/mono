package com.chitieu.app.feature.pending

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chitieu.app.core.model.PendingTransaction
import com.chitieu.app.core.notification.PendingTransactionRepository
import com.chitieu.app.core.notification.SupportedPackages
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PendingViewModel @Inject constructor(
    private val repository: PendingTransactionRepository
) : ViewModel() {

    val pendingList: StateFlow<List<PendingTransaction>> = repository
        .observePending()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingCount: StateFlow<Int> = repository
        .observePendingCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun approve(id: Long) {
        viewModelScope.launch { repository.approve(id) }
    }

    fun dismiss(id: Long) {
        viewModelScope.launch { repository.dismiss(id) }
    }

    fun sourceLabel(pkg: String): String = SupportedPackages.displayName(pkg)
}
