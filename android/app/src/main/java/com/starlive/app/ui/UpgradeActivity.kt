package com.starlive.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.starlive.app.BuildConfig
import com.starlive.app.StarLiveApp
import com.starlive.app.ui.UiTokens.dp
import com.starlive.app.upgrade.LyraPresence
import com.starlive.app.wallpaper.WallpaperRepository

class UpgradeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val installed = LyraPresence.isInstalled(this)

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(24))
            setBackgroundColor(UiTokens.bg)
        }
        col.addView(UiKit.title(this, "升级 Lyra"))
        col.addView(
            UiKit.caption(this, "星澜负责空闲壁纸，Lyra 提供歌词与播歌特效")
                .also { it.setPadding(0, dp(6), 0, dp(12)) },
        )

        val card = UiKit.card(this)
        card.addView(
            UiKit.body(
                this,
                "壁纸规格与 Lyra 一致（2990×284）。\n\n" +
                    "建议步骤：\n" +
                    "1. 安装 Lyra，完成授权并开启总开关\n" +
                    "2. 在 Lyra 中导入壁纸（可读取星澜当前图）\n" +
                    "3. 在星澜关闭「星环壁纸」，或开启「Lyra 优先」\n\n" +
                    if (installed) "状态：已检测到本机安装 Lyra"
                    else "状态：未检测到 Lyra",
            ),
        )
        col.addView(card)

        fun fullBtn(label: String, primary: Boolean = false, block: () -> Unit) {
            val b = if (primary) UiKit.primaryButton(this, label, block)
            else UiKit.secondaryButton(this, label, block)
            b.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(8) }
            col.addView(b)
        }

        if (installed) {
            fullBtn("打开 Lyra", primary = true) {
                if (!LyraPresence.launchLyra(this)) {
                    Toast.makeText(this, "无法启动 Lyra", Toast.LENGTH_SHORT).show()
                }
            }
            fullBtn("关闭星澜星环壁纸") {
                (application as StarLiveApp).orchestrator.setIdlePrefer(false)
                Toast.makeText(this, "已关闭星环壁纸", Toast.LENGTH_SHORT).show()
            }
        }
        fullBtn("导出壁纸到 Download/StarLive") {
            val ok = WallpaperRepository.exportHandoffFiles(this, BuildConfig.VERSION_NAME)
            Toast.makeText(
                this,
                if (ok) "已导出到 Download/StarLive" else "导出失败，请检查存储权限",
                Toast.LENGTH_LONG,
            ).show()
        }
        fullBtn("复制升级说明") {
            val cm = getSystemService(ClipboardManager::class.java)
            cm.setPrimaryClip(
                ClipData.newPlainText(
                    "upgrade",
                    "星澜 → Lyra：安装 Lyra → 完成授权并开启总开关 → 导入壁纸。规格 2990×284。",
                ),
            )
            Toast.makeText(this, "已复制", Toast.LENGTH_SHORT).show()
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
}
