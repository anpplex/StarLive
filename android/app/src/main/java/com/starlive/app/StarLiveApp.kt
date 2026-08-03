package com.starlive.app

import android.app.Application
import android.util.Log
import com.starlive.app.display.WallpaperCarousel
import com.starlive.app.runtime.BootRecoverScheduler
import com.starlive.app.runtime.StripOrchestrator
import com.starlive.app.service.KeepAliveService
import com.starlive.app.wallpaper.WallpaperRepository

class StarLiveApp : Application() {
    lateinit var orchestrator: StripOrchestrator
        private set
    lateinit var bootScheduler: BootRecoverScheduler
        private set
    lateinit var wallpaperCarousel: WallpaperCarousel
        private set

    override fun onCreate() {
        super.onCreate()
        orchestrator = StripOrchestrator(this)
        bootScheduler = BootRecoverScheduler(this, orchestrator)
        wallpaperCarousel = WallpaperCarousel(this)
        runCatching { WallpaperRepository.ensureSeeded(this) }
            .onFailure { Log.w(TAG, "seed failed", it) }
        orchestrator.refreshLyraHandoff("app-start")
        if (WallpaperRepository.idlePrefer(this) && !orchestrator.isHandedOffToLyra()) {
            runCatching { KeepAliveService.start(this) }
        }
        wallpaperCarousel.syncFromSettings()
        Log.i(TAG, "StarLiveApp onCreate ${BuildConfig.VERSION_NAME}")
    }

    companion object {
        private const val TAG = "StarLive"
    }
}
