package com.starlive.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.starlive.app.BuildConfig
import com.starlive.app.StarLiveApp
import com.starlive.app.wallpaper.WallpaperRepository
import java.io.File

class StartupReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action.orEmpty()
        Log.i(TAG, "startup action=$action")
        val pending = goAsync()
        val appCtx = context.applicationContext
        // Persist probe: logcat often wiped after boot on car HUs
        runCatching {
            val line =
                "${System.currentTimeMillis()}\t$action\tidle=${WallpaperRepository.idlePrefer(appCtx)}\tv=${BuildConfig.VERSION_NAME}\n"
            File(appCtx.filesDir, "boot_probe.log").appendText(line)
        }
        Handler(Looper.getMainLooper()).postDelayed({
            runCatching {
                val app = appCtx as? StarLiveApp ?: return@runCatching
                // Lyra 优先：不得在开机路径上拉起 KeepAlive / 星环恢复。
                val handoff = app.orchestrator.refreshLyraHandoff("startup-$action")
                if (handoff) {
                    Log.i(TAG, "startup skip recover — Lyra handoff")
                    return@runCatching
                }
                if (WallpaperRepository.idlePrefer(appCtx)) {
                    runCatching { KeepAliveService.start(appCtx) }
                        .onFailure { Log.e(TAG, "KeepAlive start failed", it) }
                }
                app.bootScheduler.schedule(action.ifBlank { "startup" })
            }.onFailure { Log.e(TAG, "boot schedule failed", it) }
            pending.finish()
        }, 1_200L)
    }

    companion object {
        private const val TAG = "StarLive"
    }
}
