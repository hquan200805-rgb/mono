package com.chitieu.app.di

import android.content.Context
import androidx.room.Room
import com.chitieu.app.core.database.AppDatabase
import com.chitieu.app.core.database.AccountDao
import com.chitieu.app.core.database.BudgetDao
import com.chitieu.app.core.database.CategoryDao
import com.chitieu.app.core.database.CategoryHintDao
import com.chitieu.app.core.database.PendingTransactionDao
import com.chitieu.app.core.database.RecurringRuleDao
import com.chitieu.app.core.database.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "chitieu.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides fun provideAccountDao(db: AppDatabase): AccountDao = db.accountDao()
    @Provides fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()
    @Provides fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()
    @Provides fun provideBudgetDao(db: AppDatabase): BudgetDao = db.budgetDao()
    @Provides fun provideCategoryHintDao(db: AppDatabase): CategoryHintDao = db.categoryHintDao()
    @Provides fun providePendingTransactionDao(db: AppDatabase): PendingTransactionDao = db.pendingTransactionDao()
    @Provides fun provideRecurringRuleDao(db: AppDatabase): RecurringRuleDao = db.recurringRuleDao()
}
