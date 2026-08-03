package com.starlive.app.ui

import android.graphics.Color
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
import com.starlive.app.StarLiveApp
import com.starlive.app.device.DeviceIdentity
import com.starlive.app.redeem.RedeemClient
import com.starlive.app.redeem.ThemePackInstaller
import java.io.File
import kotlin.concurrent.thread

class RedeemActivity : AppCompatActivity() {
    private lateinit var status: TextView
    private lateinit var applyBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dens = resources.displayMetrics.density
        fun dp(v: Int) = (v * dens).toInt()

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(24))
            setBackgroundColor(Color.parseColor("#0B0E12"))
        }
        col.addView(
            TextView(this).apply {
                text = "兑换主题"
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            },
        )
        col.addView(
            TextView(this).apply {
                text =
                    "输入兑换码解锁官方主题壁纸（默认 1 张）。一码一设备；同设备可重复兑换取下载。\n" +
                    "服务：${BuildConfig.REDEEM_API_BASE}"
                setTextColor(Color.parseColor("#9AABB8"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setPadding(0, dp(10), 0, dp(12))
            },
        )
        val input = EditText(this).apply {
            hint = "兑换码"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#6B7A90"))
            setBackgroundColor(Color.parseColor("#1A2430"))
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setSingleLine()
        }
        col.addView(input)
        status = TextView(this).apply {
            setTextColor(Color.parseColor("#C5D0E0"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(0, dp(12), 0, dp(8))
        }
        col.addView(status)
        val btn = Button(this).apply {
            text = "兑换并安装"
            isAllCaps = false
            setBackgroundColor(Color.parseColor("#3D6FE0"))
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48),
            )
        }
        col.addView(btn)
        applyBtn = Button(this).apply {
            text = "应用上屏"
            isAllCaps = false
            isEnabled = false
            setBackgroundColor(Color.parseColor("#2A6B4A"))
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48),
            ).apply { topMargin = dp(8) }
            setOnClickListener { applyToCluster() }
        }
        col.addView(applyBtn)
        col.addView(
            Button(this).apply {
                text = "关闭"
                isAllCaps = false
                setBackgroundColor(Color.parseColor("#243040"))
                setTextColor(Color.WHITE)
                setOnClickListener { finish() }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(44),
                ).apply { topMargin = dp(8) }
            },
        )
        setContentView(
            ScrollView(this).apply {
                setBackgroundColor(Color.parseColor("#0B0E12"))
                addView(col)
            },
        )

        btn.setOnClickListener {
            val code = input.text?.toString()?.trim().orEmpty()
            if (code.length < 4) {
                Toast.makeText(this, "请输入兑换码", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            btn.isEnabled = false
            applyBtn.isEnabled = false
            status.text = "兑换中…"
            val deviceId = DeviceIdentity.deviceId(this)
            val base = BuildConfig.REDEEM_API_BASE
            thread {
                val ex = RedeemClient.exchange(base, code, deviceId)
                ex.onFailure { e ->
                    runOnUiThread {
                        btn.isEnabled = true
                        status.text = e.message ?: "兑换失败"
                        Toast.makeText(this, status.text, Toast.LENGTH_LONG).show()
                    }
                    return@thread
                }
                val ok = ex.getOrThrow()
                runOnUiThread {
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
                        status.text = e.message ?: "下载失败"
                        Toast.makeText(this, status.text, Toast.LENGTH_LONG).show()
                    }
                    return@thread
                }
                val install = ThemePackInstaller.installZip(this, dest)
                runOnUiThread {
                    btn.isEnabled = true
                    install.onSuccess { r ->
                        status.text = "已安装「${r.title}」共 ${r.count} 张 · 可点下方应用上屏"
                        applyBtn.isEnabled = true
                        Toast.makeText(this, status.text, Toast.LENGTH_LONG).show()
                    }.onFailure { e ->
                        status.text = e.message ?: "安装失败"
                        Toast.makeText(this, status.text, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun applyToCluster() {
        val orch = (application as StarLiveApp).orchestrator
        if (orch.isEffectivelyPlaying()) {
            orch.applyCurrent("redeem-apply-deferred")
            status.text = "播歌中 · 停播后生效"
            Toast.makeText(this, status.text, Toast.LENGTH_SHORT).show()
            return
        }
        val ok = orch.applyCurrent("redeem-apply")
        status.text = if (ok) "已应用到星环" else "无法上屏 · 请确认星环 Display 可用"
        Toast.makeText(this, status.text, Toast.LENGTH_SHORT).show()
    }
}
