package com.starlive.ring

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.File
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min

/**
 * Crop rules shared with StarLive and Lyra handoff:
 * Strategy selection is [CropStrategy]; this class owns Bitmap decode / crop.
 */
object WallpaperCropper {
    private const val TAG = "RingWallpaper"

    data class Result(
        val bitmap: Bitmap,
        val strategy: CropStrategy.Strategy,
        val sourceW: Int,
        val sourceH: Int,
        val strategyLabelZh: String,
    )

    fun decodeAndCrop(stream: InputStream): Result? {
        val bytes = stream.readBytes()
        return decodeAndCrop(bytes)
    }

    fun decodeAndCrop(file: File): Result? {
        if (!file.isFile || file.length() < 32L) return null
        return decodeAndCrop(file.readBytes())
    }

    fun decodeAndCrop(bytes: ByteArray): Result? {
        if (bytes.size < 32) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val sw = bounds.outWidth
        val sh = bounds.outHeight
        if (!CropStrategy.isValidBounds(sw, sh)) {
            Log.w(TAG, "decode bounds invalid ${sw}x$sh")
            return null
        }
        val sample = sampleSizeFor(sw, sh, maxSide = 8192)
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val raw = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts) ?: return null
        return crop(raw)
    }

    fun crop(src: Bitmap): Result {
        val sw = src.width
        val sh = src.height
        val tw = StripGeometry.WALLPAPER_W
        val th = StripGeometry.WALLPAPER_H
        val strategy = CropStrategy.choose(sw, sh)
        val label = CropStrategy.labelZh(strategy)

        when (strategy) {
            CropStrategy.Strategy.EXACT -> {
                val scaled = if (sw == tw && sh == th) {
                    src
                } else {
                    Bitmap.createScaledBitmap(src, tw, th, true).also {
                        if (it !== src) src.recycle()
                    }
                }
                return Result(scaled, strategy, sw, sh, label)
            }
            CropStrategy.Strategy.BAND -> {
                val x = StripGeometry.GAUGE_RESERVE
                val y = max(0, (sh - th) / 2)
                val cropH = min(th, sh)
                val cropW = min(tw, sw - x)
                val piece = Bitmap.createBitmap(src, x, y, min(cropW, sw - x), cropH)
                if (piece !== src) src.recycle()
                val out = if (piece.width != tw || piece.height != th) {
                    Bitmap.createScaledBitmap(piece, tw, th, true).also {
                        if (it !== piece) piece.recycle()
                    }
                } else {
                    piece
                }
                return Result(out, strategy, sw, sh, label)
            }
            CropStrategy.Strategy.CENTER -> {
                val scale = max(tw.toFloat() / sw, th.toFloat() / sh)
                val scaledW = max(tw, (sw * scale).toInt())
                val scaledH = max(th, (sh * scale).toInt())
                val scaled = Bitmap.createScaledBitmap(src, scaledW, scaledH, true)
                if (scaled !== src) src.recycle()
                val x = max(0, (scaled.width - tw) / 2)
                val y = max(0, (scaled.height - th) / 2)
                val out = Bitmap.createBitmap(scaled, x, y, tw, th)
                if (out !== scaled) scaled.recycle()
                return Result(out, strategy, sw, sh, label)
            }
        }
    }

    private fun sampleSizeFor(w: Int, h: Int, maxSide: Int): Int {
        var sample = 1
        var cw = w
        var ch = h
        while (cw > maxSide || ch > maxSide) {
            sample *= 2
            cw = w / sample
            ch = h / sample
        }
        return sample.coerceAtLeast(1)
    }
}
