package com.starlive.app

import android.app.Application
import android.util.Log
import com.starlive.app.display.WallpaperCarousel
import com.starlive.app.night.AmbientWatch
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
    lateinit var ambientWatch: AmbientWatch
        private set

    override fun onCreate() {
        super.onCreate()
        orchestrator = StripOrchestrator(this)
        bootScheduler = BootRecoverScheduler(this, orchestrator)
        wallpaperCarousel = WallpaperCarousel(this)
        ambientWatch = AmbientWatch(this, orchestrator)
        runCatching { WallpaperRepository.ensureSeeded(this) }
            .onFailure { Log.w(TAG, "seed failed", it) }
        orchestrator.refreshLyraHandoff("app-start")
        ambientWatch.start()
        if (WallpaperRepository.idlePrefer(this) && !orchestrator.isHandedOffToLyra()) {
            runCatching { KeepAliveService.start(this) }
            // Cold-boot often never delivers BOOT to third-party apps on car HUs.
            // Schedule the same recover path when the process is created by user open,
            // force-stop recovery, or any other start — so "open once" restores strip.
            if (WallpaperRepository.hasImage(this)) {
                bootScheduler.schedule("process-start")
            }
        }
        wallpaperCarousel.syncFromSettings()
        Log.i(
            TAG,
            "StarLiveApp onCreate ${BuildConfig.VERSION_NAME} " +
                ambientWatch.debugSnapshot(),
        )
    }

    companion object {
        private const val TAG = "StarLive"
    }
}
