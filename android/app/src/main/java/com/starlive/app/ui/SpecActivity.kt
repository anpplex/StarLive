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
        col.addView(UiKit.title(this, "壁纸规格"))
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
                "· 表盘保留（左侧）：${StripGeometry.GAUGE_RESERVE}px\n" +
                "· 壁纸带：${StripGeometry.WALLPAPER_W}×${StripGeometry.WALLPAPER_H}\n" +
                "· 软边羽化：浅色 ${StripGeometry.EDGE_FEATHER_DAY}px / 深色 ${StripGeometry.EDGE_FEATHER_NIGHT}px\n" +
                "· 日夜底色：浅 #E8EAEE / 深 #080A0B",
        )
        section(
            "文件命名（Download）",
            "推荐：starlive_wallpaper.jpg / .png\n" +
                WallpaperRepository.DOWNLOAD_CANDIDATES
                    .filterNot { it.startsWith("starlive_wallpaper") }
                    .joinToString("\n") { "兼容：$it" },
        )
        section(
            "裁切规则",
            "· 接近 2990×284：缩放至壁纸尺寸\n" +
                "· 宽度 ≥ 4032 的条带图：裁取右侧壁纸区\n" +
                "· 其它尺寸：居中裁切",
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
