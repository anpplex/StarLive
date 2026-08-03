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
        col.addView(UiKit.title(this, "升级到 Lyra"))
        col.addView(
            UiKit.caption(this, "空闲壁纸用星澜 · 播歌特效歌词用 Lyra")
                .also { it.setPadding(0, dp(6), 0, dp(12)) },
        )

        val card = UiKit.card(this)
        card.addView(
            UiKit.body(
                this,
                "壁纸规格与 Lyra 通用（2990×284）。\n\n" +
                    "步骤：\n" +
                    "1. 安装 Lyra 并完成授权、打开「Lyra 总开关」\n" +
                    "2. Lyra「壁纸 → 下载导入」优先从星澜 ContentProvider 读当前图\n" +
                    "3. 可选：关星澜空闲显示，或开「已装 Lyra 时让路」\n\n" +
                    if (installed) "状态：已检测到本机安装 Lyra。"
                    else "状态：尚未检测到 Lyra（${LyraPresence.LYRA_PACKAGE}）。",
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
            fullBtn("让星澜停止抢星环") {
                (application as StarLiveApp).orchestrator.setIdlePrefer(false)
                Toast.makeText(this, "已关闭空闲显示 · 星环交还", Toast.LENGTH_SHORT).show()
            }
        }
        fullBtn("导出壁纸到 Download/StarLive") {
            val ok = WallpaperRepository.exportHandoffFiles(this, BuildConfig.VERSION_NAME)
            Toast.makeText(
                this,
                if (ok) "已导出 handoff 与 active 壁纸" else "导出失败 · 请检查存储权限",
                Toast.LENGTH_LONG,
            ).show()
        }
        fullBtn("复制升级说明") {
            val cm = getSystemService(ClipboardManager::class.java)
            cm.setPrimaryClip(
                ClipData.newPlainText(
                    "upgrade",
                    "星澜 → Lyra：安装 Lyra → 授权开总开关 → 使用 starlive_wallpaper / 导出包。规格 2990×284。",
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
