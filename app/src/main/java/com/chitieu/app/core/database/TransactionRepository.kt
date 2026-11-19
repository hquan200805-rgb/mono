package com.chitieu.app.core.database

import com.chitieu.app.core.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao
) {
    fun observeAll(): Flow<List<Transaction>> =
        transactionDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeInRange(from: Long, to: Long): Flow<List<Transaction>> =
        transactionDao.observeInRange(from, to).map { list -> list.map { it.toDomain() } }

    suspend fun getById(id: Long): Transaction? =
        transactionDao.getById(id)?.toDomain()

    suspend fun insert(transaction: Transaction): Long =
        transactionDao.insert(transaction.toEntity())

    suspend fun update(transaction: Transaction) =
        transactionDao.update(transaction.toEntity().copy(updatedAt = System.currentTimeMillis()))

    suspend fun delete(id: Long) =
        transactionDao.deleteById(id)
}
