package com.starlive.app.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Bitmap
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
import com.starlive.app.wallpaper.WallpaperRepository
import com.starlive.ring.StripGeometry

/**
 * Full-strip surface on cluster display: left gauge reserve + wallpaper band.
 *
 * Day/night glass + left dissolve match Lyra OEM remotescreen (base_map #E8EAEE /
 * base_map_b #080A0B). Bitmap is re-baked when ambient flips; switches use a short
 * alpha crossfade so the remote edge does not hard-cut.
 */
class ClusterStripActivity : AppCompatActivity() {
    private var root: FrameLayout? = null
    private var wallpaperPlate: View? = null
    private var wallpaperA: ImageView? = null
    private var wallpaperB: ImageView? = null
    /** True when [wallpaperA] is the visible front layer. */
    private var frontIsA: Boolean = true
    private var bakedNightish: Boolean? = null
    private var crossfadeRunning: Boolean = false
    private var pendingReload: Boolean = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_FINISH -> finish()
                ACTION_RELOAD -> reloadWallpaper(animated = true)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK

        val strip = FrameLayout(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            layoutParams = FrameLayout.LayoutParams(
                StripGeometry.STRIP_W,
                StripGeometry.STRIP_H,
            )
        }
        // Left gauge reserve — transparent so OEM instrument glass shows through.
        strip.addView(
            View(this).apply { setBackgroundColor(Color.TRANSPARENT) },
            FrameLayout.LayoutParams(StripGeometry.GAUGE_RESERVE, StripGeometry.STRIP_H),
        )

        val bandLp = FrameLayout.LayoutParams(
            StripGeometry.WALLPAPER_W,
            StripGeometry.STRIP_H,
        ).apply { leftMargin = StripGeometry.GAUGE_RESERVE }

        // Opaque glass under art — dissolve is baked into the bitmap (Lyra Option A).
        val plate = View(this).apply {
            setBackgroundColor(StripGeometry.GLASS_DAY)
        }
        wallpaperPlate = plate
        strip.addView(plate, FrameLayout.LayoutParams(bandLp))

        fun makeWp(): ImageView = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_XY
            alpha = 0f
        }
        wallpaperA = makeWp()
        wallpaperB = makeWp()
        strip.addView(wallpaperA, FrameLayout.LayoutParams(bandLp))
        strip.addView(wallpaperB, FrameLayout.LayoutParams(bandLp))

        root = strip
        setContentView(strip)
        reloadWallpaper(animated = false)
        Log.i(TAG, "ClusterStripActivity onCreate display=${display?.displayId}")
    }

    override fun onResume() {
        super.onResume()
        val id = display?.displayId ?: Display.INVALID_DISPLAY
        (application as? StarLiveApp)?.orchestrator?.onClusterAlive(id)
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
        // Re-sample ambient in case night flipped while paused.
        reloadWallpaper(animated = true)
    }

    override fun onPause() {
        runCatching { unregisterReceiver(receiver) }
        super.onPause()
    }

    override fun onDestroy() {
        (application as? StarLiveApp)?.orchestrator?.display?.onActivityDestroyed()
        wallpaperA?.setImageBitmap(null)
        wallpaperB?.setImageBitmap(null)
        super.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val night = WallpaperRepository.isNightish(this)
        if (bakedNightish != night) {
            Log.i(TAG, "config night flip baked=$bakedNightish → $night")
            reloadWallpaper(animated = true)
        }
    }

    private fun front(): ImageView? = if (frontIsA) wallpaperA else wallpaperB
    private fun back(): ImageView? = if (frontIsA) wallpaperB else wallpaperA

    private fun reloadWallpaper(animated: Boolean) {
        if (crossfadeRunning) {
            pendingReload = true
            return
        }
        val night = WallpaperRepository.isNightish(this)
        val bmp = WallpaperRepository.decodeActiveForStrip(this, night) ?: run {
            Log.w(TAG, "reloadWallpaper no bitmap night=$night")
            return
        }
        val glass = if (night) StripGeometry.GLASS_NIGHT else StripGeometry.GLASS_DAY
        wallpaperPlate?.setBackgroundColor(glass)

        val snap = (application as? StarLiveApp)?.ambientWatch?.debugSnapshot().orEmpty()
        Log.i(
            TAG,
            "reloadWallpaper night=$night glass=#${Integer.toHexString(glass)} " +
                "bmp=${bmp.width}x${bmp.height} anim=$animated $snap",
        )

        val front = front()
        val back = back()
        if (front == null || back == null) {
            bmp.recycle()
            return
        }

        // First paint — no crossfade.
        if (front.drawable == null || !animated || bakedNightish == null) {
            front.setImageBitmap(bmp)
            front.alpha = 1f
            back.alpha = 0f
            back.setImageBitmap(null)
            bakedNightish = night
            return
        }

        // Same ambient + same content path: still swap with short fade for apply/reload.
        presentWithCrossfade(back, front, bmp, night)
    }

    private fun presentWithCrossfade(
        incoming: ImageView,
        outgoing: ImageView,
        bmp: Bitmap,
        night: Boolean,
    ) {
        crossfadeRunning = true
        incoming.setImageBitmap(bmp)
        incoming.alpha = 0f
        incoming.bringToFront()

        val fadeIn = ObjectAnimator.ofFloat(incoming, View.ALPHA, 0f, 1f).setDuration(CROSSFADE_MS)
        val fadeOut = ObjectAnimator.ofFloat(outgoing, View.ALPHA, 1f, 0f).setDuration(CROSSFADE_MS)
        fadeIn.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                outgoing.setImageBitmap(null)
                outgoing.alpha = 0f
                frontIsA = incoming === wallpaperA
                bakedNightish = night
                crossfadeRunning = false
                if (pendingReload) {
                    pendingReload = false
                    reloadWallpaper(animated = true)
                }
            }
        })
        fadeOut.start()
        fadeIn.start()
    }

    companion object {
        private const val TAG = "StarLive"
        private const val CROSSFADE_MS = 320L
        const val EXTRA_DISPLAY_ID = "display_id"
        const val ACTION_FINISH = "com.starlive.app.ACTION_STRIP_FINISH"
        const val ACTION_RELOAD = "com.starlive.app.ACTION_STRIP_RELOAD"
    }
}
