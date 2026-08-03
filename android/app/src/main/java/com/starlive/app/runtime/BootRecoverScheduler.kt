package com.starlive.app.runtime

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.starlive.app.wallpaper.WallpaperRepository

class BootRecoverScheduler(
    private val app: android.app.Application,
    private val orchestrator: StripOrchestrator,
) {
    private val main = Handler(Looper.getMainLooper())
    private var lastScheduleMs = 0L

    /**
     * @param reason boot / USER_PRESENT / process-start / …
     * process-start uses a snappier first tick (user already opened the app).
     */
    fun schedule(reason: String) {
        if (!WallpaperRepository.idlePrefer(app)) {
            Log.i(TAG, "boot recover skip idle off ($reason)")
            return
        }
        val now = System.currentTimeMillis()
        if (now - lastScheduleMs < BootRecoverDelays.DEBOUNCE_MS) {
            Log.i(TAG, "boot recover debounce ($reason)")
            return
        }
        lastScheduleMs = now
        val delays = BootRecoverDelays.forReason(reason)
        delays.forEachIndexed { i, delay ->
            main.postDelayed({ tick("$reason#$i") }, delay)
        }
        Log.i(TAG, "boot recover scheduled ($reason) delays=${delays.joinToString()}")
    }

    private fun tick(reason: String) {
        if (!WallpaperRepository.idlePrefer(app)) return
        if (orchestrator.isHandedOffToLyra()) {
            Log.i(TAG, "boot tick skip lyra handoff ($reason)")
            return
        }
        if (orchestrator.isEffectivelyPlaying()) {
            Log.i(TAG, "boot tick skip playing ($reason)")
            return
        }
        if (!WallpaperRepository.hasImage(app)) return
        orchestrator.applyCurrent("boot-$reason")
    }

    companion object {
        private const val TAG = "StarLive"
    }
}

