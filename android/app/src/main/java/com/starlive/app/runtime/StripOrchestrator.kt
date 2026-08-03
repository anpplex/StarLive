package com.starlive.app.runtime

import android.app.Application
import android.util.Log
import com.starlive.app.display.ClusterDisplayController
import com.starlive.app.service.KeepAliveService
import com.starlive.app.upgrade.LyraPresence
import com.starlive.app.wallpaper.WallpaperRepository

/**
 * Single entry for show / release strip + play yield + Lyra handoff + pending apply.
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
        (app as? com.starlive.app.StarLiveApp)?.wallpaperCarousel?.reschedule()
    }

    @Volatile
    var lastError: String? = null
        private set

    @Volatile
    var showing: Boolean = false
        private set

    @Volatile
    private var handedOffToLyra: Boolean = false

    fun isEffectivelyPlaying(): Boolean = playbackGate.isEffectivelyPlaying()

    fun isHandedOffToLyra(): Boolean = handedOffToLyra

    fun onRawPlaying(playing: Boolean) {
        playbackGate.setRawPlaying(playing)
    }

    /** Call on resume / package change: yield strip if Lyra installed and policy on. */
    fun refreshLyraHandoff(reason: String): Boolean {
        val should = WallpaperRepository.yieldWhenLyraInstalled(app) &&
            LyraPresence.isInstalled(app)
        if (should == handedOffToLyra) {
            if (should) Log.i(TAG, "lyra handoff still active ($reason)")
            return should
        }
        handedOffToLyra = should
        if (should) {
            Log.i(TAG, "lyra handoff ON ($reason)")
            release("lyra-handoff")
            KeepAliveService.stop(app)
        } else {
            Log.i(TAG, "lyra handoff OFF ($reason)")
            if (WallpaperRepository.idlePrefer(app)) {
                KeepAliveService.start(app)
                if (!playbackGate.isEffectivelyPlaying()) {
                    applyCurrent("lyra-handoff-off")
                }
            }
        }
        return should
    }

    fun applyCurrent(reason: String): Boolean {
        refreshLyraHandoff("before-apply")
        if (handedOffToLyra) {
            lastError = "lyra-handoff"
            Log.i(TAG, "apply skip lyra handoff ($reason)")
            return false
        }
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
        (app as? com.starlive.app.StarLiveApp)?.wallpaperCarousel?.syncFromSettings()
        if (value) {
            if (refreshLyraHandoff("idle-on")) return
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
        if (handedOffToLyra) return
        if (showing || display.isAlive()) {
            release("play-yield")
        }
    }

    private fun onResumeAfterPlay() {
        if (!WallpaperRepository.idlePrefer(app)) return
        if (handedOffToLyra) return
        if (!WallpaperRepository.hasImage(app)) return
        applyCurrent("after-play")
    }

    companion object {
        private const val TAG = "StarLive"
    }
}
