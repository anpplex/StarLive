package com.starlive.app.ui

import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.starlive.ring.StripGeometry
import com.starlive.app.wallpaper.WallpaperRepository

class SpecActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dens = resources.displayMetrics.density
        fun dp(v: Int) = (v * dens).toInt()

        val text =
            """
            星环壁纸规格

            1. 尺寸与安全区
            · 全条：${StripGeometry.STRIP_W}×${StripGeometry.STRIP_H}
            · 表盘保留（左）：${StripGeometry.GAUGE_RESERVE}px
            · 壁纸带：${StripGeometry.WALLPAPER_W}×${StripGeometry.WALLPAPER_H}
            · 软边羽化：日 ${StripGeometry.EDGE_FEATHER_DAY} / 夜 ${StripGeometry.EDGE_FEATHER_NIGHT}

            2. 文件命名（Download）
            ${WallpaperRepository.DOWNLOAD_CANDIDATES.joinToString("\n") { "· $it" }}

            3. 裁切策略
            · 接近 2990×284：按精确尺寸
            · 宽≥4032 条带：从 x=1042 取右带
            · 其它：居中 cover

            4. 开机恢复
            · 「空闲显示壁纸」开启时，车辆启动后会尝试自动恢复
            · 请至少打开过一次本 App；部分车机需允许自启动

            5. 播歌让出
            · 检测到播放中会释放星环
            · 建议开启「通知使用权」以提高检测准确度

            6. 与 Lyra
            · 星澜负责空闲壁纸；Lyra 负责播歌特效歌词
            · 请勿两套同时抢空闲星环
            · 壁纸尺寸与导入方式通用，可升级到 Lyra
            """.trimIndent()

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(24))
            setBackgroundColor(Color.parseColor("#0B0E12"))
        }
        col.addView(
            TextView(this).apply {
                this.text = text
                setTextColor(Color.parseColor("#C5D0E0"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            },
        )
        col.addView(
            Button(this).apply {
                this.text = "关闭"
                isAllCaps = false
                setBackgroundColor(Color.parseColor("#243040"))
                setTextColor(Color.WHITE)
                setOnClickListener { finish() }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(48),
                ).apply { topMargin = dp(16) }
            },
        )
        setContentView(
            ScrollView(this).apply {
                setBackgroundColor(Color.parseColor("#0B0E12"))
                addView(col)
            },
        )
    }
}
