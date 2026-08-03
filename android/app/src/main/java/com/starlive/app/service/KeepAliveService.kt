package com.starlive.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.starlive.app.R
import com.starlive.app.StarLiveApp
import com.starlive.app.ui.MainActivity
import com.starlive.app.wallpaper.WallpaperRepository

class KeepAliveService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!WallpaperRepository.idlePrefer(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        startAsForeground(yieldPlaying = isYieldPlaying())
        return START_STICKY
    }

    private fun isYieldPlaying(): Boolean {
        return (application as? StarLiveApp)?.orchestrator?.isEffectivelyPlaying() == true
    }

    private fun startAsForeground(yieldPlaying: Boolean) {
        ensureChannel()
        val pi = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val text = if (yieldPlaying) {
            getString(R.string.notif_yield_playing)
        } else {
            getString(R.string.notif_keeping)
        }
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentIntent(pi)
            .setOngoing(true)
            .setSilent(true)
            .build()
        runCatching {
            if (Build.VERSION.SDK_INT >= 29) {
                ServiceCompat.startForeground(
                    this,
                    NOTIF_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
                )
            } else {
                @Suppress("DEPRECATION")
                startForeground(NOTIF_ID, notification)
            }
            Log.i(TAG, "startForeground yield=$yieldPlaying")
        }.onFailure {
            Log.e(TAG, "startForeground failed", it)
            stopSelf()
        }
    }

    fun refreshNotification() {
        if (!WallpaperRepository.idlePrefer(this)) {
            stopSelf()
            return
        }
        startAsForeground(isYieldPlaying())
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = getSystemService(NotificationManager::class.java)
        val ch = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notif_channel),
            NotificationManager.IMPORTANCE_LOW,
        )
        nm.createNotificationChannel(ch)
    }

    companion object {
        private const val TAG = "StarLive"
        private const val CHANNEL_ID = "starlive_keepalive"
        private const val NOTIF_ID = 7101

        fun start(app: android.content.Context) {
            if (!WallpaperRepository.idlePrefer(app)) return
            val i = Intent(app, KeepAliveService::class.java)
            if (Build.VERSION.SDK_INT >= 26) {
                app.startForegroundService(i)
            } else {
                app.startService(i)
            }
        }

        fun stop(app: android.content.Context) {
            app.stopService(Intent(app, KeepAliveService::class.java))
        }

        fun refresh(app: android.content.Context) {
            // Restart to refresh text
            if (WallpaperRepository.idlePrefer(app)) start(app)
            else stop(app)
        }
    }
}
