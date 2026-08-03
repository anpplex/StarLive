package com.starlive.app.display

import android.app.ActivityOptions
import android.app.Application
import android.content.Intent
import android.hardware.display.DisplayManager
import android.util.Log
import android.view.Display
import com.starlive.app.ui.ClusterStripActivity

/** Launch [ClusterStripActivity] on cluster / long panel when visible. */
class ClusterDisplayController(private val app: Application) {
    private val displayManager = app.getSystemService(DisplayManager::class.java)

    @Volatile
    private var alive = false

    fun isAlive(): Boolean = alive

    fun onActivityResumed(actualDisplayId: Int = Display.INVALID_DISPLAY) {
        if (actualDisplayId == Display.DEFAULT_DISPLAY) {
            alive = false
            Log.w(TAG, "cluster mis-landed on DEFAULT — not alive")
            return
        }
        alive = true
        Log.i(TAG, "cluster alive display=$actualDisplayId")
    }

    fun onActivityDestroyed() {
        alive = false
        Log.i(TAG, "cluster destroyed")
    }

    fun show(force: Boolean = false): Boolean {
        if (alive && !force) {
            app.sendBroadcast(
                Intent(ClusterStripActivity.ACTION_RELOAD).setPackage(app.packageName),
            )
            return true
        }
        val cluster = findClusterDisplay() ?: run {
            Log.w(TAG, "no cluster: ${listDisplaysForProbe()}")
            return false
        }
        return runCatching {
            val intent = Intent(app, ClusterStripActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP,
                )
                putExtra(ClusterStripActivity.EXTRA_DISPLAY_ID, cluster.displayId)
            }
            val options = ActivityOptions.makeBasic().setLaunchDisplayId(cluster.displayId)
            app.startActivity(intent, options.toBundle())
            if (force) alive = false
            Log.i(TAG, "launch cluster id=${cluster.displayId} name=${cluster.name}")
            true
        }.onFailure {
            alive = false
            Log.e(TAG, "cluster launch failed", it)
        }.getOrDefault(false)
    }

    fun release() {
        alive = false
        runCatching {
            app.sendBroadcast(
                Intent(ClusterStripActivity.ACTION_FINISH).setPackage(app.packageName),
            )
        }
    }

    fun findClusterDisplay(): Display? = displayManager.displays.firstOrNull {
        it.displayId != Display.DEFAULT_DISPLAY &&
            it.state == Display.STATE_ON &&
            (it.name.contains("cluster", true) ||
                it.name.contains("ace screen", true) ||
                runCatching { it.mode.physicalWidth >= 3000 }.getOrDefault(false))
    }

    fun listDisplaysForProbe(): String =
        displayManager.displays.joinToString("; ") { d ->
            val w = runCatching { d.mode.physicalWidth }.getOrDefault(-1)
            val h = runCatching { d.mode.physicalHeight }.getOrDefault(-1)
            "id=${d.displayId} name=${d.name} ${w}x${h} state=${d.state}"
        }.ifBlank { "(none)" }

    private companion object {
        const val TAG = "StarLive"
    }
}
