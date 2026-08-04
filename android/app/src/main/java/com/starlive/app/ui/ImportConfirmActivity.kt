package com.starlive.app.ui

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.starlive.app.R
import com.starlive.app.StarLiveApp
import com.starlive.app.runtime.PendingApplyStore
import com.starlive.app.ui.UiTokens.applyRoundedBg
import com.starlive.app.ui.UiTokens.dp
import com.starlive.app.wallpaper.WallpaperRepository
import com.starlive.ring.StripGeometry
import com.starlive.ring.WallpaperCropper
import java.io.File

/**
 * Always shown after import (INTERACTION). Auto-crop via [WallpaperCropper]; Apply = save + try strip.
 * Interactive pan/zoom deferred to a later release (see backup/0.1.38-crop-edit).
 */
class ImportConfirmActivity : AppCompatActivity() {
    private var cropped: Bitmap? = null
    private var sourceLabel: String = "导入"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(UiTokens.bg)
            setPadding(dp(20), dp(16), dp(20), dp(16))
        }
        root.addView(UiKit.title(this, "确认壁纸"))
        root.addView(
            UiKit.caption(this, "左侧为表盘保留区，请勿放置关键内容")
                .also { it.setPadding(0, dp(6), 0, dp(12)) },
        )

        val card = UiKit.card(this)
        val heroH = dp(72)
        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                heroH,
            )
            applyRoundedBg(UiTokens.heroBg, 10f)
        }
        val gaugeW = heroH * StripGeometry.GAUGE_RESERVE / StripGeometry.STRIP_H
        hero.addView(
            TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(gaugeW, heroH)
                applyRoundedBg(UiTokens.gaugeBg, 0f)
                gravity = Gravity.CENTER
                text = "表盘"
                setTextColor(UiTokens.textMuted)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            },
        )
        val preview = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_XY
            layoutParams = LinearLayout.LayoutParams(0, heroH, 1f)
            contentDescription = "裁切预览"
        }
        hero.addView(preview)
        card.addView(hero)

        val info = TextView(this).apply {
            setTextColor(UiTokens.textSecondary)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(0, dp(12), 0, dp(4))
            setLineSpacing(0f, 1.25f)
        }
        card.addView(info)
        root.addView(card)

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(16) }
        }
        row.addView(
            UiKit.secondaryButton(this, "取消") {
                cropped?.recycle()
                setResult(RESULT_CANCELED)
                finish()
            },
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dp(8)
            },
        )
        row.addView(
            UiKit.primaryButton(this, getString(R.string.btn_apply)) { commitAndApply() },
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.2f),
        )
        root.addView(row)
        setContentView(root)

        val result = loadCrop()
        if (result == null) {
            Toast.makeText(this, "无法读取图片", Toast.LENGTH_LONG).show()
            setResult(RESULT_CANCELED)
            finish()
            return
        }
        cropped = result.bitmap
        sourceLabel = intent.getStringExtra(EXTRA_LABEL) ?: "导入"
        preview.setImageBitmap(result.bitmap)
        info.text = "原图 ${result.sourceW}×${result.sourceH}\n${result.strategyLabelZh}"
    }

    private fun loadCrop(): WallpaperCropper.Result? {
        val path = intent.getStringExtra(EXTRA_PATH)
        if (!path.isNullOrBlank()) {
            return WallpaperCropper.decodeAndCrop(File(path))
        }
        @Suppress("DEPRECATION")
        val uri = intent.getParcelableExtra<android.net.Uri>(EXTRA_URI)
        if (uri != null) {
            return contentResolver.openInputStream(uri)?.use { WallpaperCropper.decodeAndCrop(it) }
        }
        return null
    }

    private fun commitAndApply() {
        val bmp = cropped ?: return
        if (!WallpaperRepository.commitCropped(this, bmp, sourceLabel)) {
            Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show()
            return
        }
        val orch = (application as StarLiveApp).orchestrator
        val ok = orch.applyCurrent("import-confirm")
        if (!ok && WallpaperRepository.idlePrefer(this)) {
            PendingApplyStore.setPending(this, true)
        } else {
            PendingApplyStore.clear(this)
        }
        Toast.makeText(
            this,
            if (ok) getString(R.string.toast_applied) else "已保存到图库。若未显示，请再次点「应用到星环」",
            Toast.LENGTH_SHORT,
        ).show()
        setResult(RESULT_OK)
        finish()
    }

    companion object {
        const val EXTRA_PATH = "path"
        const val EXTRA_URI = "uri"
        const val EXTRA_LABEL = "label"

        fun intentFromPath(ctx: android.content.Context, path: String, label: String): Intent =
            Intent(ctx, ImportConfirmActivity::class.java)
                .putExtra(EXTRA_PATH, path)
                .putExtra(EXTRA_LABEL, label)

        fun intentFromUri(ctx: android.content.Context, uri: android.net.Uri, label: String): Intent =
            Intent(ctx, ImportConfirmActivity::class.java)
                .putExtra(EXTRA_URI, uri)
                .putExtra(EXTRA_LABEL, label)
    }
}
