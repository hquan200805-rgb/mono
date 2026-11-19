package com.chitieu.app.core.smart

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultKeywordsTest {

    @Test
    fun `common merchants map to expected categories`() {
        assertEquals("Di chuyển", DefaultKeywords.map["grab"])
        assertEquals("Cà phê", DefaultKeywords.map["highlands"])
        assertEquals("Ăn uống", DefaultKeywords.map["winmart"])
        assertEquals("Mua sắm", DefaultKeywords.map["shopee"])
    }

    @Test
    fun `keywords are lowercase without diacritics style`() {
        DefaultKeywords.map.keys.forEach { key ->
            assertEquals(key, key.lowercase())
        }
    }

    @Test
    fun `has reasonable coverage`() {
        assertTrue(DefaultKeywords.map.size >= 30)
    }
}
