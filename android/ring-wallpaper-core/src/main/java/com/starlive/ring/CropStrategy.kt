package com.starlive.ring

/**
 * Pure size → strategy rules (no Android Bitmap). Shared by [WallpaperCropper] and unit tests.
 *
 * - ≈2990×284 ±2 → [Strategy.EXACT]
 * - width ≥ 4032 and strip-like height → [Strategy.BAND] (crop from x=1042)
 * - else → [Strategy.CENTER]
 */
object CropStrategy {
    const val EXACT_TOL = 2

    enum class Strategy { EXACT, BAND, CENTER }

    fun choose(sourceW: Int, sourceH: Int): Strategy {
        val tw = StripGeometry.WALLPAPER_W
        val th = StripGeometry.WALLPAPER_H
        if (kotlin.math.abs(sourceW - tw) <= EXACT_TOL && kotlin.math.abs(sourceH - th) <= EXACT_TOL) {
            return Strategy.EXACT
        }
        val stripLike =
            sourceH in 200..400 || ratioClose(sourceW, sourceH, StripGeometry.STRIP_W, StripGeometry.STRIP_H)
        if (sourceW >= StripGeometry.STRIP_W && stripLike) {
            val cropW = minOf(tw, sourceW - StripGeometry.GAUGE_RESERVE)
            if (cropW >= tw / 2) return Strategy.BAND
        }
        return Strategy.CENTER
    }

    fun labelZh(strategy: Strategy): String = when (strategy) {
        Strategy.EXACT -> "尺寸匹配 · 将裁为 2990×284"
        Strategy.BAND -> "将按星环右带（跳过表盘区）裁切为 2990×284"
        Strategy.CENTER -> "将居中裁切为 2990×284"
    }

    /** Decode bounds too small / corrupt for wallpaper. */
    fun isValidBounds(sourceW: Int, sourceH: Int): Boolean =
        sourceW >= 8 && sourceH >= 8

    private fun ratioClose(w: Int, h: Int, rw: Int, rh: Int): Boolean {
        if (h == 0 || rh == 0) return false
        val a = w.toFloat() / h
        val b = rw.toFloat() / rh
        return kotlin.math.abs(a - b) < 0.15f
    }
}
