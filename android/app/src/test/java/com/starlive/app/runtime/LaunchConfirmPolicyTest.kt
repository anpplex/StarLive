package com.starlive.app.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LaunchConfirmPolicyTest {
    @Test
    fun timeout_is_five_seconds() {
        assertEquals(5_000L, LaunchConfirmPolicy.TIMEOUT_MS)
    }

    @Test
    fun timeout_covers_main_delayed_refresh_window() {
        // MainActivity posts refresh at 900 / 3200 / 5500 — timeout must be ≤ last refresh.
        assertTrue(LaunchConfirmPolicy.TIMEOUT_MS <= 5_500L)
        assertTrue(LaunchConfirmPolicy.TIMEOUT_MS >= 4_000L)
    }
}
