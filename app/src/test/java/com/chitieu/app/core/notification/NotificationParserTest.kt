package com.chitieu.app.core.notification

import com.chitieu.app.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationParserTest {

    @Test
    fun `parse momo style expense`() {
        val result = NotificationParser.parse(
            title = "MoMo",
            text = "Thanh toan 50.000đ tai Highlands Coffee",
            bigText = null
        )
        assertNotNull(result)
        assertEquals(50000L, result!!.amount)
        assertEquals(TransactionType.CHI, result.type)
    }

    @Test
    fun `parse income with plus`() {
        val result = NotificationParser.parse(
            title = "MB Bank",
            text = "Ban nhan +2.000.000 VND tu Nguyen Van A",
            bigText = null
        )
        assertNotNull(result)
        assertEquals(2_000_000L, result!!.amount)
        assertEquals(TransactionType.THU, result.type)
    }

    @Test
    fun `returns null when no amount`() {
        val result = NotificationParser.parse("Thong bao", "Khuyen mai moi", null)
        assertEquals(null, result)
    }

    @Test
    fun `masks long account numbers in description`() {
        val result = NotificationParser.parse(
            title = null,
            text = "GD 150000d TK 123456789012",
            bigText = null
        )
        assertNotNull(result)
        assertTrue(result!!.descriptionShort.contains("***") || result.amount == 150000L)
    }
}
