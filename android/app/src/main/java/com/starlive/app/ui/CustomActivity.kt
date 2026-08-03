package com.starlive.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.starlive.app.contact.ContactConfig
import com.starlive.app.ui.UiTokens.dp

class CustomActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val wechat = ContactConfig.wechatId(this)
        val douyin = ContactConfig.douyinId(this)

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(24))
            setBackgroundColor(UiTokens.bg)
        }
        col.addView(UiKit.title(this, "壁纸定制"))
        col.addView(
            UiKit.caption(this, "专属星环壁纸，不进入公开主题列表")
                .also { it.setPadding(0, dp(6), 0, dp(12)) },
        )

        val card = UiKit.card(this)
        card.addView(
            UiKit.body(
                this,
                "交付内容\n" +
                    "· 深色与浅色各一张（2990×284，浅色可选）\n" +
                    "· 适配左侧软边与表盘安全区\n" +
                    "· 兑换码 1 个，或直发图片与导入说明\n\n" +
                    "参考价格\n" +
                    "标准 ¥39 · 高级 ¥99（含 1 次修改）\n\n" +
                    "定制图同时适用于星澜与 Lyra。\n\n" +
                    "联系方式\n" +
                    "· 微信 $wechat\n" +
                    "· 抖音 $douyin（@${ContactConfig.douyinHandle(this)}）",
            ),
        )
        col.addView(card)

        fun full(label: String, primary: Boolean = false, block: () -> Unit) {
            val b = if (primary) UiKit.primaryButton(this, label, block)
            else UiKit.secondaryButton(this, label, block)
            b.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(8) }
            col.addView(b)
        }
        full("复制微信号 $wechat", primary = true) {
            copy(wechat)
            Toast.makeText(this, "已复制微信号 $wechat", Toast.LENGTH_SHORT).show()
        }
        full("复制抖音号 $douyin") {
            copy(douyin)
            Toast.makeText(this, "已复制抖音号 $douyin", Toast.LENGTH_SHORT).show()
        }
        full("复制规格说明") {
            copy(SPEC_TEMPLATE)
            Toast.makeText(this, "已复制规格说明", Toast.LENGTH_SHORT).show()
        }
        full("关于作者") {
            startActivity(android.content.Intent(this, AuthorActivity::class.java))
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

    private fun copy(text: String) {
        val cm = getSystemService(ClipboardManager::class.java)
        cm.setPrimaryClip(ClipData.newPlainText("starlive", text))
    }

    companion object {
        private val SPEC_TEMPLATE =
            """
            【星澜壁纸定制】
            车型：阿维塔
            规格：2990×284（星环壁纸带），深色/浅色双套
            表盘区：左侧约 1042px 不放置关键内容
            微信：anpple
            抖音：anpplex
            需求：
            素材：
            """.trimIndent()
    }
}
