package com.starlive.app.ui

import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import com.starlive.app.ui.UiTokens.applyRoundedBg
import com.starlive.app.ui.UiTokens.dp

/**
 * Programmatic UI primitives tuned for car landscape (large hit targets).
 */
object UiKit {
    private const val BTN_H = 48
    private const val CHIP_H = 44

    fun sectionTitle(ctx: Context, text: String): TextView =
        TextView(ctx).apply {
            this.text = text
            setTextColor(UiTokens.textMuted)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.04f
            setPadding(0, ctx.dp(14), 0, ctx.dp(8))
        }

    fun caption(ctx: Context, text: String): TextView =
        TextView(ctx).apply {
            this.text = text
            setTextColor(UiTokens.textMuted)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setPadding(0, ctx.dp(2), 0, ctx.dp(4))
            setLineSpacing(0f, 1.15f)
        }

    fun body(ctx: Context, text: String): TextView =
        TextView(ctx).apply {
            this.text = text
            setTextColor(UiTokens.textSecondary)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setLineSpacing(0f, 1.2f)
        }

    fun title(ctx: Context, text: String): TextView =
        TextView(ctx).apply {
            this.text = text
            setTextColor(UiTokens.textPrimary)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

    fun primaryButton(ctx: Context, label: String, onClick: () -> Unit): Button =
        Button(ctx).apply {
            text = label
            isAllCaps = false
            setTextColor(UiTokens.textPrimary)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            minHeight = ctx.dp(BTN_H)
            applyRoundedBg(UiTokens.accent, 12f)
            setOnClickListener { onClick() }
            setPadding(ctx.dp(14), ctx.dp(10), ctx.dp(14), ctx.dp(10))
        }

    fun secondaryButton(ctx: Context, label: String, onClick: () -> Unit): Button =
        Button(ctx).apply {
            text = label
            isAllCaps = false
            setTextColor(UiTokens.textSecondary)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            minHeight = ctx.dp(BTN_H)
            applyRoundedBg(UiTokens.surface3, 12f, UiTokens.stroke)
            setOnClickListener { onClick() }
            setPadding(ctx.dp(12), ctx.dp(10), ctx.dp(12), ctx.dp(10))
        }

    fun ghostButton(ctx: Context, label: String, onClick: () -> Unit): Button =
        Button(ctx).apply {
            text = label
            isAllCaps = false
            setTextColor(UiTokens.textLink)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            minHeight = ctx.dp(40)
            applyRoundedBg(UiTokens.surface2, 10f)
            setOnClickListener { onClick() }
        }

    fun chip(
        ctx: Context,
        label: String,
        selected: Boolean = false,
        onClick: () -> Unit,
    ): Button =
        Button(ctx).apply {
            text = label
            isAllCaps = false
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            minHeight = ctx.dp(CHIP_H)
            setTextColor(if (selected) UiTokens.textPrimary else UiTokens.textSecondary)
            applyRoundedBg(
                if (selected) UiTokens.accent else UiTokens.surface2,
                20f,
                if (selected) null else UiTokens.stroke,
            )
            setOnClickListener { onClick() }
            setPadding(ctx.dp(14), ctx.dp(8), ctx.dp(14), ctx.dp(8))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { marginEnd = ctx.dp(8) }
        }

    fun statusPill(ctx: Context): TextView =
        TextView(ctx).apply {
            setTextColor(UiTokens.success)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(ctx.dp(12), ctx.dp(6), ctx.dp(12), ctx.dp(6))
            applyRoundedBg(UiTokens.surface2, 16f, UiTokens.stroke)
            minHeight = ctx.dp(32)
        }

    fun card(ctx: Context): LinearLayout =
        LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            applyRoundedBg(UiTokens.surface, 14f, UiTokens.stroke)
            setPadding(ctx.dp(14), ctx.dp(12), ctx.dp(14), ctx.dp(12))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = ctx.dp(10) }
        }

    fun settingRow(
        ctx: Context,
        title: String,
        subtitle: String? = null,
        initial: Boolean,
        onToggle: (Boolean) -> Unit,
    ): LinearLayout {
        val col = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        col.addView(
            TextView(ctx).apply {
                text = title
                setTextColor(UiTokens.textPrimary)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            },
        )
        if (!subtitle.isNullOrBlank()) {
            col.addView(
                TextView(ctx).apply {
                    text = subtitle
                    setTextColor(UiTokens.textMuted)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                    setPadding(0, ctx.dp(2), 0, 0)
                    setLineSpacing(0f, 1.15f)
                },
            )
        }
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, ctx.dp(8), 0, ctx.dp(4))
            minimumHeight = ctx.dp(52)
        }
        row.addView(col)
        row.addView(
            Switch(ctx).apply {
                isChecked = initial
                setOnCheckedChangeListener { _, checked -> onToggle(checked) }
            },
        )
        return row
    }

    fun hGap(ctx: Context, weight: Float = 1f, endMargin: Int = 0): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight).apply {
            if (endMargin > 0) marginEnd = endMargin
        }

    fun spacer(ctx: Context, hDp: Int): View =
        View(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                ctx.dp(hDp),
            )
        }
}
