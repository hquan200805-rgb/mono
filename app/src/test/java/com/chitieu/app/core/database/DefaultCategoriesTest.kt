package com.chitieu.app.core.database

import com.chitieu.app.core.model.CategoryType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultCategoriesTest {

    @Test
    fun `default categories should not be empty`() {
        assertTrue(DefaultCategories.list.isNotEmpty())
    }

    @Test
    fun `should contain both CHI and THU categories`() {
        val hasChi = DefaultCategories.list.any { it.type == CategoryType.CHI }
        val hasThu = DefaultCategories.list.any { it.type == CategoryType.THU }
        assertTrue(hasChi)
        assertTrue(hasThu)
    }

    @Test
    fun `all default categories should be marked isDefault`() {
        assertTrue(DefaultCategories.list.all { it.isDefault })
    }

    @Test
    fun `category names should be unique`() {
        val names = DefaultCategories.list.map { it.name }
        assertEquals(names.size, names.toSet().size)
    }

    @Test
    fun `should have expected number of categories`() {
        // 10 CHI + 4 THU = 14
        assertEquals(14, DefaultCategories.list.size)
    }
}
