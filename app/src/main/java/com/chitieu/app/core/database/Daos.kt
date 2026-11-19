package com.chitieu.app.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE isArchived = 0 ORDER BY name")
    fun observeActiveAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts ORDER BY name")
    fun observeAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getById(id: Long): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: AccountEntity): Long

    @Update
    suspend fun update(account: AccountEntity)

    @Query("UPDATE accounts SET isArchived = 1 WHERE id = :id")
    suspend fun archive(id: Long)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder, name")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE type = :type ORDER BY sortOrder, name")
    fun observeByType(type: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Update
    suspend fun update(category: CategoryEntity)

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY occurredAt DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query(
        """
        SELECT * FROM transactions 
        WHERE occurredAt >= :from AND occurredAt < :to 
        ORDER BY occurredAt DESC
        """
    )
    fun observeInRange(from: Long, to: Long): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT * FROM transactions 
        WHERE type = 'CHI' AND occurredAt >= :from AND occurredAt < :to
        """
    )
    fun observeExpensesInRange(from: Long, to: Long): Flow<List<TransactionEntity>>
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE month = :month")
    fun observeByMonth(month: String): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE month = :month AND categoryId IS NULL LIMIT 1")
    suspend fun getOverall(month: String): BudgetEntity?

    @Query("SELECT * FROM budgets WHERE month = :month AND categoryId = :categoryId LIMIT 1")
    suspend fun getByCategory(month: String, categoryId: Long): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(budget: BudgetEntity): Long

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteById(id: Long)
}


@Dao
interface CategoryHintDao {
    @Query("SELECT * FROM category_hints WHERE token = :token ORDER BY count DESC")
    suspend fun findByToken(token: String): List<CategoryHintEntity>

    @Query("SELECT * FROM category_hints WHERE token IN (:tokens) ORDER BY count DESC")
    suspend fun findByTokens(tokens: List<String>): List<CategoryHintEntity>

    @Query("SELECT * FROM category_hints WHERE token = :token AND categoryId = :categoryId LIMIT 1")
    suspend fun get(token: String, categoryId: Long): CategoryHintEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(hint: CategoryHintEntity): Long

    @Query("UPDATE category_hints SET count = count + 1 WHERE token = :token AND categoryId = :categoryId")
    suspend fun increment(token: String, categoryId: Long): Int

    @Query("DELETE FROM category_hints WHERE token = :token AND categoryId = :categoryId AND count <= 1")
    suspend fun deleteIfOne(token: String, categoryId: Long): Int

    @Query("UPDATE category_hints SET count = count - 1 WHERE token = :token AND categoryId = :categoryId AND count > 1")
    suspend fun decrement(token: String, categoryId: Long): Int
}


@Dao
interface PendingTransactionDao {
    @Query("SELECT * FROM pending_transactions WHERE status = 'CHO' ORDER BY detectedAt DESC")
    fun observePending(): Flow<List<PendingTransactionEntity>>

    @Query("SELECT COUNT(*) FROM pending_transactions WHERE status = 'CHO'")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT * FROM pending_transactions WHERE id = :id")
    suspend fun getById(id: Long): PendingTransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PendingTransactionEntity): Long

    @Query("UPDATE pending_transactions SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query(
        """
        SELECT * FROM pending_transactions 
        WHERE amount = :amount AND sourcePackage = :sourcePackage 
          AND detectedAt >= :since AND status = 'CHO'
        LIMIT 1
        """
    )
    suspend fun findDuplicate(amount: Long, sourcePackage: String, since: Long): PendingTransactionEntity?

    @Query("DELETE FROM pending_transactions WHERE status != 'CHO' AND detectedAt < :before")
    suspend fun purgeOld(before: Long)
}


@Dao
interface RecurringRuleDao {
    @Query("SELECT * FROM recurring_rules ORDER BY nextDueDate ASC")
    fun observeAll(): Flow<List<RecurringRuleEntity>>

    @Query("SELECT * FROM recurring_rules WHERE isConfirmed = 1 ORDER BY nextDueDate ASC")
    fun observeConfirmed(): Flow<List<RecurringRuleEntity>>

    @Query("SELECT * FROM recurring_rules WHERE merchantKey = :key LIMIT 1")
    suspend fun getByMerchantKey(key: String): RecurringRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: RecurringRuleEntity): Long

    @Query("UPDATE recurring_rules SET isConfirmed = 1 WHERE id = :id")
    suspend fun confirm(id: Long)

    @Query("DELETE FROM recurring_rules WHERE id = :id")
    suspend fun delete(id: Long)
}
