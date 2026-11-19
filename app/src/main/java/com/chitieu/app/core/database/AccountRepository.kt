package com.chitieu.app.core.database

import com.chitieu.app.core.model.Account
import com.chitieu.app.core.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class AccountWithBalance(
    val account: Account,
    val currentBalance: Long
)

@Singleton
class AccountRepository @Inject constructor(
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao
) {
    fun observeActiveAccounts(): Flow<List<Account>> =
        accountDao.observeActiveAccounts().map { list -> list.map { it.toDomain() } }

    /**
     * Số dư realtime = initialBalance + tổng THU - tổng CHI
     * (Giao dịch chuyển khoản sẽ được ghi nhận như CHI ở ví nguồn và THU ở ví đích)
     */
    fun observeAccountsWithBalance(): Flow<List<AccountWithBalance>> {
        return combine(
            accountDao.observeActiveAccounts(),
            transactionDao.observeAll()
        ) { accounts, transactions ->
            accounts.map { accEntity ->
                val acc = accEntity.toDomain()
                val related = transactions.filter { it.accountId == acc.id }
                val income = related
                    .filter { it.type == TransactionType.THU }
                    .sumOf { it.amount }
                val expense = related
                    .filter { it.type == TransactionType.CHI }
                    .sumOf { it.amount }
                AccountWithBalance(
                    account = acc,
                    currentBalance = acc.initialBalance + income - expense
                )
            }
        }
    }

    suspend fun getById(id: Long): Account? =
        accountDao.getById(id)?.toDomain()

    suspend fun insert(account: Account): Long =
        accountDao.insert(account.toEntity())

    suspend fun update(account: Account) =
        accountDao.update(account.toEntity())

    suspend fun archive(id: Long) =
        accountDao.archive(id)
}
