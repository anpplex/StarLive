package com.starlive.app.runtime

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log

/**
 * Playing detection with pause grace 8s and gap 3s (INTERACTION-1.0).
 * [rawPlaying] comes from MediaSession; [isEffectivelyPlaying] drives yield.
 */
class PlaybackGate(
    private val onEffectivelyPlayingChanged: (Boolean) -> Unit,
) {
    private val main = Handler(Looper.getMainLooper())
    @Volatile private var rawPlaying = false
    @Volatile private var effectivePlaying = false
    private var lastRawTrueElapsed = 0L
    private var pendingFalse: Runnable? = null

    fun setRawPlaying(playing: Boolean) {
        if (playing) {
            rawPlaying = true
            lastRawTrueElapsed = SystemClock.elapsedRealtime()
            cancelPendingFalse()
            setEffective(true)
            return
        }
        rawPlaying = false
        // Grace: if we just lost session, short gap; if pause, longer
        val grace = if (SystemClock.elapsedRealtime() - lastRawTrueElapsed < 500L) {
            GAP_MS
        } else {
            PAUSE_GRACE_MS
        }
        cancelPendingFalse()
        val r = Runnable {
            if (!rawPlaying) setEffective(false)
        }
        pendingFalse = r
        main.postDelayed(r, grace)
        Log.i(TAG, "rawPlaying=false grace=${grace}ms")
    }

    fun isEffectivelyPlaying(): Boolean = effectivePlaying

    private fun setEffective(v: Boolean) {
        if (effectivePlaying == v) return
        effectivePlaying = v
        Log.i(TAG, "effectivePlaying=$v")
        onEffectivelyPlayingChanged(v)
    }

    private fun cancelPendingFalse() {
        pendingFalse?.let { main.removeCallbacks(it) }
        pendingFalse = null
    }

    companion object {
        private const val TAG = "StarLive"
        const val PAUSE_GRACE_MS = 8_000L
        const val GAP_MS = 3_000L
    }
}
