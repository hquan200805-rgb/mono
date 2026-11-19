package com.chitieu.app.core.model

/**
 * Domain models (pure Kotlin data classes).
 * These are independent of Room / UI.
 */

enum class AccountType {
    TIEN_MAT,
    NGAN_HANG,
    VI_DIEN_TU
}

enum class CategoryType {
    CHI,
    THU
}

enum class TransactionType {
    CHI,
    THU,
    CHUYEN_KHOAN
}

enum class TransactionSource {
    THU_CONG,
    THONG_BAO
}

data class Account(
    val id: Long = 0,
    val name: String,
    val type: AccountType,
    val initialBalance: Long,
    val isArchived: Boolean = false
)

data class Category(
    val id: Long = 0,
    val name: String,
    val icon: String,
    val colorHex: String,
    val type: CategoryType,
    val isDefault: Boolean = false,
    val sortOrder: Int = 0
)

data class Transaction(
    val id: Long = 0,
    val accountId: Long,
    val categoryId: Long? = null,
    val amount: Long,
    val type: TransactionType,
    val note: String? = null,
    val merchant: String? = null,
    val occurredAt: Long,
    val source: TransactionSource = TransactionSource.THU_CONG,
    val recurringId: Long? = null,
    val transferId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Ngân sách theo tháng.
 * categoryId = null → ngân sách tổng (toàn bộ chi tiêu).
 */
data class Budget(
    val id: Long = 0,
    val categoryId: Long? = null,
    val month: String,              // yyyy-MM
    val limitAmount: Long           // đơn vị đồng
)


enum class PendingStatus {
    CHO,
    DA_DUYET,
    BO_QUA
}

/** Giao dịch chờ xác nhận từ thông báo — không lưu nội dung thông báo gốc. */
data class PendingTransaction(
    val id: Long = 0,
    val amount: Long,
    val type: TransactionType,
    val descriptionShort: String,
    val suggestedCategoryId: Long? = null,
    val detectedAt: Long = System.currentTimeMillis(),
    val sourcePackage: String,
    val status: PendingStatus = PendingStatus.CHO
)


data class RecurringRule(
    val id: Long = 0,
    val merchantKey: String,
    val categoryId: Long? = null,
    val expectedAmount: Long,
    val periodDays: Int,
    val nextDueDate: Long,          // epoch ms (start of day)
    val isConfirmed: Boolean = false
)
