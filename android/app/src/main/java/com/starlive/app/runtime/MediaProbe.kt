package com.starlive.app.runtime

import android.content.Context
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * Fallback when NotificationListener / MediaSession is unavailable on car HUs.
 * Uses [AudioManager.isMusicActive] at a modest interval.
 */
class MediaProbe(
    context: Context,
    private val onMusicActive: (Boolean) -> Unit,
) {
    private val am = context.applicationContext.getSystemService(AudioManager::class.java)
    private val main = Handler(Looper.getMainLooper())
    private var last: Boolean? = null
    private val tick = object : Runnable {
        override fun run() {
            val active = runCatching { am?.isMusicActive == true }.getOrDefault(false)
            if (last != active) {
                last = active
                Log.i(TAG, "AudioManager.isMusicActive=$active")
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

    companion object {
        private const val TAG = "StarLive"
        private const val INTERVAL_MS = 1_500L
    }
}
