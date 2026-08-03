package com.starlive.app.service

import android.content.ComponentName
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.util.Log
import com.starlive.app.StarLiveApp

/**
 * MediaSession probe for playing state only — no lyrics.
 * Without user grant, gate stays "not playing" (allow wallpaper).
 */
class StarLiveNotificationListener : NotificationListenerService() {
    private val callbacks = mutableMapOf<MediaController, MediaController.Callback>()
    private val manager by lazy { getSystemService(MediaSessionManager::class.java) }
    private val sessionsChanged = MediaSessionManager.OnActiveSessionsChangedListener {
        refresh(it.orEmpty())
    }

    override fun onListenerConnected() {
        Log.i(TAG, "NLS connected")
        val component = ComponentName(this, StarLiveNotificationListener::class.java)
        manager.addOnActiveSessionsChangedListener(sessionsChanged, component)
        refresh(manager.getActiveSessions(component))
    }

    override fun onListenerDisconnected() {
        Log.i(TAG, "NLS disconnected")
        manager.removeOnActiveSessionsChangedListener(sessionsChanged)
        callbacks.forEach { (c, cb) -> c.unregisterCallback(cb) }
        callbacks.clear()
        publishPlaying(false)
    }

    private fun refresh(controllers: List<MediaController>) {
        callbacks.keys.filter { old -> controllers.none { it.sessionToken == old.sessionToken } }
            .forEach { c -> callbacks.remove(c)?.let { c.unregisterCallback(it) } }
        controllers.forEach { controller ->
            if (callbacks.keys.none { it.sessionToken == controller.sessionToken }) {
                val cb = object : MediaController.Callback() {
                    override fun onPlaybackStateChanged(state: PlaybackState?) = publishBest()
                    override fun onSessionDestroyed() = publishBest()
                }
                controller.registerCallback(cb)
                callbacks[controller] = cb
            }
        }
        publishBest()
    }

    private fun publishBest() {
        val playing = callbacks.keys.any {
            it.playbackState?.state == PlaybackState.STATE_PLAYING
        }
        publishPlaying(playing)
    }

    private fun publishPlaying(playing: Boolean) {
        (application as? StarLiveApp)?.orchestrator?.onRawPlaying(playing)
    }

    companion object {
        private const val TAG = "StarLive"
    }
}
