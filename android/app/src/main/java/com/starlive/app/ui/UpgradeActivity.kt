package com.starlive.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.starlive.app.BuildConfig
import com.starlive.app.StarLiveApp
import com.starlive.app.upgrade.LyraPresence
import com.starlive.app.wallpaper.WallpaperRepository

class UpgradeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dens = resources.displayMetrics.density
        fun dp(v: Int) = (v * dens).toInt()
        val installed = LyraPresence.isInstalled(this)

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(24))
            setBackgroundColor(Color.parseColor("#0B0E12"))
        }
        fun body(t: String) = TextView(this).apply {
            text = t
            setTextColor(Color.parseColor("#C5D0E0"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setPadding(0, dp(8), 0, dp(8))
        }
        fun btn(label: String, primary: Boolean = false, onClick: () -> Unit) =
            Button(this).apply {
                text = label
                isAllCaps = false
                setBackgroundColor(
                    if (primary) Color.parseColor("#3D6FE0") else Color.parseColor("#243040"),
                )
                setTextColor(Color.WHITE)
                setOnClickListener { onClick() }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(48),
                ).apply { topMargin = dp(8) }
            }

        col.addView(
            TextView(this).apply {
                text = "升级到 Lyra"
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            },
        )
        col.addView(
            body(
                "星澜负责空闲壁纸；Lyra 在播歌时显示特效歌词，并可接管壁纸。\n" +
                    "你的壁纸规格与 Lyra 通用（2990×284）。\n\n" +
                    "步骤：\n" +
                    "1. 安装 Lyra 并完成授权、打开「Lyra 总开关」\n" +
                    "2. 在 Lyra「壁纸 → 下载导入」：优先从本机星澜 ContentProvider 直读当前壁纸\n" +
                    "   （无需再导出也可；导出到 Download 仍可作为兜底）\n" +
                    "3. （可选）关闭星澜「空闲显示」或开「已装 Lyra 时让路」，避免双开抢屏\n\n" +
                    if (installed) "状态：已检测到本机安装 Lyra。"
                    else "状态：尚未检测到 Lyra（${LyraPresence.LYRA_PACKAGE}）。",
            ),
        )
        if (installed) {
            col.addView(
                btn("打开 Lyra", primary = true) {
                    if (!LyraPresence.launchLyra(this)) {
                        Toast.makeText(this, "无法启动 Lyra", Toast.LENGTH_SHORT).show()
                    }
                },
            )
            col.addView(
                btn("让星澜停止抢星环") {
                    (application as StarLiveApp).orchestrator.setIdlePrefer(false)
                    Toast.makeText(this, "已关闭空闲显示 · 星环交还", Toast.LENGTH_SHORT).show()
                },
            )
        }
        col.addView(
            btn("导出壁纸到 Download/StarLive") {
                val ok = exportHandoff()
                Toast.makeText(
                    this,
                    if (ok) "已导出 handoff 与 active 壁纸" else "导出失败 · 请检查存储权限",
                    Toast.LENGTH_LONG,
                ).show()
            },
        )
        col.addView(
            btn("复制升级说明") {
                val cm = getSystemService(ClipboardManager::class.java)
                cm.setPrimaryClip(
                    ClipData.newPlainText(
                        "upgrade",
                        "星澜 → Lyra：安装 Lyra → 授权开总开关 → 使用 starlive_wallpaper / 导出包。规格 2990×284。",
                    ),
                )
                Toast.makeText(this, "已复制", Toast.LENGTH_SHORT).show()
            },
        )
        col.addView(btn("关闭") { finish() })

        setContentView(
            ScrollView(this).apply {
                setBackgroundColor(Color.parseColor("#0B0E12"))
                addView(col)
            },
        )
    }

    private fun exportHandoff(): Boolean =
        WallpaperRepository.exportHandoffFiles(this, BuildConfig.VERSION_NAME)
}
