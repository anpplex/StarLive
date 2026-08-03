package com.starlive.app.runtime

import android.content.Context
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * Fallback when NotificationListener / MediaSession is unavailable on car HUs.
 *
 * Prefer [AudioManager.getActivePlaybackConfigurations] + [MusicPlaybackFilter]
 * so Wallpaper Engine does not pin yield forever. Falls back to
 * [AudioManager.isMusicActive] only when configs are empty.
 *
 * Note: [AudioPlaybackConfiguration.getClientUid] / [getPlayerState] are not in
 * the public SDK stub — resolve via reflection (present on device runtime).
 */
class MediaProbe(
    context: Context,
    private val onMusicActive: (Boolean) -> Unit,
) {
    private val app = context.applicationContext
    private val am = app.getSystemService(AudioManager::class.java)
    private val main = Handler(Looper.getMainLooper())
    private var last: Boolean? = null
    private val tick = object : Runnable {
        override fun run() {
            val active = probeMusicActive()
            if (last != active) {
                last = active
                Log.i(TAG, "musicActive=$active (filtered)")
                onMusicActive(active)
            }
            main.postDelayed(this, INTERVAL_MS)
        }
    }

    fun start() {
        main.removeCallbacks(tick)
        main.post(tick)
        Log.i(TAG, "MediaProbe start")
    }

    fun stop() {
        main.removeCallbacks(tick)
        last = null
        Log.i(TAG, "MediaProbe stop")
    }

    private fun probeMusicActive(): Boolean {
        val fallback = runCatching { am?.isMusicActive == true }.getOrDefault(false)
        if (am == null) return fallback
        if (Build.VERSION.SDK_INT < 26) return fallback
        val configs = runCatching { am.activePlaybackConfigurations }.getOrNull()
            ?: return fallback
        if (configs.isEmpty()) return fallback

        val players = configs.map { cfg ->
            val uid = clientUid(cfg)
            val pkg = packageForUid(uid) ?: packageFromToString(cfg)
            MusicPlaybackFilter.Player(
                packageName = pkg,
                usage = cfg.audioAttributes.usage,
                started = isPlayerStarted(cfg),
            )
        }
        val result = MusicPlaybackFilter.isEffectivelyMusicPlaying(players, isMusicActiveFallback = false)
        // Do not use bare isMusicActive when we successfully listed configs —
        // it stays true for wallpaper engines. Only fall back if list empty (above).
        if (fallback != result) {
            val sticky = players.joinToString { p ->
                "${p.packageName ?: "?"} u=${p.usage} started=${p.started}"
            }
            Log.i(TAG, "music filter fallback=$fallback → $result [$sticky]")
        }
        return result
    }

    private fun packageForUid(uid: Int): String? {
        if (uid <= 0) return null
        return runCatching {
            app.packageManager.getPackagesForUid(uid)?.firstOrNull()
        }.getOrNull()
    }

    /** Some OEM stubs omit getClientUid; toString often embeds package or uid. */
    private fun packageFromToString(cfg: AudioPlaybackConfiguration): String? {
        val s = runCatching { cfg.toString() }.getOrNull() ?: return null
        PACKAGE_IN_TOSTRING.find(s)?.groupValues?.getOrNull(1)?.let { return it }
        UID_IN_TOSTRING.find(s)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { uid ->
            return packageForUid(uid)
        }
        return null
    }

    companion object {
        private const val TAG = "StarLive"
        private const val INTERVAL_MS = 1_500L
        /** android.media.AudioPlaybackConfiguration.PLAYER_STATE_STARTED */
        private const val PLAYER_STATE_STARTED = 2

        private val clientUidMethod by lazy {
            runCatching {
                AudioPlaybackConfiguration::class.java.getMethod("getClientUid")
            }.getOrNull()
        }
        private val playerStateMethod by lazy {
            runCatching {
                AudioPlaybackConfiguration::class.java.getMethod("getPlayerState")
            }.getOrNull()
        }

        private fun clientUid(cfg: AudioPlaybackConfiguration): Int =
            runCatching { clientUidMethod?.invoke(cfg) as? Int }.getOrNull() ?: -1

        private fun isPlayerStarted(cfg: AudioPlaybackConfiguration): Boolean {
            val state = runCatching { playerStateMethod?.invoke(cfg) as? Int }.getOrNull()
            if (state != null) return state == PLAYER_STATE_STARTED
            // Reflection miss: parse "state:started" from toString (dumpsys-style).
            val s = runCatching { cfg.toString() }.getOrNull().orEmpty()
            if (s.contains("state:started", ignoreCase = true)) return true
            if (s.contains("state:", ignoreCase = true)) return false
            // Unknown — do not assume started (would pin yield on every config row).
            return false
        }

        private val PACKAGE_IN_TOSTRING =
            Regex("""(?:package|pkg|clientPackage)\s*[=:]\s*([A-Za-z0-9._]+)""")
        private val UID_IN_TOSTRING =
            Regex("""u(?:id)?/pid[:=]\s*(\d+)/""", RegexOption.IGNORE_CASE)
    }
}
