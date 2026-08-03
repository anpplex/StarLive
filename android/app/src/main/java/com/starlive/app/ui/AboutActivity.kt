package com.starlive.app.ui

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.starlive.app.BuildConfig
import com.starlive.app.upgrade.LyraPresence
import com.starlive.app.upgrade.UpdateChecker
import kotlin.concurrent.thread

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dens = resources.displayMetrics.density
        fun dp(v: Int) = (v * dens).toInt()
        val lyra = LyraPresence.isInstalled(this)

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(24))
            setBackgroundColor(Color.parseColor("#0B0E12"))
        }
        fun body(t: String) = TextView(this).apply {
            text = t
            setTextColor(Color.parseColor("#C5D0E0"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setPadding(0, dp(6), 0, dp(6))
        }
        fun btn(label: String, onClick: () -> Unit) = Button(this).apply {
            text = label
            isAllCaps = false
            setBackgroundColor(Color.parseColor("#243040"))
            setTextColor(Color.WHITE)
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(44),
            ).apply { topMargin = dp(6) }
        }

        col.addView(
            TextView(this).apply {
                text = "星澜 StarLive"
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            },
        )
        col.addView(body("版本 ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"))
        val updateStatus = body("检查更新：未检查（需联网访问 GitHub）")
        col.addView(updateStatus)
        col.addView(
            body(
                "与 Lyra：星澜是 Lyra 生态的壁纸子项目，可升级获得播歌特效歌词。\n" +
                    if (lyra) "本机已安装 Lyra。" else "本机未检测到 Lyra。",
            ),
        )
        col.addView(
            body(
                "免责声明：第三方工具，与阿维塔 / 华为官方无关。系统升级可能导致功能变化或失效。\n\n" +
                    "安全：请在停车时设置壁纸，勿在驾驶过程中操作。\n\n" +
                    "隐私：默认不强制联网；图片仅本地处理；媒体播放状态仅用于播歌让出，不做歌词分析。" +
                    "使用「兑换主题」或「检查更新」时会联网。详见仓库 docs/PRIVACY.md。\n\n" +
                    "协议：Apache-2.0",
            ),
        )
        col.addView(
            btn("检查更新") {
                updateStatus.text = "检查更新中…"
                thread {
                    val r = UpdateChecker.fetchLatest()
                    runOnUiThread {
                        r.onSuccess { rel ->
                            if (rel.isNewer) {
                                updateStatus.text = "有新版本 ${rel.tag}（当前 ${BuildConfig.VERSION_NAME}）"
                                Toast.makeText(this, "发现 ${rel.tag}", Toast.LENGTH_SHORT).show()
                                runCatching {
                                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(rel.htmlUrl)))
                                }
                            } else {
                                updateStatus.text = "已是最新（远端 ${rel.tag}）"
                                Toast.makeText(this, "已是最新", Toast.LENGTH_SHORT).show()
                            }
                        }.onFailure { e ->
                            updateStatus.text = e.message ?: "检查失败"
                            Toast.makeText(this, updateStatus.text, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            },
        )
        col.addView(
            btn("兑换主题") {
                startActivity(Intent(this, RedeemActivity::class.java))
            },
        )
        col.addView(
            btn("升级到 Lyra") {
                startActivity(Intent(this, UpgradeActivity::class.java))
            },
        )
        col.addView(
            btn("规格说明") {
                startActivity(Intent(this, SpecActivity::class.java))
            },
        )
        col.addView(
            btn("私人定制") {
                startActivity(Intent(this, CustomActivity::class.java))
            },
        )
        col.addView(
            btn("开源仓库") {
                runCatching {
                    startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/anpplex/StarLive")),
                    )
                }.onFailure {
                    Toast.makeText(this, "无法打开浏览器", Toast.LENGTH_SHORT).show()
                }
            },
        )
        col.addView(
            btn("通知使用权（播歌让出）") {
                BatteryHelper.openNotificationListenerSettings(this)
            },
        )
        col.addView(
            btn("电池优化 / 自启动") {
                BatteryHelper.openBatterySettings(this)
                Toast.makeText(
                    this,
                    if (BatteryHelper.isIgnoringOptimizations(this)) "已忽略电池优化"
                    else "请允许星澜忽略电池优化，利于开机恢复",
                    Toast.LENGTH_LONG,
                ).show()
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
}
