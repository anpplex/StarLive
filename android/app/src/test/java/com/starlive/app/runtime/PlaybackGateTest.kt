package com.starlive.app.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * M3: track-change gap <3s must not clear effective playing.
 * Pause ≥8s clears yield.
 */
class PlaybackGateTest {

    private class FakeScheduler : PlaybackGate.Scheduler {
        data class Job(val at: Long, val action: () -> Unit, var cancelled: Boolean = false)

        var now = 0L
        private val jobs = mutableListOf<Job>()

        override fun postDelayed(delayMs: Long, action: () -> Unit): PlaybackGate.Cancelable {
            val job = Job(now + delayMs, action)
            jobs += job
            return PlaybackGate.Cancelable { job.cancelled = true }
        }

        fun advance(ms: Long) {
            now += ms
            val due = jobs.filter { !it.cancelled && it.at <= now }
            jobs.removeAll { it.cancelled || it.at <= now }
            due.sortedBy { it.at }.forEach { it.action() }
        }
    }

    @Test
    fun track_gap_under_3s_keeps_yield_m3() {
        val sched = FakeScheduler()
        val flips = mutableListOf<Boolean>()
        val gate = PlaybackGate(
            onEffectivelyPlayingChanged = { flips += it },
            clockMs = { sched.now },
            scheduler = sched,
        )

        gate.setRawPlaying(true)
        assertTrue(gate.isEffectivelyPlaying())
        assertEquals(listOf(true), flips)

        // Track change: false then true within <3s (and sinceTrue <500 → GAP_MS)
        gate.setRawPlaying(false)
        assertTrue("still effective during grace", gate.isEffectivelyPlaying())
        sched.advance(2_000L)
        assertTrue(gate.isEffectivelyPlaying())
        gate.setRawPlaying(true)
        assertTrue(gate.isEffectivelyPlaying())
        // previous false timer cancelled — still playing
        sched.advance(5_000L)
        assertTrue(gate.isEffectivelyPlaying())
        assertEquals(listOf(true), flips) // no false flip
    }

    @Test
    fun pause_8s_clears_yield() {
        val sched = FakeScheduler()
        val flips = mutableListOf<Boolean>()
        val gate = PlaybackGate(
            onEffectivelyPlayingChanged = { flips += it },
            clockMs = { sched.now },
            scheduler = sched,
        )

        gate.setRawPlaying(true)
        sched.now = 10_000L // been playing a while → PAUSE_GRACE
        gate.setRawPlaying(false)
        assertTrue(gate.isEffectivelyPlaying())
        sched.advance(7_999L)
        assertTrue(gate.isEffectivelyPlaying())
        sched.advance(1L)
        assertFalse(gate.isEffectivelyPlaying())
        assertEquals(listOf(true, false), flips)
    }

    @Test
    fun short_session_uses_3s_gap() {
        val sched = FakeScheduler()
        val gate = PlaybackGate(
            onEffectivelyPlayingChanged = {},
            clockMs = { sched.now },
            scheduler = sched,
        )
        gate.setRawPlaying(true)
        // immediately false → sinceTrue ~0 < 500 → GAP 3s
        gate.setRawPlaying(false)
        sched.advance(2_999L)
        assertTrue(gate.isEffectivelyPlaying())
        sched.advance(1L)
        assertFalse(gate.isEffectivelyPlaying())
    }
}
