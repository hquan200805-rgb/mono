package com.chitieu.app.core.database

import com.chitieu.app.core.model.CategoryType

/**
 * Danh mục mặc định được seed khi app khởi chạy lần đầu.
 */
object DefaultCategories {

    val list: List<CategoryEntity> = listOf(
        // Chi
        CategoryEntity(name = "Ăn uống", icon = "🍜", colorHex = "#FF7043", type = CategoryType.CHI, isDefault = true, sortOrder = 1),
        CategoryEntity(name = "Cà phê", icon = "☕", colorHex = "#8D6E63", type = CategoryType.CHI, isDefault = true, sortOrder = 2),
        CategoryEntity(name = "Di chuyển", icon = "🚗", colorHex = "#42A5F5", type = CategoryType.CHI, isDefault = true, sortOrder = 3),
        CategoryEntity(name = "Mua sắm", icon = "🛒", colorHex = "#AB47BC", type = CategoryType.CHI, isDefault = true, sortOrder = 4),
        CategoryEntity(name = "Nhà cửa", icon = "🏠", colorHex = "#66BB6A", type = CategoryType.CHI, isDefault = true, sortOrder = 5),
        CategoryEntity(name = "Hóa đơn", icon = "📄", colorHex = "#78909C", type = CategoryType.CHI, isDefault = true, sortOrder = 6),
        CategoryEntity(name = "Sức khỏe", icon = "💊", colorHex = "#EF5350", type = CategoryType.CHI, isDefault = true, sortOrder = 7),
        CategoryEntity(name = "Giải trí", icon = "🎮", colorHex = "#EC407A", type = CategoryType.CHI, isDefault = true, sortOrder = 8),
        CategoryEntity(name = "Giáo dục", icon = "📚", colorHex = "#5C6BC0", type = CategoryType.CHI, isDefault = true, sortOrder = 9),
        CategoryEntity(name = "Khác", icon = "📦", colorHex = "#BDBDBD", type = CategoryType.CHI, isDefault = true, sortOrder = 99),

        // Thu
        CategoryEntity(name = "Lương", icon = "💰", colorHex = "#26A69A", type = CategoryType.THU, isDefault = true, sortOrder = 1),
        CategoryEntity(name = "Thưởng", icon = "🎁", colorHex = "#FFA726", type = CategoryType.THU, isDefault = true, sortOrder = 2),
        CategoryEntity(name = "Đầu tư", icon = "📈", colorHex = "#29B6F6", type = CategoryType.THU, isDefault = true, sortOrder = 3),
        CategoryEntity(name = "Thu khác", icon = "💵", colorHex = "#9CCC65", type = CategoryType.THU, isDefault = true, sortOrder = 99),
    )
}
