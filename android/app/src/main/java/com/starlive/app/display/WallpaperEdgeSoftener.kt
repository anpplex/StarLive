package com.starlive.app.display

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Bake left-edge dissolve into wallpaper (ported from Lyra, package-local). */
object WallpaperEdgeSoftener {
    private val BAYER_4 = intArrayOf(
        0, 8, 2, 10,
        12, 4, 14, 6,
        3, 11, 1, 9,
        15, 7, 13, 5,
    )

    fun softenLeftEdge(src: Bitmap, fadePx: Int, glassArgb: Int): Bitmap {
        val w = src.width
        val h = src.height
        if (w < 8 || h < 1) return src
        val fade = fadePx.coerceIn(24, (w * 0.12f).toInt().coerceAtLeast(24))
        val out = if (src.config == Bitmap.Config.ARGB_8888 && src.isMutable) {
            src
        } else {
            src.copy(Bitmap.Config.ARGB_8888, true) ?: return src
        }
        val gr = Color.red(glassArgb)
        val gg = Color.green(glassArgb)
        val gb = Color.blue(glassArgb)
        val row = IntArray(w)
        val blurRadius = max(1, fade / 28)
        for (y in 0 until h) {
            out.getPixels(row, 0, w, 0, y, w, 1)
            val srcRow = row.copyOf()
            for (x in 0 until fade) {
                val u = x.toFloat() / (fade - 1).coerceAtLeast(1)
                val s0 = u * u * (3f - 2f * u)
                val s = s0 * s0 * (3f - 2f * s0)
                val dither = (BAYER_4[(y and 3) * 4 + (x and 3)] - 7.5f) / 900f
                val t = (s + dither).coerceIn(0f, 1f)
                val (sr, sg, sb) = blurredSample(srcRow, w, x, blurRadius)
                val r = (sr * t + gr * (1f - t)).roundToInt().coerceIn(0, 255)
                val g = (sg * t + gg * (1f - t)).roundToInt().coerceIn(0, 255)
                val b = (sb * t + gb * (1f - t)).roundToInt().coerceIn(0, 255)
                row[x] = Color.argb(255, r, g, b)
            }
            out.setPixels(row, 0, w, 0, y, w, 1)
        }
        return out
    }

    private fun blurredSample(row: IntArray, w: Int, x: Int, radius: Int): Triple<Float, Float, Float> {
        var rs = 0
        var gs = 0
        var bs = 0
        var n = 0
        val lo = max(0, x - radius)
        val hi = min(w - 1, x + radius)
        for (i in lo..hi) {
            val c = row[i]
            rs += Color.red(c)
            gs += Color.green(c)
            bs += Color.blue(c)
            n++
        }
        val inv = 1f / n
        return Triple(rs * inv, gs * inv, bs * inv)
    }
}
