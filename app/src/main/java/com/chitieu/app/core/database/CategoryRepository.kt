package com.chitieu.app.core.database

import com.chitieu.app.core.model.Category
import com.chitieu.app.core.model.CategoryType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao
) {
    fun observeAll(): Flow<List<Category>> =
        categoryDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeByType(type: CategoryType): Flow<List<Category>> =
        categoryDao.observeByType(type.name).map { list -> list.map { it.toDomain() } }

    suspend fun getById(id: Long): Category? =
        categoryDao.getById(id)?.toDomain()

    suspend fun seedDefaultsIfNeeded() {
        if (categoryDao.count() == 0) {
            categoryDao.insertAll(DefaultCategories.list)
        }
    }
}
