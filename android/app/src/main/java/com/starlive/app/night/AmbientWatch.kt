package com.starlive.app.night

import android.app.Application
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.starlive.app.runtime.StripOrchestrator
import com.starlive.app.ui.ClusterStripActivity
import com.starlive.app.wallpaper.WallpaperRepository

/**
 * Poll + observe vehicle day/night (Lyra-style) and re-bake the remote strip
 * when light/dark flips — including demo dark/light asset swap.
 */
class AmbientWatch(
    private val app: Application,
    private val orchestrator: StripOrchestrator,
    private val nightMode: RemoteNightMode = RemoteNightMode(app),
) {
    private val main = Handler(Looper.getMainLooper())
    private var started = false
    private var lastNightish: Boolean? = null

    fun nightMode(): RemoteNightMode = nightMode

    fun start() {
        if (started) return
        started = true
        lastNightish = effectiveNightish()
        nightMode.registerUiNightModeObserver(main) {
            onAmbientMaybeChanged("secure-observer")
            main.postDelayed({ onAmbientMaybeChanged("secure-observer-settle") }, 400L)
            main.postDelayed({ onAmbientMaybeChanged("secure-observer-settle2") }, 1_200L)
        }
        main.postDelayed(pollTick, POLL_MS)
        Log.i(TAG, "AmbientWatch start nightish=$lastNightish snap=${nightMode.debugSnapshot()}")
    }

    fun effectiveNightish(): Boolean =
        when (WallpaperRepository.nightMode(app)) {
            "dark" -> true
            "light" -> false
            else -> nightMode.isNightish()
        }

    fun debugSnapshot(): String =
        "pref=${WallpaperRepository.nightMode(app)} " +
            "effective=${if (effectiveNightish()) "night" else "day"} " +
            nightMode.debugSnapshot()

    private val pollTick = object : Runnable {
        override fun run() {
            onAmbientMaybeChanged("poll")
            main.postDelayed(this, POLL_MS)
        }
    }

    private fun onAmbientMaybeChanged(reason: String) {
        val now = effectiveNightish()
        val prev = lastNightish
        if (prev != null && prev == now) return
        lastNightish = now
        Log.i(
            TAG,
            "ambient flip ($reason) ${if (prev == true) "night" else "day"} → " +
                "${if (now) "night" else "day"} ${debugSnapshot()}",
        )
        // Demo dual assets follow remote/system night.
        val id = WallpaperRepository.activeId(app)
        if (id != "custom" && !id.startsWith("lib:")) {
            WallpaperRepository.applyDemo(app, id)
        }
        // Re-bake glass dissolve on live strip (even if apply deferred by music).
        runCatching {
            app.sendBroadcast(
                Intent(ClusterStripActivity.ACTION_RELOAD).setPackage(app.packageName),
            )
        }
        if (orchestrator.showing || orchestrator.display.isAlive()) {
            if (!orchestrator.isEffectivelyPlaying() && !orchestrator.isHandedOffToLyra()) {
                orchestrator.applyCurrent("ambient-$reason")
            }
        }
        orchestrator.notifyUi("ambient-$reason")
    }

    companion object {
        private const val TAG = "StarLive"
        private const val POLL_MS = 2_000L
    }
}
