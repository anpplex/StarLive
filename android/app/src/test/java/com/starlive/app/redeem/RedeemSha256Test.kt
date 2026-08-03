package com.starlive.app.redeem

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class RedeemSha256Test {
    @Test
    fun sha256_known() {
        val f = File.createTempFile("starlive-sha", ".bin")
        f.writeBytes("hello".toByteArray())
        // echo -n hello | shasum -a 256
        assertEquals(
            "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
            RedeemClient.sha256Hex(f),
        )
        f.delete()
    }
}
