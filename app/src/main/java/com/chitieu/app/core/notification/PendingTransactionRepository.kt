package com.chitieu.app.core.notification

import com.chitieu.app.core.database.PendingTransactionDao
import com.chitieu.app.core.database.toDomain
import com.chitieu.app.core.database.toEntity
import com.chitieu.app.core.model.PendingStatus
import com.chitieu.app.core.model.PendingTransaction
import com.chitieu.app.core.model.Transaction
import com.chitieu.app.core.model.TransactionSource
import com.chitieu.app.core.database.AccountRepository
import com.chitieu.app.core.database.TransactionRepository
import com.chitieu.app.core.smart.CategoryClassifier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PendingTransactionRepository @Inject constructor(
    private val pendingDao: PendingTransactionDao,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryClassifier: CategoryClassifier
) {
    fun observePending(): Flow<List<PendingTransaction>> =
        pendingDao.observePending().map { list -> list.map { it.toDomain() } }

    fun observePendingCount(): Flow<Int> = pendingDao.observePendingCount()

    /**
     * Thêm pending nếu không trùng (cùng amount + package trong 5 phút).
     * @return id mới hoặc null nếu bị coi là trùng.
     */
    suspend fun addIfNotDuplicate(pending: PendingTransaction): Long? {
        val since = System.currentTimeMillis() - 5 * 60 * 1000L
        val dup = pendingDao.findDuplicate(pending.amount, pending.sourcePackage, since)
        if (dup != null) return null
        return pendingDao.insert(pending.toEntity())
    }

    suspend fun approve(id: Long, accountId: Long? = null): Boolean {
        val entity = pendingDao.getById(id) ?: return false
        val pending = entity.toDomain()
        if (pending.status != PendingStatus.CHO) return false

        val accounts = accountRepository.observeActiveAccounts().first()
        val targetAccountId = accountId ?: accounts.firstOrNull()?.id ?: return false

        // Gợi ý danh mục từ mô tả ngắn
        val categories = emptyList<com.chitieu.app.core.model.Category>() // classifier cần list; học sau
        val suggested = pending.suggestedCategoryId

        transactionRepository.insert(
            Transaction(
                accountId = targetAccountId,
                categoryId = suggested,
                amount = pending.amount,
                type = pending.type,
                note = pending.descriptionShort,
                merchant = SupportedPackages.displayName(pending.sourcePackage),
                occurredAt = pending.detectedAt,
                source = TransactionSource.THONG_BAO
            )
        )

        if (suggested != null) {
            categoryClassifier.learn(
                note = pending.descriptionShort,
                merchant = SupportedPackages.displayName(pending.sourcePackage),
                newCategoryId = suggested
            )
        }

        pendingDao.updateStatus(id, PendingStatus.DA_DUYET.name)
        return true
    }

    suspend fun dismiss(id: Long) {
        pendingDao.updateStatus(id, PendingStatus.BO_QUA.name)
    }
}
