package com.chitieu.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        CategoryHintEntity::class,
        PendingTransactionEntity::class,
        RecurringRuleEntity::class
    ],
    version = 5,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun categoryHintDao(): CategoryHintDao
    abstract fun pendingTransactionDao(): PendingTransactionDao
    abstract fun recurringRuleDao(): RecurringRuleDao
}
