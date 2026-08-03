package com.starlive.app.night

import android.app.Application
import android.content.ComponentCallbacks2
import android.content.Intent
import android.content.res.Configuration
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

    private val configCallbacks = object : ComponentCallbacks2 {
        override fun onConfigurationChanged(newConfig: Configuration) {
            onAmbientMaybeChanged("config-callback")
            // HU may deliver uiMode before glass resources settle.
            main.postDelayed({ onAmbientMaybeChanged("config-callback-settle") }, 350L)
        }

        override fun onLowMemory() {}
        override fun onTrimMemory(level: Int) {}
    }

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
        runCatching { app.registerComponentCallbacks(configCallbacks) }
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
            // Adaptive needs snappier polls — secure stays 9 while effective bit flips.
            val adaptive = runCatching {
                val s = android.provider.Settings.Secure.getInt(
                    app.contentResolver,
                    "ui_night_mode",
                    -1,
                )
                s == RemoteNightMode.MODE_ADAPTIVE || s == RemoteNightMode.MODE_AOSP_AUTO
            }.getOrDefault(true)
            main.postDelayed(this, if (adaptive) POLL_MS_ADAPTIVE else POLL_MS)
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
        // Drop cached edge bake so glass/feather match the new effective bit.
        WallpaperRepository.invalidateStripCache()
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
        private const val POLL_MS_ADAPTIVE = 800L
    }
}
