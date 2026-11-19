package com.chitieu.app.feature.insight

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chitieu.app.core.designsystem.theme.Expense
import com.chitieu.app.core.smart.AnomalyFlag
import com.chitieu.app.core.smart.RecurringCandidate
import com.chitieu.app.core.model.RecurringRule
import com.chitieu.app.core.smart.ForecastResult
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun InsightScreen(
    viewModel: InsightViewModel = hiltViewModel()
) {
    val snapshot by viewModel.snapshot.collectAsState()

    if (snapshot == null) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val data = snapshot!!

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Gợi ý thông minh",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Chạy hoàn toàn trên máy — median+MAD, định kỳ, dự báo",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        ForecastCard(data.forecast)

        if (data.upcomingRecurring.isNotEmpty()) {
            SectionTitle("Sắp đến hạn")
            data.upcomingRecurring.forEach { rule ->
                UpcomingCard(rule)
            }
        }

        if (data.anomalies.isNotEmpty()) {
            SectionTitle("Chi bất thường")
            data.anomalies.take(10).forEach { flag ->
                AnomalyCard(flag, data.anomalyTransactions[flag.transactionId]?.note)
            }
        } else {
            SectionTitle("Chi bất thường")
            HintCard("Chưa có khoản bất thường (cần ≥10 giao dịch/danh mục trong 90 ngày).")
        }

        if (data.recurringSuggestions.isNotEmpty()) {
            SectionTitle("Có thể là khoản định kỳ")
            data.recurringSuggestions.forEach { c ->
                RecurringSuggestionCard(
                    candidate = c,
                    onConfirm = { viewModel.confirmRecurring(c) }
                )
            }
        }

        if (data.confirmedRecurring.isNotEmpty()) {
            SectionTitle("Khoản định kỳ đã xác nhận")
            data.confirmedRecurring.forEach { rule ->
                ConfirmedRecurringCard(
                    rule = rule,
                    onDelete = { viewModel.deleteRecurring(rule.id) }
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun ForecastCard(f: ForecastResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier = Modifier.padding(16.dp)) {
            Text(
                "Dự báo chi cuối tháng",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(6.dp))
            Text(
                formatMoney(f.projectedTotal),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Đã chi ${formatMoney(f.alreadySpent)} · TB ${formatMoney(f.dailyAverage)}/ngày · còn ${f.daysRemaining} ngày",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun AnomalyCard(flag: AnomalyFlag, note: String?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(Modifier = Modifier.padding(14.dp)) {
            Text(
                formatMoney(flag.amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Expense
            )
            if (!note.isNullOrBlank()) {
                Text(note, style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                "Vượt ngưỡng ${formatMoney(flag.threshold)} (median ${formatMoney(flag.median)})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RecurringSuggestionCard(
    candidate: RecurringCandidate,
    onConfirm: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                candidate.merchantKey.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "${formatMoney(candidate.expectedAmount)} · mỗi ~${candidate.periodDays} ngày · ${candidate.occurrenceCount} lần",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Dự kiến tiếp: ${formatDate(candidate.nextDueDate)}",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                )
            ) {
                Text("Xác nhận định kỳ")
            }
        }
    }
}

@Composable
private fun ConfirmedRecurringCard(rule: RecurringRule, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(rule.merchantKey, fontWeight = FontWeight.Medium)
                Text(
                    "${formatMoney(rule.expectedAmount)} / ${rule.periodDays} ngày · hạn ${formatDate(rule.nextDueDate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(onClick = onDelete) {
                Text("Xóa")
            }
        }
    }
}

@Composable
private fun UpcomingCard(rule: RecurringRule) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                "Sắp đến: ${rule.merchantKey}",
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Text(
                "${formatMoney(rule.expectedAmount)} · ${formatDate(rule.nextDueDate)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@Composable
private fun HintCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Text(
            text,
            modifier = Modifier.padding(14.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatMoney(amount: Long): String {
    val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
    return formatter.format(amount) + "đ"
}

private fun formatDate(epochMs: Long): String {
    val d = Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).toLocalDate()
    return d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
}
