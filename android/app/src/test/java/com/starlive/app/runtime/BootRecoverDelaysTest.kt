package com.starlive.app.runtime

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BootRecoverDelaysTest {
    @Test
    fun process_start_is_snappier() {
        val d = BootRecoverDelays.forReason("process-start")
        assertEquals(400L, d[0])
        assertTrue(d[0] < BootRecoverDelays.BOOT[0])
    }

    @Test
    fun boot_default() {
        assertArrayEquals(BootRecoverDelays.BOOT, BootRecoverDelays.forReason("boot"))
        assertArrayEquals(BootRecoverDelays.BOOT, BootRecoverDelays.forReason("USER_PRESENT"))
        assertArrayEquals(BootRecoverDelays.BOOT, BootRecoverDelays.forReason("MY_PACKAGE_REPLACED"))
    }

    @Test
    fun process_start_prefix() {
        assertArrayEquals(
            BootRecoverDelays.PROCESS_START,
            BootRecoverDelays.forReason("process-start#0"),
        )
    }
}
