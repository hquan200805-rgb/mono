package com.chitieu.app.core.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.chitieu.app.core.model.AccountType
import com.chitieu.app.core.model.CategoryType
import com.chitieu.app.core.model.TransactionSource
import com.chitieu.app.core.model.TransactionType

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: AccountType,
    val initialBalance: Long,
    val isArchived: Boolean = false
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String,
    val colorHex: String,
    val type: CategoryType,
    val isDefault: Boolean = false,
    val sortOrder: Int = 0
)

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("occurredAt"),
        Index("categoryId", "occurredAt"),
        Index("accountId"),
        Index("transferId")
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
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

@Entity(
    tableName = "budgets",
    indices = [Index(value = ["month", "categoryId"], unique = true)]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long? = null,
    val month: String,
    val limitAmount: Long
)

@Entity(
    tableName = "category_hints",
    indices = [
        Index(value = ["token", "categoryId"], unique = true),
        Index("token")
    ]
)
data class CategoryHintEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val token: String,
    val categoryId: Long,
    val count: Int = 1
)


@Entity(
    tableName = "pending_transactions",
    indices = [Index("status"), Index("detectedAt")]
)
data class PendingTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Long,
    val type: TransactionType,
    val descriptionShort: String,
    val suggestedCategoryId: Long? = null,
    val detectedAt: Long = System.currentTimeMillis(),
    val sourcePackage: String,
    val status: String = "CHO"  // CHO | DA_DUYET | BO_QUA
)


@Entity(
    tableName = "recurring_rules",
    indices = [Index("merchantKey"), Index("nextDueDate")]
)
data class RecurringRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val merchantKey: String,
    val categoryId: Long? = null,
    val expectedAmount: Long,
    val periodDays: Int,
    val nextDueDate: Long,
    val isConfirmed: Boolean = false
)
