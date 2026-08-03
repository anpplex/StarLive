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
            UiKit.caption(this, "星澜是空闲壁纸入门；Lyra 是完整星环体验")
                .also { it.setPadding(0, dp(6), 0, dp(12)) },
        )

        val card = UiKit.card(this)
        card.addView(
            UiKit.body(
                this,
                "Lyra 更完整的能力\n" +
                    "· 播歌时星环歌词与动态特效（远超静态壁纸）\n" +
                    "· 更完整的星环占用与场景联动\n" +
                    "· 商业主题与授权体系\n" +
                    "· 与星澜相同的壁纸规格（2990×284），可无缝承接你的图\n\n" +
                    "星澜适合：免费换空闲壁纸、导入与定制。\n" +
                    "Lyra 适合：要歌词特效、完整体验与持续能力更新。\n\n" +
                    "升级建议\n" +
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
                    "星澜 → Lyra：安装 Lyra → 完成授权并开启总开关 → 导入壁纸。" +
                        "Lyra 提供播歌歌词特效与更完整星环能力；壁纸规格 2990×284 通用。",
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
