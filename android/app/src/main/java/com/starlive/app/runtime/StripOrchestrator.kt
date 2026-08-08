package com.starlive.app.runtime

import android.app.Application
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.starlive.app.display.ClusterDisplayController
import com.starlive.app.service.KeepAliveService
import com.starlive.app.upgrade.LyraPresence
import com.starlive.app.wallpaper.WallpaperRepository

/**
 * Single entry for show / release strip + play yield + Lyra handoff + pending apply.
 *
 * Launch confirm: [display.show] sets [showing] before the activity lands on the
 * cluster display. If [onClusterAlive] never confirms (or mis-lands on DEFAULT),
 * a timeout clears stuck「连接中…」and optionally retries once per apply.
 */
class StripOrchestrator(private val app: Application) {
    val display = ClusterDisplayController(app)

    private val main = Handler(Looper.getMainLooper())

    /** True after the silent force-retry for the current apply; reset on new apply. */
    private var launchRetryUsed: Boolean = false

    /** True while waiting for cluster resume after show() — destroy during relaunch must not clear. */
    @Volatile
    private var launchConfirmPending: Boolean = false

    private val launchConfirmRunnable = Runnable { onLaunchConfirmTimeout() }

    /** Local broadcast so Main (and others) refresh capsule after async cluster launch. */
    fun notifyUi(reason: String) {
        runCatching {
            app.sendBroadcast(
                Intent(ACTION_UI_REFRESH).setPackage(app.packageName).putExtra(EXTRA_REASON, reason),
            )
        }
    }

    private val playbackGate = PlaybackGate(
        onEffectivelyPlayingChanged = { playing ->
            if (playing) {
                onYieldPlaying()
            } else {
                onResumeAfterPlay()
            }
            KeepAliveService.refresh(app)
            (app as? com.starlive.app.StarLiveApp)?.wallpaperCarousel?.reschedule()
        },
    )

    @Volatile
    var lastError: String? = null
        private set

    @Volatile
    var showing: Boolean = false
        private set

    /** Reconcile capsule when cluster is already up but flags lagged. Never overrides Lyra handoff. */
    fun syncShowingFromDisplay() {
        if (handedOffToLyra) {
            // Strip must stay down while Lyra owns the ring.
            if (showing || display.isAlive() || launchConfirmPending) {
                release("lyra-sync")
            }
            return
        }
        if (display.isAlive()) {
            showing = true
            lastError = null
            cancelLaunchConfirm()
        }
    }

    @Volatile
    private var handedOffToLyra: Boolean = false

    fun isEffectivelyPlaying(): Boolean = playbackGate.isEffectivelyPlaying()

    fun isHandedOffToLyra(): Boolean = handedOffToLyra

    fun onRawPlaying(playing: Boolean) {
        playbackGate.setRawPlaying(playing)
    }

    /**
     * Yield strip if Lyra is installed and policy is on.
     * When already handed off, still force-release any reappeared strip (idempotent).
     */
    fun refreshLyraHandoff(reason: String): Boolean {
        val should = WallpaperRepository.yieldWhenLyraInstalled(app) &&
            LyraPresence.isInstalled(app)
        if (should == handedOffToLyra) {
            if (should) {
                // Re-assert: boot/timeout/race must not leave StarLive on the ring over Lyra.
                ensureReleasedForLyra(reason)
                Log.i(TAG, "lyra handoff still active ($reason)")
            }
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

    /** Finish any live/launching strip while Lyra owns the display. */
    private fun ensureReleasedForLyra(reason: String) {
        if (showing || display.isAlive() || launchConfirmPending) {
            Log.w(TAG, "lyra handoff re-release strip still up ($reason)")
            release("lyra-handoff-reassert-$reason")
        }
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
        if (ok) {
            PendingApplyStore.clear(app)
            // showing=true before activity lands → UI shows「连接中…」until confirm/timeout.
            scheduleLaunchConfirm(resetRetry = true)
        } else {
            cancelLaunchConfirm()
        }
        notifyUi("apply-$reason")
        Log.i(TAG, "applyCurrent ($reason) ok=$ok")
        return ok
    }

    /**
     * ClusterStrip landed — confirm on real display, or clear stuck state on DEFAULT mis-land.
     * Never claim ownership when Lyra handoff is active.
     */
    fun onClusterAlive(displayId: Int) {
        // Re-sample policy: Lyra may have been installed while a launch was in flight.
        if (WallpaperRepository.yieldWhenLyraInstalled(app) && LyraPresence.isInstalled(app)) {
            handedOffToLyra = true
            display.onActivityResumed(displayId)
            release("lyra-cluster-alive-block")
            KeepAliveService.stop(app)
            Log.i(TAG, "cluster resume blocked — Lyra handoff displayId=$displayId")
            return
        }
        display.onActivityResumed(displayId)
        if (display.isAlive()) {
            showing = true
            lastError = null
            cancelLaunchConfirm()
            notifyUi("cluster-alive")
        } else {
            // Mis-land on DEFAULT_DISPLAY: activity resumed but not the cluster.
            showing = false
            lastError = "launch-failed"
            cancelLaunchConfirm()
            notifyUi("cluster-misland")
            Log.w(TAG, "cluster mis-land displayId=$displayId — cleared showing")
        }
    }

    /**
     * Activity destroyed. If a launch confirm is in flight (force re-launch), keep
     * [showing] so UI stays「连接中…」until the new instance resumes or times out.
     * Otherwise clear so home does not stick on connecting after a real teardown.
     */
    fun onClusterDestroyed() {
        display.onActivityDestroyed()
        if (launchConfirmPending) {
            Log.i(TAG, "cluster destroyed during launch confirm — keep showing")
            notifyUi("cluster-destroyed-relaunch")
            return
        }
        showing = false
        cancelLaunchConfirm()
        notifyUi("cluster-destroyed")
        Log.i(TAG, "cluster destroyed — cleared showing")
    }

    fun release(reason: String) {
        cancelLaunchConfirm()
        display.release()
        showing = false
        notifyUi("release-$reason")
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

    private fun scheduleLaunchConfirm(resetRetry: Boolean) {
        cancelLaunchConfirm()
        if (resetRetry) launchRetryUsed = false
        launchConfirmPending = true
        main.postDelayed(launchConfirmRunnable, LaunchConfirmPolicy.TIMEOUT_MS)
    }

    private fun cancelLaunchConfirm() {
        main.removeCallbacks(launchConfirmRunnable)
        launchConfirmPending = false
    }

    /**
     * Confirm window elapsed without alive cluster.
     * Clear stuck「连接中…」; one silent force-retry per apply, then leave launch-failed.
     */
    private fun onLaunchConfirmTimeout() {
        launchConfirmPending = false
        // Lyra may have been installed during the confirm window — never re-own the ring.
        if (WallpaperRepository.yieldWhenLyraInstalled(app) && LyraPresence.isInstalled(app)) {
            handedOffToLyra = true
            showing = false
            lastError = "lyra-handoff"
            if (display.isAlive()) {
                display.release()
            }
            KeepAliveService.stop(app)
            notifyUi("launch-timeout-lyra")
            Log.i(TAG, "launch confirm timeout — yield to Lyra")
            return
        }
        if (display.isAlive()) {
            showing = true
            lastError = null
            return
        }
        showing = false
        lastError = "launch-failed"
        notifyUi("launch-timeout")
        Log.w(TAG, "launch confirm timeout — cleared stuck launching (retryUsed=$launchRetryUsed)")

        if (launchRetryUsed) return
        if (!WallpaperRepository.idlePrefer(app)) return
        if (handedOffToLyra) return
        if (playbackGate.isEffectivelyPlaying()) return
        if (!WallpaperRepository.hasImage(app)) return

        launchRetryUsed = true
        val ok = display.show(force = true)
        if (ok) {
            showing = true
            lastError = null
            scheduleLaunchConfirm(resetRetry = false)
            notifyUi("launch-retry")
            Log.i(TAG, "launch silent force-retry after timeout")
        } else {
            showing = false
            lastError = "launch-failed"
            launchConfirmPending = false
            notifyUi("launch-retry-failed")
            Log.w(TAG, "launch silent force-retry failed")
        }
    }

    companion object {
        private const val TAG = "StarLive"
        const val ACTION_UI_REFRESH = "com.starlive.app.action.UI_REFRESH"
        const val EXTRA_REASON = "reason"
    }
}
