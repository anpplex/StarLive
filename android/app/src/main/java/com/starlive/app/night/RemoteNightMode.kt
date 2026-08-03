package com.starlive.app.night

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import java.util.Calendar

/**
 * Vehicle / system day-night for the **remote star-ring strip** (Lyra-aligned).
 *
 * Captured on Avatr HU (设置 → 显示 → 显示模式), user 12:
 * | UI label | secure `ui_night_mode` | config.uiMode night |
 * |----------|------------------------|---------------------|
 * | 浅色     | **1**                  | NO                  |
 * | 深色     | **2**                  | YES                 |
 * | 自适应   | **9** (OEM)            | follows lamp        |
 *
 * OEM remotescreen paints from the **effective** theme night bit. Prefer secure
 * hard 浅色/深色 first, then Configuration, then soft clock window (22:00–06:00).
 *
 * Read-only for StarLive (does not force system display mode).
 */
class RemoteNightMode(context: Context) {
    private val app = context.applicationContext

    /** 0 day … 1 night. */
    fun nightFraction(): Float {
        val secure = readUiNightModeSecure()
        when (secure) {
            MODE_LIGHT -> return DAY
            MODE_DARK -> return NIGHT
        }
        return adaptiveEffectiveNightFraction()
    }

    fun isNightish(): Boolean = nightFraction() >= 0.45f

    /**
     * Effective paint for 自动. Order: Configuration night bit → clock schedule.
     */
    fun adaptiveEffectiveNightFraction(): Float {
        configNightFraction()?.let { return it }
        return clockNightFraction()
    }

    fun debugSnapshot(): String {
        val secure = readUiNightModeSecure()
        val cfg = app.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val cfgLabel = when (cfg) {
            Configuration.UI_MODE_NIGHT_YES -> "YES"
            Configuration.UI_MODE_NIGHT_NO -> "NO"
            else -> "UNDEF"
        }
        val modeLabel = when (secure) {
            MODE_LIGHT -> "light(1)"
            MODE_DARK -> "dark(2)"
            MODE_ADAPTIVE -> "adaptive(9)"
            MODE_AOSP_AUTO -> "aosp_auto(0)"
            else -> "other($secure)"
        }
        val uiMgr = readUiModeManagerNight()
        return "mode=$modeLabel cfg=$cfgLabel uimgr=$uiMgr " +
            "policy=${nightFraction()} adaptive=${adaptiveEffectiveNightFraction()}"
    }

    fun effectiveLabelZh(): String =
        if (isNightish()) "深色" else "浅色"

    fun registerUiNightModeObserver(
        handler: Handler = Handler(Looper.getMainLooper()),
        onChange: () -> Unit,
    ) {
        val cr = app.contentResolver
        val uri = Settings.Secure.getUriFor(KEY_UI_NIGHT_MODE)
        runCatching {
            cr.registerContentObserver(
                uri,
                false,
                object : ContentObserver(handler) {
                    override fun onChange(selfChange: Boolean, uri: Uri?) {
                        Log.i(TAG, "ui_night_mode changed → ${debugSnapshot()}")
                        onChange()
                    }
                },
            )
        }.onFailure { Log.w(TAG, "register ui_night_mode observer failed", it) }
    }

    private fun readUiNightModeSecure(): Int =
        runCatching {
            Settings.Secure.getInt(app.contentResolver, KEY_UI_NIGHT_MODE, -1)
        }.getOrDefault(-1).let { v ->
            if (v >= 0) return@let v
            runCatching {
                Settings.System.getInt(app.contentResolver, KEY_UI_NIGHT_MODE, -1)
            }.getOrDefault(-1)
        }

    private fun readUiModeManagerNight(): Int =
        runCatching {
            app.getSystemService(UiModeManager::class.java)?.nightMode ?: -1
        }.getOrDefault(-1)

    private fun configNightFraction(): Float? =
        when (app.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> NIGHT
            Configuration.UI_MODE_NIGHT_NO -> DAY
            else -> null
        }

    /** Avatr custom auto window: 22:00 → 06:00 (same as Lyra dumpsys capture). */
    private fun clockNightFraction(): Float {
        val cal = Calendar.getInstance()
        val mins = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val night = mins >= 22 * 60 || mins < 6 * 60
        return if (night) NIGHT else DAY
    }

    companion object {
        private const val TAG = "StarLive"
        private const val KEY_UI_NIGHT_MODE = "ui_night_mode"

        const val MODE_LIGHT = 1
        const val MODE_DARK = 2
        const val MODE_AOSP_AUTO = 0
        /** Huawei / Avatr adaptive (显示模式·自适应). */
        const val MODE_ADAPTIVE = 9

        private const val DAY = 0f
        private const val NIGHT = 1f
    }
}
