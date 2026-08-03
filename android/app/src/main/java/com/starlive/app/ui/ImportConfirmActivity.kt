package com.starlive.app.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.starlive.app.StarLiveApp
import com.starlive.ring.StripGeometry
import com.starlive.app.runtime.PendingApplyStore
import com.starlive.ring.WallpaperCropper
import com.starlive.app.wallpaper.WallpaperRepository
import java.io.File

/**
 * Always shown after import (INTERACTION). Apply = save + try strip.
 */
class ImportConfirmActivity : AppCompatActivity() {
    private var cropped: Bitmap? = null
    private var strategyLabel: String = ""
    private var sourceLabel: String = "导入"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dens = resources.displayMetrics.density
        fun dp(v: Int) = (v * dens).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0B0E12"))
            setPadding(dp(20), dp(16), dp(20), dp(16))
        }
        root.addView(
            TextView(this).apply {
                text = "确认壁纸"
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            },
        )

        val heroH = dp(64)
        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#151A22"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                heroH,
            ).apply { topMargin = dp(12) }
        }
        val gaugeW = heroH * StripGeometry.GAUGE_RESERVE / StripGeometry.STRIP_H
        hero.addView(
            TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(gaugeW, heroH)
                setBackgroundColor(Color.parseColor("#2A303A"))
                gravity = Gravity.CENTER
                text = "表盘"
                setTextColor(Color.parseColor("#5A6578"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            },
        )
        val preview = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_XY
            layoutParams = LinearLayout.LayoutParams(0, heroH, 1f)
        }
        hero.addView(preview)
        root.addView(hero)

        val info = TextView(this).apply {
            setTextColor(Color.parseColor("#9AABB8"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(0, dp(10), 0, dp(16))
        }
        root.addView(info)

        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(
            Button(this).apply {
                text = "取消"
                isAllCaps = false
                setBackgroundColor(Color.parseColor("#243040"))
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginEnd = dp(8) }
                setOnClickListener {
                    cropped?.recycle()
                    setResult(RESULT_CANCELED)
                    finish()
                }
            },
        )
        row.addView(
            Button(this).apply {
                text = "应用上屏"
                isAllCaps = false
                setBackgroundColor(Color.parseColor("#3D6FE0"))
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(0, dp(48), 1f)
                setOnClickListener { commitAndApply() }
            },
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
        strategyLabel = result.strategyLabelZh
        sourceLabel = intent.getStringExtra(EXTRA_LABEL) ?: "导入"
        preview.setImageBitmap(result.bitmap)
        info.text = "原图 ${result.sourceW}×${result.sourceH}\n$strategyLabel\n左侧表盘区不放贴图。"
    }

    private fun loadCrop(): WallpaperCropper.Result? {
        val path = intent.getStringExtra(EXTRA_PATH)
        if (!path.isNullOrBlank()) {
            return WallpaperCropper.decodeAndCrop(File(path))
        }
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
        // Phase 3: if playing → pending only
        val ok = orch.applyCurrent("import-confirm")
        if (!ok && WallpaperRepository.idlePrefer(this)) {
            // Still saved; mark pending if we later add play gate
            PendingApplyStore.setPending(this, true)
        } else {
            PendingApplyStore.clear(this)
        }
        Toast.makeText(
            this,
            if (ok) "壁纸已应用" else "已保存 · 若未上屏请检查星环 Display",
            Toast.LENGTH_SHORT,
        ).show()
        setResult(RESULT_OK)
        finish()
    }

    override fun onDestroy() {
        // Don't recycle if committed — ImageView may still hold; only recycle cancel path
        super.onDestroy()
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
