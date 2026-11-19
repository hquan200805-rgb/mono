package com.chitieu.app.feature.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.chitieu.app.core.backup.BackupPreview
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var listenerEnabled by remember {
        mutableStateOf(
            NotificationManagerCompat.getEnabledListenerPackages(context)
                .contains(context.packageName)
        )
    }
    LaunchedEffect(Unit) {
        while (true) {
            listenerEnabled = NotificationManagerCompat.getEnabledListenerPackages(context)
                .contains(context.packageName)
            delay(1500)
        }
    }

    val openBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val name = uri.lastPathSegment ?: ""
            val gzipped = name.endsWith(".gz") || name.contains(".json.gz")
            viewModel.previewRestore(uri, { u ->
                context.contentResolver.openInputStream(u)
            }, gzipped)
        }
    }

    // Share last export
    LaunchedEffect(uiState.lastExportFile) {
        val file = uiState.lastExportFile ?: return@LaunchedEffect
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val share = Intent(Intent.ACTION_SEND).apply {
                type = if (file.name.endsWith(".csv")) "text/csv" else "application/gzip"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(share, "Chia sẻ sao lưu"))
        } catch (_: Exception) { /* file vẫn nằm trong backups/ */ }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Cài đặt", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        // --- Thông báo ---
        Section("Nhập từ thông báo")
        SettingsCard(
            title = "Quyền đọc thông báo",
            subtitle = if (listenerEnabled) "Đã bật" else "Tắt — chạm để mở Settings hệ thống",
            trailing = if (listenerEnabled) "Bật" else "Tắt",
            trailingColor = if (listenerEnabled) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.error
        ) {
            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        // --- Sao lưu ---
        Section("Sao lưu & phục hồi")
        Text(
            "Schema v${uiState.schemaVersion} · JSON.gz + CSV · preview trước khi ghi đè",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = viewModel::exportBackup,
                enabled = !uiState.isBusy,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
            ) { Text("Xuất JSON") }
            Button(
                onClick = viewModel::exportCsv,
                enabled = !uiState.isBusy,
                modifier = Modifier.weight(1f)
            ) { Text("Xuất CSV") }
        }
        Button(
            onClick = { openBackup.launch(arrayOf("application/gzip", "application/json", "*/*")) },
            enabled = !uiState.isBusy,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Khôi phục từ file…") }

        if (uiState.isBusy) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp))
                Spacer(Modifier.padding(8.dp))
                Text("Đang xử lý…")
            }
        }
        uiState.message?.let {
            Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
        }

        // --- Khóa app ---
        Section("Khóa ứng dụng")
        Text(
            "PIN tùy chọn khi mở app. MVP không mã hóa database — chỉ chặn mở UI.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SettingsCard(
            title = "Khóa PIN",
            subtitle = if (uiState.lockEnabled) "Đang bật" else "Đang tắt",
            trailing = if (uiState.lockEnabled) "Tắt…" else "Bật…",
            trailingColor = MaterialTheme.colorScheme.primary
        ) {
            if (uiState.lockEnabled) viewModel.openDisablePin()
            else viewModel.openSetupPin()
        }
    }

    // Preview restore dialog
    uiState.restorePreview?.let { preview ->
        RestorePreviewDialog(
            preview = preview,
            onConfirm = {
                val gzipped = (uiState.pendingRestoreUri?.lastPathSegment ?: "").let {
                    it.endsWith(".gz") || it.contains(".json.gz")
                }
                viewModel.confirmRestore({ u -> context.contentResolver.openInputStream(u) }, gzipped)
            },
            onDismiss = viewModel::cancelRestorePreview
        )
    }

    if (uiState.showPinDialog) {
        PinDialog(
            mode = uiState.pinDialogMode,
            onDismiss = viewModel::dismissPinDialog,
            onSubmit = { pin, confirm -> viewModel.submitPin(pin, confirm) }
        )
    }
}

@Composable
private fun Section(title: String) {
    Spacer(Modifier.height(8.dp))
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun SettingsCard(
    title: String,
    subtitle: String,
    trailing: String,
    trailingColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(4.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(trailing, style = MaterialTheme.typography.labelLarge,
                color = trailingColor, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RestorePreviewDialog(
    preview: BackupPreview,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val meta = preview.meta
    val range = buildString {
        meta.minOccurredAt?.let {
            append(DateFormat.getDateInstance().format(Date(it)))
        }
        if (meta.minOccurredAt != null && meta.maxOccurredAt != null) append(" → ")
        meta.maxOccurredAt?.let {
            append(DateFormat.getDateInstance().format(Date(it)))
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Xem trước khôi phục") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Schema: v${meta.schemaVersion}")
                Text("Giao dịch: ${meta.transactionCount}")
                Text("Ví: ${meta.accountCount} · Danh mục: ${meta.categoryCount}")
                if (range.isNotBlank()) Text("Khoảng ngày: $range")
                Text(preview.message, color = if (preview.compatible)
                    MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = preview.compatible,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
            ) { Text("Ghi đè & khôi phục") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}

@Composable
private fun PinDialog(
    mode: PinDialogMode,
    onDismiss: () -> Unit,
    onSubmit: (pin: String, confirm: String?) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (mode == PinDialogMode.SETUP) "Đặt PIN (4–8 số)" else "Nhập PIN để tắt khóa")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 8 && it.all { c -> c.isDigit() }) pin = it },
                    label = { Text("PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                )
                if (mode == PinDialogMode.SETUP) {
                    OutlinedTextField(
                        value = confirm,
                        onValueChange = { if (it.length <= 8 && it.all { c -> c.isDigit() }) confirm = it },
                        label = { Text("Nhập lại PIN") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmit(pin, if (mode == PinDialogMode.SETUP) confirm else null)
                },
                enabled = pin.length >= 4
            ) { Text("Xác nhận") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}
