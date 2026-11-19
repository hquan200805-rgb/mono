package com.chitieu.app.di

import com.chitieu.app.core.database.AccountRepository
import com.chitieu.app.core.database.CategoryRepository
import com.chitieu.app.core.database.TransactionRepository
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Các Repository đã dùng @Inject constructor + @Singleton
 * nên Hilt tự cung cấp. Module này để dành cho binding sau này nếu cần.
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule
