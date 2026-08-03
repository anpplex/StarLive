package com.starlive.ring

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.File
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min

/**
 * Crop rules (INTERACTION-1.0 / shared with StarLive + future Lyra):
 * - ≈2990×284 ±2 → exact (may only scale if needed)
 * - width ≥ 4032 and strip-like height → band crop from x=1042
 * - else center cover to 2990×284
 */
object WallpaperCropper {
    private const val TAG = "RingWallpaper"
    private const val EXACT_TOL = 2

    enum class Strategy { EXACT, BAND, CENTER }

    data class Result(
        val bitmap: Bitmap,
        val strategy: Strategy,
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
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val sw = bounds.outWidth
        val sh = bounds.outHeight
        if (sw < 8 || sh < 8) {
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

        if (kotlin.math.abs(sw - tw) <= EXACT_TOL && kotlin.math.abs(sh - th) <= EXACT_TOL) {
            val scaled = if (sw == tw && sh == th) {
                src
            } else {
                Bitmap.createScaledBitmap(src, tw, th, true).also {
                    if (it !== src) src.recycle()
                }
            }
            return Result(scaled, Strategy.EXACT, sw, sh, "尺寸匹配 · 将裁为 2990×284")
        }

        val stripLike = sh in 200..400 || ratioClose(sw, sh, StripGeometry.STRIP_W, StripGeometry.STRIP_H)
        if (sw >= StripGeometry.STRIP_W && stripLike) {
            val x = StripGeometry.GAUGE_RESERVE
            val y = max(0, (sh - th) / 2)
            val cropH = min(th, sh)
            val cropW = min(tw, sw - x)
            if (cropW >= tw / 2) {
                val piece = Bitmap.createBitmap(src, x, y, min(cropW, sw - x), cropH)
                if (piece !== src) src.recycle()
                val out = if (piece.width != tw || piece.height != th) {
                    Bitmap.createScaledBitmap(piece, tw, th, true).also {
                        if (it !== piece) piece.recycle()
                    }
                } else {
                    piece
                }
                return Result(out, Strategy.BAND, sw, sh, "将按星环右带（跳过表盘区）裁切为 2990×284")
            }
        }

        val scale = max(tw.toFloat() / sw, th.toFloat() / sh)
        val scaledW = max(tw, (sw * scale).toInt())
        val scaledH = max(th, (sh * scale).toInt())
        val scaled = Bitmap.createScaledBitmap(src, scaledW, scaledH, true)
        if (scaled !== src) src.recycle()
        val x = max(0, (scaled.width - tw) / 2)
        val y = max(0, (scaled.height - th) / 2)
        val out = Bitmap.createBitmap(scaled, x, y, tw, th)
        if (out !== scaled) scaled.recycle()
        return Result(out, Strategy.CENTER, sw, sh, "将居中裁切为 2990×284")
    }

    private fun ratioClose(w: Int, h: Int, rw: Int, rh: Int): Boolean {
        val a = w.toFloat() / h
        val b = rw.toFloat() / rh
        return kotlin.math.abs(a - b) < 0.15f
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
