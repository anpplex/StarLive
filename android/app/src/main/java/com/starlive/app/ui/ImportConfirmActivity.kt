package com.starlive.app.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.starlive.app.R
import com.starlive.app.StarLiveApp
import com.starlive.app.runtime.PendingApplyStore
import com.starlive.app.ui.UiTokens.dp
import com.starlive.app.wallpaper.AnimatedMedia
import com.starlive.app.wallpaper.WallpaperRepository
import com.starlive.ring.CropStrategy
import com.starlive.ring.StripGeometry
import java.io.File

/**
 * Import confirm: interactive pan/zoom crop to 2990×284 wallpaper band, then apply.
 * Static → JPEG export; animated GIF/WebP → keep original bytes + crop rect.
 */
class ImportConfirmActivity : AppCompatActivity() {
    private var source: Bitmap? = null
    private var sourceLabel: String = "导入"
    private var sourceW: Int = 0
    private var sourceH: Int = 0
    private var isAnimated = false
    private var sourceMime: String? = null
    private var sourceBytes: ByteArray? = null
    private lateinit var cropView: CropBandView
    private lateinit var info: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(UiTokens.bg)
            setPadding(dp(20), dp(16), dp(20), dp(16))
        }
        col.addView(UiKit.title(this, "调整裁切"))
        col.addView(
            UiKit.caption(this, "拖动移动画面 · 双指或按钮缩放 · 框内为星环壁纸区（2990×284）")
                .also { it.setPadding(0, dp(6), 0, dp(10)) },
        )

        val card = UiKit.card(this)
        cropView = CropBandView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(168),
            )
            contentDescription = "裁切预览"
            onTransformChanged = { updateInfo() }
        }
        card.addView(cropView)

        info = TextView(this).apply {
            setTextColor(UiTokens.textSecondary)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(0, dp(12), 0, dp(4))
            setLineSpacing(0f, 1.25f)
        }
        card.addView(info)

        val zoomRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, 0)
        }
        fun zoomBtn(label: String, block: () -> Unit) =
            UiKit.secondaryButton(this@ImportConfirmActivity, label, block).also {
                it.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginEnd = dp(8)
                }
            }
        zoomRow.addView(zoomBtn("缩小") { cropView.zoomBy(0.85f) })
        zoomRow.addView(zoomBtn("放大") { cropView.zoomBy(1.18f) })
        zoomRow.addView(
            UiKit.secondaryButton(this, "重置") { cropView.resetToCover() }.also {
                it.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            },
        )
        card.addView(zoomRow)
        col.addView(card)

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(16) }
        }
        row.addView(
            UiKit.secondaryButton(this, "取消") {
                recycleSource()
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
        col.addView(row)

        setContentView(
            ScrollView(this).apply {
                setBackgroundColor(UiTokens.bg)
                isFillViewport = true
                addView(col)
            },
        )

        sourceLabel = intent.getStringExtra(EXTRA_LABEL) ?: "导入"
        val bmp = loadSource()
        if (bmp == null) {
            Toast.makeText(this, "无法读取图片", Toast.LENGTH_LONG).show()
            setResult(RESULT_CANCELED)
            finish()
            return
        }
        source = bmp
        sourceW = bmp.width
        sourceH = bmp.height
        cropView.setSourceBitmap(bmp)
        updateInfo()
    }

    override fun onDestroy() {
        recycleSource()
        super.onDestroy()
    }

    private fun updateInfo() {
        if (!::info.isInitialized) return
        val strategy = CropStrategy.choose(sourceW, sourceH)
        val hint = when (strategy) {
            CropStrategy.Strategy.EXACT -> "接近标准壁纸尺寸"
            CropStrategy.Strategy.BAND -> "检测到全条图，可拖动选择壁纸区"
            CropStrategy.Strategy.CENTER -> "居中适配，可拖动 / 缩放调整构图"
        }
        val z = if (::cropView.isInitialized) {
            String.format("%.0f%%", cropView.zoomFactor() * 100f)
        } else {
            "—"
        }
        val animNote = if (isAnimated) {
            val kind = when {
                sourceMime?.contains("gif", ignoreCase = true) == true -> "GIF"
                sourceMime?.contains("webp", ignoreCase = true) == true -> "WebP"
                else -> "GIF/WebP"
            }
            "\n$kind 动图保留动画（裁切参数会随壁纸保存）"
        } else {
            ""
        }
        info.text =
            "原图 ${sourceW}×${sourceH} · 输出 ${StripGeometry.WALLPAPER_W}×${StripGeometry.WALLPAPER_H}\n" +
                "$hint · 缩放 $z$animNote"
    }

    private fun loadSource(): Bitmap? {
        val bytes = loadBytes() ?: return null
        sourceBytes = bytes
        detectAnimated(bytes)
        return decodeBounded(bytes)
    }

    private fun loadBytes(): ByteArray? {
        val path = intent.getStringExtra(EXTRA_PATH)
        if (!path.isNullOrBlank()) {
            val f = File(path)
            if (!f.isFile) return null
            sourceMime = when (f.extension.lowercase()) {
                "gif" -> "image/gif"
                "webp" -> "image/webp"
                "png" -> "image/png"
                "jpg", "jpeg" -> "image/jpeg"
                else -> null
            }
            return runCatching { f.readBytes() }.getOrNull()
        }
        @Suppress("DEPRECATION")
        val uri = intent.getParcelableExtra<android.net.Uri>(EXTRA_URI)
        if (uri != null) {
            sourceMime = contentResolver.getType(uri)
            return contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }
        return null
    }

    private fun detectAnimated(bytes: ByteArray) {
        val kind = AnimatedMedia.kindFromMimeOrBytes(sourceMime, bytes)
        if (AnimatedMedia.isAnimatedKind(kind)) {
            isAnimated = true
            sourceMime = when (kind) {
                AnimatedMedia.KIND_GIF -> "image/gif"
                AnimatedMedia.KIND_WEBP -> "image/webp"
                else -> sourceMime ?: AnimatedMedia.sniffMime(bytes)
            }
            return
        }
        // Prefer sniffed mime for static path labeling.
        if (sourceMime.isNullOrBlank()) {
            sourceMime = AnimatedMedia.sniffMime(bytes)
        }
        isAnimated = false
    }

    /** Decode with side cap to avoid OOM on HU; interactive crop uses this bitmap. */
    private fun decodeBounded(bytes: ByteArray): Bitmap? {
        if (bytes.size < 32) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val sw = bounds.outWidth
        val sh = bounds.outHeight
        if (!CropStrategy.isValidBounds(sw, sh)) return null
        var sample = 1
        var cw = sw
        var ch = sh
        val maxSide = 4096
        while (cw > maxSide || ch > maxSide) {
            sample *= 2
            cw = sw / sample
            ch = sh / sample
        }
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    }

    private fun commitAndApply() {
        val okSave = if (isAnimated) {
            val bytes = sourceBytes
            if (bytes == null || bytes.size < 32) {
                Toast.makeText(this, "动图数据丢失", Toast.LENGTH_SHORT).show()
                return
            }
            val cropDisplay = cropView.sourceCropRect()
            val cropFull = scaleCropToOriginal(cropDisplay)
                ?: RectF(0f, 0f, sourceW.toFloat().coerceAtLeast(1f), sourceH.toFloat().coerceAtLeast(1f))
            val mime = sourceMime
                ?: AnimatedMedia.sniffMime(bytes)
                ?: "image/gif"
            WallpaperRepository.commitAnimated(this, bytes, mime, cropFull, sourceLabel)
        } else {
            val bmp = cropView.exportCropped()
            if (bmp == null) {
                Toast.makeText(this, "裁切失败", Toast.LENGTH_SHORT).show()
                return
            }
            WallpaperRepository.commitCropped(this, bmp, sourceLabel)
        }
        if (!okSave) {
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

    /**
     * [sourceCropRect] is in the (possibly subsampled) decode bitmap space.
     * Map back to original file pixel coordinates for animated playback crop.
     */
    private fun scaleCropToOriginal(crop: RectF?): RectF? {
        if (crop == null) return null
        val bmp = source ?: return crop
        if (bmp.width <= 0 || bmp.height <= 0) return crop
        val bytes = sourceBytes ?: return crop
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val ow = bounds.outWidth
        val oh = bounds.outHeight
        if (ow <= 0 || oh <= 0 || (ow == bmp.width && oh == bmp.height)) return crop
        val sx = ow.toFloat() / bmp.width
        val sy = oh.toFloat() / bmp.height
        return RectF(
            crop.left * sx,
            crop.top * sy,
            crop.right * sx,
            crop.bottom * sy,
        )
    }

    private fun recycleSource() {
        source?.let {
            if (!it.isRecycled) it.recycle()
        }
        source = null
        sourceBytes = null
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
