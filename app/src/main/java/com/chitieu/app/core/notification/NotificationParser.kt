package com.chitieu.app.core.notification

import com.chitieu.app.core.model.TransactionType
import java.util.regex.Pattern

data class ParseResult(
    val amount: Long,
    val type: TransactionType,
    val descriptionShort: String
)

/**
 * Parser thông báo ngân hàng/ví bằng regex.
 * Mẫu để trong code (MVP); sau có thể chuyển file cấu hình.
 *
 * Không lưu nội dung thông báo gốc — chỉ trả về amount, type, mô tả ngắn.
 */
object NotificationParser {

    // Số tiền: 50.000đ | 50,000 VND | 50000d | -50.000
    private val amountPatterns = listOf(
        Pattern.compile(
            """(?i)(?:so\s*tien|số\s*tiền|amount|gd|giao\s*dịch)?[:\s]*([+-]?\s*[\d.,]+)\s*(?:đ|d|vnd|vnđ)?""",
            Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE
        ),
        Pattern.compile("""([+-]?\s*[\d]{1,3}(?:[.,\s]\d{3})+|\d+)\s*(?:đ|d|vnd|vnđ)""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(?:^|[^\d])([+-]?\d{4,})(?:[^\d]|$)""")
    )

    private val expenseKeywords = listOf(
        "chi", "thanh toan", "thanh toán", "debit", "spent", "mua", "rut", "rút",
        "chuyen di", "chuyển đi", "payment", "pay", "-"
    )
    private val incomeKeywords = listOf(
        "nhan", "nhận", "credit", "received", "thu", "nap", "nạp",
        "chuyen den", "chuyển đến", "refund", "+"
    )

    fun parse(title: String?, text: String?, bigText: String?): ParseResult? {
        val combined = listOfNotNull(title, text, bigText)
            .joinToString(" ")
            .trim()
        if (combined.isBlank()) return null

        val amount = extractAmount(combined) ?: return null
        if (amount <= 0) return null

        val type = detectType(combined)
        val description = buildDescription(title, text, combined)

        return ParseResult(
            amount = amount,
            type = type,
            descriptionShort = description.take(120)
        )
    }

    private fun extractAmount(text: String): Long? {
        for (pattern in amountPatterns) {
            val matcher = pattern.matcher(text)
            while (matcher.find()) {
                val raw = matcher.group(1) ?: continue
                val normalized = raw
                    .replace(" ", "")
                    .replace(".", "")
                    .replace(",", "")
                    .replace("+", "")
                    .replace("-", "")
                val value = normalized.toLongOrNull() ?: continue
                // Bỏ số quá nhỏ (năm, mã) hoặc quá lớn vô lý
                if (value in 1_000..500_000_000_000L) {
                    return value
                }
            }
        }
        return null
    }

    private fun detectType(text: String): TransactionType {
        val lower = text.lowercase()
        val hasExpense = expenseKeywords.any { lower.contains(it) }
        val hasIncome = incomeKeywords.any { lower.contains(it) }
        return when {
            hasExpense && !hasIncome -> TransactionType.CHI
            hasIncome && !hasExpense -> TransactionType.THU
            // Dấu trừ gần số tiền
            Regex("""-\s*[\d.,]+""").containsMatchIn(text) -> TransactionType.CHI
            Regex("""\+\s*[\d.,]+""").containsMatchIn(text) -> TransactionType.THU
            else -> TransactionType.CHI // mặc định chi
        }
    }

    private fun buildDescription(title: String?, text: String?, combined: String): String {
        val base = when {
            !title.isNullOrBlank() && title.length <= 80 -> title
            !text.isNullOrBlank() -> text.take(80)
            else -> combined.take(80)
        }
        // Che bớt số tài khoản dài nếu có
        return base.replace(Regex("""\d{6,}"""), "***")
    }
}
