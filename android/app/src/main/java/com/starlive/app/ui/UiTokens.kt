package com.starlive.app.ui

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.View

/**
 * Shared visual tokens for StarLive car UI (landscape HU).
 * Keep values central so Main / Redeem / Import stay consistent.
 */
object UiTokens {
    // Surfaces
    val bg = Color.parseColor("#0B0E12")
    val surface = Color.parseColor("#121820")
    val surface2 = Color.parseColor("#1A2230")
    val surface3 = Color.parseColor("#243040")
    val stroke = Color.parseColor("#2A3548")

    // Text
    val textPrimary = Color.parseColor("#F2F5FA")
    val textSecondary = Color.parseColor("#A8B4C8")
    val textMuted = Color.parseColor("#6B7A90")
    val textLink = Color.parseColor("#7BA3F5")

    // Brand / actions
    val accent = Color.parseColor("#4C7EF0")
    val accentPressed = Color.parseColor("#3A68D4")
    val success = Color.parseColor("#5FD39A")
    val warning = Color.parseColor("#E0B35A")
    val danger = Color.parseColor("#E07A7A")
    val info = Color.parseColor("#7EB6E8")
    val lyra = Color.parseColor("#B8A0E0")

    // Hero
    val gaugeBg = Color.parseColor("#1E2530")
    val heroBg = Color.parseColor("#0F141C")

    fun Context.dp(v: Int): Int =
        (v * resources.displayMetrics.density).toInt()

    fun Context.sp(v: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, resources.displayMetrics)

    fun roundedRect(
        fill: Int,
        cornerDp: Float,
        density: Float,
        strokeColor: Int? = null,
        strokeDp: Float = 1f,
    ): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(fill)
            cornerRadius = cornerDp * density
            if (strokeColor != null) {
                setStroke((strokeDp * density).toInt().coerceAtLeast(1), strokeColor)
            }
        }

    fun View.applyRoundedBg(
        fill: Int,
        cornerDp: Float = 12f,
        strokeColor: Int? = null,
    ) {
        background = roundedRect(fill, cornerDp, resources.displayMetrics.density, strokeColor)
    }
}
