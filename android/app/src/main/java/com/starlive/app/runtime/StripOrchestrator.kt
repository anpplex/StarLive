package com.starlive.app.runtime

import android.app.Application
import android.util.Log
import com.starlive.app.display.ClusterDisplayController
import com.starlive.app.wallpaper.WallpaperRepository

/**
 * Single entry for show / release strip. Playing gate lands in Phase 3.
 */
class StripOrchestrator(private val app: Application) {
    val display = ClusterDisplayController(app)

    @Volatile
    var lastError: String? = null
        private set

    @Volatile
    var showing: Boolean = false
        private set

    fun applyCurrent(reason: String): Boolean {
        if (!WallpaperRepository.idlePrefer(app)) {
            Log.i(TAG, "apply skip idle off ($reason)")
            release("idle-off")
            return false
        }
        if (!WallpaperRepository.hasImage(app)) {
            lastError = "no-image"
            Log.w(TAG, "apply no image ($reason)")
            return false
        }
        // Phase 3: if playing → pending only
        val ok = display.show(force = true)
        showing = ok
        lastError = if (ok) null else "launch-failed"
        Log.i(TAG, "applyCurrent ($reason) ok=$ok displays=${display.listDisplaysForProbe()}")
        return ok
    }

    fun release(reason: String) {
        display.release()
        showing = false
        Log.i(TAG, "release ($reason)")
    }

    fun setIdlePrefer(value: Boolean) {
        WallpaperRepository.setIdlePrefer(app, value)
        if (value) {
            applyCurrent("idle-on")
        } else {
            release("idle-off")
        }
    }

    companion object {
        private const val TAG = "StarLive"
    }
}
