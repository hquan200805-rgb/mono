package com.chitieu.app.core.smart

/**
 * Chuẩn hóa văn bản tiếng Việt để match từ khóa / lịch sử.
 * - lower case
 * - bỏ dấu
 * - tách token theo khoảng trắng và ký tự đặc biệt
 */
object TextNormalizer {

    private val VIETNAMESE_MAP = mapOf(
        'à' to 'a', 'á' to 'a', 'ả' to 'a', 'ã' to 'a', 'ạ' to 'a',
        'ă' to 'a', 'ằ' to 'a', 'ắ' to 'a', 'ẳ' to 'a', 'ẵ' to 'a', 'ặ' to 'a',
        'â' to 'a', 'ầ' to 'a', 'ấ' to 'a', 'ẩ' to 'a', 'ẫ' to 'a', 'ậ' to 'a',
        'è' to 'e', 'é' to 'e', 'ẻ' to 'e', 'ẽ' to 'e', 'ẹ' to 'e',
        'ê' to 'e', 'ề' to 'e', 'ế' to 'e', 'ể' to 'e', 'ễ' to 'e', 'ệ' to 'e',
        'ì' to 'i', 'í' to 'i', 'ỉ' to 'i', 'ĩ' to 'i', 'ị' to 'i',
        'ò' to 'o', 'ó' to 'o', 'ỏ' to 'o', 'õ' to 'o', 'ọ' to 'o',
        'ô' to 'o', 'ồ' to 'o', 'ố' to 'o', 'ổ' to 'o', 'ỗ' to 'o', 'ộ' to 'o',
        'ơ' to 'o', 'ờ' to 'o', 'ớ' to 'o', 'ở' to 'o', 'ỡ' to 'o', 'ợ' to 'o',
        'ù' to 'u', 'ú' to 'u', 'ủ' to 'u', 'ũ' to 'u', 'ụ' to 'u',
        'ư' to 'u', 'ừ' to 'u', 'ứ' to 'u', 'ử' to 'u', 'ữ' to 'u', 'ự' to 'u',
        'ỳ' to 'y', 'ý' to 'y', 'ỷ' to 'y', 'ỹ' to 'y', 'ỵ' to 'y',
        'đ' to 'd'
    )

    fun removeDiacritics(input: String): String {
        val sb = StringBuilder(input.length)
        for (c in input.lowercase()) {
            sb.append(VIETNAMESE_MAP[c] ?: c)
        }
        return sb.toString()
    }

    /** Trả về danh sách token đã chuẩn hóa, bỏ token quá ngắn. */
    fun tokenize(input: String): List<String> {
        if (input.isBlank()) return emptyList()
        val normalized = removeDiacritics(input)
        return normalized
            .split(Regex("[\\s\\p{Punct}]+"))
            .map { it.trim() }
            .filter { it.length >= 2 }
            .distinct()
    }
}
