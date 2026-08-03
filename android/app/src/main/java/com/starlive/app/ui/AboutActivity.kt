package com.starlive.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.starlive.app.BuildConfig
import com.starlive.app.ui.UiTokens.dp
import com.starlive.app.upgrade.LyraPresence
import com.starlive.app.upgrade.UpdateChecker
import kotlin.concurrent.thread

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val lyra = LyraPresence.isInstalled(this)

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(24))
            setBackgroundColor(UiTokens.bg)
        }

        col.addView(UiKit.title(this, "关于"))
        col.addView(
            UiKit.caption(this, "星澜 ${BuildConfig.VERSION_NAME}（${BuildConfig.VERSION_CODE}）")
                .also { it.setPadding(0, dp(6), 0, dp(10)) },
        )

        val updateCard = UiKit.card(this)
        val updateStatus = TextView(this).apply {
            text = "检查更新需要联网"
            setTextColor(UiTokens.textSecondary)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setLineSpacing(0f, 1.2f)
        }
        updateCard.addView(updateStatus)
        updateCard.addView(
            UiKit.primaryButton(this, "检查更新") {
                updateStatus.setTextColor(UiTokens.info)
                updateStatus.text = "正在检查…"
                thread {
                    val r = UpdateChecker.fetchLatest()
                    runOnUiThread {
                        r.onSuccess { rel ->
                            if (rel.isNewer) {
                                updateStatus.setTextColor(UiTokens.success)
                                updateStatus.text = "发现新版本 ${rel.tag}（当前 ${BuildConfig.VERSION_NAME}）"
                                Toast.makeText(this, "发现新版本 ${rel.tag}", Toast.LENGTH_SHORT).show()
                                runCatching {
                                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(rel.htmlUrl)))
                                }
                            } else {
                                updateStatus.setTextColor(UiTokens.textSecondary)
                                updateStatus.text = "已是最新版本"
                                Toast.makeText(this, "已是最新版本", Toast.LENGTH_SHORT).show()
                            }
                        }.onFailure { e ->
                            updateStatus.setTextColor(UiTokens.danger)
                            updateStatus.text = "检查失败，请稍后重试"
                            Toast.makeText(this, updateStatus.text, Toast.LENGTH_LONG).show()
                            android.util.Log.w("About", "update check failed", e)
                        }
                    }
                }
            }.also {
                it.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { topMargin = dp(10) }
            },
        )
        col.addView(updateCard)

        col.addView(UiKit.sectionTitle(this, "简介"))
        val info = UiKit.card(this)
        info.addView(
            UiKit.body(
                this,
                "星澜为阿维塔星环提供空闲壁纸。" +
                    if (lyra) "本机已安装 Lyra，可在显示设置中选择 Lyra 优先。"
                    else "如需歌词与播歌特效，可安装 Lyra。",
            ),
        )
        info.addView(UiKit.spacer(this, 8))
        info.addView(
            UiKit.body(
                this,
                "若开机后未自动显示，请在系统中允许自启动、关联启动与忽略电池优化，或手动打开星澜一次。",
            ),
        )
        info.addView(UiKit.spacer(this, 8))
        info.addView(
            UiKit.caption(
                this,
                "第三方工具，与阿维塔 / 华为官方无关。\n" +
                    "请在车辆静止时设置壁纸。\n" +
                    "默认不强制联网；兑换主题与检查更新时会访问网络。\n" +
                    "许可证：Apache-2.0",
            ),
        )
        col.addView(info)

        col.addView(UiKit.sectionTitle(this, "系统权限"))
        fun nav(label: String, block: () -> Unit) {
            val b = UiKit.secondaryButton(this, label, block)
            b.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(8) }
            col.addView(b)
        }
        nav("关于作者") {
            startActivity(Intent(this, AuthorActivity::class.java))
        }
        nav("开源仓库") {
            runCatching {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/anpplex/StarLive")))
            }.onFailure {
                Toast.makeText(this, "无法打开浏览器", Toast.LENGTH_SHORT).show()
            }
        }
        nav("通知使用权") {
            BatteryHelper.openNotificationListenerSettings(this)
        }
        nav("电池优化与自启动") {
            BatteryHelper.openBatterySettings(this)
            Toast.makeText(
                this,
                if (BatteryHelper.isIgnoringOptimizations(this)) {
                    "已忽略电池优化，建议同时在系统自启动列表中允许星澜"
                } else {
                    "请允许忽略电池优化，并在系统自启动列表中允许星澜"
                },
                Toast.LENGTH_LONG,
            ).show()
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
