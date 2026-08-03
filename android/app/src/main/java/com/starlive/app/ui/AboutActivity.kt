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

        col.addView(UiKit.title(this, "星澜 StarLive"))
        col.addView(
            UiKit.caption(this, "版本 ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                .also { it.setPadding(0, dp(6), 0, dp(10)) },
        )

        val updateCard = UiKit.card(this)
        val updateStatus = TextView(this).apply {
            text = "检查更新：未检查（需联网访问 GitHub）"
            setTextColor(UiTokens.textSecondary)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setLineSpacing(0f, 1.2f)
        }
        updateCard.addView(updateStatus)
        updateCard.addView(
            UiKit.primaryButton(this, "检查更新") {
                updateStatus.setTextColor(UiTokens.info)
                updateStatus.text = "检查更新中…"
                thread {
                    val r = UpdateChecker.fetchLatest()
                    runOnUiThread {
                        r.onSuccess { rel ->
                            if (rel.isNewer) {
                                updateStatus.setTextColor(UiTokens.success)
                                updateStatus.text = "有新版本 ${rel.tag}（当前 ${BuildConfig.VERSION_NAME}）"
                                Toast.makeText(this, "发现 ${rel.tag}", Toast.LENGTH_SHORT).show()
                                runCatching {
                                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(rel.htmlUrl)))
                                }
                            } else {
                                updateStatus.setTextColor(UiTokens.textSecondary)
                                updateStatus.text = "已是最新（远端 ${rel.tag}）"
                                Toast.makeText(this, "已是最新", Toast.LENGTH_SHORT).show()
                            }
                        }.onFailure { e ->
                            updateStatus.setTextColor(UiTokens.danger)
                            updateStatus.text = e.message ?: "检查失败"
                            Toast.makeText(this, updateStatus.text, Toast.LENGTH_LONG).show()
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

        col.addView(UiKit.sectionTitle(this, "说明"))
        val info = UiKit.card(this)
        info.addView(
            UiKit.body(
                this,
                "与 Lyra：星澜是 Lyra 生态的壁纸子项目，可升级获得播歌特效歌词。\n" +
                    if (lyra) "本机已安装 Lyra。" else "本机未检测到 Lyra。",
            ),
        )
        info.addView(UiKit.spacer(this, 8))
        info.addView(
            UiKit.body(
                this,
                "开机恢复：空闲显示开启时，进程启动后会尝试自动上屏。" +
                    "部分车机不向第三方投递开机广播，冷启后可能需打开星澜一次；" +
                    "请在系统中允许「自启动 / 关联启动 / 忽略电池优化」。",
            ),
        )
        info.addView(UiKit.spacer(this, 8))
        info.addView(
            UiKit.caption(
                this,
                "免责声明：第三方工具，与阿维塔 / 华为官方无关。\n" +
                    "安全：请在停车时设置壁纸。\n" +
                    "隐私：默认不强制联网；兑换/检查更新会联网。\n" +
                    "协议：Apache-2.0",
            ),
        )
        col.addView(info)

        col.addView(UiKit.sectionTitle(this, "入口"))
        fun nav(label: String, primary: Boolean = false, block: () -> Unit) {
            val b = if (primary) {
                UiKit.primaryButton(this, label, block)
            } else {
                UiKit.secondaryButton(this, label, block)
            }
            b.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(8) }
            col.addView(b)
        }
        nav("兑换主题") { startActivity(Intent(this, RedeemActivity::class.java)) }
        nav("升级到 Lyra", primary = true) {
            startActivity(Intent(this, UpgradeActivity::class.java))
        }
        nav("规格说明") { startActivity(Intent(this, SpecActivity::class.java)) }
        nav("私人定制") { startActivity(Intent(this, CustomActivity::class.java)) }
        nav("开源仓库") {
            runCatching {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/anpplex/StarLive")))
            }.onFailure {
                Toast.makeText(this, "无法打开浏览器", Toast.LENGTH_SHORT).show()
            }
        }
        nav("通知使用权（播歌让出）") {
            BatteryHelper.openNotificationListenerSettings(this)
        }
        nav("电池优化 / 自启动") {
            BatteryHelper.openBatterySettings(this)
            Toast.makeText(
                this,
                if (BatteryHelper.isIgnoringOptimizations(this)) {
                    "已忽略电池优化 · 仍建议在车机「自启动」中允许星澜"
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
