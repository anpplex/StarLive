package com.starlive.app.redeem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class RedeemExchangeParserTest {

    @Test
    fun success_first_bind() {
        val body =
            """{"ok":true,"pack_id":"pack_night","title":"夜色","version":2,"sha256":"abc","download_url":"https://x/p","already_bound":false}"""
        val ok = RedeemExchangeParser.parse(200, body)
        assertEquals("pack_night", ok.packId)
        assertEquals("夜色", ok.title)
        assertEquals(2, ok.version)
        assertEquals("abc", ok.sha256)
        assertEquals("https://x/p", ok.downloadUrl)
        assertFalse(ok.alreadyBound)
    }

    @Test
    fun success_idempotent_r2() {
        val body =
            """{"ok":true,"pack_id":"p1","title":"t","version":1,"sha256":"s","download_url":"https://d","already_bound":true}"""
        val ok = RedeemExchangeParser.parse(200, body)
        assertTrue(ok.alreadyBound)
    }

    @Test
    fun invalid_code_uses_server_error() {
        val ex = assertThrows(IllegalStateException::class.java) {
            RedeemExchangeParser.parse(400, """{"ok":false,"error":"兑换码无效"}""")
        }
        assertEquals("兑换码无效", ex.message)
    }

    @Test
    fun revoked_r4_http_410() {
        val ex = assertThrows(IllegalStateException::class.java) {
            RedeemExchangeParser.parse(410, """{"ok":false,"error":"兑换码不可用"}""")
        }
        assertEquals("兑换码不可用", ex.message)
    }

    @Test
    fun device_limit_r3_http_409() {
        val msg = RedeemExchangeParser.userMessage(409, null, null)
        assertTrue(msg.contains("上限"))
    }

    @Test
    fun offline_r6_http_0() {
        val msg = RedeemExchangeParser.userMessage(0, null, null)
        assertTrue(msg.contains("网络"))
    }

    @Test
    fun missing_fields_fail() {
        assertThrows(IllegalStateException::class.java) {
            RedeemExchangeParser.parse(200, """{"ok":true,"pack_id":"p"}""")
        }
    }
}
