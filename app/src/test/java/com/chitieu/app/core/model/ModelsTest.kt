package com.chitieu.app.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelsTest {

    @Test
    fun `transaction amount should always be positive conceptually`() {
        val tx = Transaction(
            accountId = 1,
            amount = 50000,
            type = TransactionType.CHI,
            occurredAt = System.currentTimeMillis()
        )
        assertTrue(tx.amount > 0)
    }

    @Test
    fun `account initial balance can be zero`() {
        val account = Account(
            name = "Tiền mặt",
            type = AccountType.TIEN_MAT,
            initialBalance = 0
        )
        assertEquals(0L, account.initialBalance)
    }

    @Test
    fun `transferId is null for normal transactions`() {
        val tx = Transaction(
            accountId = 1,
            amount = 10000,
            type = TransactionType.CHI,
            occurredAt = System.currentTimeMillis()
        )
        assertEquals(null, tx.transferId)
    }
}
