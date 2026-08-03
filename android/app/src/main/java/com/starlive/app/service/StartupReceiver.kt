package com.starlive.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.starlive.app.StarLiveApp
import com.starlive.app.wallpaper.WallpaperRepository

class StartupReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action.orEmpty()
        Log.i(TAG, "startup action=$action")
        val pending = goAsync()
        val appCtx = context.applicationContext
        if (WallpaperRepository.idlePrefer(appCtx)) {
            runCatching { KeepAliveService.start(appCtx) }
        }
        Handler(Looper.getMainLooper()).postDelayed({
            runCatching {
                val app = appCtx as? StarLiveApp ?: return@runCatching
                app.bootScheduler.schedule(action.ifBlank { "startup" })
            }
            pending.finish()
        }, 1_200L)
    }

    companion object {
        private const val TAG = "StarLive"
    }
}
