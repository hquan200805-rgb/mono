package com.chitieu.app.core.smart

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextNormalizerTest {

    @Test
    fun `remove diacritics vietnamese`() {
        assertEquals("ca phe", TextNormalizer.removeDiacritics("Cà phê"))
        assertEquals("phuc long", TextNormalizer.removeDiacritics("Phúc Long"))
        assertEquals("bach hoa xanh", TextNormalizer.removeDiacritics("Bách hóa xanh"))
    }

    @Test
    fun `tokenize splits and filters short`() {
        val tokens = TextNormalizer.tokenize("Grab Food - Hà Nội!")
        assertTrue(tokens.contains("grab"))
        assertTrue(tokens.contains("food"))
        assertTrue(tokens.contains("ha"))
        assertTrue(tokens.contains("noi"))
        assertFalse(tokens.any { it.length < 2 })
    }

    @Test
    fun `empty input returns empty`() {
        assertTrue(TextNormalizer.tokenize("").isEmpty())
        assertTrue(TextNormalizer.tokenize("   ").isEmpty())
    }
}
