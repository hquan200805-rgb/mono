package com.chitieu.app.core.common

import com.chitieu.app.core.database.AccountRepository
import com.chitieu.app.core.database.CategoryRepository
import com.chitieu.app.core.model.Account
import com.chitieu.app.core.model.AccountType
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SeedData @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository
) {
    suspend fun seedIfNeeded() {
        categoryRepository.seedDefaultsIfNeeded()

        val accounts = accountRepository.observeActiveAccounts().first()
        if (accounts.isEmpty()) {
            accountRepository.insert(
                Account(
                    name = "Tiền mặt",
                    type = AccountType.TIEN_MAT,
                    initialBalance = 0
                )
            )
            accountRepository.insert(
                Account(
                    name = "Ngân hàng",
                    type = AccountType.NGAN_HANG,
                    initialBalance = 0
                )
            )
        }
    }
}
