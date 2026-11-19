package com.chitieu.app.core.smart

import com.chitieu.app.core.database.CategoryHintDao
import com.chitieu.app.core.database.CategoryHintEntity
import com.chitieu.app.core.model.Category
import javax.inject.Inject
import javax.inject.Singleton

data class ClassificationResult(
    val categoryId: Long?,
    val confidence: Confidence,
    val matchedToken: String? = null
)

enum class Confidence {
    HISTORY,    // học từ lịch sử cá nhân
    KEYWORD,    // từ khóa mặc định
    NONE
}

@Singleton
class CategoryClassifier @Inject constructor(
    private val categoryHintDao: CategoryHintDao
) {
    /**
     * Gợi ý danh mục từ note + merchant.
     * Ưu tiên: lịch sử cá nhân (count cao nhất) → từ khóa mặc định → null.
     */
    suspend fun suggest(
        note: String?,
        merchant: String?,
        categories: List<Category>
    ): ClassificationResult {
        val text = listOfNotNull(merchant, note).joinToString(" ")
        val tokens = TextNormalizer.tokenize(text)
        if (tokens.isEmpty()) {
            return ClassificationResult(null, Confidence.NONE)
        }

        // Lớp 1: lịch sử
        val hints = categoryHintDao.findByTokens(tokens)
        if (hints.isNotEmpty()) {
            val best = hints.maxByOrNull { it.count }
            if (best != null && best.count > 0) {
                val exists = categories.any { it.id == best.categoryId }
                if (exists) {
                    return ClassificationResult(
                        categoryId = best.categoryId,
                        confidence = Confidence.HISTORY,
                        matchedToken = best.token
                    )
                }
            }
        }

        // Lớp 2: từ khóa mặc định
        val nameToId = categories.associate { it.name.lowercase() to it.id }
        for (token in tokens) {
            val categoryName = DefaultKeywords.map[token] ?: continue
            val id = nameToId[categoryName.lowercase()]
            if (id != null) {
                return ClassificationResult(
                    categoryId = id,
                    confidence = Confidence.KEYWORD,
                    matchedToken = token
                )
            }
        }

        // Thử match cụm (token nối)
        val joined = tokens.joinToString(" ")
        for ((keyword, categoryName) in DefaultKeywords.map) {
            if (keyword.contains(" ") && joined.contains(keyword)) {
                val id = nameToId[categoryName.lowercase()]
                if (id != null) {
                    return ClassificationResult(
                        categoryId = id,
                        confidence = Confidence.KEYWORD,
                        matchedToken = keyword
                    )
                }
            }
        }

        return ClassificationResult(null, Confidence.NONE)
    }

    /**
     * Học khi user chọn/sửa danh mục.
     * Tăng count cho token + category mới; giảm cho category cũ (nếu có).
     */
    suspend fun learn(
        note: String?,
        merchant: String?,
        newCategoryId: Long,
        oldCategoryId: Long? = null
    ) {
        val tokens = TextNormalizer.tokenize(
            listOfNotNull(merchant, note).joinToString(" ")
        )
        if (tokens.isEmpty()) return

        for (token in tokens) {
            if (oldCategoryId != null && oldCategoryId != newCategoryId) {
                categoryHintDao.decrement(token, oldCategoryId)
                categoryHintDao.deleteIfOne(token, oldCategoryId)
            }
            val existing = categoryHintDao.get(token, newCategoryId)
            if (existing != null) {
                categoryHintDao.increment(token, newCategoryId)
            } else {
                categoryHintDao.upsert(
                    CategoryHintEntity(token = token, categoryId = newCategoryId, count = 1)
                )
            }
        }
    }
}
