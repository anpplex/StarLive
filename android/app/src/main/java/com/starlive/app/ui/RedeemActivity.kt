package com.starlive.app.ui

import android.os.Bundle
import android.util.TypedValue
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.starlive.app.BuildConfig
import com.starlive.app.R
import com.starlive.app.StarLiveApp
import com.starlive.app.device.DeviceIdentity
import com.starlive.app.redeem.RedeemClient
import com.starlive.app.redeem.ThemePackInstaller
import com.starlive.app.ui.UiTokens.applyRoundedBg
import com.starlive.app.ui.UiTokens.dp
import java.io.File
import kotlin.concurrent.thread

class RedeemActivity : AppCompatActivity() {
    private lateinit var status: TextView
    private lateinit var applyBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(24))
            setBackgroundColor(UiTokens.bg)
        }

        col.addView(UiKit.title(this, "兑换主题"))
        col.addView(
            UiKit.caption(
                this,
                "一码一设备，同设备可重复兑换下载；需要联网",
            ).also { it.setPadding(0, dp(8), 0, dp(14)) },
        )

        val card = UiKit.card(this)
        val input = EditText(this).apply {
            hint = "输入兑换码"
            setTextColor(UiTokens.textPrimary)
            setHintTextColor(UiTokens.textMuted)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            applyRoundedBg(UiTokens.surface2, 10f, UiTokens.stroke)
            setPadding(dp(14), dp(14), dp(14), dp(14))
            setSingleLine()
            minHeight = dp(52)
        }
        card.addView(input)
        status = TextView(this).apply {
            setTextColor(UiTokens.textSecondary)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(0, dp(12), 0, dp(4))
            setLineSpacing(0f, 1.2f)
            text = "兑换成功后可立即应用到星环"
        }
        card.addView(status)
        col.addView(card)

        val btn = UiKit.primaryButton(this, "兑换并安装") {}.also {
            it.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(14) }
        }
        col.addView(btn)
        applyBtn = UiKit.secondaryButton(this, getString(R.string.btn_apply)) {
            applyToCluster()
        }.also {
            it.isEnabled = false
            it.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(8) }
        }
        col.addView(applyBtn)
        col.addView(
            UiKit.ghostButton(this, "关闭") { finish() }.also {
                it.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { topMargin = dp(10) }
            },
        )

        btn.setOnClickListener {
            val code = input.text?.toString().orEmpty().trim()
            if (code.isBlank()) {
                status.setTextColor(UiTokens.warning)
                status.text = "请输入兑换码"
                return@setOnClickListener
            }
            btn.isEnabled = false
            status.setTextColor(UiTokens.info)
            status.text = "兑换中…"
            val deviceId = DeviceIdentity.deviceId(this)
            val base = BuildConfig.REDEEM_API_BASE
            thread {
                val ex = RedeemClient.exchange(base, code, deviceId)
                ex.onFailure { e ->
                    runOnUiThread {
                        btn.isEnabled = true
                        status.setTextColor(UiTokens.danger)
                        status.text = e.message ?: "兑换失败"
                        Toast.makeText(this, status.text, Toast.LENGTH_LONG).show()
                    }
                    return@thread
                }
                val ok = ex.getOrThrow()
                runOnUiThread {
                    status.setTextColor(UiTokens.info)
                    status.text = if (ok.alreadyBound) {
                        "本机已兑换过「${ok.title}」，重新下载中…"
                    } else {
                        "已授权「${ok.title}」，下载中…"
                    }
                }
                val dest = File(cacheDir, "pack_${ok.packId}.zip")
                val dl = RedeemClient.downloadTo(ok.downloadUrl, dest, ok.sha256)
                dl.onFailure { e ->
                    runOnUiThread {
                        btn.isEnabled = true
                        status.setTextColor(UiTokens.danger)
                        status.text = e.message ?: "下载失败"
                        Toast.makeText(this, status.text, Toast.LENGTH_LONG).show()
                    }
                    return@thread
                }
                val install = ThemePackInstaller.installZip(this@RedeemActivity, dest)
                runOnUiThread {
                    btn.isEnabled = true
                    install.onSuccess { r ->
                        status.setTextColor(UiTokens.success)
                        val bound = if (ok.alreadyBound) "（本机已绑定）" else ""
                        status.text = "已安装「${r.title}」（${r.count} 张）$bound。可点下方应用到星环"
                        applyBtn.isEnabled = true
                        Toast.makeText(this, "兑换成功", Toast.LENGTH_SHORT).show()
                    }.onFailure { e ->
                        status.setTextColor(UiTokens.danger)
                        status.text = e.message ?: "安装失败"
                        applyBtn.isEnabled = false
                        Toast.makeText(this, status.text, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        setContentView(
            ScrollView(this).apply {
                setBackgroundColor(UiTokens.bg)
                addView(col)
            },
        )
    }

    private fun applyToCluster() {
        val orch = (application as StarLiveApp).orchestrator
        if (orch.isEffectivelyPlaying()) {
            orch.applyCurrent("redeem-apply-deferred")
            Toast.makeText(this, getString(R.string.toast_apply_deferred), Toast.LENGTH_SHORT).show()
            return
        }
        val ok = orch.applyCurrent("redeem-apply")
        Toast.makeText(
            this,
            if (ok) {
                getString(R.string.toast_applied)
            } else {
                "已保存到图库。若未显示，请再次点「应用到星环」"
            },
            Toast.LENGTH_SHORT,
        ).show()
    }
}
