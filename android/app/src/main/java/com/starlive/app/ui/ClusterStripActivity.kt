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
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
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
 * base_map_b #080A0B). Static bitmaps bake the dissolve; animated GIF/WebP use a
 * left gradient overlay instead. Static switches use a short alpha crossfade.
 */
class ClusterStripActivity : AppCompatActivity() {
    private var root: FrameLayout? = null
    /** Full-strip glass (incl. left gauge) — must match day/night dissolve, never leave black void. */
    private var glassBase: View? = null
    private var wallpaperPlate: View? = null
    private var wallpaperA: ImageView? = null
    private var wallpaperB: ImageView? = null
    /** Thin left-edge glass fade for animated media (static path bakes dissolve into bitmap). */
    private var leftEdgeOverlay: View? = null
    /** True when [wallpaperA] is the visible front layer. */
    private var frontIsA: Boolean = true
    private var bakedNightish: Boolean? = null
    private var showingAnimated: Boolean = false
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
        // Match initial glass; reloadWallpaper updates to day/night.
        val initialGlass = if (WallpaperRepository.isNightish(this)) {
            StripGeometry.GLASS_NIGHT
        } else {
            StripGeometry.GLASS_DAY
        }
        window.statusBarColor = initialGlass
        window.navigationBarColor = initialGlass

        val strip = FrameLayout(this).apply {
            setBackgroundColor(initialGlass)
            layoutParams = FrameLayout.LayoutParams(
                StripGeometry.STRIP_W,
                StripGeometry.STRIP_H,
            )
        }
        // Full-width glass under everything. StarLive owns the private cluster
        // display — transparent left was showing pure black and desynced from
        // system 浅色/深色 base_map at the wallpaper junction.
        val base = View(this).apply { setBackgroundColor(initialGlass) }
        glassBase = base
        strip.addView(
            base,
            FrameLayout.LayoutParams(StripGeometry.STRIP_W, StripGeometry.STRIP_H),
        )

        val bandLp = FrameLayout.LayoutParams(
            StripGeometry.WALLPAPER_W,
            StripGeometry.STRIP_H,
        ).apply { leftMargin = StripGeometry.GAUGE_RESERVE }

        // Opaque glass under art — dissolve is baked into the bitmap (Lyra Option A).
        val plate = View(this).apply {
            setBackgroundColor(initialGlass)
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

        // Left-edge dissolve for animated media (not baked per frame).
        val fadeW = StripGeometry.EDGE_FEATHER_DAY
        val edge = View(this).apply {
            background = edgeGradient(initialGlass)
            visibility = View.GONE
        }
        leftEdgeOverlay = edge
        strip.addView(
            edge,
            FrameLayout.LayoutParams(fadeW, StripGeometry.STRIP_H).apply {
                leftMargin = StripGeometry.GAUGE_RESERVE
            },
        )

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
        // Clear orchestrator.showing (not only display.alive) so home never sticks on「连接中…」.
        (application as? StarLiveApp)?.orchestrator?.onClusterDestroyed()
        stopAnimatedDrawables()
        wallpaperA?.setImageDrawable(null)
        wallpaperB?.setImageDrawable(null)
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

    /** Keep full strip + plate + chrome on the same base_map as the baked dissolve. */
    private fun applyGlassColor(night: Boolean) {
        val glass = if (night) StripGeometry.GLASS_NIGHT else StripGeometry.GLASS_DAY
        glassBase?.setBackgroundColor(glass)
        wallpaperPlate?.setBackgroundColor(glass)
        root?.setBackgroundColor(glass)
        window.statusBarColor = glass
        window.navigationBarColor = glass
        updateEdgeOverlay(night, visible = showingAnimated)
    }

    private fun updateEdgeOverlay(night: Boolean, visible: Boolean) {
        val edge = leftEdgeOverlay ?: return
        if (!visible) {
            edge.visibility = View.GONE
            return
        }
        val glass = if (night) StripGeometry.GLASS_NIGHT else StripGeometry.GLASS_DAY
        val fadeW = if (night) {
            StripGeometry.EDGE_FEATHER_NIGHT
        } else {
            StripGeometry.EDGE_FEATHER_DAY
        }
        edge.background = edgeGradient(glass)
        val lp = edge.layoutParams as? FrameLayout.LayoutParams
        if (lp != null) {
            lp.width = fadeW
            lp.height = StripGeometry.STRIP_H
            lp.leftMargin = StripGeometry.GAUGE_RESERVE
            edge.layoutParams = lp
        }
        edge.visibility = View.VISIBLE
        edge.bringToFront()
    }

    private fun edgeGradient(glassArgb: Int): GradientDrawable {
        // Opaque glass → transparent, matching left dissolve intent without baking frames.
        return GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(glassArgb, Color.TRANSPARENT),
        )
    }

    private fun reloadWallpaper(animated: Boolean) {
        if (crossfadeRunning) {
            pendingReload = true
            return
        }
        val night = WallpaperRepository.isNightish(this)
        if (WallpaperRepository.isActiveAnimated(this)) {
            presentAnimated(night)
            return
        }
        // Leaving animated path: stop drawables and hide gradient overlay.
        if (showingAnimated) {
            stopAnimatedDrawables()
            showingAnimated = false
            updateEdgeOverlay(night, visible = false)
            // Reset scale type for static FIT_XY bake path.
            wallpaperA?.scaleType = ImageView.ScaleType.FIT_XY
            wallpaperB?.scaleType = ImageView.ScaleType.FIT_XY
            wallpaperA?.imageMatrix = Matrix()
            wallpaperB?.imageMatrix = Matrix()
        }
        presentStatic(night, crossfade = animated)
    }

    private fun presentAnimated(night: Boolean) {
        val drawable = WallpaperRepository.loadActiveDrawable(this) ?: run {
            Log.w(TAG, "presentAnimated: no drawable, fall back static first-frame")
            showingAnimated = false
            updateEdgeOverlay(night, visible = false)
            presentStatic(night, crossfade = false)
            return
        }
        val front = front()
        val back = back()
        if (front == null || back == null) {
            stopDrawable(drawable)
            return
        }

        // Stop any previous animation on both layers before swapping.
        stopAnimatedDrawables()

        applyGlassColor(night)
        showingAnimated = true
        updateEdgeOverlay(night, visible = true)

        val crop = WallpaperRepository.activeCropRect(this)
        applyCropToImageView(front, drawable, crop)
        front.setImageDrawable(drawable)
        front.alpha = 1f
        if (drawable is AnimatedImageDrawable) {
            drawable.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
            drawable.start()
        }

        // Back layer idle; no crossfade for animated (set drawable on front).
        stopDrawable(back.drawable)
        back.setImageDrawable(null)
        back.alpha = 0f

        bakedNightish = night
        val snap = (application as? StarLiveApp)?.ambientWatch?.debugSnapshot().orEmpty()
        Log.i(
            TAG,
            "presentAnimated night=$night crop=$crop " +
                "size=${drawable.intrinsicWidth}x${drawable.intrinsicHeight} $snap",
        )
    }

    private fun presentStatic(night: Boolean, crossfade: Boolean) {
        val bmp = WallpaperRepository.decodeActiveForStrip(this, night) ?: run {
            Log.w(TAG, "reloadWallpaper no bitmap night=$night")
            return
        }
        applyGlassColor(night)
        showingAnimated = false
        updateEdgeOverlay(night, visible = false)

        val glass = if (night) StripGeometry.GLASS_NIGHT else StripGeometry.GLASS_DAY
        val snap = (application as? StarLiveApp)?.ambientWatch?.debugSnapshot().orEmpty()
        Log.i(
            TAG,
            "reloadWallpaper night=$night glass=#${Integer.toHexString(glass)} " +
                "bmp=${bmp.width}x${bmp.height} anim=$crossfade $snap",
        )

        val front = front()
        val back = back()
        if (front == null || back == null) {
            bmp.recycle()
            return
        }

        front.scaleType = ImageView.ScaleType.FIT_XY
        back.scaleType = ImageView.ScaleType.FIT_XY

        // First paint — no crossfade.
        if (front.drawable == null || !crossfade || bakedNightish == null) {
            stopDrawable(front.drawable)
            front.setImageBitmap(bmp)
            front.alpha = 1f
            back.alpha = 0f
            stopDrawable(back.drawable)
            back.setImageDrawable(null)
            bakedNightish = night
            return
        }

        // Same ambient + same content path: still swap with short fade for apply/reload.
        presentWithCrossfade(back, front, bmp, night)
    }

    /**
     * Map [crop] (source pixels) onto the wallpaper band view (WALLPAPER_W×STRIP_H)
     * via [ImageView.ScaleType.MATRIX]. Without crop, [FIT_XY] fills the band.
     */
    private fun applyCropToImageView(iv: ImageView, drawable: Drawable, crop: RectF?) {
        val viewW = StripGeometry.WALLPAPER_W.toFloat()
        val viewH = StripGeometry.STRIP_H.toFloat()
        val imgW = drawable.intrinsicWidth.takeIf { it > 0 } ?: return
        val imgH = drawable.intrinsicHeight.takeIf { it > 0 } ?: return

        val src = if (crop != null && crop.width() > 1f && crop.height() > 1f) {
            RectF(
                crop.left.coerceIn(0f, imgW.toFloat()),
                crop.top.coerceIn(0f, imgH.toFloat()),
                crop.right.coerceIn(1f, imgW.toFloat()),
                crop.bottom.coerceIn(1f, imgH.toFloat()),
            ).also {
                if (it.width() < 1f || it.height() < 1f) {
                    it.set(0f, 0f, imgW.toFloat(), imgH.toFloat())
                }
            }
        } else {
            RectF(0f, 0f, imgW.toFloat(), imgH.toFloat())
        }

        // Full source (or exact band size): FIT_XY is enough and cheaper.
        if (src.left <= 0.5f && src.top <= 0.5f &&
            src.right >= imgW - 0.5f && src.bottom >= imgH - 0.5f
        ) {
            iv.scaleType = ImageView.ScaleType.FIT_XY
            iv.imageMatrix = Matrix()
            return
        }

        val dst = RectF(0f, 0f, viewW, viewH)
        val m = Matrix()
        m.setRectToRect(src, dst, Matrix.ScaleToFit.FILL)
        iv.scaleType = ImageView.ScaleType.MATRIX
        iv.imageMatrix = m
    }

    private fun presentWithCrossfade(
        incoming: ImageView,
        outgoing: ImageView,
        bmp: Bitmap,
        night: Boolean,
    ) {
        crossfadeRunning = true
        stopDrawable(incoming.drawable)
        incoming.scaleType = ImageView.ScaleType.FIT_XY
        incoming.setImageBitmap(bmp)
        incoming.alpha = 0f
        incoming.bringToFront()
        leftEdgeOverlay?.bringToFront()

        val fadeIn = ObjectAnimator.ofFloat(incoming, View.ALPHA, 0f, 1f).setDuration(CROSSFADE_MS)
        val fadeOut = ObjectAnimator.ofFloat(outgoing, View.ALPHA, 1f, 0f).setDuration(CROSSFADE_MS)
        fadeIn.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                stopDrawable(outgoing.drawable)
                outgoing.setImageDrawable(null)
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

    private fun stopAnimatedDrawables() {
        stopDrawable(wallpaperA?.drawable)
        stopDrawable(wallpaperB?.drawable)
    }

    private fun stopDrawable(drawable: Drawable?) {
        if (drawable is AnimatedImageDrawable) {
            runCatching { drawable.stop() }
        }
    }

    companion object {
        private const val TAG = "StarLive"
        private const val CROSSFADE_MS = 320L
        const val EXTRA_DISPLAY_ID = "display_id"
        const val ACTION_FINISH = "com.starlive.app.ACTION_STRIP_FINISH"
        const val ACTION_RELOAD = "com.starlive.app.ACTION_STRIP_RELOAD"
    }
}
