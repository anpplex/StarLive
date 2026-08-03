package com.starlive.app.runtime

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log

/**
 * Playing detection with pause grace 8s and gap 3s (INTERACTION-1.0).
 * [rawPlaying] comes from MediaSession; [isEffectivelyPlaying] drives yield.
 *
 * Injectable [clockMs] / [scheduler] for JVM unit tests (M3 切歌空隙).
 */
class PlaybackGate(
    private val onEffectivelyPlayingChanged: (Boolean) -> Unit,
    private val clockMs: () -> Long = { SystemClock.elapsedRealtime() },
    private val scheduler: Scheduler = AndroidScheduler(),
) {
    fun interface Scheduler {
        fun postDelayed(delayMs: Long, action: () -> Unit): Cancelable
    }

    fun interface Cancelable {
        fun cancel()
    }

    private class AndroidScheduler : Scheduler {
        private val main = Handler(Looper.getMainLooper())
        override fun postDelayed(delayMs: Long, action: () -> Unit): Cancelable {
            val r = Runnable { action() }
            main.postDelayed(r, delayMs)
            return Cancelable { main.removeCallbacks(r) }
        }
    }

    @Volatile private var rawPlaying = false
    @Volatile private var effectivePlaying = false
    private var lastRawTrueElapsed = 0L
    private var pendingFalse: Cancelable? = null

    fun setRawPlaying(playing: Boolean) {
        if (playing) {
            rawPlaying = true
            lastRawTrueElapsed = clockMs()
            cancelPendingFalse()
            setEffective(true)
            return
        }
        rawPlaying = false
        // Grace: short gap if session just flipped (track change); longer if pause
        val sinceTrue = clockMs() - lastRawTrueElapsed
        val grace = if (sinceTrue < 500L) GAP_MS else PAUSE_GRACE_MS
        cancelPendingFalse()
        pendingFalse = scheduler.postDelayed(grace) {
            if (!rawPlaying) setEffective(false)
        }
        Log.i(TAG, "rawPlaying=false grace=${grace}ms sinceTrue=${sinceTrue}ms")
    }

    fun isEffectivelyPlaying(): Boolean = effectivePlaying

    private fun setEffective(v: Boolean) {
        if (effectivePlaying == v) return
        effectivePlaying = v
        Log.i(TAG, "effectivePlaying=$v")
        onEffectivelyPlayingChanged(v)
    }

    private fun cancelPendingFalse() {
        pendingFalse?.cancel()
        pendingFalse = null
    }

    companion object {
        private const val TAG = "StarLive"
        const val PAUSE_GRACE_MS = 8_000L
        const val GAP_MS = 3_000L
    }
}
