package com.chitieu.app.feature.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chitieu.app.core.database.BudgetProgress
import com.chitieu.app.core.designsystem.theme.Expense
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    viewModel: BudgetViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::openCreateOverall,
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Thêm ngân sách")
            }
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Ngân sách ${formatMonth(uiState.month)}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Theo dõi hạn mức và còn được tiêu mỗi ngày",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    if (uiState.progressList.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(Modifier = Modifier.padding(20.dp)) {
                                    Text(
                                        "Chưa có ngân sách nào",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "Nhấn + để đặt hạn mức chi tiêu tháng này",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(uiState.progressList, key = { it.budget.id }) { progress ->
                            BudgetProgressCard(
                                progress = progress,
                                onClick = { viewModel.openEdit(progress) }
                            )
                        }
                    }

                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    if (uiState.showEditor) {
        BudgetEditorDialog(
            uiState = uiState,
            onLimitChange = viewModel::onLimitChange,
            onCategorySelect = viewModel::onEditorCategorySelect,
            onSave = viewModel::saveBudget,
            onDismiss = viewModel::closeEditor,
            onDelete = {
                uiState.editingBudgetId?.let { viewModel.deleteBudget(it) }
                viewModel.closeEditor()
            }
        )
    }
}

@Composable
private fun BudgetProgressCard(
    progress: BudgetProgress,
    onClick: () -> Unit
) {
    val barColor = when {
        progress.isOver -> Expense
        progress.isWarning -> Color(0xFFF59E0B) // amber
        else -> MaterialTheme.colorScheme.primary
    }
    val title = if (progress.category != null) {
        "${progress.category.icon} ${progress.category.name}"
    } else {
        "📊 Ngân sách tổng"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (progress.isOver) {
                    Text(
                        "Vượt mức",
                        style = MaterialTheme.typography.labelMedium,
                        color = Expense,
                        fontWeight = FontWeight.Bold
                    )
                } else if (progress.isWarning) {
                    Text(
                        "Sắp hết",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFF59E0B),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress.percent.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = barColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Đã chi ${formatMoney(progress.spent)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Hạn mức ${formatMoney(progress.budget.limitAmount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Còn ${formatMoney(progress.remaining)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "≈ ${formatMoney(progress.dailyRemaining)}/ngày",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun BudgetEditorDialog(
    uiState: BudgetUiState,
    onLimitChange: (String) -> Unit,
    onCategorySelect: (Long?) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    val isEdit = uiState.editingBudgetId != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isEdit) "Sửa ngân sách" else "Thêm ngân sách")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Áp dụng cho", style = MaterialTheme.typography.labelLarge)

                // Tổng
                FilterChip(
                    selected = uiState.editorCategoryId == null,
                    onClick = { onCategorySelect(null) },
                    label = { Text("📊 Ngân sách tổng") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                // Theo danh mục chi
                uiState.categories.forEach { cat ->
                    FilterChip(
                        selected = uiState.editorCategoryId == cat.id,
                        onClick = { onCategorySelect(cat.id) },
                        label = { Text("${cat.icon} ${cat.name}") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }

                OutlinedTextField(
                    value = uiState.editorLimitText,
                    onValueChange = onLimitChange,
                    label = { Text("Hạn mức (đ)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                if (uiState.editorError != null) {
                    Text(
                        uiState.editorError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = !uiState.isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                )
            ) {
                Text(if (uiState.isSaving) "Đang lưu..." else "Lưu")
            }
        },
        dismissButton = {
            Row {
                if (isEdit) {
                    TextButton(onClick = onDelete) {
                        Text("Xóa", color = Expense)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Hủy")
                }
            }
        }
    )
}

private fun formatMoney(amount: Long): String {
    val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
    return formatter.format(amount) + "đ"
}

private fun formatMonth(yyyyMM: String): String {
    return try {
        val parts = yyyyMM.split("-")
        "tháng ${parts[1]}/${parts[0]}"
    } catch (_: Exception) {
        yyyyMM
    }
}
