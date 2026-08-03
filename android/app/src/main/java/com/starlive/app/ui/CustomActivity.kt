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
import com.starlive.app.contact.ContactConfig

class CustomActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dens = resources.displayMetrics.density
        fun dp(v: Int) = (v * dens).toInt()
        val wechat = ContactConfig.wechatId(this)

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(24))
            setBackgroundColor(Color.parseColor("#0B0E12"))
        }
        fun title(t: String) = TextView(this).apply {
            text = t
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
        }
        fun body(t: String) = TextView(this).apply {
            text = t
            setTextColor(Color.parseColor("#C5D0E0"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setPadding(0, dp(10), 0, dp(8))
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

        col.addView(title("私人定制"))
        col.addView(
            body(
                "支持专属星环壁纸；成品仅你使用，不进公开列表。\n\n" +
                    "【你会得到】\n" +
                    "· 深色 + 浅色各一张（2990×284）\n" +
                    "· 适配左侧软边与表盘安全区\n" +
                    "· 导入说明一份\n\n" +
                    "【价格】\n" +
                    "标准 ¥39 · 高级 ¥99（含 1 次修改）\n\n" +
                    "定制图同时适用于星澜与 Lyra。",
            ),
        )
        col.addView(
            btn("复制微信号", primary = true) {
                if (ContactConfig.isPlaceholder(wechat)) {
                    Toast.makeText(this, "请先在 assets/contact/wechat.txt 填写微信号", Toast.LENGTH_LONG).show()
                    return@btn
                }
                copy(wechat)
                Toast.makeText(this, "已复制微信号", Toast.LENGTH_SHORT).show()
            },
        )
        col.addView(
            btn("复制规格发给设计师") {
                copy(SPEC_TEMPLATE)
                Toast.makeText(this, "已复制规格模板", Toast.LENGTH_SHORT).show()
            },
        )
        col.addView(
            TextView(this).apply {
                text = "当前配置微信：$wechat"
                setTextColor(Color.parseColor("#6B7A90"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setPadding(0, dp(12), 0, 0)
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

    private fun copy(text: String) {
        val cm = getSystemService(ClipboardManager::class.java)
        cm.setPrimaryClip(ClipData.newPlainText("starlive", text))
    }

    companion object {
        private val SPEC_TEMPLATE =
            """
            【星澜定制】
            车型：阿维塔
            规格：2990×284（星环壁纸带）+ 深浅双套
            表盘保留：左侧约 1042px 不放关键内容
            需求：
            素材：
            """.trimIndent()
    }
}
