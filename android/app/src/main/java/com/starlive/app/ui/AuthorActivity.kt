package com.starlive.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.starlive.app.BuildConfig
import com.starlive.app.R
import com.starlive.app.contact.ContactConfig
import com.starlive.app.ui.UiTokens.applyRoundedBg
import com.starlive.app.ui.UiTokens.dp

/**
 * 关于作者 — aligned with Lyra author card (name, bio, Douyin/WeChat IDs + QR).
 */
class AuthorActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val name = ContactConfig.authorName(this)
        val bio = ContactConfig.authorBio(this)
        val wechat = ContactConfig.wechatId(this)
        val douyin = ContactConfig.douyinId(this)
        val douyinHandle = ContactConfig.douyinHandle(this)

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(24))
            setBackgroundColor(UiTokens.bg)
        }
        col.addView(UiKit.title(this, "关于作者"))
        col.addView(
            UiKit.caption(this, "星澜与 Lyra 由同一作者维护")
                .also { it.setPadding(0, dp(6), 0, dp(12)) },
        )

        // Brand mark B — photographic 星环光环
        runCatching {
            assets.open("about/logo_brand.png").use { stream ->
                val bmp = BitmapFactory.decodeStream(stream) ?: return@use
                col.addView(
                    ImageView(this).apply {
                        setImageBitmap(bmp)
                        scaleType = ImageView.ScaleType.CENTER_CROP
                        contentDescription = getString(R.string.app_name)
                        layoutParams = LinearLayout.LayoutParams(dp(96), dp(96)).apply {
                            gravity = Gravity.CENTER_HORIZONTAL
                            bottomMargin = dp(14)
                        }
                        clipToOutline = true
                        outlineProvider = object : android.view.ViewOutlineProvider() {
                            override fun getOutline(view: android.view.View, outline: android.graphics.Outline) {
                                outline.setRoundRect(0, 0, view.width, view.height, dp(22).toFloat())
                            }
                        }
                    },
                )
            }
        }

        val card = UiKit.card(this)
        card.addView(
            TextView(this).apply {
                text = name
                setTextColor(UiTokens.textPrimary)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            },
        )
        card.addView(
            TextView(this).apply {
                text = bio
                setTextColor(UiTokens.textSecondary)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setPadding(0, dp(6), 0, 0)
                setLineSpacing(0f, 1.25f)
            },
        )
        card.addView(
            TextView(this).apply {
                text = "抖音 @$douyinHandle · $douyin"
                setTextColor(UiTokens.textMuted)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setPadding(0, dp(8), 0, 0)
            },
        )
        card.addView(
            TextView(this).apply {
                text = "微信 $wechat"
                setTextColor(UiTokens.textMuted)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setPadding(0, dp(2), 0, 0)
            },
        )
        card.addView(
            TextView(this).apply {
                text = "星澜 ${BuildConfig.VERSION_NAME}"
                setTextColor(UiTokens.textMuted)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setPadding(0, dp(4), 0, dp(12))
            },
        )

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        row.addView(
            qrColumn("抖音", "扫码关注", "about/qr_douyin.jpg"),
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dp(8)
            },
        )
        row.addView(
            qrColumn("微信", "扫码加好友", "about/qr_wechat.jpg"),
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(8)
            },
        )
        card.addView(row)
        card.addView(
            TextView(this).apply {
                text = "车机可截图后用手机扫码，或打开抖音搜索 $douyin"
                setTextColor(UiTokens.textMuted)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setPadding(0, dp(12), 0, 0)
                setLineSpacing(0f, 1.2f)
            },
        )
        col.addView(card)

        fun full(label: String, primary: Boolean = false, block: () -> Unit) {
            val b = if (primary) UiKit.primaryButton(this, label, block)
            else UiKit.secondaryButton(this, label, block)
            b.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(8) }
            col.addView(b)
        }
        full("复制微信号 $wechat", primary = true) {
            copy(wechat)
            Toast.makeText(this, "已复制微信号 $wechat", Toast.LENGTH_SHORT).show()
        }
        full("复制抖音号 $douyin") {
            copy(douyin)
            Toast.makeText(this, "已复制抖音号 $douyin", Toast.LENGTH_SHORT).show()
        }
        col.addView(
            UiKit.ghostButton(this, "关闭") { finish() }.also {
                it.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { topMargin = dp(12) }
            },
        )

        setContentView(
            ScrollView(this).apply {
                setBackgroundColor(UiTokens.bg)
                addView(col)
            },
        )
    }

    private fun qrColumn(title: String, caption: String, assetPath: String): LinearLayout {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            applyRoundedBg(UiTokens.surface2, 12f, UiTokens.stroke)
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }
        col.addView(
            TextView(this).apply {
                text = title
                setTextColor(UiTokens.textPrimary)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                gravity = Gravity.CENTER
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            },
        )
        col.addView(
            TextView(this).apply {
                text = caption
                setTextColor(UiTokens.textMuted)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                gravity = Gravity.CENTER
                setPadding(0, dp(2), 0, dp(8))
            },
        )
        val qrSize = dp(168)
        val image = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = LinearLayout.LayoutParams(qrSize, qrSize)
            contentDescription = title
            val bmp = runCatching {
                assets.open(assetPath).use { BitmapFactory.decodeStream(it) }
            }.getOrNull()
            if (bmp != null) {
                setImageBitmap(bmp)
            } else {
                setBackgroundColor(UiTokens.surface3)
            }
            // White plate behind QR for easier phone scan from HU screenshot.
            background = GradientDrawable().apply {
                setColor(0xFFFFFFFF.toInt())
                cornerRadius = 8f * resources.displayMetrics.density
            }
            setPadding(dp(6), dp(6), dp(6), dp(6))
        }
        col.addView(image)
        return col
    }

    private fun copy(text: String) {
        val cm = getSystemService(ClipboardManager::class.java)
        cm.setPrimaryClip(ClipData.newPlainText("starlive-contact", text))
    }
}
