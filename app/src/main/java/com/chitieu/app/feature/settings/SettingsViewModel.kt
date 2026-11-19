package com.chitieu.app.feature.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chitieu.app.core.backup.BACKUP_SCHEMA_VERSION
import com.chitieu.app.core.backup.BackupPreview
import com.chitieu.app.core.backup.BackupRepository
import com.chitieu.app.core.lock.AppLockRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class SettingsUiState(
    val lockEnabled: Boolean = false,
    val message: String? = null,
    val isBusy: Boolean = false,
    val lastExportFile: File? = null,
    val restorePreview: BackupPreview? = null,
    val pendingRestoreUri: Uri? = null,
    val showPinDialog: Boolean = false,
    val pinDialogMode: PinDialogMode = PinDialogMode.SETUP,
    val schemaVersion: Int = BACKUP_SCHEMA_VERSION
)

enum class PinDialogMode { SETUP, DISABLE }

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val backupRepository: BackupRepository,
    private val appLockRepository: AppLockRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val lockEnabled = appLockRepository.lockEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        viewModelScope.launch {
            appLockRepository.lockEnabled.collect { enabled ->
                _uiState.update { it.copy(lockEnabled = enabled) }
            }
        }
    }

    fun exportBackup() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, message = null) }
            try {
                val file = backupRepository.exportToGzipFile()
                _uiState.update {
                    it.copy(isBusy = false, lastExportFile = file, message = "Đã tạo: ${file.name}")
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isBusy = false, message = "Xuất lỗi: ${e.message}") }
            }
        }
    }

    fun exportCsv() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, message = null) }
            try {
                val file = backupRepository.exportCsvFile()
                _uiState.update {
                    it.copy(isBusy = false, lastExportFile = file, message = "CSV: ${file.name}")
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isBusy = false, message = "Xuất CSV lỗi: ${e.message}") }
            }
        }
    }

    fun previewRestore(uri: Uri, inputProvider: (Uri) -> java.io.InputStream?, gzipped: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, message = null) }
            try {
                val stream = inputProvider(uri) ?: throw IllegalStateException("Không đọc được file")
                stream.use {
                    val preview = backupRepository.previewFromStream(it, gzipped)
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            restorePreview = preview,
                            pendingRestoreUri = uri
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isBusy = false, message = "Xem trước lỗi: ${e.message}")
                }
            }
        }
    }

    fun confirmRestore(inputProvider: (Uri) -> java.io.InputStream?, gzipped: Boolean) {
        val uri = _uiState.value.pendingRestoreUri ?: return
        if (_uiState.value.restorePreview?.compatible != true) return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, message = null) }
            try {
                val stream = inputProvider(uri) ?: throw IllegalStateException("Không đọc được file")
                stream.use { backupRepository.restoreFromStream(it, gzipped) }
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        restorePreview = null,
                        pendingRestoreUri = null,
                        message = "Khôi phục thành công"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isBusy = false, message = "Khôi phục lỗi: ${e.message}") }
            }
        }
    }

    fun cancelRestorePreview() {
        _uiState.update { it.copy(restorePreview = null, pendingRestoreUri = null) }
    }

    fun openSetupPin() {
        _uiState.update { it.copy(showPinDialog = true, pinDialogMode = PinDialogMode.SETUP) }
    }

    fun openDisablePin() {
        _uiState.update { it.copy(showPinDialog = true, pinDialogMode = PinDialogMode.DISABLE) }
    }

    fun dismissPinDialog() {
        _uiState.update { it.copy(showPinDialog = false) }
    }

    fun submitPin(pin: String, confirmPin: String? = null) {
        viewModelScope.launch {
            try {
                when (_uiState.value.pinDialogMode) {
                    PinDialogMode.SETUP -> {
                        if (confirmPin != null && pin != confirmPin) {
                            _uiState.update { it.copy(message = "PIN không khớp") }
                            return@launch
                        }
                        appLockRepository.setPin(pin)
                        _uiState.update {
                            it.copy(showPinDialog = false, message = "Đã bật khóa PIN")
                        }
                    }
                    PinDialogMode.DISABLE -> {
                        if (!appLockRepository.verifyPin(pin)) {
                            _uiState.update { it.copy(message = "PIN sai") }
                            return@launch
                        }
                        appLockRepository.disableLock()
                        _uiState.update {
                            it.copy(showPinDialog = false, message = "Đã tắt khóa PIN")
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(message = e.message) }
            }
        }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
