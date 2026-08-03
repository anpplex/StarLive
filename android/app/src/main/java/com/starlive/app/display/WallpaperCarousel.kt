package com.starlive.app.display

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.starlive.app.StarLiveApp
import com.starlive.app.wallpaper.WallpaperRepository

/**
 * Cycles built-in demos while idle wallpaper is on, carousel enabled, not playing.
 */
class WallpaperCarousel(private val app: Application) {
    private val main = Handler(Looper.getMainLooper())
    private var running = false

    private val tick = object : Runnable {
        override fun run() {
            if (!running) return
            if (!shouldRun()) {
                scheduleNext()
                return
            }
            val id = WallpaperRepository.advanceToNextDemo(app)
            if (id != null) {
                Log.i(TAG, "carousel → $id")
                (app as? StarLiveApp)?.orchestrator?.applyCurrent("carousel-$id")
            }
            scheduleNext()
        }
    }

    fun syncFromSettings() {
        if (WallpaperRepository.isCarouselEnabled(app) &&
            WallpaperRepository.idlePrefer(app)
        ) {
            start()
        } else {
            stop()
        }
    }

    fun start() {
        if (running) {
            reschedule()
            return
        }
        running = true
        Log.i(TAG, "carousel start")
        scheduleNext()
    }

    fun stop() {
        running = false
        main.removeCallbacks(tick)
        Log.i(TAG, "carousel stop")
    }

    fun reschedule() {
        if (!running) {
            if (WallpaperRepository.isCarouselEnabled(app)) start()
            return
        }
        main.removeCallbacks(tick)
        scheduleNext()
    }

    private fun shouldRun(): Boolean {
        if (!WallpaperRepository.isCarouselEnabled(app)) return false
        if (!WallpaperRepository.idlePrefer(app)) return false
        val orch = (app as? StarLiveApp)?.orchestrator ?: return false
        if (orch.isEffectivelyPlaying()) return false
        if (orch.isHandedOffToLyra()) return false
        val active = WallpaperRepository.activeId(app)
        if (active == "custom" || active.startsWith("lib:")) return false
        return WallpaperRepository.demos(app).size >= 2
    }

    private fun scheduleNext() {
        main.removeCallbacks(tick)
        if (!running) return
        val min = WallpaperRepository.carouselIntervalMinutes(app)
        main.postDelayed(tick, min * 60_000L)
        Log.i(TAG, "carousel next in ${min}m")
    }

    private companion object {
        const val TAG = "StarLive"
    }
}
