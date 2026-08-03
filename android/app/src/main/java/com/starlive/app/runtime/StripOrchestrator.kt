package com.starlive.app.runtime

import android.app.Application
import android.util.Log
import com.starlive.app.display.ClusterDisplayController
import com.starlive.app.service.KeepAliveService
import com.starlive.app.wallpaper.WallpaperRepository

/**
 * Single entry for show / release strip + play yield + pending apply.
 */
class StripOrchestrator(private val app: Application) {
    val display = ClusterDisplayController(app)

    private val playbackGate = PlaybackGate { playing ->
        if (playing) {
            onYieldPlaying()
        } else {
            onResumeAfterPlay()
        }
        KeepAliveService.refresh(app)
    }

    @Volatile
    var lastError: String? = null
        private set

    @Volatile
    var showing: Boolean = false
        private set

    fun isEffectivelyPlaying(): Boolean = playbackGate.isEffectivelyPlaying()

    fun onRawPlaying(playing: Boolean) {
        playbackGate.setRawPlaying(playing)
    }

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
        if (playbackGate.isEffectivelyPlaying()) {
            PendingApplyStore.setPending(app, true)
            lastError = null
            Log.i(TAG, "apply deferred playing ($reason)")
            release("playing-yield")
            return false
        }
        val ok = display.show(force = true)
        showing = ok
        lastError = if (ok) null else "launch-failed"
        if (ok) PendingApplyStore.clear(app)
        Log.i(TAG, "applyCurrent ($reason) ok=$ok")
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
            KeepAliveService.start(app)
            if (playbackGate.isEffectivelyPlaying()) {
                PendingApplyStore.setPending(app, true)
                release("idle-on-playing")
            } else {
                applyCurrent("idle-on")
            }
        } else {
            PendingApplyStore.clear(app)
            release("idle-off")
            KeepAliveService.stop(app)
        }
    }

    private fun onYieldPlaying() {
        if (!WallpaperRepository.idlePrefer(app)) return
        if (showing || display.isAlive()) {
            release("play-yield")
        }
    }

    private fun onResumeAfterPlay() {
        if (!WallpaperRepository.idlePrefer(app)) return
        if (!WallpaperRepository.hasImage(app)) return
        if (PendingApplyStore.isPending(app) || true) {
            // Always try re-show after play ends when idle on
            applyCurrent("after-play")
        }
    }

    companion object {
        private const val TAG = "StarLive"
    }
}
