package com.starlive.app.ui

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity
import com.starlive.app.ui.UiTokens.dp
import com.starlive.app.wallpaper.WallpaperRepository
import com.starlive.ring.StripGeometry

class SpecActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(24))
            setBackgroundColor(UiTokens.bg)
        }
        col.addView(UiKit.title(this, "规格说明"))
        col.addView(
            UiKit.caption(this, "星环几何与导入约定")
                .also { it.setPadding(0, dp(6), 0, dp(12)) },
        )

        fun section(title: String, body: String) {
            col.addView(UiKit.sectionTitle(this, title))
            val card = UiKit.card(this)
            card.addView(UiKit.body(this, body))
            col.addView(card)
        }

        section(
            "尺寸与安全区",
            "· 全条：${StripGeometry.STRIP_W}×${StripGeometry.STRIP_H}\n" +
                "· 表盘保留（左）：${StripGeometry.GAUGE_RESERVE}px\n" +
                "· 壁纸带：${StripGeometry.WALLPAPER_W}×${StripGeometry.WALLPAPER_H}\n" +
                "· 软边羽化：日 ${StripGeometry.EDGE_FEATHER_DAY} / 夜 ${StripGeometry.EDGE_FEATHER_NIGHT}\n" +
                "· 日夜玻璃：浅 #E8EAEE / 深 #080A0B（跟车机显示模式，与 Lyra 同源）",
        )
        section(
            "文件命名（Download）",
            WallpaperRepository.DOWNLOAD_CANDIDATES.joinToString("\n") { "· $it" },
        )
        section(
            "裁切策略",
            "· 接近 2990×284：精确尺寸\n" +
                "· 宽≥4032 条带：从 x=1042 取右带\n" +
                "· 其它：居中 cover",
        )
        section(
            "开机与播歌",
            "· 空闲显示开启时，打开 App 会尝试恢复上屏\n" +
                "· 部分车机需允许自启动\n" +
                "· 播歌中自动让出星环（建议通知使用权）",
        )
        section(
            "与 Lyra",
            "· 星澜：空闲壁纸 · Lyra：播歌特效歌词\n" +
                "· 请勿两套同时抢空闲星环\n" +
                "· 尺寸与导入方式通用，可升级到 Lyra",
        )

        col.addView(
            UiKit.secondaryButton(this, "关闭") { finish() }.also {
                it.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { topMargin = dp(16) }
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
