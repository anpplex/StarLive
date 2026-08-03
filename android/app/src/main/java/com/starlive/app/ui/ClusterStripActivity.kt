package com.starlive.app.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Display
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.starlive.app.StarLiveApp
import com.starlive.app.display.StripGeometry
import com.starlive.app.wallpaper.WallpaperRepository

/**
 * Full-strip surface on cluster display: left gauge reserve + wallpaper band.
 */
class ClusterStripActivity : AppCompatActivity() {
    private var wallpaperView: ImageView? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_FINISH -> finish()
                ACTION_RELOAD -> reloadWallpaper()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            layoutParams = FrameLayout.LayoutParams(
                StripGeometry.STRIP_W,
                StripGeometry.STRIP_H,
            )
        }
        // Left gauge reserve — keep transparent/black so OEM glass shows
        val plate = View(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
        }
        val wp = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_XY
            wallpaperView = this
        }
        root.addView(
            plate,
            FrameLayout.LayoutParams(StripGeometry.GAUGE_RESERVE, StripGeometry.STRIP_H),
        )
        root.addView(
            wp,
            FrameLayout.LayoutParams(StripGeometry.WALLPAPER_W, StripGeometry.STRIP_H).apply {
                leftMargin = StripGeometry.GAUGE_RESERVE
            },
        )
        setContentView(root)
        reloadWallpaper()
        Log.i(TAG, "ClusterStripActivity onCreate display=${display?.displayId}")
    }

    override fun onResume() {
        super.onResume()
        val id = display?.displayId ?: Display.INVALID_DISPLAY
        (application as? StarLiveApp)?.orchestrator?.display?.onActivityResumed(id)
        val filter = IntentFilter().apply {
            addAction(ACTION_FINISH)
            addAction(ACTION_RELOAD)
        }
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(receiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(receiver, filter)
        }
    }

    override fun onPause() {
        runCatching { unregisterReceiver(receiver) }
        super.onPause()
    }

    override fun onDestroy() {
        (application as? StarLiveApp)?.orchestrator?.display?.onActivityDestroyed()
        wallpaperView?.setImageBitmap(null)
        super.onDestroy()
    }

    private fun reloadWallpaper() {
        val night = WallpaperRepository.isNightish(this)
        val bmp = WallpaperRepository.decodeActiveForStrip(this, night)
        wallpaperView?.setImageBitmap(bmp)
        val glass = if (night) StripGeometry.GLASS_NIGHT else StripGeometry.GLASS_DAY
        // Optional plate under band for soft edge bake match
        wallpaperView?.setBackgroundColor(glass)
        Log.i(TAG, "reloadWallpaper night=$night bmp=${bmp?.width}x${bmp?.height}")
    }

    companion object {
        private const val TAG = "StarLive"
        const val EXTRA_DISPLAY_ID = "display_id"
        const val ACTION_FINISH = "com.starlive.app.ACTION_STRIP_FINISH"
        const val ACTION_RELOAD = "com.starlive.app.ACTION_STRIP_RELOAD"
    }
}
